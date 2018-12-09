package info.jdip.test.loading;

import dip.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

public class WorldComparator {
    private static final Logger logger = LoggerFactory.getLogger(WorldComparator.class);

    static Set<Object> visited;

    static boolean  compareWorlds(World legacyWorld, info.jdip.world.World newWorld) throws Exception {
        visited = new HashSet<>();
        return deepCompare("root",legacyWorld, newWorld);
    }

    private static boolean deepCompare(String path,Object legacyObject, Object newObject) throws IllegalAccessException {
        if (visited.contains(legacyObject)){
            return true;
        }
        if (legacyObject==null){
            return newObject == null;
        }
        if (newObject == null){
            return false;
        }
        visited.add(legacyObject);
        if (primitiveType(legacyObject)){
            return legacyObject.equals(newObject);
        }
        boolean result = true;
        Field[] fields = legacyObject.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (Modifier.isTransient(field.getModifiers())) {
                continue;
            }
            String fieldPath = path + "." + field.getName();
            logger.debug("Checking field {}", fieldPath);
            field.setAccessible(true);
            Object legacyFieldValue = field.get(legacyObject);
            Field newField = null;
            try {
                newField = newObject.getClass().getDeclaredField(field.getName());
                newField.setAccessible(true);
                Object newFieldValue = newField.get(newObject);
                if (! deepCompare(fieldPath,legacyFieldValue,newFieldValue)){
                    logger.warn("Field {} does not match. Legacy type: {} Legacy value: {} NewType: {} NewValue: {}",
                            fieldPath,field.getType(),legacyFieldValue,newField.getType(),newFieldValue);
                    result=false;
                }
            } catch (NoSuchFieldException e) {
                logger.warn("Field {} missing from new Word", fieldPath);
            }
        }
        return result;
    }

    private static boolean primitiveType(Object legacyObject) {
        return legacyObject instanceof String;
    }
}
