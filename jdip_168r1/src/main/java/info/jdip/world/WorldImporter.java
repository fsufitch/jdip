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

/**
 * This class is not thread safe.
 */
public class WorldImporter {

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

    private void handleObject(StartElement startElement) throws XMLStreamException {
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
            if (enclosingObject.isMap()) {
                MapInformation map = (MapInformation)enclosingObject;
                if (map.hasKey()) {
                    map.addValue(objectInformation);
                } else {
                    map.addKey(objectInformation);
                }
            } else if (enclosingObject.isCollection()) {
                CollectionInformation collection = (CollectionInformation)enclosingObject;
                collection.addValue(objectInformation);
            } else {
                if (fieldAttr != null) {
                    enclosingObject.addAttribute(fieldAttr.getValue(), objectInformation);
                }
            }
        }
        objectStack.addFirst(objectInformation);
        objectLookup.put(startElement.getAttributeByName(new QName("id")).getValue(),
                objectInformation);
    }

    private void handleReference(StartElement startElement) throws XMLStreamException {
        String idref = startElement.getAttributeByName(new QName("idref")).getValue();
        ObjectInformation objectInformation = objectLookup.get(idref);
        Attribute fieldAttr = startElement.getAttributeByName(new QName("field"));
        if (!objectStack.isEmpty()) {
            ObjectInformation enclosingObject = objectStack.peekFirst();
            if (enclosingObject.isMap()) {
                MapInformation map = (MapInformation)enclosingObject;
                if (map.hasKey()) {
                    map.addValue(objectInformation);
                } else {
                    map.addKey(objectInformation);
                }
            } else if (enclosingObject.isCollection()) {
                CollectionInformation collection = (CollectionInformation)enclosingObject;
                collection.addValue(objectInformation);
            } else {
                if (fieldAttr != null) {
                    enclosingObject.addAttribute(fieldAttr.getValue(), objectInformation);
                }
            }
        }
    }

    private void handleCollection(StartElement startElement) throws XMLStreamException {
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
            if (enclosingObject.isMap()) {
                MapInformation map = (MapInformation)enclosingObject;
                if (map.hasKey()) {
                    map.addValue(objectInformation);
                } else {
                    map.addKey(objectInformation);
                }
            } else if (enclosingObject.isCollection()) {
                CollectionInformation collection = (CollectionInformation)enclosingObject;
                collection.addValue(objectInformation);
            } else {
                if (fieldAttr != null) {
                    enclosingObject.addAttribute(fieldAttr.getValue(), objectInformation);
                }
            }
        }
        objectStack.addFirst(objectInformation);
        objectLookup.put(startElement.getAttributeByName(new QName("id")).getValue(),
                objectInformation);
    }

    private void handlePrimitive(StartElement startElement) throws XMLStreamException {
        String elementName = startElement.getName().getLocalPart();
        String typeName;
        if ("string".equals(elementName)) {
            typeName = "string";
        } else {
            typeName = startElement.getAttributeByName(new QName("type")).getValue();
        }
        PrimitiveInformation objectInformation = new PrimitiveInformation(typeName);
        Attribute fieldAttr = startElement.getAttributeByName(new QName("field"));
        if (!objectStack.isEmpty()) {
            ObjectInformation enclosingObject = objectStack.peekFirst();
            if (enclosingObject.isCollection()) {
                CollectionInformation collection = (CollectionInformation)enclosingObject;
                collection.addValue(objectInformation);
            } else {
                if (fieldAttr != null) {
                    enclosingObject.addAttribute(fieldAttr.getValue(), objectInformation);
                }
            }
        }
        objectInformation.setValue(startElement.getAttributeByName(new QName("value")).getValue());
    }

    private void handleNull(StartElement startElement) throws XMLStreamException {
        ObjectInformation objectInformation = new ObjectInformation("null");
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

        return new World(map);
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

            String adjective;
            SerializeInformation adjectiveSerInfo = powerInfo.getAttribute("adjective");
            if (adjectiveSerInfo == null || !activeSerInfo.isPrimitive() ||
                    !"string".equals(adjectiveSerInfo.getClassName())) {
                throw new JDipException("Expected a power to contain a string adjective");
            }
            PrimitiveInformation adjectiveInfo = (PrimitiveInformation)adjectiveSerInfo;
            adjective = adjectiveInfo.getValue();

            powers.put(powerInfo.getUuid(), new Power(names.toArray(new String[names.size()]), adjective, isActive));
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

            SerializeInformation fullNameSerInfo = provinceInfo.getAttribute("fullName");
            if (fullNameSerInfo == null || !fullNameSerInfo.isPrimitive()||
                    !"string".equals(fullNameSerInfo.getClassName())) {
                throw new JDipException("Expected a province to contain a string fullName");
            }
            PrimitiveInformation fullNameInfo = (PrimitiveInformation)fullNameSerInfo;
            String fullName = fullNameInfo.getValue();

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

            SerializeInformation indexSerInfo = provinceInfo.getAttribute("index");
            if (indexSerInfo == null || !indexSerInfo.isPrimitive()||
                    !"int".equals(indexSerInfo.getClassName())) {
                throw new JDipException("Expected a province to contain an int index");
            }
            PrimitiveInformation indexInfo = (PrimitiveInformation)indexSerInfo;
            int index = Integer.parseInt(indexInfo.getValue());

            SerializeInformation isConvoyableCoastSerInfo = provinceInfo.getAttribute("isConvoyableCoast");
            if (isConvoyableCoastSerInfo == null || !isConvoyableCoastSerInfo.isPrimitive()||
                    !"boolean".equals(isConvoyableCoastSerInfo.getClassName())) {
                throw new JDipException("Expected a province to contain a boolean isConvoyableCoast");
            }
            PrimitiveInformation isConvoyableCoastInfo = (PrimitiveInformation)isConvoyableCoastSerInfo;
            boolean isConvoyableCoast = Boolean.parseBoolean(isConvoyableCoastInfo.getValue());

            SerializeInformation supplyCenterSerInfo = provinceInfo.getAttribute("supplyCenter");
            if (supplyCenterSerInfo == null || !supplyCenterSerInfo.isPrimitive()||
                    !"boolean".equals(supplyCenterSerInfo.getClassName())) {
                throw new JDipException("Expected a province to contain a boolean supplyCenter");
            }
            PrimitiveInformation supplyCenterInfo = (PrimitiveInformation)supplyCenterSerInfo;
            boolean supplyCenter = Boolean.parseBoolean(supplyCenterInfo.getValue());

            Province province = new Province(fullName, shortNames.toArray(new String[shortNames.size()]), index,
                    isConvoyableCoast);
            province.setSupplyCenter(supplyCenter);
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

            for (Map.Entry<ObjectInformation, ObjectInformation> adjLocEntry: adjLocInfo.getMap().entrySet()) {
                ObjectInformation adjLocKey = adjLocEntry.getKey();
                if (adjLocKey == null || !"dip.world.Coast".equals(adjLocKey.getClassName())) {
                    throw new JDipException("Expected the key of the adjLoc entry to be a dip.world.Coast");
                }
                ObjectInformation adjLocValue = adjLocEntry.getValue();
                if (adjLocValue == null || !adjLocValue.isCollection() ||
                        !"dip.world.Location".equals(adjLocValue.getClassName())) {
                    throw new JDipException(
                            "Expected the value of the adjLoc entry to be a collection of dip.world.Location");
                }
                CollectionInformation adjLocLocations = (CollectionInformation)adjLocValue;

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
        SerializeInformation indexSerInfo = coastInfo.getAttribute("index");
        if (indexSerInfo == null || !indexSerInfo.isPrimitive()||
                !"int".equals(indexSerInfo.getClassName())) {
            throw new JDipException("Expected a coast to contain an int index");
        }
        PrimitiveInformation indexInfo = (PrimitiveInformation)indexSerInfo;
        int index = Integer.parseInt(indexInfo.getValue());

        return Coast.getCoast(index);
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

        Map<ObjectInformation, ObjectInformation> encapsulatedMap;

        private ObjectInformation key;

        public MapInformation(String className) {
            super(className);
            this.encapsulatedMap = new LinkedHashMap<>();
            key = null;
        }

        @Override
        public boolean isObject() {
            return false;
        }

        @Override
        public boolean isMap() {
            return true;
        }

        public boolean hasKey() {
            return key != null;
        }

        public void addKey(ObjectInformation key) {
            this.key = key;
        }

        public ObjectInformation addValue(ObjectInformation value) {
            ObjectInformation returnValue = null;
            if (key == null) {
                throw new IllegalStateException("Try to add value without key.");
            } else {
                returnValue = this.encapsulatedMap.put(key, value);
                this.key = null;
            }
            return returnValue;
        }

        public Map<ObjectInformation, ObjectInformation> getMap() {
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
