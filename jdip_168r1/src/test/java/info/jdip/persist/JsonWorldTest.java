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
import info.jdip.world.Phase;
import info.jdip.world.Power;
import info.jdip.world.World;
import info.jdip.world.WorldFactory;
import info.jdip.world.variant.data.BorderData;
import info.jdip.world.variant.data.InitialState;
import info.jdip.world.variant.data.ProvinceData;
import info.jdip.world.variant.data.SupplyCenter;
import info.jdip.world.variant.data.Variant;
import java.io.IOException;
import java.io.StringWriter;
import java.util.LinkedList;
import java.util.List;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

public class JsonWorldTest {

    private World createWorld() throws InvalidWorldException {
        Variant variant = new Variant();
        variant.setName("WorldPersistVariant");

        ProvinceData[] provinceDatas = new ProvinceData[0];
        variant.setProvinceData(provinceDatas);

        BorderData[] borderDatas = new BorderData[0];
        variant.setBorderData(borderDatas);

        List<Power> powers = new LinkedList<>();
        variant.setPowers(powers);

        Phase startingPhase = new Phase(Phase.SeasonType.SPRING, new Phase.YearType(701), Phase.PhaseType.MOVEMENT);
        variant.setStartingPhase(startingPhase);

        List<SupplyCenter> supplyCenters = new LinkedList<>();
        variant.setSupplyCenters(supplyCenters);

        List<InitialState> initialStates = new LinkedList<>();
        variant.setInitialStates(initialStates);

        variant.setMaxGameTimeYears(2999);

        WorldFactory worldFactory = WorldFactory.getInstance();
        return worldFactory.createWorld(variant);
    }

    @Test
    public void testPersistEmptyWorld() throws IOException, JSONException {
        JsonWorld world = new JsonWorld();
        ObjectMapper mapper = new ObjectMapper();
        StringWriter writer = new StringWriter();
        mapper.writeValue(writer, world);
        JSONAssert.assertEquals("{\"map\":null}", writer.toString(), false);
    }

    @Test
    public void testPersistWorld() throws IOException, JSONException, InvalidWorldException {
        JsonWorld world = new JsonWorld(createWorld());
        ObjectMapper mapper = new ObjectMapper();
        StringWriter writer = new StringWriter();
        mapper.writeValue(writer, world);
        JSONAssert.assertEquals(
                "{" +
                    "\"map\":{" +
                        "\"powers\":[]" +
                    "}" +
                "}",
                writer.toString(), JSONCompareMode.STRICT);
    }

}
