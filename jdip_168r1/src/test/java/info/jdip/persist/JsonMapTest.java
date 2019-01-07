/*
 * jDip - a Java Diplomacy Adjudicator and Mapper
 * Copyright (C) 2019 Uwe Plonus and the jDip development team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package info.jdip.persist;

import com.fasterxml.jackson.databind.ObjectMapper;
import info.jdip.world.InvalidWorldException;
import info.jdip.world.Map;
import info.jdip.world.Power;
import info.jdip.world.Province;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import org.json.JSONException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

/**
 *
 * @author Uwe Plonus
 */
public class JsonMapTest {

    private ObjectMapper mapper;

    private StringWriter writer;

    @BeforeEach
    public void setUp() {
        mapper = new ObjectMapper();
        writer = new StringWriter();
    }

    private Map createMap(int countPowers) throws InvalidWorldException {
        List<Power> powers = new LinkedList<>();
        if (countPowers > 0) {
            for (int i = 0; i < countPowers; i++) {
                String name = new StringBuilder("Test").append(i).toString();
                Power power = new Power(new String[]{name}, new StringBuilder(name).append("ish").toString(), true);
                powers.add(power);
            }
        }
        return new Map(powers.toArray(new Power[countPowers]), new Province[0]);
    }

    @Test
    public void testCreatePower() throws IOException, JSONException {
        JsonMap map = new JsonMap();
        mapper.writeValue(writer, map);
        JSONAssert.assertEquals(new StringBuilder()
                .append("{")
                .append(  "\"$id\":\"").append(map.getId()).append("\",")
                .append(  "\"powers\":[")
                .append(  "]")
                .append("}")
                .toString(),
                writer.toString(), JSONCompareMode.STRICT);
    }

    @Test
    public void testPersistMap() throws IOException, JSONException, InvalidWorldException {
        JsonMap map = new JsonMap(createMap(0));
        mapper.writeValue(writer, map);
        JSONAssert.assertEquals(new StringBuilder()
                .append("{")
                .append(  "\"$id\":\"").append(map.getId()).append("\",")
                .append(  "\"powers\":[")
                .append(  "]")
                .append("}")
                .toString(),
                writer.toString(), JSONCompareMode.STRICT);
    }

    @Test
    public void testPersistMapWithSinglePower() throws IOException, JSONException, InvalidWorldException {
        JsonMap map = new JsonMap(createMap(1));
        mapper.writeValue(writer, map);
        JSONAssert.assertEquals(new StringBuilder()
                .append("{")
                .append(  "\"$id\":\"").append(map.getId()).append("\",")
                .append(  "\"powers\":[")
                .append(    "{")
                .append(      "\"$id\":\"").append(map.getPowers().get(0).getId()).append("\",")
                .append(      "\"names\":[")
                .append(        "\"Test0\"")
                .append(      "],")
                .append(      "\"adjective\":\"Test0ish\",")
                .append(      "\"isActive\":true")
                .append(    "}")
                .append(  "]")
                .append("}")
                .toString(),
                writer.toString(), JSONCompareMode.STRICT);
    }

    @Test
    public void testPersistMapWith2Powers() throws IOException, JSONException, InvalidWorldException {
        JsonMap map = new JsonMap(createMap(2));
        mapper.writeValue(writer, map);
        JSONAssert.assertEquals(new StringBuilder()
                .append("{")
                .append(  "\"$id\":\"").append(map.getId()).append("\",")
                .append(  "\"powers\":[")
                .append(    "{")
                .append(      "\"$id\":\"").append(map.getPowers().get(0).getId()).append("\",")
                .append(      "\"names\":[")
                .append(        "\"Test0\"")
                .append(      "],")
                .append(      "\"adjective\":\"Test0ish\",")
                .append(      "\"isActive\":true")
                .append(    "},")
                .append(    "{")
                .append(      "\"$id\":\"").append(map.getPowers().get(1).getId()).append("\",")
                .append(      "\"names\":[")
                .append(        "\"Test1\"")
                .append(      "],")
                .append(      "\"adjective\":\"Test1ish\",")
                .append(      "\"isActive\":true")
                .append(    "}")
                .append(  "]")
                .append("}")
                .toString(),
                writer.toString(), JSONCompareMode.STRICT);
    }

    @Test
    public void testLoadEmptyMap() throws IOException, JSONException, InvalidWorldException {
        String id = UUID.randomUUID().toString();
        String jsonMap = new StringBuilder()
                .append("{")
                .append(  "\"$id\":\"").append(id).append("\",")
                .append(  "\"powers\":[")
                .append(  "]")
                .append("}")
                .toString();
        JsonMap map = mapper.readValue(new StringReader(jsonMap), JsonMap.class);

        Assertions.assertEquals(id, map.getId());
        Map loadedMap = map.getMap();
        Assertions.assertNotNull(loadedMap);
        Assertions.assertEquals(0, loadedMap.getPowers().length);
    }

    @Test
    public void testLoadMapWithSinglePower() throws IOException, JSONException, InvalidWorldException {
        String mapId = UUID.randomUUID().toString();
        String powerId = UUID.randomUUID().toString();
        String jsonMap = new StringBuilder()
                .append("{")
                .append(  "\"$id\":\"").append(mapId).append("\",")
                .append(  "\"powers\":[")
                .append(    "{")
                .append(      "\"$id\":\"").append(powerId).append("\",")
                .append(      "\"names\":[")
                .append(        "\"Test\"")
                .append(      "],")
                .append(      "\"adjective\":\"Testish\",")
                .append(      "\"isActive\":true")
                .append(    "}")
                .append(  "]")
                .append("}")
                .toString();
        JsonMap map = mapper.readValue(new StringReader(jsonMap), JsonMap.class);

        Assertions.assertEquals(mapId, map.getId());
        Map loadedMap = map.getMap();
        Assertions.assertNotNull(loadedMap);
        Assertions.assertEquals(1, loadedMap.getPowers().length);
        Assertions.assertNotNull(loadedMap.getPowers()[0]);
        Assertions.assertEquals("Test", loadedMap.getPowers()[0].getName());
    }

    @Test
    public void testLoadMapWithTwoPowers() throws IOException, JSONException, InvalidWorldException {
        String mapId = UUID.randomUUID().toString();
        String power0Id = UUID.randomUUID().toString();
        String power1Id = UUID.randomUUID().toString();
        String jsonMap = new StringBuilder()
                .append("{")
                .append(  "\"$id\":\"").append(mapId).append("\",")
                .append(  "\"powers\":[")
                .append(    "{")
                .append(      "\"$id\":\"").append(power0Id).append("\",")
                .append(      "\"names\":[")
                .append(        "\"Test0\"")
                .append(      "],")
                .append(      "\"adjective\":\"Test0ish\",")
                .append(      "\"isActive\":true")
                .append(    "},")
                .append(    "{")
                .append(      "\"$id\":\"").append(power1Id).append("\",")
                .append(      "\"names\":[")
                .append(        "\"Test1\"")
                .append(      "],")
                .append(      "\"adjective\":\"Test1ish\",")
                .append(      "\"isActive\":true")
                .append(    "}")
                .append(  "]")
                .append("}")
                .toString();
        JsonMap map = mapper.readValue(new StringReader(jsonMap), JsonMap.class);

        Assertions.assertEquals(mapId, map.getId());
        Map loadedMap = map.getMap();
        Assertions.assertNotNull(loadedMap);
        Assertions.assertEquals(2, loadedMap.getPowers().length);
        Assertions.assertNotNull(loadedMap.getPowers()[0]);
        Assertions.assertEquals("Test0", loadedMap.getPowers()[0].getName());
        Assertions.assertNotNull(loadedMap.getPowers()[1]);
        Assertions.assertEquals("Test1", loadedMap.getPowers()[1].getName());
    }

}
