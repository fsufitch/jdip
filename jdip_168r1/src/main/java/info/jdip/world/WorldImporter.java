/*
 *    jDip - a Java Diplomacy Adjudicator and Mapper
 *    Copyright (C) 2018 Uwe Plonus and the jDip development team
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package info.jdip.world;

import info.jdip.JDipException;
import info.jdip.gui.DefaultGUIGameSetup;
import info.jdip.order.Orderable;
import info.jdip.order.result.Result;
import info.jdip.world.Province.Adjacency;
import info.jdip.world.metadata.GameMetadata;
import info.jdip.world.metadata.PlayerMetadata;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.zip.GZIPInputStream;
import javax.xml.stream.events.Attribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is not thread safe.
 */
public class WorldImporter {

    private static final Logger logger = LoggerFactory.getLogger(WorldImporter.class);

    private boolean isRecognizedFile = false;

    private ObjectInformation world = null;

    private Deque<ObjectInformation> objectStack;

    private Deque<XMLEvent> eventStack;

    private Map<String, ObjectInformation> objectLookup;

    private final Map<UUID, Power> powers = new LinkedHashMap();

    private final Map<UUID, Province> provinces = new LinkedHashMap<>();

    private final Map<UUID, Orderable> orders = new LinkedHashMap<>();

    public WorldImporter() {
        objectStack = new LinkedList<>();
        eventStack = new LinkedList<>();
        objectLookup = new HashMap<>();
    }

    public World importGame(File file) throws IOException, XMLStreamException, JDipException {
        GZIPInputStream gis = new GZIPInputStream(new BufferedInputStream(new FileInputStream(file), 8192));
        XMLInputFactory inputFactory = XMLInputFactory.newFactory();
        XMLEventReader eventReader = inputFactory.createXMLEventReader(gis);
        boolean isDone = false;
        while (eventReader.hasNext() && !isDone) {
            XMLEvent event = eventReader.nextTag();
            if (event.isStartElement()) {
                eventStack.addFirst(event);
                StartElement startElement = event.asStartElement();
                if (isRecognizedFile) {
                    String elementName = startElement.getName().getLocalPart();
                    switch (elementName) {
                        case "object":
                            handleObject(startElement);
                            break;
                        case "reference":
                            handleReference(startElement);
                            break;
                        case "collection":
                        case "array":
                            handleCollection(startElement);
                            break;
                        case "primitive":
                        case "string":
                            handlePrimitive(startElement);
                            break;
                        case "null":
                            handleNull(startElement);
                            break;
                        case "declaredClass":
                        case "default":
                            // We know this elements but they do not have informations for us
                            break;
                        default:
                            // TODO add logging here
                            throw new XMLStreamException(new StringBuilder("Unknown Element \"").append(elementName)
                                    .append("\"").toString());
                    }
                } else if ("jsx".equals(startElement.getName().getLocalPart())) {
                    isRecognizedFile = true;
                } else {
                    throw new IOException("Old saved game file is in unexpected format.");
                }
            } else if (event.isEndElement()) {
                eventStack.removeFirst();
                EndElement endElement = event.asEndElement();
                switch (endElement.getName().getLocalPart()) {
                    case "object":
                    case "array":
                    case "collection":
                        objectStack.removeFirst();
                        break;
                    default:
                        break;
                }
                isDone = eventStack.isEmpty();
            }
        }

        return extractWorld(world).orElseThrow(() -> new JDipException("Cannot find a world to import"));
    }

    private void handleObject(StartElement startElement) throws XMLStreamException, JDipException {
        String className = startElement.getAttributeByName(new QName("class")).getValue();
        ObjectInformation objectInformation;
        if ("java.util.TreeMap".equals(className) || "java.util.HashMap".equals(className)) {
            objectInformation = new MapInformation(className);
        } else {
            objectInformation = new ObjectInformation(className);
        }
        if ("dip.world.World".equals(className)) {
            world = objectInformation;
        }
        Attribute fieldAttr = startElement.getAttributeByName(new QName("field"));
        if (!objectStack.isEmpty()) {
            ObjectInformation enclosingObject = objectStack.peekFirst();
            if (fieldAttr != null) {
                enclosingObject.addAttribute(fieldAttr.getValue(), objectInformation);
            } else if (enclosingObject.isMap()) {
                MapInformation map = (MapInformation)enclosingObject;
                map.addValue(objectInformation);
            } else if (enclosingObject.isCollection()) {
                CollectionInformation collection = (CollectionInformation)enclosingObject;
                collection.addValue(objectInformation);
            } else {
                throw new JDipException("Object is neither field nor value of collection or map...");
            }
        }
        objectStack.addFirst(objectInformation);
        objectLookup.put(startElement.getAttributeByName(new QName("id")).getValue(),
                objectInformation);
    }

    private void handleReference(StartElement startElement) throws XMLStreamException, JDipException {
        String idref = startElement.getAttributeByName(new QName("idref")).getValue();
        ObjectInformation objectInformation = objectLookup.get(idref);
        Attribute fieldAttr = startElement.getAttributeByName(new QName("field"));
        if (!objectStack.isEmpty()) {
            ObjectInformation enclosingObject = objectStack.peekFirst();
            if (fieldAttr != null) {
                enclosingObject.addAttribute(fieldAttr.getValue(), objectInformation);
            } else if (enclosingObject.isMap()) {
                MapInformation map = (MapInformation)enclosingObject;
                map.addValue(objectInformation);
            } else if (enclosingObject.isCollection()) {
                CollectionInformation collection = (CollectionInformation)enclosingObject;
                collection.addValue(objectInformation);
            } else {
                throw new JDipException("Reference is neither field nor value of collection or map...");
            }
        }
    }

    private void handleCollection(StartElement startElement) throws XMLStreamException, JDipException {
        String className;
        if ("array".equals(startElement.getName().getLocalPart())) {
            className = startElement.getAttributeByName(new QName("base")).getValue();
        } else {
            className = startElement.getAttributeByName(new QName("class")).getValue();
        }
        ObjectInformation objectInformation = new CollectionInformation(className);
        Attribute fieldAttr = startElement.getAttributeByName(new QName("field"));
        if (!objectStack.isEmpty()) {
            ObjectInformation enclosingObject = objectStack.peekFirst();
            if (fieldAttr != null) {
                enclosingObject.addAttribute(fieldAttr.getValue(), objectInformation);
            } else if (enclosingObject.isMap()) {
                MapInformation map = (MapInformation)enclosingObject;
                map.addValue(objectInformation);
            } else if (enclosingObject.isCollection()) {
                CollectionInformation collection = (CollectionInformation)enclosingObject;
                collection.addValue(objectInformation);
            } else {
                throw new JDipException("Collection is neither field nor value of collection or map...");
            }
        }
        objectStack.addFirst(objectInformation);
        objectLookup.put(startElement.getAttributeByName(new QName("id")).getValue(),
                objectInformation);
    }

    private void handlePrimitive(StartElement startElement) throws XMLStreamException, JDipException {
        String elementName = startElement.getName().getLocalPart();
        String typeName;
        if ("string".equals(elementName)) {
            typeName = "string";
        } else {
            typeName = startElement.getAttributeByName(new QName("type")).getValue();
        }
        PrimitiveInformation primitiveInformation = new PrimitiveInformation(typeName);
        Attribute fieldAttr = startElement.getAttributeByName(new QName("field"));
        if (!objectStack.isEmpty()) {
            ObjectInformation enclosingObject = objectStack.peekFirst();
            if (fieldAttr != null) {
                enclosingObject.addAttribute(fieldAttr.getValue(), primitiveInformation);
            } else if (enclosingObject.isMap()) {
                MapInformation map = (MapInformation)enclosingObject;
                map.addValue(primitiveInformation);
            } else if (enclosingObject.isCollection()) {
                CollectionInformation collection = (CollectionInformation)enclosingObject;
                collection.addValue(primitiveInformation);
            } else {
                throw new JDipException("Primitive is neither field nor value of collection or map...");
            }
        }
        primitiveInformation.setValue(startElement.getAttributeByName(new QName("value")).getValue());
    }

    private void handleNull(StartElement startElement) throws XMLStreamException {
        PrimitiveInformation objectInformation = new PrimitiveInformation("null");
        Attribute fieldAttr = startElement.getAttributeByName(new QName("field"));
        if (!objectStack.isEmpty() && fieldAttr != null) {
            ObjectInformation enclosingObject = objectStack.peekFirst();
            enclosingObject.addAttribute(fieldAttr.getValue(), objectInformation);
        }
    }

    private Optional<World> extractWorld(SerializeInformation worldSerInfo) throws JDipException {
        if (worldSerInfo == null || !worldSerInfo.isObject() ||
                !"dip.world.World".equals(worldSerInfo.getClassName())) {
            return Optional.empty();
        }
        ObjectInformation worldInfo = (ObjectInformation)worldSerInfo;
        info.jdip.world.Map map = extractMap(worldInfo.getAttribute("map")).orElseThrow(
                () -> new JDipException("Expected a world to contain a map"));
        World resultWorld = new World(map);

        NonTurnDataProxy nonTurnData = extractNonTurnData(worldInfo.getAttribute("nonTurnData"))
                .orElseThrow(() -> new JDipException("Found unexpected element in non turn data"));
        nonTurnData.transferNonTurnData(resultWorld);

        extractTurnStates(resultWorld);

        return Optional.of(resultWorld);
    }

    private Optional<info.jdip.world.Map> extractMap(SerializeInformation mapSerInfo) throws JDipException {
        if (mapSerInfo == null || !mapSerInfo.isObject() || !"dip.world.Map".equals(mapSerInfo.getClassName())) {
            return Optional.empty();
        }
        ObjectInformation mapInfo = (ObjectInformation)mapSerInfo;

        return Optional.of(new info.jdip.world.Map(
                extractPowers(mapInfo.getAttribute("powers"))
                    .orElseThrow(() -> new JDipException("Expected the map to contain an array of powers")),
                extractProvinces(mapInfo.getAttribute("provinces"))
                    .orElseThrow(() -> new JDipException("Expected the map to contain an array of provinces"))
        ));
    }

    private Optional<Power[]> extractPowers(SerializeInformation powersSerInfo) throws JDipException {
        if (powersSerInfo == null || !powersSerInfo.isCollection() ||
                !"dip.world.Power".equals(powersSerInfo.getClassName())) {
            return Optional.empty();
        }
        CollectionInformation powersInfo = (CollectionInformation)powersSerInfo;
        List<Power> powersList = new LinkedList<>();
        for (SerializeInformation powerSerInfo: powersInfo.getCollectionEntries()) {
            Optional<Power> power = extractPower(powerSerInfo);
            if (power.isPresent()) {
                powersList.add(power.get());
            }
        }
        return Optional.of(powersList.toArray(new Power[powersList.size()]));
    }

    private Optional<Power> extractPower(SerializeInformation powerSerInfo) throws JDipException {
        if (powerSerInfo == null || !powerSerInfo.isObject() ||
                !"dip.world.Power".equals(powerSerInfo.getClassName())) {
            return Optional.empty();
        }
        Power power = powers.get(powerSerInfo.getUuid());
        if (power == null) {
            ObjectInformation powerInfo = (ObjectInformation)powerSerInfo;

            power = new Power(extractStringArray(powerInfo.getAttribute("names")),
                    extractString(powerInfo.getAttribute("adjective"))
                        .orElseThrow(() -> new JDipException("The adjective of a power may not be null")),
                    extractBoolean(powerInfo.getAttribute("isActive")));
            powers.put(powerInfo.getUuid(), power);
        }
        return Optional.of(power);
    }

    private Optional<Province[]> extractProvinces(SerializeInformation provincesSerInfo) throws JDipException {
        if (provincesSerInfo == null || !provincesSerInfo.isCollection() ||
                !"dip.world.Province".equals(provincesSerInfo.getClassName())) {
            return Optional.empty();
        }
        CollectionInformation provincesInfo = (CollectionInformation)provincesSerInfo;
        List<Province> provincesList = new LinkedList<>();
        for (SerializeInformation provinceSerInfo: provincesInfo.getCollectionEntries()) {
            Optional<Province> province = extractProvince(provinceSerInfo);
            if (province.isPresent()) {
                provincesList.add(province.get());
            }
        }
        return Optional.of(provincesList.toArray(new Province[provincesList.size()]));
    }

    private Optional<Province> extractProvince(SerializeInformation provinceSerInfo) throws JDipException {
        if (provinceSerInfo == null || !provinceSerInfo.isObject() ||
                !"dip.world.Province".equals(provinceSerInfo.getClassName())) {
            return Optional.empty();
        }
        Province province = provinces.get(provinceSerInfo.getUuid());
        if (province == null) {
            ObjectInformation provinceInfo = (ObjectInformation)provinceSerInfo;
            province = new Province(extractString(provinceInfo.getAttribute("fullName"))
                        .orElseThrow(() -> new JDipException("The fullName of a province may not be null")),
                    extractStringArray(provinceInfo.getAttribute("shortNames")),
                    extractInt(provinceInfo.getAttribute("index"))
                            .orElseThrow(() -> new JDipException("Expected the province to contain an index")),
                    extractBoolean(provinceInfo.getAttribute("isConvoyableCoast")));
            province.setSupplyCenter(extractBoolean(provinceInfo.getAttribute("supplyCenter")));
            // TODO extractBorders
            provinces.put(provinceInfo.getUuid(), province);

            extractAdjacency(provinceInfo.getAttribute("adjacency"))
                        .orElseThrow(() -> new JDipException("Expected a province to contain adjacency information"))
                    .transferToAdjacency(province.getAdjacency());
        }
        return Optional.of(province);
    }

    private Optional<AdjacencyProxy> extractAdjacency(SerializeInformation adjacencySerInfo) throws JDipException {
        if (adjacencySerInfo == null || !adjacencySerInfo.isObject() ||
                !"dip.world.Province$Adjacency".equals(adjacencySerInfo.getClassName())) {
            return Optional.empty();
        }
        ObjectInformation adjacencyInfo = (ObjectInformation)adjacencySerInfo;
        return extractAdjacencyMap(adjacencyInfo.getAttribute("adjLoc"));
    }

    private Optional<AdjacencyProxy> extractAdjacencyMap(SerializeInformation adjacencyMapSerInfo)
            throws JDipException {
        if (adjacencyMapSerInfo == null || !adjacencyMapSerInfo.isMap() ||
                !"java.util.HashMap".equals(adjacencyMapSerInfo.getClassName())) {
            return Optional.empty();
        }
        MapInformation adjacencyMapInfo = (MapInformation)adjacencyMapSerInfo;
        AdjacencyProxy adjacency = new AdjacencyProxy();
        for (Map.Entry<SerializeInformation, SerializeInformation> adjacencyMapEntrySerInfo:
                adjacencyMapInfo.getMap().entrySet()) {
            adjacency.addAdjacency(extractCoast(adjacencyMapEntrySerInfo.getKey())
                        .orElseThrow(() -> new JDipException("Expected a known coast in adjacency")),
                    extractLocations(adjacencyMapEntrySerInfo.getValue())
                        .orElseThrow(() -> new JDipException("Expected alocation array in adjacency"))
            );
        }
        return Optional.of(adjacency);
    }

    private Optional<Coast> extractCoast(SerializeInformation coastSerInfo) throws JDipException {
        if (coastSerInfo == null || !coastSerInfo.isObject() ||
                !"dip.world.Coast".equals(coastSerInfo.getClassName())) {
            return Optional.empty();
        }
        ObjectInformation coastInfo = (ObjectInformation)coastSerInfo;
        return Optional.ofNullable(Coast.getCoast(extractInt(coastInfo.getAttribute("index"))
                .orElseThrow(() -> new JDipException("Expected the coast to contain an index"))));
    }

    private Optional<Location[]> extractLocations(SerializeInformation locationsSerInfo) throws JDipException {
        if (locationsSerInfo == null || !locationsSerInfo.isCollection() ||
                !"dip.world.Location".equals(locationsSerInfo.getClassName())) {
            return Optional.empty();
        }
        CollectionInformation locationsInfo = (CollectionInformation)locationsSerInfo;
        List<Location> locations = new LinkedList<>();
        for (SerializeInformation locationSerInfo: locationsInfo.getCollectionEntries()) {
            Optional<Location> location = extractLocation(locationSerInfo);
            if (location.isPresent()) {
                locations.add(location.get());
            }
        }
        return Optional.of(locations.toArray(new Location[locations.size()]));
    }

    private Optional<Location> extractLocation(SerializeInformation locationSerInfo) throws JDipException {
        if (locationSerInfo == null || !locationSerInfo.isObject()||
                !"dip.world.Location".equals(locationSerInfo.getClassName())) {
            return Optional.empty();
        }
        ObjectInformation locationInfo = (ObjectInformation)locationSerInfo;
        return Optional.of(new Location(
                extractProvince(locationInfo.getAttribute("province"))
                    .orElseThrow(() -> new JDipException("Expected a location to contain a province")),
                extractCoast(locationInfo.getAttribute("coast"))
                    .orElseThrow(() -> new JDipException("Expected a location to contain a coast"))
        ));
    }

    private Optional<NonTurnDataProxy> extractNonTurnData(SerializeInformation nonTurnDataSerInfo)
            throws JDipException {
        if (nonTurnDataSerInfo == null || !nonTurnDataSerInfo.isMap() ||
                !"java.util.HashMap".equals(nonTurnDataSerInfo.getClassName())) {
            return Optional.empty();
        }
        MapInformation nonTurnDataInfo = (MapInformation)nonTurnDataSerInfo;

        NonTurnDataProxy nonTurnDataProxy = new NonTurnDataProxy();
        for (Map.Entry<SerializeInformation, SerializeInformation> nonTurnData: nonTurnDataInfo.getMap().entrySet()) {
            SerializeInformation keySerInfo = nonTurnData.getKey();
            if (keySerInfo.isObject()) {
                if ("dip.world.Power".equals(keySerInfo.getClassName())) {
                    Power power = extractPower(keySerInfo).orElseThrow(
                            () -> new JDipException("Expected the non turn data map to contain a power as key"));
                    PlayerMetadata playerMetadata = extractPlayerMetadata(nonTurnData.getValue())
                            .orElseThrow(() -> new JDipException(
                                    "Expected the non turn data map to contain player metadata as value"));
                    nonTurnDataProxy.addPlayerMetadata(power, playerMetadata);
                } else {
                    return Optional.empty();
                }
            } else if (keySerInfo.isPrimitive()) {
                if ("string".equals(keySerInfo.getClassName())) {
                    PrimitiveInformation keyInfo = (PrimitiveInformation)keySerInfo;
                    switch (keyInfo.getValue()) {
                        case "_variant_info_":
                            nonTurnDataProxy.setVariantInfo(extractVariantInfo(nonTurnData.getValue())
                                    .orElseThrow(() -> new JDipException(
                                            "Expected the non turn data to contain a variant info")));
                            break;
                        case "_undo_redo_manager_":
                            // Ignored
                            break;
                        case "_victory_conditions_":
                            nonTurnDataProxy.setVictoryConditions(extractVictoryConditions(nonTurnData.getValue())
                                    .orElseThrow(() -> new JDipException(
                                            "Expected the non turn data to contain victory conditions")));
                            break;
                        case "_world_metadata_":
                            nonTurnDataProxy.setGameMetadata(extractGameMetadata(nonTurnData.getValue())
                                    .orElseThrow(() -> new JDipException(
                                            "Expected the non turn data to contain game metadata")));
                            break;
                        case "_game_setup_":
                            nonTurnDataProxy.setGameSetup(extractGameSetup(nonTurnData.getValue())
                                    .orElseThrow(() -> new JDipException(
                                            "Expected the non turn data to contain a game setup")));
                            break;
                        default:
                            logger.warn(new StringBuilder("Unknown entry in nonTurnData found: \"")
                                    .append(keyInfo.getValue()).append("\"").toString());
                            break;
                    }
                }
            }
        }
        return Optional.of(nonTurnDataProxy);
    }

    private Optional<PlayerMetadata> extractPlayerMetadata(SerializeInformation playerMetadataSerInfo)
            throws JDipException {
        if (playerMetadataSerInfo == null || !playerMetadataSerInfo.isObject()||
                !"dip.world.metadata.PlayerMetadata".equals(playerMetadataSerInfo.getClassName())) {
            return Optional.empty();
        }
        ObjectInformation playerMetadataInfo = (ObjectInformation)playerMetadataSerInfo;

        PlayerMetadata playerMetadata = new PlayerMetadata();
        playerMetadata.setName(extractString(playerMetadataInfo.getAttribute("name"))
                .orElseThrow(() -> new JDipException("The name of a PlayerMetadata may not be null"))
        );
        playerMetadata.setEmailAddresses(extractStringArray(playerMetadataInfo.getAttribute("email")));
        playerMetadata.setURI(extractURI(playerMetadataInfo.getAttribute("uri")).orElse(null));
        playerMetadata.setNotes(extractString(playerMetadataInfo.getAttribute("notes"))
                .orElseThrow(() -> new JDipException("The notes of a PlayerMetadata may not be null")));
        return Optional.of(playerMetadata);
    }

    private Optional<World.VariantInfo> extractVariantInfo(SerializeInformation variantInfoSerInfo)
            throws JDipException {
        if (variantInfoSerInfo == null || !variantInfoSerInfo.isObject() ||
                !"dip.world.World$VariantInfo".equals(variantInfoSerInfo.getClassName())) {
            return Optional.empty();
        }
        ObjectInformation variantInfoInfo = (ObjectInformation)variantInfoSerInfo;

        World.VariantInfo variantInfo = new World.VariantInfo();
        variantInfo.setVariantName(extractString(variantInfoInfo.getAttribute("variantName"))
                .orElseThrow(() -> new JDipException("Expected the variant info to contain a variantName")));
        variantInfo.setMapName(extractString(variantInfoInfo.getAttribute("mapName"))
                .orElseThrow(() -> new JDipException("Expected the variantInfo to contain a mapName")));
        variantInfo.setSymbolPackName(extractString(variantInfoInfo.getAttribute("symbolsName"))
                .orElseThrow(() -> new JDipException("Expected the variantInfo to contain a symbolsName")));
        variantInfo.setVariantVersion(extractFloat(variantInfoInfo.getAttribute("variantVersion"))
                .orElseThrow(() -> new JDipException("Expected the variantInfo to contain a variantVersion")));
        variantInfo.setSymbolPackVersion(extractFloat(variantInfoInfo.getAttribute("symbolsVersion"))
                .orElseThrow(() -> new JDipException("Expected the variant info to contain a symbolPackVersion")));
        variantInfo.setRuleOptions(extractRuleOptions(variantInfoInfo.getAttribute("ruleOptions"))
                .orElseThrow(() -> new JDipException("Expected the variant info to contain ruleOptions")));

        return Optional.of(variantInfo);
    }

    private Optional<RuleOptions> extractRuleOptions(SerializeInformation ruleOptionsSerInfo) throws JDipException {
        if (ruleOptionsSerInfo == null || !ruleOptionsSerInfo.isObject() ||
                !"dip.world.RuleOptions".equals(ruleOptionsSerInfo.getClassName())) {
            return Optional.empty();
        }
        ObjectInformation ruleOptionsInfo = (ObjectInformation)ruleOptionsSerInfo;
        return extractOptionMap(ruleOptionsInfo.getAttribute("optionMap"));
    }

    private Optional<RuleOptions> extractOptionMap(SerializeInformation optionMapSerInfo) throws JDipException {
        if (optionMapSerInfo == null || !optionMapSerInfo.isMap()||
                !"java.util.HashMap".equals(optionMapSerInfo.getClassName())) {
            return Optional.empty();
        }
        MapInformation optionMapInfo = (MapInformation)optionMapSerInfo;

        RuleOptions ruleOptions = new RuleOptions();
        for (Map.Entry<SerializeInformation, SerializeInformation> optionSerInfo: optionMapInfo.getMap().entrySet()) {
            RuleOptions.Option option = extractOption(optionSerInfo.getKey())
                    .orElseThrow(() -> new JDipException("Expected the option map to contain an option as key"));
            RuleOptions.OptionValue optionValue = extractOptionValue(optionSerInfo.getValue()).orElseThrow(
                    () -> new JDipException("Expected the option map to contain an option value as value"));
            ruleOptions.setOption(option, optionValue);
        }
        return Optional.of(ruleOptions);
    }

    private Optional<RuleOptions.Option> extractOption(SerializeInformation optionSerInfo) throws JDipException {
        if (optionSerInfo == null || !optionSerInfo.isObject() ||
                !"dip.world.RuleOptions$Option".equals(optionSerInfo.getClassName())) {
            return Optional.empty();
        }
        ObjectInformation optionInfo = (ObjectInformation)optionSerInfo;

        String optionName = extractString(optionInfo.getAttribute("name"))
                .orElseThrow(() -> new JDipException("Expected an option to contain a name"));
        RuleOptions.OptionValue[] allowed = extractOptionValueArray(optionInfo.getAttribute("allowed"))
                .orElseThrow(() -> new JDipException("Expected an option to contain an array with allowed values"));
        RuleOptions.OptionValue defaultValue = extractOptionValue(optionInfo.getAttribute("defaultValue"))
                .orElseThrow(() -> new JDipException("Expected an option to contain a defaultValue"));
        return Optional.of(new RuleOptions.Option(optionName, defaultValue, allowed));
    }

    private Optional<RuleOptions.OptionValue[]> extractOptionValueArray(SerializeInformation optionValuesSerInfo)
            throws JDipException {
        if (optionValuesSerInfo == null || !optionValuesSerInfo.isCollection() ||
                !"dip.world.RuleOptions$OptionValue".equals(optionValuesSerInfo.getClassName())) {
            return Optional.empty();
        }
        CollectionInformation optionValuesInfo = (CollectionInformation)optionValuesSerInfo;

        List<RuleOptions.OptionValue> optionValues = new LinkedList<>();
        for (SerializeInformation optionValueSerInfo: optionValuesInfo.getCollectionEntries()) {
            Optional<RuleOptions.OptionValue> optionValue = extractOptionValue(optionValueSerInfo);
            if (optionValue.isPresent()) {
                optionValues.add(optionValue.get());
            }
        }
        return Optional.of(optionValues.toArray(new RuleOptions.OptionValue[optionValues.size()]));
    }

    private Optional<RuleOptions.OptionValue> extractOptionValue(SerializeInformation optionValueSerInfo)
            throws JDipException {
        if (optionValueSerInfo == null || !optionValueSerInfo.isObject() ||
                !"dip.world.RuleOptions$OptionValue".equals(optionValueSerInfo.getClassName())) {
            return Optional.empty();
        }
        ObjectInformation optionValueInfo = (ObjectInformation)optionValueSerInfo;

        return Optional.of(new RuleOptions.OptionValue(extractString(optionValueInfo.getAttribute("name"))
                .orElseThrow(() -> new JDipException("Expected an option value to contain a name"))));
    }

    private Optional<VictoryConditions> extractVictoryConditions(SerializeInformation victoryConditionsSerInfo)
            throws JDipException {
        if (victoryConditionsSerInfo == null || !victoryConditionsSerInfo.isObject() ||
                !"dip.world.VictoryConditions".equals(victoryConditionsSerInfo.getClassName())) {
            return Optional.empty();
        }
        ObjectInformation victoryConditionsInfo = (ObjectInformation)victoryConditionsSerInfo;

        int numSCForVictory = extractInt(victoryConditionsInfo.getAttribute("numSCForVictory"))
                .orElseThrow(() -> new JDipException("Expected the victory conditions to contain numSCForVictory"));
        int maxYearsNoSCChange = extractInt(victoryConditionsInfo.getAttribute("maxYearsNoSCChange"))
                .orElseThrow(() -> new JDipException("Expected the victory conditions to contain maxYearsNoSCChange"));
        int maxGameTimeYears = extractInt(victoryConditionsInfo.getAttribute("maxGameTimeYears"))
                .orElseThrow(() -> new JDipException("Expected the victory conditions to contain maxGameTimeYears"));
        int initialYear = extractInt(victoryConditionsInfo.getAttribute("initialYear"))
                .orElseThrow(() -> new JDipException("Expected the victory conditions to contain initialYear"));

        return Optional.of(new VictoryConditions(numSCForVictory, maxYearsNoSCChange, maxGameTimeYears,
                new Phase(Phase.SeasonType.SPRING, initialYear, Phase.PhaseType.MOVEMENT)));
    }

    private Optional<GameMetadata> extractGameMetadata(SerializeInformation gameMetadataSerInfo) throws JDipException {
        if (gameMetadataSerInfo == null || !gameMetadataSerInfo.isObject() ||
                !"dip.world.metadata.GameMetadata".equals(gameMetadataSerInfo.getClassName())) {
            return Optional.empty();
        }
        ObjectInformation gameMetadataInfo = (ObjectInformation)gameMetadataSerInfo;

        GameMetadata gameMetadata = new GameMetadata();
        gameMetadata.setComment(extractString(gameMetadataInfo.getAttribute("comment"))
                .orElseThrow(() -> new JDipException("Expected the game metadata to contain a comment")));
        gameMetadata.setGameName(extractString(gameMetadataInfo.getAttribute("gameName"))
                .orElseThrow(() -> new JDipException("Expected the game metadata to contain a gameName")));
        gameMetadata.setModeratorName(extractString(gameMetadataInfo.getAttribute("moderator")).orElse(null));
        gameMetadata.setModeratorEmail(extractString(gameMetadataInfo.getAttribute("moderatorEmail")).orElse(null));
        gameMetadata.setModeratorURI(extractURI(gameMetadataInfo.getAttribute("moderatorURI")).orElse(null));
        gameMetadata.setJudgeName(extractString(gameMetadataInfo.getAttribute("judgeName")).orElse(null));
        gameMetadata.setGameURI(extractURI(gameMetadataInfo.getAttribute("gameURI")).orElse(null));
        gameMetadata.setNotes(extractString(gameMetadataInfo.getAttribute("notes"))
                .orElseThrow(() -> new JDipException("Expected the game metadata to contain notes")));
        gameMetadata.setGameID(extractString(gameMetadataInfo.getAttribute("id"))
                .orElseThrow(() -> new JDipException("Expected the game metadata to contain a game ID")));
        return Optional.of(gameMetadata);
    }

    private Optional<GameSetup> extractGameSetup(SerializeInformation gameSetupSerInfo) throws JDipException {
        if (gameSetupSerInfo == null || !gameSetupSerInfo.isObject()) {
            return Optional.empty();
        }
        return Optional.of(new DefaultGUIGameSetup());
    }

    private Optional<Map<Phase, TurnState>> extractSyncTurnStates(SerializeInformation turnStateSerInfo)
            throws JDipException {
        if (turnStateSerInfo == null || !turnStateSerInfo.isObject() ||
                !"java.util.Collections$SynchronizedSortedMap".equals(turnStateSerInfo.getClassName())) {
            return Optional.empty();
        }
        ObjectInformation turnStateInfo = (ObjectInformation)turnStateSerInfo;
        return extractTurnStates(turnStateInfo.getAttribute("m"));
    }

    private Optional<Map<Phase, TurnState>> extractTurnStates(SerializeInformation turnStateSerInfo)
            throws JDipException {
        if (turnStateSerInfo == null || !turnStateSerInfo.isMap()) {
            return Optional.empty();
        }
        MapInformation turnStateInfo = (MapInformation)turnStateSerInfo;
        return Optional.empty();
    }

    private void extractTurnStates(World resultWorld) throws JDipException {
        SerializeInformation turnStatesSyncMapSerInfo = world.getAttribute("turnStates");
        if (turnStatesSyncMapSerInfo == null || !turnStatesSyncMapSerInfo.isObject()) {
            throw new JDipException("Expected the world to have a synchronized map of turnStates");
        }
        ObjectInformation turnStatesSyncMapInfo = (ObjectInformation)turnStatesSyncMapSerInfo;

        SerializeInformation turnStatesMapSerInfo = turnStatesSyncMapInfo.getAttribute("m");
        if (turnStatesMapSerInfo == null || !turnStatesMapSerInfo.isMap()) {
            throw new JDipException("Expected the synchronized map to contain a map");
        }
        MapInformation turnStatesMapInfo = (MapInformation)turnStatesMapSerInfo;

        for (Map.Entry<SerializeInformation, SerializeInformation> turnStateEntry:
                turnStatesMapInfo.getMap().entrySet()) {
            TurnState turnState = new TurnState(extractPhase(turnStateEntry.getKey()));

            SerializeInformation turnStateSerInfo = turnStateEntry.getValue();
            if (!turnStateSerInfo.isObject() || !"dip.world.TurnState".equals(turnStateSerInfo.getClassName())) {
                throw new JDipException("Expected a turn state to be an object of type dip.world.TurnState");
            }
            ObjectInformation turnStateInfo = (ObjectInformation)turnStateSerInfo;

            Map<Power, List<Orderable>> orderMap = extractOrderMap(turnStateInfo.getAttribute("orderMap"));
            turnState.setResultList(extractResultList(turnStateInfo.getAttribute("resultList")));
            // TODO orderMap

            turnState.setSCOwnerChanged(extractBooleanAttribute(turnStateInfo, "isSCOwnerChanged"));

            // TODO position

            turnState.setEnded(extractBooleanAttribute(turnStateInfo, "isEnded"));
            turnState.setResolved(extractBooleanAttribute(turnStateInfo, "isResolved"));
        }
    }

    private Phase extractPhase(SerializeInformation phaseSerInfo) throws JDipException {
        if (phaseSerInfo == null || !phaseSerInfo.isObject() ||
                !"dip.world.Phase".equals(phaseSerInfo.getClassName())) {
            throw new JDipException("Expected a phase to be an object of type dip.world.Phase");
        }
        ObjectInformation phaseInfo = (ObjectInformation)phaseSerInfo;

        SerializeInformation seasonTypeSerInfo = phaseInfo.getAttribute("seasonType");
        if (seasonTypeSerInfo == null || !seasonTypeSerInfo.isObject() ||
                !"dip.world.Phase$SeasonType".equals(seasonTypeSerInfo.getClassName())) {
            throw new JDipException("Expected a phase to contain an object of type dip.world.Phase$SeasonType");
        }
        ObjectInformation seasonTypeInfo = (ObjectInformation)seasonTypeSerInfo;

        int seasonTypePosition = extractIntAttribute(seasonTypeInfo, "position");
        Phase.SeasonType seasonType;
        if (seasonTypePosition == 1000) {
            seasonType = Phase.SeasonType.SPRING;
        } else {
            seasonType = Phase.SeasonType.FALL;
        }

        SerializeInformation yearTypeSerInfo = phaseInfo.getAttribute("yearType");
        if (yearTypeSerInfo == null || !yearTypeSerInfo.isObject() ||
                !"dip.world.Phase$YearType".equals(yearTypeSerInfo.getClassName())) {
            throw new JDipException("Expected a phase to contain an object of type dip.world.Phase$YearType");
        }
        ObjectInformation yearTypeInfo = (ObjectInformation)yearTypeSerInfo;

        int yearTypeYear = extractIntAttribute(yearTypeInfo, "year");
        Phase.YearType yearType = new Phase.YearType(yearTypeYear);

        SerializeInformation phaseTypeSerInfo = phaseInfo.getAttribute("phaseType");
        if (phaseTypeSerInfo == null || !phaseTypeSerInfo.isObject() ||
                !"dip.world.Phase$PhaseType".equals(phaseTypeSerInfo.getClassName())) {
            throw new JDipException("Expected a phase to contain an object of type dip.world.Phase$PhaseType");
        }
        ObjectInformation phaseTypeInfo = (ObjectInformation)phaseTypeSerInfo;

        Phase.PhaseType phaseType = Phase.PhaseType.parse(extractStringAttribute(phaseTypeInfo, "constName"));

        return new Phase(seasonType, yearType, phaseType);
    }

    private Map<Power, List<Orderable>> extractOrderMap(SerializeInformation orderMapSerInfo)
            throws JDipException {
        if (orderMapSerInfo == null || !orderMapSerInfo.isMap() ||
                !"java.util.HashMap".equals(orderMapSerInfo.getClassName())) {
            throw new JDipException("Expected the order map to be an object of type java.util.HashMap");
        }
        ObjectInformation orderMapInfo = (ObjectInformation)orderMapSerInfo;
        return null;
    }

    private List<Result> extractResultList(SerializeInformation resultListSerInfo)
            throws JDipException {
        if (resultListSerInfo == null || !resultListSerInfo.isCollection() ||
                !"java.util.ArrayList".equals(resultListSerInfo.getClassName())) {
            throw new JDipException("Expected the result list to be a collection of type java.util.ArrayList");
        }
        CollectionInformation resultListInfo = (CollectionInformation)resultListSerInfo;

        List<Result> results = new LinkedList<>();
        for (SerializeInformation resultListEntrySerInfo: resultListInfo.getCollectionEntries()) {
            if (!resultListEntrySerInfo.isObject()) {
                throw new JDipException("Expected the content of a result list to be an object");
            }
            ObjectInformation resultListEntryInfo = (ObjectInformation)resultListEntrySerInfo;

//            results.add(extractResult(resultListEntryInfo, powers));
        }

        return results;
    }

//    private Result extractResult(ObjectInformation resultInfo, Map<UUID, Power> powers) throws JDipException {
//        Result result;
//        Power power;
//        SerializeInformation powerSerInfo = resultInfo.getAttribute("power");
//        if (powerSerInfo.isPrimitive() && "null".equals(powerSerInfo.getClassName())) {
//            power = null;
//        } else {
//            power = powers.get(powerSerInfo.getUuid());
//        }
//        switch (resultInfo.getClassName()) {
//            case "dip.order.result.Result":
//                result = new Result(power, extractStringAttribute(resultInfo, "message"));
//                break;
//            case "dip.order.result.TimeResult":
//                break;
//            case "dip.order.result.OrderResult":
//                break;
//            case "dip.order.result.BouncedResult":
//                break;
//            case "dip.order.result.ConvoyPathResult":
//                break;
//            case "dip.order.result.DependentMoveFailedResult":
//                break;
//            case "dip.order.result.DislodgedResult":
//                break;
//            case "dip.order.result.SubstitutedResult":
//                break;
//            default:
//                throw new JDipException("The class of the result list entry is unknown");
//        }
//        return result;
//    }

    private String[] extractStringArray(SerializeInformation stringArraySerInfo) throws JDipException {
        if (stringArraySerInfo == null || !stringArraySerInfo.isCollection() ||
                !"java.lang.String".equals(stringArraySerInfo.getClassName())) {
            throw new JDipException("Expected the string array to be a collection of type java.lang.String");
        }
        CollectionInformation stringArrayInfo = (CollectionInformation)stringArraySerInfo;

        List<String> stringArray = new LinkedList<>();
        for (SerializeInformation stringSerInfo: stringArrayInfo.getCollectionEntries()) {
            stringArray.add(extractString(stringSerInfo)
                    .orElseThrow(() -> new JDipException("Expected the content of a string array not to be null")));
        }
        return stringArray.toArray(new String[stringArray.size()]);
    }

    private Optional<String> extractString(SerializeInformation stringSerInfo) throws JDipException {
        if (stringSerInfo == null || !stringSerInfo.isPrimitive()) {
            throw new JDipException("The argument to extractString must be a primitive");
        }
        if ("null".equals(stringSerInfo.getClassName())) {
            return Optional.empty();
        } else {
            PrimitiveInformation stringInfo = (PrimitiveInformation)stringSerInfo;
            return Optional.of(stringInfo.getValue());
        }
    }

    /**
     * Extract a string from the given object with the given attribute name. May return null if the string is null.
     *
     * @param object the object containing the string
     * @param attributeName the name of the string attribute
     * @return the string or null
     * @throws JDipException if an error occurs
     */
    private String extractStringAttribute(ObjectInformation object, String attributeName) throws JDipException {
        SerializeInformation attributeSerInfo = object.getAttribute(attributeName);
        if (attributeSerInfo == null || !attributeSerInfo.isPrimitive() ||
                (!"string".equals(attributeSerInfo.getClassName()) &&
                    !"null".equals(attributeSerInfo.getClassName()))) {
            throw new JDipException(new StringBuilder("Expected the ").append(object.getClassName())
                    .append(" to contain a string attribute with the name ").append(attributeName).toString());
        }
        PrimitiveInformation attributeInfo = (PrimitiveInformation)attributeSerInfo;
        return attributeInfo.getValue();
    }

    private Optional<Float> extractFloat(SerializeInformation floatSerInfo) throws JDipException {
        if (floatSerInfo == null || !floatSerInfo.isPrimitive()) {
            throw new JDipException("The argument to extractFloat must be a primitive");
        }
        if ("null".equals(floatSerInfo.getClassName())) {
            return Optional.empty();
        } else {
            PrimitiveInformation floatInfo = (PrimitiveInformation)floatSerInfo;
            return Optional.of(Float.valueOf(floatInfo.getValue()));
        }
    }

    private Optional<Integer> extractInt(SerializeInformation intSerInfo) throws JDipException {
        if (intSerInfo == null || !intSerInfo.isPrimitive() || ! "int".equals(intSerInfo.getClassName())) {
            Optional.empty();
        }
        return Optional.of(Integer.valueOf(((PrimitiveInformation)intSerInfo).getValue()));
    }

    private int extractIntAttribute(ObjectInformation object, String attributeName) throws JDipException {
        SerializeInformation attributeSerInfo = object.getAttribute(attributeName);
        if (attributeSerInfo == null || !attributeSerInfo.isPrimitive() ||
                !"int".equals(attributeSerInfo.getClassName())) {
            throw new JDipException(new StringBuilder("Expected the ").append(object.getClassName())
                    .append(" to contain an int attribute with the name ").append(attributeName).toString());
        }
        PrimitiveInformation attributeInfo = (PrimitiveInformation)attributeSerInfo;
        return Integer.parseInt(attributeInfo.getValue());
    }

    private boolean extractBoolean(SerializeInformation booleanSerInfo) throws JDipException {
        if (booleanSerInfo == null || !booleanSerInfo.isPrimitive() ||
                ! "boolean".equals(booleanSerInfo.getClassName())) {
            throw new JDipException("Expected a boolean to be a primitive");
        }
        return Boolean.parseBoolean(((PrimitiveInformation)booleanSerInfo).getValue());
    }

    private boolean extractBooleanAttribute(ObjectInformation object, String attributeName) throws JDipException {
        SerializeInformation attributeSerInfo = object.getAttribute(attributeName);
        if (attributeSerInfo == null || !attributeSerInfo.isPrimitive() ||
                !"boolean".equals(attributeSerInfo.getClassName())) {
            throw new JDipException(new StringBuilder("Expected the ").append(object.getClassName())
                    .append(" to contain a boolean attribute with the name ").append(attributeName).toString());
        }
        PrimitiveInformation attributeInfo = (PrimitiveInformation)attributeSerInfo;
        return Boolean.parseBoolean(attributeInfo.getValue());
    }

    private Optional<URI> extractURI(SerializeInformation uriSerInfo) throws JDipException {
        if (uriSerInfo == null) {
            throw new JDipException("The argument to extractURI may not be null");
        }
        if (uriSerInfo.isPrimitive() && "null".equals(uriSerInfo.getClassName())) {
            return Optional.empty();
        }
        if (!uriSerInfo.isObject() || !"java.net.URI".equals(uriSerInfo.getClassName())) {
            throw new JDipException("Expected an URI to be an object of class java.net.URI");
        }
        URI result;
        ObjectInformation uriInfo = (ObjectInformation)uriSerInfo;
        SerializeInformation innerUriSerInfo = uriInfo.getAttribute("string");
        if (innerUriSerInfo == null || !innerUriSerInfo.isPrimitive() ||
                !"string".equals(innerUriSerInfo.getClassName())) {
            // if the class java.net.URI is changed then we ignore it and set the uri to null
            result = null;
        } else {
            try {
                result = new URI(((PrimitiveInformation)innerUriSerInfo).getValue());
            } catch (URISyntaxException usex) {
                result = null;
            }
        }
        return Optional.ofNullable(result);
    }


    private static abstract class SerializeInformation {

        private final UUID uuid = UUID.randomUUID();

        private final String className;

        public SerializeInformation(String className) {
            this.className = className;
        }

        public UUID getUuid() {
            return uuid;
        }

        public String getClassName() {
            return className;
        }

        public boolean isPrimitive() {
            return false;
        }

        public boolean isObject() {
            return false;
        }

        public boolean isMap() {
            return false;
        }

        public boolean isCollection() {
            return false;
        }

        @Override
        public String toString() {
            return new StringBuilder(getClass().getSimpleName()).append("{className='").append(className)
                    .append("\'}").toString();
        }

    }


    private static class PrimitiveInformation extends SerializeInformation {

        private String value;

        public PrimitiveInformation(String className) {
            super(className);
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        @Override
        public boolean isPrimitive() {
            return true;
        }

    }


    private static class ObjectInformation extends SerializeInformation {

        private final Map<String, SerializeInformation> attributes;

        public ObjectInformation(String className) {
            super(className);
            this.attributes = new LinkedHashMap<>();
        }

        public void addAttribute(String name, SerializeInformation value) {
            this.attributes.put(name, value);
        }

        public SerializeInformation getAttribute(String name) {
            return attributes.get(name);
        }

        @Override
        public boolean isObject() {
            return true;
        }

    }


    private static class MapInformation extends ObjectInformation {

        Map<SerializeInformation, SerializeInformation> encapsulatedMap;

        private SerializeInformation key;

        /** This contains the number of int values that can be ignored (meta information of the map). */
        private int ignorableMetaData;

        public MapInformation(String className) throws JDipException {
            super(className);
            this.encapsulatedMap = new LinkedHashMap<>();
            key = null;
            if ("java.util.HashMap".equals(className)) {
                ignorableMetaData = 2;
            } else if ("java.util.TreeMap".equals(className)) {
                ignorableMetaData = 1;
            } else {
                throw new JDipException(new StringBuilder("Unknown map class \"").append(className)
                        .append("\"").toString());
            }
        }

        @Override
        public boolean isObject() {
            return false;
        }

        @Override
        public boolean isMap() {
            return true;
        }

        public void addValue(SerializeInformation value) {
            if (ignorableMetaData > 0) {
                ignorableMetaData--;
            } else {
                if (key == null) {
                    key = value;
                } else {
                    this.encapsulatedMap.put(key, value);
                    this.key = null;
                }
            }
        }

        public Map<SerializeInformation, SerializeInformation> getMap() {
            if (key != null) {
                throw new IllegalStateException("Found key without value");
            }
            return Collections.unmodifiableMap(encapsulatedMap);
        }

    }


    private static class CollectionInformation extends ObjectInformation {

        List<SerializeInformation> encapsulatedList;

        public CollectionInformation(String className) {
            super(className);
            this.encapsulatedList = new LinkedList<>();
        }

        @Override
        public boolean isObject() {
            return false;
        }

        @Override
        public boolean isCollection() {
            return true;
        }

        public boolean addValue(SerializeInformation value) {
            return encapsulatedList.add(value);
        }

        public List<SerializeInformation> getCollectionEntries() {
            return Collections.unmodifiableList(encapsulatedList);
        }

    }


    private static class AdjacencyProxy {

        private final Map<Coast, Location[]> adjacencyLocations = new LinkedHashMap<>();

        public void addAdjacency(Coast coast, Location[] locations) {
            adjacencyLocations.put(coast, locations);
        }

        public void transferToAdjacency(Adjacency adjacency) {
            adjacencyLocations.entrySet().forEach((adjacencyLocation) -> {
                adjacency.setLocations(adjacencyLocation.getKey(), adjacencyLocation.getValue());
            });
        }

    }


    private static class NonTurnDataProxy {

        /** The player metadata per power. */
        private Map<Power, PlayerMetadata> playerMetadata;

        /** The victory conditions. */
        private VictoryConditions victoryConditions;

        /** The game metadata. */
        private GameMetadata gameMetadata;

        /** The game setup. */
        private GameSetup gameSetup;

        /** The variant info. */
        private World.VariantInfo variantInfo;

        public NonTurnDataProxy() {
            playerMetadata = new LinkedHashMap<>();
        }

        public void addPlayerMetadata(Power power, PlayerMetadata playerMetadata) {
            this.playerMetadata.put(power, playerMetadata);
        }

        public void setVictoryConditions(VictoryConditions victoryConditions) {
            this.victoryConditions = victoryConditions;
        }

        public void setGameMetadata(GameMetadata gameMetadata) {
            this.gameMetadata = gameMetadata;
        }

        public void setGameSetup(GameSetup gameSetup) {
            this.gameSetup = gameSetup;
        }

        public void setVariantInfo(World.VariantInfo variantInfo) {
            this.variantInfo = variantInfo;
        }

        public void transferNonTurnData(World world) {
            for (Map.Entry<Power, PlayerMetadata> playerData: playerMetadata.entrySet()) {
                world.setPlayerMetadata(playerData.getKey(), playerData.getValue());
            }
            world.setVictoryConditions(victoryConditions);
            world.setGameMetadata(gameMetadata);
            world.setGameSetup(gameSetup);
            world.setVariantInfo(variantInfo);
        }

    }

}
