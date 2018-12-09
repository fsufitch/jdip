package info.jdip.test.loading;

import dip.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class WorldComparator {
    private static final Logger logger = LoggerFactory.getLogger(WorldComparator.class);

    static Set<Object> visited;

    static boolean compareWorlds(World legacyWorld, info.jdip.world.World newWorld) throws Exception {
        visited = new HashSet<>();
        return deepCompare("root", legacyWorld, newWorld);
    }

    private static boolean deepCompare(String path, Object legacyObject, Object newObject) throws IllegalAccessException {
        if (visited.contains(legacyObject)) {
            return true;
        }
        if (legacyObject == null) {
            return newObject == null;
        }
        if (newObject == null) {
            return false;
        }
        visited.add(legacyObject);
        if (legacyObject instanceof String) {
            return String.valueOf(legacyObject).compareToIgnoreCase(String.valueOf(newObject)) == 0;
        }
        if (primitiveType(legacyObject)) {
            return legacyObject.equals(newObject);
        }
        if (legacyObject.getClass().isArray()) {
            return compareArrays(path, legacyObject, newObject);
        }
        if (legacyObject instanceof Map) {
            return compareMaps(path, (Map) legacyObject, (Map) newObject);
        }
        if (legacyObject instanceof List){
            return compareLists(path, (List) legacyObject, (List)newObject);
        }
        boolean result = true;
        Field[] fields = legacyObject.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (Modifier.isTransient(field.getModifiers())) {
                continue;
            }
            String fieldPath = path + "." + field.getName();
            field.setAccessible(true);
            Object legacyFieldValue = field.get(legacyObject);
            try {
                Field newField = newObject.getClass().getDeclaredField(field.getName());
                newField.setAccessible(true);
                Object newFieldValue = newField.get(newObject);
                if (!deepCompare(fieldPath, legacyFieldValue, newFieldValue)) {
                    if (isNotIgnored(field)) {
                        logger.warn("{} does not match. Legacy ({}): {} New ({}): {}",
                                fieldPath, field.getType(), legacyFieldValue, newField.getType(), newFieldValue);
                        result = false;
                    }
                }
            } catch (NoSuchFieldException e) {
                if (isNotIgnored(field)) {
                    logger.warn("{} is missing", fieldPath);
                    result = false;
                }
            }
        }
        return result;
    }

    private static boolean compareLists(String path, List legacyList, List newList) throws IllegalAccessException {
        if (legacyList.size() != newList.size()) {
            logger.warn("lists have different size {}", path);
            return false;
        }
        boolean result = true;
        for (int i = 0; i < legacyList.size(); i++) {
            String fieldPath = path + "[" + i + "]";
            if(!deepCompare(fieldPath,legacyList.get(i),newList.get(i))){
                logger.warn("{} does not match. Legacy: {} New: {}",
                        fieldPath, legacyList.get(i), newList.get(i));
                result = false;
            }
        }
        return result;
    }

    private static boolean isNotIgnored(Field field) {
        Annotation[] annotations = field.getDeclaredAnnotations();

        for (Annotation annotation : annotations) {
            if (annotation instanceof IgnoreComparisonResult) {
                return false;
            }
        }
        return true;
    }

    private static boolean compareMaps(String path, Map legacyObject, Map newObject) throws IllegalAccessException {
        Set legacyKeys = legacyObject.keySet();
        Set newKeys = newObject.keySet();

        boolean result = true;
        for (Object legacyKey : legacyKeys) {
            boolean found = false;
            for (Object newKey : newKeys) {
                if (deepCompare(String.valueOf(newKey), legacyKey, newKey)) {
                    found = true;
                    if (!deepCompare(path + "[" + newKey + "]", legacyObject.get(legacyKey), newObject.get(newKey))) {
                        result = false;
                    }
                    break;
                }
            }
            if (!found) {
                logger.warn("Could not find key {}", legacyKey);
                result = false;
            }
        }
        return result;
    }

    private static boolean compareArrays(String path, Object legacyObject, Object newObject) throws IllegalAccessException {
        int length = Array.getLength(legacyObject);
        if (length != Array.getLength(newObject)) {
            return false;
        }
        boolean result = true;
        for (int i = 0; i < length; i++) {
            Object legacyElement = Array.get(legacyObject, i);
            Object newElement = Array.get(newObject, i);
            String fieldPath = path + "[" + i + "]";
            boolean result1 = deepCompare(fieldPath, legacyObject, newObject);
            if (result1 == false) {
                logger.warn("Field {} does not match. Legacy ({}): {} New ({}): {}",
                        fieldPath, legacyObject.getClass().getComponentType(), newObject.getClass().getComponentType(), newElement);
                result = false;
            }
        }
        return result;
    }

    private static boolean primitiveType(Object legacyObject) {
        return legacyObject instanceof Integer || legacyObject instanceof Boolean || legacyObject instanceof Long || legacyObject instanceof Float;
    }
}
