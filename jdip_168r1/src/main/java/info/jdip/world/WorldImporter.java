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
import java.util.UUID;
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

        return createWorld();
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

    private World createWorld() throws JDipException {
        SerializeInformation mapSerInfo = world.getAttribute("map");
        if (mapSerInfo == null || !mapSerInfo.isObject() || !"dip.world.Map".equals(mapSerInfo.getClassName())) {
            throw new JDipException("Expected the world to have a map of type dip.world.Map");
        }
        ObjectInformation mapInfo = (ObjectInformation)mapSerInfo;

        SerializeInformation powersSerInfo = mapInfo.getAttribute("powers");
        if (powersSerInfo == null || !powersSerInfo.isCollection() ||
                !"dip.world.Power".equals(powersSerInfo.getClassName())) {
            throw new JDipException("Expected the map to have powers of type dip.world.Power");
        }
        CollectionInformation powersInfo = (CollectionInformation)powersSerInfo;
        Map<UUID, Power> powers = extractPowers(powersInfo);

        SerializeInformation provincesSerInfo = mapInfo.getAttribute("provinces");
        if (provincesSerInfo == null || !provincesSerInfo.isCollection() ||
                !"dip.world.Province".equals(provincesSerInfo.getClassName())) {
            throw new JDipException("Expected the map to have provinces of type dip.world.Province");
        }
        CollectionInformation provincesInfo = (CollectionInformation)provincesSerInfo;
        Map<UUID, Province> provinces = extractProvinces(provincesInfo);

        info.jdip.world.Map map = new info.jdip.world.Map(powers.values().toArray(new Power[powers.size()]),
                provinces.values().toArray(new Province[provinces.size()]));

        World resultWorld = new World(map);

        handleNonTurnData(resultWorld, powers);

        return resultWorld;
    }

    /**
     * Extract the powers out of a collection information containing only powers.
     *
     * @param powersInfo the CollectionInformation containing all powers.
     * @return a map with UUIDs (for lookup) and the powers extracted. The returned Map retains the order information
     *  given in the collection.
     * @throws JDipException if there are unexpected informations in the given CollectionInformation.
     */
    private Map<UUID, Power> extractPowers(CollectionInformation powersInfo) throws JDipException {
        Map<UUID, Power> powers = new LinkedHashMap<>();
        for (SerializeInformation powerSerInfo: powersInfo.getCollectionEntries()) {
            if (!powerSerInfo.isObject() || !"dip.world.Power".equals(powerSerInfo.getClassName())) {
                throw new JDipException("Expected the collection of powers to be of type dip.world.Power");
            }
            ObjectInformation powerInfo = (ObjectInformation)powerSerInfo;

            SerializeInformation namesSerInfo = powerInfo.getAttribute("names");
            if (namesSerInfo == null || !namesSerInfo.isCollection() ||
                    !"java.lang.String".equals(namesSerInfo.getClassName())) {
                throw new JDipException(
                        "Expected a power to contain a collection of names of the type java.lang.String");
            }

            List<String> names = new LinkedList<>();
            CollectionInformation namesInfo = (CollectionInformation)namesSerInfo;
            for (SerializeInformation nameSerInfo: namesInfo.getCollectionEntries()) {
                if (!nameSerInfo.isPrimitive() || !"string".equals(nameSerInfo.getClassName())) {
                    throw new JDipException("Expected the content of the names collection to be a string");
                }
                PrimitiveInformation nameInfo = (PrimitiveInformation)nameSerInfo;
                names.add(nameInfo.getValue());
            }

            boolean isActive;
            SerializeInformation activeSerInfo = powerInfo.getAttribute("isActive");
            if (activeSerInfo == null || !activeSerInfo.isPrimitive() ||
                    !"boolean".equals(activeSerInfo.getClassName())) {
                throw new JDipException("Expected a power to contain a boolean isActive");
            }
            PrimitiveInformation activeInfo = (PrimitiveInformation)activeSerInfo;
            isActive = Boolean.parseBoolean(activeInfo.getValue());

            powers.put(powerInfo.getUuid(), new Power(names.toArray(new String[names.size()]),
                    extractStringAttribute(powerInfo, "adjective"), isActive));
        }

        return powers;
    }

    /**
     * Extract the powers out of a collection information containing only provinces.
     *
     * @param provincesInfo the CollectionInformation containing all provinces.
     * @return a map with UUIDs (for lookup) and the provinces extracted. The returned Map retains the order information
     *  given in the collection.
     * @throws JDipException if there are unexpected informations in the given CollectionInformation.
     */
    private Map<UUID, Province> extractProvinces(CollectionInformation provincesInfo) throws JDipException {
        Map<UUID, Province> provinces = new LinkedHashMap<>();
        // this map collects the raw adjacency informations from the saved games file to handle it in a second step
        // the UUID is the same as in the provinces map
        Map<UUID, ObjectInformation> adjacencyInfos = new HashMap<>();
        for (SerializeInformation provinceSerInfo: provincesInfo.getCollectionEntries()) {
            if (!provinceSerInfo.isObject() || !"dip.world.Province".equals(provinceSerInfo.getClassName())) {
                throw new JDipException("Expected the collection of provinces to be of type dip.world.Province");
            }
            ObjectInformation provinceInfo = (ObjectInformation)provinceSerInfo;

            SerializeInformation shortNamesSerInfo = provinceInfo.getAttribute("shortNames");
            if (shortNamesSerInfo == null || !shortNamesSerInfo.isCollection() ||
                    !"java.lang.String".equals(shortNamesSerInfo.getClassName())) {
                throw new JDipException(
                        "Expected a province to contain a collection of shortNames of the type java.lang.String");
            }

            List<String> shortNames = new LinkedList<>();
            CollectionInformation shortNamesInfo = (CollectionInformation)shortNamesSerInfo;
            for (SerializeInformation shortNameSerInfo: shortNamesInfo.getCollectionEntries()) {
                if (!shortNameSerInfo.isPrimitive() || !"string".equals(shortNameSerInfo.getClassName())) {
                    throw new JDipException("Expected the content of the shortNames collection to be a string");
                }
                PrimitiveInformation shortNameInfo = (PrimitiveInformation)shortNameSerInfo;
                shortNames.add(shortNameInfo.getValue());
            }

            Province province = new Province(extractStringAttribute(provinceInfo, "fullName"),
                    shortNames.toArray(new String[shortNames.size()]),
                    extractIntAttribute(provinceInfo, "index"),
                    extractBooleanAttribute(provinceInfo, "isConvoyableCoast"));
            province.setSupplyCenter(extractBooleanAttribute(provinceInfo, "supplyCenter"));
            provinces.put(provinceInfo.getUuid(), province);

            SerializeInformation adjacencySerInfo = provinceInfo.getAttribute("adjacency");
            if (adjacencySerInfo == null || !adjacencySerInfo.isObject() ||
                    !"dip.world.Province$Adjacency".equals(adjacencySerInfo.getClassName())) {
                throw new JDipException(
                        "Expected a province to contain adjacency of type dip.world.Province$Adjacency");
            }
            adjacencyInfos.put(provinceInfo.getUuid(), (ObjectInformation)adjacencySerInfo);
        }

        assignProvincesToAdjacencies(provinces, adjacencyInfos);

        return provinces;
    }

    private void assignProvincesToAdjacencies(Map<UUID, Province> provincesMap,
            Map<UUID, ObjectInformation> adjacencyInfos) throws JDipException {
        for (Map.Entry<UUID, Province> provinceEntry: provincesMap.entrySet()) {
            ObjectInformation adjacencyInfo = adjacencyInfos.get(provinceEntry.getKey());
            SerializeInformation adjLocSerInfo = adjacencyInfo.getAttribute("adjLoc");
            if (adjLocSerInfo == null || !adjLocSerInfo.isMap()) {
                throw new JDipException("Expected a adjLoc to be a map");
            }
            MapInformation adjLocInfo = (MapInformation)adjLocSerInfo;

            for (Map.Entry<SerializeInformation, SerializeInformation> adjLocEntry: adjLocInfo.getMap().entrySet()) {
                SerializeInformation adjLocKeySer = adjLocEntry.getKey();
                if (adjLocKeySer == null || !adjLocKeySer.isObject() ||
                        !"dip.world.Coast".equals(adjLocKeySer.getClassName())) {
                    throw new JDipException("Expected the key of the adjLoc entry to be a dip.world.Coast");
                }
                ObjectInformation adjLocKey = (ObjectInformation)adjLocKeySer;
                SerializeInformation adjLocValueSer = adjLocEntry.getValue();
                if (adjLocValueSer == null || !adjLocValueSer.isCollection() ||
                        !"dip.world.Location".equals(adjLocValueSer.getClassName())) {
                    throw new JDipException(
                            "Expected the value of the adjLoc entry to be a collection of dip.world.Location");
                }
                CollectionInformation adjLocLocations = (CollectionInformation)adjLocValueSer;

                Coast coast = extractCoast(adjLocKey);

                List<Location> locations = new LinkedList<>();
                for (SerializeInformation locationSerInfo: adjLocLocations.getCollectionEntries()) {
                    if (!locationSerInfo.isObject() || !"dip.world.Location".equals(locationSerInfo.getClassName())) {
                        throw new JDipException(
                                "Expected the entries of the collection adjLoc to be of dip.world.Location");
                    }
                    ObjectInformation locationInfo = (ObjectInformation)locationSerInfo;

                    SerializeInformation provinceSerInfo = locationInfo.getAttribute("province");
                    if (provinceSerInfo == null || !provinceSerInfo.isObject() ||
                            !"dip.world.Province".equals(provinceSerInfo.getClassName())) {
                        throw new JDipException("Expected the province in the location to be of dip.world.Province");
                    }
                    locations.add(new Location(provincesMap.get(provinceSerInfo.getUuid()), coast));
                }
                provinceEntry.getValue().getAdjacency().setLocations(coast,
                        locations.toArray(new Location[locations.size()]));
            }
        }
    }

    private Coast extractCoast(ObjectInformation coastInfo) throws JDipException {
        return Coast.getCoast(extractIntAttribute(coastInfo, "index"));
    }

    private void handleNonTurnData(World resultWorld, Map<UUID, Power> powers) throws JDipException {
        SerializeInformation nonTurnSerInfo = world.getAttribute("nonTurnData");
        if (nonTurnSerInfo == null || !nonTurnSerInfo.isMap()) {
            throw new JDipException("Expected the world to have a map of nonTurnData");
        }
        MapInformation nonTurnInfo = (MapInformation)nonTurnSerInfo;

        for (Map.Entry<SerializeInformation, SerializeInformation> nonTurnData: nonTurnInfo.getMap().entrySet()) {
            SerializeInformation keySerInfo = nonTurnData.getKey();
            if (keySerInfo.isObject()) {
                if ("dip.world.Power".equals(keySerInfo.getClassName())) {
                    SerializeInformation playerMetaDataSer = nonTurnData.getValue();
                    if (!playerMetaDataSer.isObject() ||
                            !"dip.world.metadata.PlayerMetadata".equals(playerMetaDataSer.getClassName())) {
                        throw new JDipException(
                                "Non turn data with key Power must have a value of type PlayerMetadata");
                    }
                    PlayerMetadata playerMetaData = extractPlayerMetaData((ObjectInformation)playerMetaDataSer);
                    resultWorld.setPlayerMetadata(powers.get(keySerInfo.getUuid()), playerMetaData);
                }
            } else if (keySerInfo.isPrimitive()) {
                if ("string".equals(keySerInfo.getClassName())) {
                    PrimitiveInformation keyInfo = (PrimitiveInformation)keySerInfo;
                    switch (keyInfo.getValue()) {
                        case "_variant_info_":
                            resultWorld.setVariantInfo(extractVariantInfo(nonTurnData.getValue()));
                            break;
                        case "_undo_redo_manager_":
                            // Ignored
                            break;
                        case "_victory_conditions_":
                            resultWorld.setVictoryConditions(extractVictoryConditions(nonTurnData.getValue()));
                            break;
                        case "_world_metadata_":
                            resultWorld.setGameMetadata(extractGameMetadata(nonTurnData.getValue()));
                            break;
                        case "_game_setup_":
                            break;
                        default:
                            logger.warn(new StringBuilder("Unknown entry in nonTurnData found: \"")
                                    .append(keyInfo.getValue()).append("\"").toString());
                            break;
                    }
                }
            }
        }
    }

    private PlayerMetadata extractPlayerMetaData(ObjectInformation playerMetaDataInfo) throws JDipException {
        PlayerMetadata playerMetadata = new PlayerMetadata();

        playerMetadata.setName(extractStringAttribute(playerMetaDataInfo, "name"));

        SerializeInformation emailSerInfo = playerMetaDataInfo.getAttribute("email");
        if (emailSerInfo == null || !emailSerInfo.isCollection() ||
                !"java.lang.String".equals(emailSerInfo.getClassName())) {
            throw new JDipException("Expected the player metadata to contain a collection of email addresses");
        }
        CollectionInformation emailInfo = (CollectionInformation)emailSerInfo;
        List<String> emailAddresses = new LinkedList<>();
        for (SerializeInformation emailEntry: emailInfo.getCollectionEntries()) {
            if (!emailEntry.isPrimitive() || !"string".equals(emailEntry.getClassName())) {
                throw new JDipException("Expected the email address of a player metadata to be a string");
            }
            PrimitiveInformation email = (PrimitiveInformation)emailEntry;
            emailAddresses.add(email.getValue());
        }
        playerMetadata.setEmailAddresses(emailAddresses.toArray(new String[emailAddresses.size()]));

        playerMetadata.setURI(extractURIAttribute(playerMetaDataInfo, "uri"));
        playerMetadata.setNotes(extractStringAttribute(playerMetaDataInfo, "notes"));

        return playerMetadata;
    }

    private World.VariantInfo extractVariantInfo(SerializeInformation variantInfoSerInfo) throws JDipException {
        World.VariantInfo variantInfo = new World.VariantInfo();

        if (variantInfoSerInfo == null || !variantInfoSerInfo.isObject() ||
                !"dip.world.World$VariantInfo".equals(variantInfoSerInfo.getClassName())) {
            throw new JDipException("Expected the variant info to be an object of class dip.world.World$VariantInfo");
        }
        ObjectInformation variantInfoInfo = (ObjectInformation)variantInfoSerInfo;

        variantInfo.setVariantName(extractStringAttribute(variantInfoInfo, "variantName"));
        variantInfo.setMapName(extractStringAttribute(variantInfoInfo, "mapName"));
        variantInfo.setSymbolPackName(extractStringAttribute(variantInfoInfo, "symbolsName"));
        variantInfo.setVariantVersion(extractFloatAttribute(variantInfoInfo, "variantVersion"));
        variantInfo.setSymbolPackVersion(extractFloatAttribute(variantInfoInfo, "symbolsVersion"));

        RuleOptions ruleOptions = new RuleOptions();
        variantInfo.setRuleOptions(ruleOptions);

        SerializeInformation ruleOptionsSerInfo = variantInfoInfo.getAttribute("ruleOptions");
        if (ruleOptionsSerInfo == null || !ruleOptionsSerInfo.isObject() ||
                !"dip.world.RuleOptions".equals(ruleOptionsSerInfo.getClassName())) {
            throw new JDipException(
                    "Expected the variant info to contain an attribute ruleOptions of type dip.world.RuleOptions");
        }
        ObjectInformation ruleOptionsInfo = (ObjectInformation)ruleOptionsSerInfo;
        SerializeInformation ruleOptionMapSerInfo = ruleOptionsInfo.getAttribute("optionMap");
        if (ruleOptionMapSerInfo == null || !ruleOptionMapSerInfo.isMap()) {
            throw new JDipException("Expected the ruleOptions to contain a map optionMap");
        }
        MapInformation ruleOptionsMapInfo = (MapInformation)ruleOptionMapSerInfo;
        for (Map.Entry<SerializeInformation, SerializeInformation> ruleOptionEntry:
                ruleOptionsMapInfo.getMap().entrySet()) {
            SerializeInformation ruleOptionKeySer = ruleOptionEntry.getKey();
            if (!ruleOptionKeySer.isObject() ||
                    !"dip.world.RuleOptions$Option".equals(ruleOptionKeySer.getClassName())) {
                throw new JDipException(
                        "Expected the ruleOptions map to have a key of type dip.world.RuleOptions$Option");
            }
            ObjectInformation ruleOptionKey = (ObjectInformation)ruleOptionKeySer;

            List<RuleOptions.OptionValue> allowed = new LinkedList<>();
            SerializeInformation allowedSerInfo = (SerializeInformation)ruleOptionKey.getAttribute("allowed");
            if (allowedSerInfo == null || !allowedSerInfo.isCollection() ||
                    !"dip.world.RuleOptions$OptionValue".equals(allowedSerInfo.getClassName())) {
                throw new JDipException("Expected the key of a rule option to contain a collection allowed");
            }
            CollectionInformation allowedInfo = (CollectionInformation)allowedSerInfo;
            for (SerializeInformation optionValueSerInfo: allowedInfo.getCollectionEntries()) {
                if (!optionValueSerInfo.isObject() ||
                        !"dip.world.RuleOptions$OptionValue".equals(optionValueSerInfo.getClassName())) {
                    throw new JDipException("Expected the optionValue to be an object");
                }
                ObjectInformation optionValueInfo = (ObjectInformation)optionValueSerInfo;
                allowed.add(new RuleOptions.OptionValue(extractStringAttribute(optionValueInfo, "name")));
            }

            SerializeInformation defaultValueSerInfo = ruleOptionKey.getAttribute("defaultValue");
            if (defaultValueSerInfo == null || !defaultValueSerInfo.isObject() ||
                    !"dip.world.RuleOptions$OptionValue".equals(defaultValueSerInfo.getClassName())) {
                throw new JDipException(
                        "Expected the default value to be an object of type dip.world.RuleOptions$OptionValue");
            }
            ObjectInformation defaultValueInfo = (ObjectInformation)defaultValueSerInfo;

            RuleOptions.OptionValue defaultValue =
                    new RuleOptions.OptionValue(extractStringAttribute(defaultValueInfo, "name"));

            RuleOptions.Option option = new RuleOptions.Option(extractStringAttribute(ruleOptionKey, "name"),
                    defaultValue, allowed.toArray(new RuleOptions.OptionValue[allowed.size()]));

            SerializeInformation ruleOptionValueSerInfo = ruleOptionEntry.getValue();
            if (!ruleOptionValueSerInfo.isObject() ||
                    !"dip.world.RuleOptions$OptionValue".equals(defaultValueSerInfo.getClassName())) {
                throw new JDipException(
                        "Expected the ruleOptions map to have a value of type dip.world.RuleOptions$OptionValue");
            }
            ObjectInformation ruleOptionValueInfo = (ObjectInformation)ruleOptionValueSerInfo;
            RuleOptions.OptionValue optionValue =
                    new RuleOptions.OptionValue(extractStringAttribute(ruleOptionValueInfo, "name"));

            ruleOptions.setOption(option, optionValue);
        }

        return variantInfo;
    }

    private VictoryConditions extractVictoryConditions(SerializeInformation victoryConditionsSerInfo)
            throws JDipException {
        if (victoryConditionsSerInfo == null || !victoryConditionsSerInfo.isObject() ||
                !"dip.world.VictoryConditions".equals(victoryConditionsSerInfo.getClassName())) {
            throw new JDipException(
                    "Expected the victory conditions to be an object of class dip.world.VictoryConditions");
        }
        ObjectInformation victoryConditionsInfo = (ObjectInformation)victoryConditionsSerInfo;

        int numSCForVictory = extractIntAttribute(victoryConditionsInfo, "numSCForVictory");
        int maxYearsNoSCChange = extractIntAttribute(victoryConditionsInfo, "maxYearsNoSCChange");
        int maxGameTimeYears = extractIntAttribute(victoryConditionsInfo, "maxGameTimeYears");
        int initialYear = extractIntAttribute(victoryConditionsInfo, "initialYear");

        return new VictoryConditions(numSCForVictory, maxYearsNoSCChange, maxGameTimeYears,
                new Phase(Phase.SeasonType.SPRING, initialYear, Phase.PhaseType.MOVEMENT));
    }

    private GameMetadata extractGameMetadata(SerializeInformation gameMetadataSerInfo) throws JDipException {
        GameMetadata gameMetadata = new GameMetadata();
        if (gameMetadataSerInfo == null || !gameMetadataSerInfo.isObject() ||
                !"dip.world.metadata.GameMetadata".equals(gameMetadataSerInfo.getClassName())) {
            throw new JDipException(
                    "Expected the game metadata to be an object of class dip.world.metadata.GameMetadata");
        }
        ObjectInformation gameMetadataInfo = (ObjectInformation)gameMetadataSerInfo;

        gameMetadata.setComment(extractStringAttribute(gameMetadataInfo, "comment"));
        gameMetadata.setGameName(extractStringAttribute(gameMetadataInfo, "gameName"));
        gameMetadata.setModeratorName(extractStringAttribute(gameMetadataInfo, "moderator"));
        gameMetadata.setModeratorEmail(extractStringAttribute(gameMetadataInfo, "moderatorEmail"));
        gameMetadata.setModeratorURI(extractURIAttribute(gameMetadataInfo, "moderatorURI"));
        gameMetadata.setJudgeName(extractStringAttribute(gameMetadataInfo, "judgeName"));
        gameMetadata.setGameURI(extractURIAttribute(gameMetadataInfo, "gameURI"));
        gameMetadata.setNotes(extractStringAttribute(gameMetadataInfo, "notes"));
        gameMetadata.setGameID(extractStringAttribute(gameMetadataInfo, "id"));

        return gameMetadata;
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

    private float extractFloatAttribute(ObjectInformation object, String attributeName) throws JDipException {
        SerializeInformation attributeSerInfo = object.getAttribute(attributeName);
        if (attributeSerInfo == null || !attributeSerInfo.isPrimitive() ||
                !"float".equals(attributeSerInfo.getClassName())) {
            throw new JDipException(new StringBuilder("Expected the ").append(object.getClassName())
                    .append(" to contain a float attribute with the name ").append(attributeName).toString());
        }
        PrimitiveInformation attributeInfo = (PrimitiveInformation)attributeSerInfo;
        return Float.parseFloat(attributeInfo.getValue());
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

    /**
     * Extract an URI from the given object with the given attribute name. May return null if either the URI is null or
     * the URI is invalid.
     *
     * @param object the object containing the URI
     * @param attributeName the name of the URI attribute
     * @return the URI or null
     * @throws JDipException if an error occurs
     */
    private URI extractURIAttribute(ObjectInformation object, String attributeName) throws JDipException {
        URI result;
        SerializeInformation uriSerInfo = object.getAttribute(attributeName);
        // the attribute uri must either be null or an object of the class java.net.URI
        if (!((uriSerInfo.isPrimitive() && "null".equals(uriSerInfo.getClassName())) ||
                (uriSerInfo.isObject() && "java.net.URI".equals(uriSerInfo.getClassName())))) {
            throw new JDipException(new StringBuilder("Expected the ").append(object.getClassName())
                    .append(" to contain an URI attribute with the name ").append(attributeName).toString());
        }
        if (uriSerInfo.isPrimitive()) {
            result = null;
        } else {
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
        }
        return result;
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

}
