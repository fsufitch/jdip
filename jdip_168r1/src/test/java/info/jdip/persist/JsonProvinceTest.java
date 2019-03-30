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
import info.jdip.world.Province;
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

public class JsonProvinceTest {

    private ObjectMapper mapper;

    private StringWriter writer;

    @BeforeEach
    public void setUp() {
        mapper = new ObjectMapper();
        writer = new StringWriter();
    }

    @Test
    public void testPersistProvince() throws IOException, JSONException {
        Province p = new Province("", new String[]{"abc"}, 0, false);
        JsonProvince province = new JsonProvince(p);
        mapper.writeValue(writer, province);
        JSONAssert.assertEquals(new StringBuilder()
                .append("{")
                .append(  "\"$id\":\"").append(province.getId()).append("\",")
                .append(  "\"fullName\":\"\",")
                .append(  "\"shortNames\":[")
                .append(    "\"abc\"")
                .append(  "],")
                .append(  "\"index\":0,")
                .append(  "\"isConvoyableCoast\":false")
                .append("}")
                .toString(),
                writer.toString(), JSONCompareMode.STRICT);
    }

    @Test
    public void testPersistProvinceWith2ShortNames() throws IOException, JSONException {
        Province p = new Province("Test", new String[]{"abc", "def"}, 1, true);
        JsonProvince province = new JsonProvince(p);
        mapper.writeValue(writer, province);
        JSONAssert.assertEquals(new StringBuilder()
                .append("{")
                .append(  "\"$id\":\"").append(province.getId()).append("\",")
                .append(  "\"fullName\":\"Test\",")
                .append(  "\"shortNames\":[")
                .append(    "\"abc\",")
                .append(    "\"def\"")
                .append(  "],")
                .append(  "\"index\":1,")
                .append(  "\"isConvoyableCoast\":true")
                .append("}")
                .toString(),
                writer.toString(), JSONCompareMode.STRICT);
    }

    @Test
    public void testLoadProvince() throws IOException, JSONException {
        UUID id = UUID.randomUUID();
        String jsonProvince = new StringBuilder()
                .append("{")
                .append(  "\"$id\":\"").append(id).append("\",")
                .append(  "\"fullName\":\"Test\",")
                .append(  "\"shortNames\":[")
                .append(    "\"abc\"")
                .append(  "],")
                .append(  "\"index\":1,")
                .append(  "\"isConvoyableCoast\":true")
                .append("}")
                .toString();

        JsonProvince province = mapper.readValue(new StringReader(jsonProvince), JsonProvince.class);

        Assertions.assertEquals(id.toString(), province.getId());
        Province loadedProvince = province.getProvince();
        Assertions.assertNotNull(loadedProvince);
        Assertions.assertEquals("Test", loadedProvince.getFullName());
        Assertions.assertArrayEquals(new String[]{"abc"}, loadedProvince.getShortNames());
        Assertions.assertEquals(1, loadedProvince.getIndex());
        Assertions.assertTrue(loadedProvince.isConvoyableCoast());
    }

}
