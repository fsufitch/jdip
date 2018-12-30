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
import info.jdip.gui.F2FGUIGameSetup;
import info.jdip.gui.F2FOrderDisplayPanel;
import info.jdip.gui.order.GUIMove;
import info.jdip.gui.order.GUIMoveExplicit;
import info.jdip.gui.order.GUIOrderFactory;
import info.jdip.order.Move;
import info.jdip.order.OrderFactory;
import info.jdip.order.Orderable;
import info.jdip.order.result.BouncedResult;
import info.jdip.order.result.ConvoyPathResult;
import info.jdip.order.result.DependentMoveFailedResult;
import info.jdip.order.result.DislodgedResult;
import info.jdip.order.result.OrderResult;
import info.jdip.order.result.Result;
import info.jdip.order.result.SubstitutedResult;
import info.jdip.order.result.TimeResult;
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
import java.util.zip.GZIPInputStream;
import javax.xml.stream.events.Attribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is not thread safe.
 */
public class WorldImporter {

    private static final Logger logger = LoggerFactory.getLogger(WorldImporter.class);

    private boolean isRecognizedFile;

    private ObjectInformation world;

    private Deque<ObjectInformation> objectStack;

    private Deque<XMLEvent> eventStack;

    private Map<String, SerializeInformation> objectLookup;

    private Map<UUID, Power> powers;

    private Map<UUID, Province> provinces;

    private Map<UUID, Orderable> orders;

    private info.jdip.world.Map map;

    public WorldImporter() {
    }

    public World importGame(File file) throws IOException, XMLStreamException, JDipException {
        isRecognizedFile = false;
        world = null;
        objectStack = new LinkedList<>();
        eventStack = new LinkedList<>();
        objectLookup = new HashMap<>();
        powers = new LinkedHashMap();
        provinces = new LinkedHashMap<>();
        orders = new LinkedHashMap<>();
        map = null;

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
        SerializeInformation serializeInformation = objectLookup.get(idref);
        Attribute fieldAttr = startElement.getAttributeByName(new QName("field"));
        if (!objectStack.isEmpty()) {
            ObjectInformation enclosingObject = objectStack.peekFirst();
            if (fieldAttr != null) {
                enclosingObject.addAttribute(fieldAttr.getValue(), serializeInformation);
            } else if (enclosingObject.isMap()) {
                MapInformation map = (MapInformation)enclosingObject;
                map.addValue(serializeInformation);
            } else if (enclosingObject.isCollection()) {
                CollectionInformation collection = (CollectionInformation)enclosingObject;
                collection.addValue(serializeInformation);
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
        if ("string".equals(typeName)) {
            objectLookup.put(startElement.getAttributeByName(new QName("id")).getValue(),
                    primitiveInformation);
        }
    }

    private void handleNull(StartElement startElement) throws XMLStreamException {
        PrimitiveInformation primitiveInformation = new PrimitiveInformation("null");
        Attribute fieldAttr = startElement.getAttributeByName(new QName("field"));
        if (!objectStack.isEmpty()) {
            ObjectInformation enclosingObject = objectStack.peekFirst();
            if (fieldAttr != null) {
                enclosingObject.addAttribute(fieldAttr.getValue(), primitiveInformation);
            } else if (objectStack.peekFirst().isCollection()) {
                ((CollectionInformation)enclosingObject).addValue(null);
            }
        }
    }

    private Optional<World> extractWorld(SerializeInformation worldSerInfo) throws JDipException {
        if (worldSerInfo == null || !worldSerInfo.isObject() ||
                !"dip.world.World".equals(worldSerInfo.getClassName())) {
            return Optional.empty();
        }
        ObjectInformation worldInfo = (ObjectInformation)worldSerInfo;
        map = extractMap(worldInfo.getAttribute("map")).orElseThrow(
                () -> new JDipException("Expected a world to contain a map"));
        World resultWorld = new World(map);

        NonTurnDataProxy nonTurnData = extractNonTurnData(worldInfo.getAttribute("nonTurnData"))
                .orElseThrow(() -> new JDipException("Found unexpected element in non turn data"));
        nonTurnData.transferNonTurnData(resultWorld);
        List<TurnState> turnStates = extractSyncTurnStates(worldInfo.getAttribute("turnStates"))
                .orElseThrow(() -> new JDipException("Found unexpected element in turn states"));
        for (TurnState turnState: turnStates) {
            resultWorld.setTurnState(turnState);
        }
        return Optional.of(resultWorld);
    }

    private Optional<info.jdip.world.Map> extractMap(SerializeInformation mapSerInfo) throws JDipException {
        if (mapSerInfo == null || !mapSerInfo.isObject() || !"dip.world.Map".equals(mapSerInfo.getClassName())) {
            return Optional.empty();
        }
        ObjectInformation mapInfo = (ObjectInformation)mapSerInfo;

        if (map == null) {
            map = new info.jdip.world.Map(
                    extractPowers(mapInfo.getAttribute("powers"))
                        .orElseThrow(() -> new JDipException("Expected the map to contain an array of powers")),
                    extractProvinces(mapInfo.getAttribute("provinces"))
                        .orElseThrow(() -> new JDipException("Expected the map to contain an array of provinces"))
            );
        }
        return Optional.of(map);
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
                    extractBoolean(powerInfo.getAttribute("isActive"))
                        .orElseThrow(() -> new JDipException("The isActive flag of a power may not be null")));
            powers.put(powerInfo.getUuid(), power);
        }
        return Optional.of(power);
    }

    private Optional<List<Province[]>> extractProvincesList(SerializeInformation provincesListSerInfo)
            throws JDipException {
        if (provincesListSerInfo == null || !provincesListSerInfo.isCollection() ||
                !"java.util.ArrayList".equals(provincesListSerInfo.getClassName())) {
            return Optional.empty();
        }
        CollectionInformation provincesListInfo = (CollectionInformation)provincesListSerInfo;

        List<Province[]> provincesInfo = new LinkedList<>();
        for (SerializeInformation provincesSerInfo: provincesListInfo.getCollectionEntries()) {
            provincesInfo.add(extractProvinces(provincesSerInfo)
                    .orElseThrow(() -> new JDipException("Expected the provinces list to contain provinces")));
        }
        return Optional.of(provincesInfo);
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
                    extractBoolean(provinceInfo.getAttribute("isConvoyableCoast"))
                            .orElseThrow(() -> new JDipException(
                                    "The isConvoyableCoast flag of a province may not be null")));
            province.setSupplyCenter(extractBoolean(provinceInfo.getAttribute("supplyCenter"))
                    .orElseThrow(() -> new JDipException("The supplyCenter flag may not be null")));
            provinces.put(provinceInfo.getUuid(), province);
            Optional<Border[]> borders = extractBorders(provinceInfo.getAttribute("borders"));
            if (borders.isPresent()) {
                province.setBorders(borders.get());
            }

            extractAdjacency(provinceInfo.getAttribute("adjacency"))
                        .orElseThrow(() -> new JDipException("Expected a province to contain adjacency information"))
                    .transferToAdjacency(province.getAdjacency());
        }
        return Optional.of(province);
    }

    private Optional<Border[]> extractBorders(SerializeInformation bordersSerInfo) throws JDipException {
        if (bordersSerInfo == null || !bordersSerInfo.isCollection() ||
                !"dip.world.Border".equals(bordersSerInfo.getClassName())) {
            return Optional.empty();
        }
        CollectionInformation bordersInfo = (CollectionInformation)bordersSerInfo;

        List<Border> borders = new LinkedList<>();
        for (SerializeInformation borderSerInfo: bordersInfo.getCollectionEntries()) {
            borders.add(extractBorder(borderSerInfo)
                    .orElseThrow(() -> new JDipException("Expected borders to contain a border")));
        }
        return Optional.of(borders.toArray(new Border[borders.size()]));
    }

    private Optional<Border> extractBorder(SerializeInformation borderSerInfo) throws JDipException {
        if (borderSerInfo == null || !borderSerInfo.isObject() ||
                !"dip.world.Border".equals(borderSerInfo.getClassName())) {
            return Optional.empty();
        }
        ObjectInformation borderInfo = (ObjectInformation)borderSerInfo;

        try {
            return Optional.of(new Border(
                    extractString(borderInfo.getAttribute("id"))
                        .orElseThrow(() -> new JDipException("Expected a border to contain an id")),
                    extractString(borderInfo.getAttribute("description"))
                        .orElseThrow(() -> new JDipException("Expected a border to contain a description")),
                    extractUnitTypesAsString(borderInfo.getAttribute("unitTypes")).orElse(""),
                    extractLocations(borderInfo.getAttribute("from")).orElse(null),
                    extractClassesArrayAsString(borderInfo.getAttribute("orderClasses")).orElse(""),
                    extractBaseMoveModifier(borderInfo.getAttribute("baseMoveModifier"))
                        .orElseThrow(() -> new JDipException("Expected a border to contain a base move modifier")),
                    extractSeasonTypesAsString(borderInfo.getAttribute("seasons")).orElse(""),
                    extractPhaseTypesAsString(borderInfo.getAttribute("phases")).orElse(""),
                    extractBorderYear(borderInfo.getAttribute("yearMin"), borderInfo.getAttribute("yearMax"),
                            borderInfo.getAttribute("yearModifier"))
                        .orElseThrow(() -> new JDipException("Expected the border to contain year informations"))
            ));
        } catch (InvalidBorderException ibex) {
            throw new JDipException(ibex);
        }
    }

    private Optional<String> extractUnitTypesAsString(SerializeInformation unitsSerInfo) throws JDipException {
        if (unitsSerInfo == null || !unitsSerInfo.isCollection() ||
                !"dip.world.Unit$Type".equals(unitsSerInfo.getClassName())) {
            return Optional.empty();
        }

        Optional<Unit.Type[]> unitTypesArray = extractUnitTypeArray(unitsSerInfo);
        if (unitTypesArray.isPresent()) {
            Unit.Type[] unitTypes = unitTypesArray.get();
            StringBuilder sb = new StringBuilder(unitTypes[0].getInternalName());
            if (unitTypes.length > 1) {
                for (int i = 1; i < unitTypes.length; i++) {
                    sb.append(",");
                    sb.append(unitTypes[i].getInternalName());
                }
            }
            return Optional.of(sb.toString());
        }

        return Optional.empty();
    }

    private Optional<String> extractClassesArrayAsString(SerializeInformation classesArraySerInfo)
            throws JDipException {
        if (classesArraySerInfo == null || !classesArraySerInfo.isCollection() ||
                !"java.lang.Class".equals(classesArraySerInfo.getClassName())) {
            return Optional.empty();
        }
        CollectionInformation classesArrayInfo = (CollectionInformation)classesArraySerInfo;

        StringBuilder sb = new StringBuilder("");
        for (SerializeInformation classSerInfo: classesArrayInfo.getCollectionEntries()) {
            String className = extractClassString(classSerInfo)
                    .orElseThrow(() -> new JDipException("Expected a classes array to contain class names"));
            if (sb.length() > 0) {
                sb.append(",");
            }
            sb.append(className);
        }
        return Optional.of(sb.toString());
    }

    private Optional<String> extractClassString(SerializeInformation classSerInfo) throws JDipException {
        if (classSerInfo == null || !classSerInfo.isObject() ||
                !"java.lang.Class".equals(classSerInfo.getClassName())) {
            return Optional.empty();
        }
        ObjectInformation classInfo = (ObjectInformation)classSerInfo;

        return extractString(classInfo.getAttribute("name"));
    }

    private Optional<String> extractBaseMoveModifier(SerializeInformation baseMoveModifierSerInfo)
            throws JDipException {
        int baseMoveModifier = extractInt(baseMoveModifierSerInfo)
                .orElseThrow(() -> new JDipException("Expected the base move modifier to be an int"));
        return Optional.of(Integer.toString(baseMoveModifier));
    }

    private Optional<String> extractSeasonTypesAsString(SerializeInformation seasonTypesSerInfo) throws JDipException {
        Optional<List<Phase.SeasonType>> seasonTypes = extractSeasonTypes(seasonTypesSerInfo);
        if (seasonTypes.isPresent()) {
            StringBuilder sb = new StringBuilder("");
            for (Phase.SeasonType seasonType: seasonTypes.get()) {
                if (sb.length() > 0) {
                    sb.append(",");
                }
                sb.append(seasonType.getBriefName());
            }
            return Optional.of(sb.toString());
        } else {
            return Optional.empty();
        }
    }

    private Optional<String> extractPhaseTypesAsString(SerializeInformation phaseTypesSerInfo) throws JDipException {
        Optional<List<Phase.PhaseType>> phaseTypes = extractPhaseTypes(phaseTypesSerInfo);
        if (phaseTypes.isPresent()) {
            StringBuilder sb = new StringBuilder("");
            for (Phase.PhaseType phaseType: phaseTypes.get()) {
                if (sb.length() > 0) {
                    sb.append(",");
                }
                sb.append(phaseType.getBriefName());
            }
            return Optional.of(sb.toString());
        } else {
            return Optional.empty();
        }
    }

    private Optional<String> extractBorderYear(SerializeInformation yearMinSerInfo, SerializeInformation yearMaxSerInfo,
            SerializeInformation yearModifierSerInfo) throws JDipException {
        if (yearMinSerInfo == null || !yearMinSerInfo.isPrimitive() ||
                yearMaxSerInfo == null || !yearMaxSerInfo.isPrimitive() ||
                yearModifierSerInfo == null || !yearModifierSerInfo.isPrimitive()) {
            return Optional.empty();
        }

        StringBuilder result = new StringBuilder();
        Integer yearModifier = extractInt(yearModifierSerInfo)
                .orElseThrow(() -> new JDipException("Expected the border to contain a year modifier"));
        switch (yearModifier) {
            case 0:
                result.append("");
                break;
            case 1:
                int yearMin = extractInt(yearMinSerInfo).orElseThrow(
                        () -> new JDipException("Cannot parse min year of border"));
                int yearMax = extractInt(yearMaxSerInfo).orElseThrow(
                        () -> new JDipException("Cannot parse max year of border"));
                if (yearMin > Integer.MIN_VALUE) {
                    result.append(Integer.toString(yearMin));
                }
                if (yearMax < Integer.MAX_VALUE) {
                    result.append(",");
                    result.append(Integer.toString(yearMax));
                }
                break;
            case 2:
                result.append("odd");
                break;
            case 3:
                result.append("even");
                break;
            default:
                throw new JDipException("Unknown year modifier in border");
        }
        return Optional.of(result.toString());
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
        if ("dip.gui.F2FGUIGameSetup".equals(gameSetupSerInfo.getClassName())) {
            // TODO implement reading of F2FGUIGameSetup
            return extractF2FGUIGameSetup(gameSetupSerInfo);
        } else {
            return Optional.of(new DefaultGUIGameSetup());
        }
    }

    private Optional<GameSetup> extractF2FGUIGameSetup(SerializeInformation gameSetupSerInfo)
            throws JDipException {
        if (gameSetupSerInfo == null || !gameSetupSerInfo.isObject() ||
                !"dip.gui.F2FGUIGameSetup".equals(gameSetupSerInfo.getClassName())) {
            return Optional.empty();
        }
        ObjectInformation gameSetupInfo = (ObjectInformation)gameSetupSerInfo;

        return Optional.of(new F2FGUIGameSetup(extractF2FState(gameSetupInfo.getAttribute("state"))
                .orElseThrow(() -> new JDipException("Expected a F2FGUIGameSetup to contain a state"))));
    }

    private Optional<F2FOrderDisplayPanel.F2FState> extractF2FState(SerializeInformation stateSerInfo)
            throws JDipException {
        if (stateSerInfo == null || !stateSerInfo.isObject() ||
                !"dip.gui.F2FOrderDisplayPanel$F2FState".equals(stateSerInfo.getClassName())) {
            return Optional.empty();
        }
        ObjectInformation stateInfo = (ObjectInformation)stateSerInfo;

        F2FOrderDisplayPanel.F2FState state = new F2FOrderDisplayPanel.F2FState();
        Map<Power, Boolean> submittedMap = extractSubmittedMap(stateInfo.getAttribute("submittedMap"))
                .orElseThrow(() -> new JDipException("Expected the F2FState to contain a submittedMap"));
        for (Map.Entry<Power, Boolean> mapEntry: submittedMap.entrySet()) {
            state.setSubmitted(mapEntry.getKey(), mapEntry.getValue());
        }
        state.setCurrentPower(extractPower(stateInfo.getAttribute("currentPower"))
                .orElseThrow(() -> new JDipException("Expected the F2FState to contain a currentPower")));

        return Optional.of(state);
    }

    private Optional<Map<Power, Boolean>> extractSubmittedMap(SerializeInformation submittedMapSerInfo)
            throws JDipException {
        if (submittedMapSerInfo == null || !submittedMapSerInfo.isMap()) {
            return Optional.empty();
        }
        MapInformation submittedMapInfo = (MapInformation)submittedMapSerInfo;

        Map<Power, Boolean> submittedMap = new LinkedHashMap<>();
        for (Map.Entry<SerializeInformation, SerializeInformation> mapEntry: submittedMapInfo.getMap().entrySet()) {
            Power power = extractPower(mapEntry.getKey())
                    .orElseThrow(() -> new JDipException("Expected a power as key of a submittedMap"));
            Boolean value = extractBooleanObject(mapEntry.getValue())
                    .orElseThrow(() -> new JDipException("Expected a boolean as value of a submittedMap"));
            submittedMap.put(power, value);
        }
        return Optional.of(submittedMap);
    }

    private Optional<List<TurnState>> extractSyncTurnStates(SerializeInformation turnStatesSerInfo)
            throws JDipException {
        if (turnStatesSerInfo == null || !turnStatesSerInfo.isObject() ||
                !"java.util.Collections$SynchronizedSortedMap".equals(turnStatesSerInfo.getClassName())) {
            return Optional.empty();
        }
        ObjectInformation turnStatesInfo = (ObjectInformation)turnStatesSerInfo;
        return extractTurnStates(turnStatesInfo.getAttribute("m"));
    }

    private Optional<List<TurnState>> extractTurnStates(SerializeInformation turnStatesSerInfo)
            throws JDipException {
        if (turnStatesSerInfo == null || !turnStatesSerInfo.isMap()) {
            return Optional.empty();
        }
        MapInformation turnStatesInfo = (MapInformation)turnStatesSerInfo;

        List<TurnState> turnStates = new LinkedList<>();
        for (SerializeInformation turnStateSerInfo: turnStatesInfo.getMap().values()) {
            turnStates.add(extractTurnState(turnStateSerInfo)
                    .orElseThrow(() -> new JDipException(
                            "Expected the values of the turn states map to be a TurnState")));
        }
        return Optional.of(turnStates);
    }

    private Optional<TurnState> extractTurnState(SerializeInformation turnStateSerInfo) throws JDipException {
        if (turnStateSerInfo == null || !turnStateSerInfo.isObject() ||
                !"dip.world.TurnState".equals(turnStateSerInfo.getClassName())) {
            return Optional.empty();
        }
        ObjectInformation turnStateInfo = (ObjectInformation)turnStateSerInfo;

        TurnState turnState = new TurnState(extractPhase(turnStateInfo.getAttribute("phase"))
                .orElseThrow(() -> new JDipException("Expected a turn state to contain a phase")));

        Map<Power, List<Orderable>> orderMap = extractOrderMap(turnStateInfo.getAttribute("orderMap"))
                .orElseThrow(() -> new JDipException("Expected a turn state to contain an orderMap"));
        for (Map.Entry<Power, List<Orderable>> orderMapEntry: orderMap.entrySet()) {
            turnState.setOrders(orderMapEntry.getKey(), orderMapEntry.getValue());
        }
        turnState.setResultList(extractResultList(turnStateInfo.getAttribute("resultList"))
                .orElseThrow(() -> new JDipException("Expected a turn state to contain a resultList")));
        turnState.setPosition(extractPosition(turnStateInfo.getAttribute("position"))
                .orElseThrow(() -> new JDipException("Expected a turn state to contain a position")));

        turnState.setSCOwnerChanged(extractBoolean(turnStateInfo.getAttribute("isSCOwnerChanged"))
                .orElseThrow(() -> new JDipException("Expected a turn state to contain isSCOwnerChanged")));
        turnState.setEnded(extractBoolean(turnStateInfo.getAttribute("isEnded"))
                .orElseThrow(() -> new JDipException("Expected a turn state to contain isEnded")));
        turnState.setResolved(extractBoolean(turnStateInfo.getAttribute("isResolved"))
                .orElseThrow(() -> new JDipException("Expected a turn state to contain isResolved")));

        return Optional.of(turnState);
    }

    private Optional<Phase> extractPhase(SerializeInformation phaseSerInfo) throws JDipException {
        if (phaseSerInfo == null || !phaseSerInfo.isObject() ||
                !"dip.world.Phase".equals(phaseSerInfo.getClassName())) {
            return Optional.empty();
        }
        ObjectInformation phaseInfo = (ObjectInformation)phaseSerInfo;

        Phase.SeasonType seasonType = extractSeasonType(phaseInfo.getAttribute("seasonType"))
                .orElseThrow(() -> new JDipException("Expected a phase to contain a seasonType"));
        Phase.YearType yearType = extractYearType(phaseInfo.getAttribute("yearType"))
                .orElseThrow(() -> new JDipException("Expected a phase to contain a yearType"));
        Phase.PhaseType phaseType = extractPhaseType(phaseInfo.getAttribute("phaseType"))
                .orElseThrow(() -> new JDipException("Expected a phase to contain a phaseType"));
        return Optional.of(new Phase(seasonType, yearType, phaseType));
    }

    private Optional<List<Phase.SeasonType>> extractSeasonTypes(SerializeInformation seasonTypesSerInfo)
            throws JDipException {
        if (seasonTypesSerInfo == null || !seasonTypesSerInfo.isCollection() ||
                !"dip.world.Phase$SeasonType".equals(seasonTypesSerInfo.getClassName())) {
            return Optional.empty();
        }
        CollectionInformation seasonTypesInfo = (CollectionInformation)seasonTypesSerInfo;

        List<Phase.SeasonType> result = new LinkedList<>();
        for (SerializeInformation seasonTypeSerInfo: seasonTypesInfo.getCollectionEntries()) {
            result.add(extractSeasonType(seasonTypeSerInfo).orElseThrow(
                    () -> new JDipException("Expected a collection of season types to contain season types"))
            );
        }
        return Optional.of(result);
    }

    private Optional<Phase.SeasonType> extractSeasonType(SerializeInformation seasonTypeSerInfo) throws JDipException {
        if (seasonTypeSerInfo == null || !seasonTypeSerInfo.isObject() ||
                !"dip.world.Phase$SeasonType".equals(seasonTypeSerInfo.getClassName())) {
            return Optional.empty();
        }
        ObjectInformation seasonTypeInfo = (ObjectInformation)seasonTypeSerInfo;

        int seasonTypePosition = extractInt(seasonTypeInfo.getAttribute("position"))
                .orElseThrow(() -> new JDipException("Expected a season type to contain a position"));
        Phase.SeasonType seasonType;
        if (seasonTypePosition == 1000) {
            seasonType = Phase.SeasonType.SPRING;
        } else {
            seasonType = Phase.SeasonType.FALL;
        }
        return Optional.of(seasonType);
    }

    private Optional<Phase.YearType> extractYearType(SerializeInformation yearTypeSerInfo) throws JDipException {
        if (yearTypeSerInfo == null || !yearTypeSerInfo.isObject() ||
                !"dip.world.Phase$YearType".equals(yearTypeSerInfo.getClassName())) {
            throw new JDipException("Expected a phase to contain an object of type dip.world.Phase$YearType");
        }
        ObjectInformation yearTypeInfo = (ObjectInformation)yearTypeSerInfo;

        int yearTypeYear = extractInt(yearTypeInfo.getAttribute("year"))
                .orElseThrow(() -> new JDipException("Expected a year type to contain a year"));
        return Optional.of(new Phase.YearType(yearTypeYear));
    }

    private Optional<List<Phase.PhaseType>> extractPhaseTypes(SerializeInformation phaseTypesSerInfo)
            throws JDipException {
        if (phaseTypesSerInfo == null || !phaseTypesSerInfo.isCollection() ||
                !"dip.world.Phase$PhaseType".equals(phaseTypesSerInfo.getClassName())) {
            return Optional.empty();
        }
        CollectionInformation phaseTypesInfo = (CollectionInformation)phaseTypesSerInfo;

        List<Phase.PhaseType> result = new LinkedList<>();
        for (SerializeInformation phaseTypeSerInfo: phaseTypesInfo.getCollectionEntries()) {
            result.add(extractPhaseType(phaseTypeSerInfo)
                    .orElseThrow(() -> new JDipException("Expected a collection of phase types to contain phase types"))
            );
        }
        return Optional.of(result);
    }

    private Optional<Phase.PhaseType> extractPhaseType(SerializeInformation phaseTypeSerInfo) throws JDipException {
        if (phaseTypeSerInfo == null || !phaseTypeSerInfo.isObject() ||
                !"dip.world.Phase$PhaseType".equals(phaseTypeSerInfo.getClassName())) {
            throw new JDipException("Expected a phase to contain an object of type dip.world.Phase$PhaseType");
        }
        ObjectInformation phaseTypeInfo = (ObjectInformation)phaseTypeSerInfo;

        return Optional.of(Phase.PhaseType.parse(extractString(phaseTypeInfo.getAttribute("constName"))
                .orElseThrow(() -> new JDipException("Expected a phase type to contain a constName"))));
    }

    private Optional<Map<Power, List<Orderable>>> extractOrderMap(SerializeInformation orderMapSerInfo)
            throws JDipException {
        if (orderMapSerInfo == null || !orderMapSerInfo.isMap() ||
                !"java.util.HashMap".equals(orderMapSerInfo.getClassName())) {
            return Optional.empty();
        }
        MapInformation orderMapInfo = (MapInformation)orderMapSerInfo;

        Map<Power, List<Orderable>> orderMap = new LinkedHashMap<>();
        for (Map.Entry<SerializeInformation, SerializeInformation> orderMapEntry: orderMapInfo.getMap().entrySet()) {
            Power orderMapKey = extractPower(orderMapEntry.getKey())
                    .orElseThrow(() -> new JDipException("Expected the key of an orderMap to be a Power"));
            List<Orderable> orderMapValue = extractOrderList(orderMapEntry.getValue())
                    .orElseThrow(() -> new JDipException(
                            "Expected the value of an orderMap to be a list of Orderable"));
            orderMap.put(orderMapKey, orderMapValue);
        }
        return Optional.of(orderMap);
    }

    private Optional<List<Orderable>> extractOrderList(SerializeInformation orderListSerInfo) throws JDipException {
        if (orderListSerInfo == null || !orderListSerInfo.isCollection() ||
                !"java.util.ArrayList".equals(orderListSerInfo.getClassName())) {
            return Optional.empty();
        }
        CollectionInformation orderListInfo = (CollectionInformation)orderListSerInfo;

        List<Orderable> ordersInfo = new LinkedList<>();
        for (SerializeInformation orderSerInfo: orderListInfo.getCollectionEntries()) {
            ordersInfo.add(extractOrder(orderSerInfo)
                    .orElseThrow(() -> new JDipException("Expected the orders list to contain orders")));
        }
        return Optional.of(ordersInfo);
    }

    private Optional<Orderable> extractOrder(SerializeInformation orderSerInfo) throws JDipException {
        if (orderSerInfo == null || !orderSerInfo.isObject()) {
            return Optional.empty();
        }
        ObjectInformation orderInfo = (ObjectInformation)orderSerInfo;

        Orderable order = orders.get(orderInfo.getUuid());
        if (order == null) {
            switch (orderInfo.getClassName()) {
                case "dip.gui.order.GUIBuild":
                    order = extractGuiBuild(orderInfo)
                            .orElseThrow(() -> new JDipException("Expected a build order"));
                    break;
                case "dip.gui.order.GUIConvoy":
                    order = extractGuiConvoy(orderInfo)
                            .orElseThrow(() -> new JDipException("Expected a convoy order"));
                    break;
                case "dip.gui.order.GUIDisband":
                    order = extractGuiDisband(orderInfo)
                            .orElseThrow(() -> new JDipException("Expected a disband order"));
                    break;
                case "dip.gui.order.GUIHold":
                    order = extractGuiHold(orderInfo)
                            .orElseThrow(() -> new JDipException("Expected a hold order"));
                    break;
                case "dip.gui.order.GUIMove":
                    order = extractGuiMove(orderInfo)
                            .orElseThrow(() -> new JDipException("Expected a move order"));
                    break;
                case "dip.gui.order.GUIMoveExplicit":
                    order = extractGuiMoveExplicit(orderInfo)
                            .orElseThrow(() -> new JDipException("Expected an explicit move order"));
                    break;
                case "dip.gui.order.GUIRemove":
                    order = extractGuiRemove(orderInfo)
                            .orElseThrow(() -> new JDipException("Expected a remove order"));
                    break;
                case "dip.gui.order.GUIRetreat":
                    order = extractGuiRetreat(orderInfo)
                            .orElseThrow(() -> new JDipException("Expected a retreat order"));
                    break;
                case "dip.gui.order.GUISupport":
                    order = extractGuiSupport(orderInfo)
                            .orElseThrow(() -> new JDipException("Expected a support order"));
                    break;
                case "dip.gui.order.GUIWaive":
                    order = extractGuiWaive(orderInfo)
                            .orElseThrow(() -> new JDipException("Expected a waive order"));
                    break;
                default:
                    return Optional.empty();
            }
            orders.put(orderInfo.getUuid(), order);
        }
        return Optional.of(order);
    }

    private Optional<Orderable> extractGuiBuild(ObjectInformation orderInfo) throws JDipException {
        Power power = extractPower(orderInfo.getAttribute("power"))
                .orElseThrow(() -> new JDipException("Expected an order to contain a power"));
        Location src = extractLocation(orderInfo.getAttribute("src"))
                .orElseThrow(() -> new JDipException("Expected an order to contain a source location"));
        Unit.Type srcUnitType = extractUnitType(orderInfo.getAttribute("srcUnitType"))
                .orElseThrow(() -> new JDipException("Expected an order to contain a unit type"));

        return Optional.of(new GUIOrderFactory().createBuild(power, src, srcUnitType));
    }

    private Optional<Orderable> extractGuiConvoy(ObjectInformation orderInfo) throws JDipException {
        Power power = extractPower(orderInfo.getAttribute("power"))
                .orElseThrow(() -> new JDipException("Expected an order to contain a power"));
        Location src = extractLocation(orderInfo.getAttribute("src"))
                .orElseThrow(() -> new JDipException("Expected an order to contain a source location"));
        Unit.Type srcUnitType = extractUnitType(orderInfo.getAttribute("srcUnitType"))
                .orElseThrow(() -> new JDipException("Expected an order to contain a unit type"));
        Location convoySrc = extractLocation(orderInfo.getAttribute("convoySrc"))
                .orElseThrow(() -> new JDipException("Expected an order to contain a convoy source"));
        Location convoyDest = extractLocation(orderInfo.getAttribute("convoyDest"))
                .orElseThrow(() -> new JDipException("Expected an order to contain a convoy destination"));
        Unit.Type convoyUnitType = extractUnitType(orderInfo.getAttribute("convoyUnitType"))
                .orElseThrow(() -> new JDipException("Expected an order to contain a supported unit type"));
        Power convoyPower = extractPower(orderInfo.getAttribute("convoyPower")).orElse(null);

        return Optional.of(new GUIOrderFactory().createConvoy(power, src, srcUnitType, convoySrc, convoyPower,
                convoyUnitType, convoyDest));
    }

    private Optional<Orderable> extractGuiHold(ObjectInformation orderInfo) throws JDipException {
        Power power = extractPower(orderInfo.getAttribute("power"))
                .orElseThrow(() -> new JDipException("Expected an order to contain a power"));
        Location src = extractLocation(orderInfo.getAttribute("src"))
                .orElseThrow(() -> new JDipException("Expected an order to contain a source location"));
        Unit.Type unitType = extractUnitType(orderInfo.getAttribute("srcUnitType"))
                .orElseThrow(() -> new JDipException("Expected an order to contain a unit type"));

        return Optional.of(new GUIOrderFactory().createHold(power, src, unitType));
    }

    private Optional<Orderable> extractGuiDisband(ObjectInformation orderInfo) throws JDipException {
        Power power = extractPower(orderInfo.getAttribute("power"))
                .orElseThrow(() -> new JDipException("Expected an order to contain a power"));
        Location src = extractLocation(orderInfo.getAttribute("src"))
                .orElseThrow(() -> new JDipException("Expected an order to contain a source location"));
        Unit.Type srcUnitType = extractUnitType(orderInfo.getAttribute("srcUnitType"))
                .orElseThrow(() -> new JDipException("Expected an order to contain a unit type"));

        return Optional.of(new GUIOrderFactory().createDisband(power, src, srcUnitType));
    }

    private Optional<Orderable> extractGuiMove(ObjectInformation orderInfo) throws JDipException {
        Power power = extractPower(orderInfo.getAttribute("power"))
                .orElseThrow(() -> new JDipException("Expected an order to contain a power"));
        Location src = extractLocation(orderInfo.getAttribute("src"))
                .orElseThrow(() -> new JDipException("Expected an order to contain a source location"));
        Unit.Type srcUnitType = extractUnitType(orderInfo.getAttribute("srcUnitType"))
                .orElseThrow(() -> new JDipException("Expected an order to contain a unit type"));
        Location dest = extractLocation(orderInfo.getAttribute("dest"))
                .orElseThrow(() -> new JDipException("Expected an order to contain a destination location"));
        Optional<List<Province[]>> convoyRoutes = extractProvincesList(orderInfo.getAttribute("convoyRoutes"));
        if (convoyRoutes.isPresent()) {
            return Optional.of(new GUIOrderFactory().createMove(power, src, srcUnitType, dest, convoyRoutes.get()));
        } else {
            return Optional.of(new GUIOrderFactory().createMove(power, src, srcUnitType, dest));
        }
    }

    private Optional<Orderable> extractGuiMoveExplicit(ObjectInformation orderInfo) throws JDipException {
        Power power = extractPower(orderInfo.getAttribute("power"))
                .orElseThrow(() -> new JDipException("Expected an order to contain a power"));
        Location src = extractLocation(orderInfo.getAttribute("src"))
                .orElseThrow(() -> new JDipException("Expected an order to contain a source location"));
        Unit.Type srcUnitType = extractUnitType(orderInfo.getAttribute("srcUnitType"))
                .orElseThrow(() -> new JDipException("Expected an order to contain a unit type"));
        Location dest = extractLocation(orderInfo.getAttribute("dest"))
                .orElseThrow(() -> new JDipException("Expected an order to contain a destination location"));
        List<Province[]> convoyRoutes = extractProvincesList(orderInfo.getAttribute("convoyRoutes"))
                .orElseThrow(() -> new JDipException("Expected an explicit move to contain convoy routes"));
        GUIMoveExplicit moveExplicit = new GUIOrderFactory().createGUIMoveExplicit();
        moveExplicit.deriveFrom(new GUIOrderFactory().createMove(power, src, srcUnitType, dest, convoyRoutes));
        return Optional.of(moveExplicit);
    }

    private Optional<Orderable> extractGuiRemove(ObjectInformation orderInfo) throws JDipException {
        Power power = extractPower(orderInfo.getAttribute("power"))
                .orElseThrow(() -> new JDipException("Expected an order to contain a power"));
        Location src = extractLocation(orderInfo.getAttribute("src"))
                .orElseThrow(() -> new JDipException("Expected an order to contain a source location"));
        Unit.Type srcUnitType = extractUnitType(orderInfo.getAttribute("srcUnitType"))
                .orElseThrow(() -> new JDipException("Expected an order to contain a unit type"));

        return Optional.of(new GUIOrderFactory().createRemove(power, src, srcUnitType));
    }

    private Optional<Orderable> extractGuiRetreat(ObjectInformation orderInfo) throws JDipException {
        Power power = extractPower(orderInfo.getAttribute("power"))
                .orElseThrow(() -> new JDipException("Expected an order to contain a power"));
        Location src = extractLocation(orderInfo.getAttribute("src"))
                .orElseThrow(() -> new JDipException("Expected an order to contain a source location"));
        Unit.Type srcUnitType = extractUnitType(orderInfo.getAttribute("srcUnitType"))
                .orElseThrow(() -> new JDipException("Expected an order to contain a unit type"));
        Location dest = extractLocation(orderInfo.getAttribute("dest"))
                .orElse(null);

        return Optional.of(new GUIOrderFactory().createRetreat(power, src, srcUnitType, dest));
    }

    private Optional<Orderable> extractGuiSupport(ObjectInformation orderInfo) throws JDipException {
        Power power = extractPower(orderInfo.getAttribute("power"))
                .orElseThrow(() -> new JDipException("Expected an order to contain a power"));
        Location src = extractLocation(orderInfo.getAttribute("src"))
                .orElseThrow(() -> new JDipException("Expected an order to contain a source location"));
        Unit.Type srcUnitType = extractUnitType(orderInfo.getAttribute("srcUnitType"))
                .orElseThrow(() -> new JDipException("Expected an order to contain a unit type"));
        Location supSrc = extractLocation(orderInfo.getAttribute("supSrc"))
                .orElseThrow(() -> new JDipException("Expected an order to contain a supported source"));
        Location supDest = extractLocation(orderInfo.getAttribute("supDest"))
                .orElse(null);
        Unit.Type supUnitType = extractUnitType(orderInfo.getAttribute("supUnitType"))
                .orElseThrow(() -> new JDipException("Expected an order to contain a supported unit type"));
        Power supPower = extractPower(orderInfo.getAttribute("supPower"))
                .orElseThrow(() -> new JDipException("Expected an order to contain a supported power"));

        return Optional.of(new GUIOrderFactory().createSupport(power, src, srcUnitType, supSrc, supPower,
                supUnitType, supDest));
    }

    private Optional<Orderable> extractGuiWaive(ObjectInformation orderInfo) throws JDipException {
        Power power = extractPower(orderInfo.getAttribute("power"))
                .orElseThrow(() -> new JDipException("Expected an order to contain a power"));
        Location src = extractLocation(orderInfo.getAttribute("src"))
                .orElseThrow(() -> new JDipException("Expected an order to contain a source location"));

        return Optional.of(new GUIOrderFactory().createWaive(power, src));
    }

    private Optional<Unit.Type[]> extractUnitTypeArray(SerializeInformation unitTypeArraySerInfo) throws JDipException {
        if (unitTypeArraySerInfo == null || !unitTypeArraySerInfo.isCollection() ||
                !"dip.world.Unit$Type".equals(unitTypeArraySerInfo.getClassName())) {
            return Optional.empty();
        }
        CollectionInformation unitTypeArrayInfo = (CollectionInformation)unitTypeArraySerInfo;

        List<Unit.Type> unitTypeArray = new LinkedList<>();
        for (SerializeInformation unitTypeSerInfo: unitTypeArrayInfo.getCollectionEntries()) {
            unitTypeArray.add(extractUnitType(unitTypeSerInfo)
                    .orElseThrow(() -> new JDipException("Expected the content of a unit type array not to be null")));
        }
        return Optional.of(unitTypeArray.toArray(new Unit.Type[unitTypeArray.size()]));
    }

    private Optional<Unit.Type> extractUnitType(SerializeInformation unitTypeSerInfo) throws JDipException {
        if (unitTypeSerInfo == null || !unitTypeSerInfo.isObject() ||
                !"dip.world.Unit$Type".equals(unitTypeSerInfo.getClassName())) {
            return Optional.empty();
        }
        ObjectInformation unitTypeInfo = (ObjectInformation)unitTypeSerInfo;

        return Optional.of(Unit.Type.parse(extractString(unitTypeInfo.getAttribute("internalName"))
                .orElseThrow(() -> new JDipException("Expected an unit type to contain an internal name"))));
    }

    private Optional<List<Result>> extractResultList(SerializeInformation resultListSerInfo)
            throws JDipException {
        if (resultListSerInfo == null || !resultListSerInfo.isCollection() ||
                !"java.util.ArrayList".equals(resultListSerInfo.getClassName())) {
            return Optional.empty();
        }
        CollectionInformation resultListInfo = (CollectionInformation)resultListSerInfo;

        List<Result> results = new LinkedList<>();
        for (SerializeInformation resultListEntrySerInfo: resultListInfo.getCollectionEntries()) {
            results.add(extractResult(resultListEntrySerInfo)
                    .orElseThrow(() -> new JDipException("Expected a result list to contain a result")));
        }

        return Optional.of(results);
    }

    private Optional<Result> extractResult(SerializeInformation resultSerInfo) throws JDipException {
        if (resultSerInfo == null || !resultSerInfo.isObject()) {
            return Optional.empty();
        }
        ObjectInformation resultInfo = (ObjectInformation)resultSerInfo;

        Power power = extractPower(resultInfo.getAttribute("power")).orElse(null);
        String message = extractString(resultInfo.getAttribute("message")).orElse(null);
        Optional<Result> result;
        switch (resultInfo.getClassName()) {
            case "dip.order.result.Result":
                result = Optional.of(new Result(power, message));
                break;
            case "dip.order.result.TimeResult":
                result = Optional.of(new TimeResult(power, message,
                        extractLong(resultInfo.getAttribute("timeStamp"))
                                .orElseThrow(() -> new JDipException("Expected a time result to contain a timeStamp")))
                );
                break;
            case "dip.order.result.OrderResult":
                result = Optional.of(new OrderResult(
                        extractOrder(resultInfo.getAttribute("order"))
                                .orElseThrow(() -> new JDipException("Expected an order result to contain an order")),
                        extractResultType(resultInfo.getAttribute("resultType"))
                                .orElseThrow(() -> new JDipException(
                                        "Expected an order result to contain an'result type")),
                        message));
                break;
            case "dip.order.result.BouncedResult":
                result = extractBouncedResult(resultInfo);
                break;
            case "dip.order.result.ConvoyPathResult":
                result = Optional.of(new ConvoyPathResult(extractOrder(resultInfo.getAttribute("order"))
                            .orElseThrow(() -> new JDipException("Expected a convoy path result to contain an order")),
                        extractProvinces(resultInfo.getAttribute("convoyPath"))
                            .orElseThrow(() -> new JDipException(
                                "Expected a convoy path result to contain a convoy path"))
                ));
                break;
            case "dip.order.result.DependentMoveFailedResult":
                result = Optional.of(new DependentMoveFailedResult(
                        extractOrder(resultInfo.getAttribute("order"))
                            .orElseThrow(() -> new JDipException(
                                "Expected a dependent move failed result to contain an order")),
                        extractOrder(resultInfo.getAttribute("dependentOrder"))
                            .orElseThrow(() -> new JDipException(
                                    "Expected a dependent move failed result to contain a dependentOrder"))
                ));
                break;
            case "dip.order.result.DislodgedResult":
                result = extractDislodgedResult(resultInfo);
                break;
            case "dip.order.result.SubstitutedResult":
                result = Optional.of(new SubstitutedResult(
                        extractOrder(resultInfo.getAttribute("order")).orElse(null),
                        extractOrder(resultInfo.getAttribute("newOrder"))
                            .orElseThrow(() -> new JDipException(
                                "Expected a substituted result to contain a new order")),
                        message));
                break;
            default:
                throw new JDipException("The class of the result list entry is unknown");
        }
        return result;
    }

    private Optional<Result> extractBouncedResult(SerializeInformation bouncedResultSerInfo) throws JDipException {
        if (bouncedResultSerInfo == null || !bouncedResultSerInfo.isObject() ||
                !"dip.order.result.BouncedResult".equals(bouncedResultSerInfo.getClassName())) {
            return Optional.empty();
        }
        ObjectInformation bouncedResultInfo = (ObjectInformation)bouncedResultSerInfo;

        BouncedResult result = new BouncedResult(extractOrder(bouncedResultInfo.getAttribute("order"))
                .orElseThrow(() -> new JDipException("Expected a bounced result to contain an order")));
        result.setBouncer(extractProvince(bouncedResultInfo.getAttribute("bouncer"))
                .orElseThrow(() -> new JDipException("Expected a bounced result to contain a bouncer")));
        result.setAttackStrength(extractInt(bouncedResultInfo.getAttribute("atkStrength"))
                .orElseThrow(() -> new JDipException("Expected a bounced result to contain an atkStrength")));
        result.setDefenseStrength(extractInt(bouncedResultInfo.getAttribute("defStrength"))
                .orElseThrow(() -> new JDipException("Expected a bounced result to contain a defStrength")));
        return Optional.of(result);
    }

    private Optional<Result> extractDislodgedResult(SerializeInformation dislodgedResultSerInfo)
            throws JDipException {
        if (dislodgedResultSerInfo == null || !dislodgedResultSerInfo.isObject() ||
                !"dip.order.result.DislodgedResult".equals(dislodgedResultSerInfo.getClassName())) {
            return Optional.empty();
        }
        ObjectInformation dislodgedResultInfo = (ObjectInformation)dislodgedResultSerInfo;

        DislodgedResult result = new DislodgedResult(
                extractOrder(dislodgedResultInfo.getAttribute("order"))
                    .orElseThrow(() -> new JDipException("Expected a dislodged result to contain an order")),
                extractString(dislodgedResultInfo.getAttribute("message")).orElse(null),
                extractLocations(dislodgedResultInfo.getAttribute("retreatLocations")).orElse(null)
        );
        result.setDislodger(extractProvince(dislodgedResultInfo.getAttribute("dislodger")).orElse(null));
        result.setAttackStrength(extractInt(dislodgedResultInfo.getAttribute("atkStrength"))
                .orElseThrow(() -> new JDipException("Expected a dislodged result to contain an atkStrength")));
        result.setDefenseStrength(extractInt(dislodgedResultInfo.getAttribute("defStrength"))
                .orElseThrow(() -> new JDipException("Expected a dislodged result to contain a defStrength")));

        return Optional.of(result);
    }

    private Optional<OrderResult.ResultType> extractResultType(SerializeInformation resultTypeSerInfo)
            throws JDipException {
        if (resultTypeSerInfo == null || !resultTypeSerInfo.isObject() ||
                !"dip.order.result.OrderResult$ResultType".equals(resultTypeSerInfo.getClassName())) {
            return Optional.empty();
        }
        ObjectInformation resultTypeInfo = (ObjectInformation)resultTypeSerInfo;

        int ordering = extractInt(resultTypeInfo.getAttribute("ordering"))
                .orElseThrow(() -> new JDipException("Expected a result type to contain an ordering"));
        switch (ordering) {
            case 10:
                return Optional.of(OrderResult.ResultType.VALIDATION_FAILURE);
            case 20:
                return Optional.of(OrderResult.ResultType.SUCCESS);
            case 30:
                return Optional.of(OrderResult.ResultType.FAILURE);
            case 40:
                return Optional.of(OrderResult.ResultType.DISLODGED);
            case 50:
                return Optional.of(OrderResult.ResultType.CONVOY_PATH_TAKEN);
            case 60:
                return Optional.of(OrderResult.ResultType.TEXT);
            case 70:
                return Optional.of(OrderResult.ResultType.SUBSTITUTED);
            default:
                return Optional.empty();
        }
    }

    private Optional<Position> extractPosition(SerializeInformation positionSerInfo)
            throws JDipException {
        if (positionSerInfo == null || !positionSerInfo.isObject()||
                !"dip.world.Position".equals(positionSerInfo.getClassName())) {
            return Optional.empty();
        }
        ObjectInformation positionInfo = (ObjectInformation)positionSerInfo;

        info.jdip.world.Map positionMap = extractMap(positionInfo.getAttribute("map"))
                .orElseThrow(() -> new JDipException("Expected a position to contain a map"));
        Position position = new Position(positionMap);

        // The following code has another structure than the other methods because we extract a private internal class
        // and cannot set directly the data
        SerializeInformation provArraySerInfo = positionInfo.getAttribute("provArray");
        if (provArraySerInfo == null || !provArraySerInfo.isCollection() ||
                !"dip.world.Position$ProvinceData".equals(provArraySerInfo.getClassName())) {
            throw new JDipException("Expected a position to contain a provArray");
        }
        CollectionInformation provArrayInfo = (CollectionInformation)provArraySerInfo;
        for (int i = 0; i < provArrayInfo.getCollectionEntries().size(); i++) {
            SerializeInformation provinceDataSerInfo = provArrayInfo.getCollectionEntries().get(i);
            if (provinceDataSerInfo != null && provinceDataSerInfo.isObject() &&
                    "dip.world.Position$ProvinceData".equals(provinceDataSerInfo.getClassName())) {
                ObjectInformation provinceDataInfo = (ObjectInformation)provinceDataSerInfo;
                Province province = positionMap.getProvinces()[i];

                Optional<Unit> unit = extractUnit(provinceDataInfo.getAttribute("unit"));
                if (unit.isPresent()) {
                    position.setUnit(province, unit.get());
                }

                Optional<Unit> dislodgedUnit = extractUnit(provinceDataInfo.getAttribute("dislodgedUnit"));
                if (dislodgedUnit.isPresent()) {
                    position.setDislodgedUnit(province, dislodgedUnit.get());
                }

                Optional<Power> scOwner = extractPower(provinceDataInfo.getAttribute("SCOwner"));
                if (scOwner.isPresent()) {
                    position.setSupplyCenterOwner(province, scOwner.get());
                }

                Optional<Power> scHomePower = extractPower(provinceDataInfo.getAttribute("SCHomePower"));
                if (scHomePower.isPresent()) {
                    position.setSupplyCenterHomePower(province, scHomePower.get());
                }

                Optional<Power> lastOccupier = extractPower(provinceDataInfo.getAttribute("lastOccupier"));
                if (lastOccupier.isPresent()) {
                    position.setLastOccupier(province, lastOccupier.get());
                }
            } // else if the data is null then we can safely ignore the data
        }

        SerializeInformation powerMapSerInfo = positionInfo.getAttribute("powerMap");
        if (powerMapSerInfo == null || !powerMapSerInfo.isMap() ||
                !"java.util.HashMap".equals(powerMapSerInfo.getClassName())) {
            throw new JDipException("Expected a position to contain a power map");
        }
        MapInformation powerMapInfo = (MapInformation)powerMapSerInfo;
        for (Map.Entry<SerializeInformation, SerializeInformation> powerMapEntry: powerMapInfo.getMap().entrySet()) {
            Power power = extractPower(powerMapEntry.getKey())
                    .orElseThrow(() -> new JDipException("Expected the key of a power map to be a power"));
            SerializeInformation powerDataSerInfo = powerMapEntry.getValue();
            if (powerDataSerInfo == null || !powerDataSerInfo.isObject() ||
                    !"dip.world.Position$PowerData".equals(powerDataSerInfo.getClassName())) {
                throw new JDipException("Expected the value of a power map to be a power data");
            }
            ObjectInformation powerDataInfo = (ObjectInformation)powerDataSerInfo;
            Boolean isEliminated = extractBoolean(powerDataInfo.getAttribute("isEliminated"))
                    .orElseThrow(() -> new JDipException("Expected a power data to contain a isEliminated flag"));
            position.setEliminated(power, isEliminated);
        }

        return Optional.of(position);
    }

    private Optional<Unit> extractUnit(SerializeInformation unitSerInfo) throws JDipException {
        if (unitSerInfo == null || !unitSerInfo.isObject() || !"dip.world.Unit".equals(unitSerInfo.getClassName())) {
            return Optional.empty();
        }
        ObjectInformation unitInfo = (ObjectInformation)unitSerInfo;

        Unit unit = new Unit(extractPower(unitInfo.getAttribute("owner"))
                    .orElseThrow(() -> new JDipException("Expected a unit to contain an owner")),
                extractUnitType(unitInfo.getAttribute("type"))
                    .orElseThrow(() -> new JDipException("Expected a unit to contain a type")));
        unit.setCoast(extractCoast(unitInfo.getAttribute("coast"))
                .orElseThrow(() -> new JDipException("Expected a unit to contain a coast")));
        return Optional.of(unit);
    }

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

    private Optional<Long> extractLong(SerializeInformation longSerInfo) throws JDipException {
        if (longSerInfo == null || !longSerInfo.isPrimitive() || ! "long".equals(longSerInfo.getClassName())) {
            Optional.empty();
        }
        return Optional.of(Long.valueOf(((PrimitiveInformation)longSerInfo).getValue()));
    }

    private Optional<Boolean> extractBooleanObject(SerializeInformation booleanSerInfo) throws JDipException {
        if (booleanSerInfo == null || !booleanSerInfo.isObject()||
                ! "java.lang.Boolean".equals(booleanSerInfo.getClassName())) {
            return Optional.empty();
        }
        ObjectInformation booleanInfo = (ObjectInformation)booleanSerInfo;

        return extractBoolean(booleanInfo.getAttribute("value"));
    }

    private Optional<Boolean> extractBoolean(SerializeInformation booleanSerInfo) throws JDipException {
        if (booleanSerInfo == null || !booleanSerInfo.isPrimitive() ||
                ! "boolean".equals(booleanSerInfo.getClassName())) {
            return Optional.empty();
        }
        return Optional.of(Boolean.valueOf(((PrimitiveInformation)booleanSerInfo).getValue()));
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
