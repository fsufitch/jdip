package info.jdip.test.loading;

import dip.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;

public class WorldComparator {
    private static final Logger logger = LoggerFactory.getLogger(WorldComparator.class);

    static boolean  compareWorlds(World legacyWorld, info.jdip.world.World newWorld) throws Exception {

        return deepCompare(legacyWorld, newWorld);
    }

    private static boolean deepCompare(Object legacyObject, Object newObject) throws Exception {
        if (primitiveType(legacyObject)){
            return legacyObject.equals(newObject);
        }
        boolean result = true;
        Field[] fields = legacyObject.getClass().getFields();
        for (Field field : fields) {
            Object legacyFieldValue = field.get(legacyObject);
            Object newFieldValue = newObject.getClass().getField(field.getName());
            if (! deepCompare(legacyFieldValue,newFieldValue)){
                logger.warn("Field {} does not match. Legacy type: {} Legacy value: {} NewType: {} NewValue: {}");
                result=false;
            }
        }
        return result;
    }

    private static boolean primitiveType(Object legacyObject) {
        return legacyObject instanceof String;
    }
}
