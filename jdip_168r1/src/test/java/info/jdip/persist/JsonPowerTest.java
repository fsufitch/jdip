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
import info.jdip.world.Power;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.UUID;
import org.json.JSONException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

public class JsonPowerTest {

    private ObjectMapper mapper;

    private StringWriter writer;

    @BeforeEach
    public void setUp() {
        mapper = new ObjectMapper();
        writer = new StringWriter();
    }

    private Power createPower() {
        return new Power(new String[]{"Test"}, "Testish", true);
    }

    @Test
    public void testCreatePower() throws IOException, JSONException {
        JsonPower power = new JsonPower();
        mapper.writeValue(writer, power);
        JSONAssert.assertEquals(new StringBuilder()
                .append("{")
                .append(  "\"$id\":\"").append(power.getId()).append("\",")
                .append(  "\"names\":[")
                .append(  "],")
                .append(  "\"adjective\":null,")
                .append(  "\"isActive\":false")
                .append("}")
                .toString(),
                writer.toString(), JSONCompareMode.STRICT);
    }

    @Test
    public void testPersistPower() throws IOException, JSONException {
        JsonPower power = new JsonPower(createPower());
        mapper.writeValue(writer, power);
        JSONAssert.assertEquals(new StringBuilder()
                .append("{")
                .append(  "\"$id\":\"").append(power.getId()).append("\",")
                .append(  "\"names\":[")
                .append(    "\"Test\"")
                .append(  "],")
                .append(  "\"adjective\":\"Testish\",")
                .append(  "\"isActive\":true")
                .append("}")
                .toString(),
                writer.toString(), JSONCompareMode.STRICT);
    }

    @Test
    public void testLoadPower() throws IOException, JSONException {
        UUID id = UUID.randomUUID();
        String jsonPower = new StringBuilder()
                .append("{")
                .append(  "\"$id\":\"").append(id).append("\",")
                .append(  "\"names\":[")
                .append(    "\"Test\"")
                .append(  "],")
                .append(  "\"adjective\":\"Testish\",")
                .append(  "\"isActive\":true")
                .append("}")
                .toString();
        JsonPower power = mapper.readValue(new StringReader(jsonPower), JsonPower.class);

        Assertions.assertEquals(id.toString(), power.getId());
        Power loadedPower = power.getPower();
        Assertions.assertNotNull(loadedPower);
        Assertions.assertArrayEquals(new String[]{"Test"}, loadedPower.getNames());
        Assertions.assertEquals("Testish", loadedPower.getAdjective());
        Assertions.assertTrue(loadedPower.isActive());
    }

}
