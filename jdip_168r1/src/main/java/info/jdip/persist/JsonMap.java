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

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import info.jdip.world.Map;
import info.jdip.world.Power;
import info.jdip.world.Province;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

/**
 *
 * @author Uwe Plonus
 */
@JsonPropertyOrder({"$id", "powers"})
@JsonIgnoreProperties({"map"})
public class JsonMap {

    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class)
    @JsonProperty(value = "$id")
    private String id;

    @JsonProperty
    private List<JsonPower> powers = new LinkedList<>();

    public JsonMap() {
        id = UUID.randomUUID().toString();
    }

    public JsonMap(Map map) {
        this();
        for (Power power: map.getPowers()) {
            powers.add(new JsonPower(power));
        }
    }

    public String getId() {
        return id;
    }

    public List<JsonPower> getPowers() {
        return powers;
    }

    public Map getMap() {
        Power[] mapPowers = new Power[powers.size()];
        for (int i = 0; i < mapPowers.length; i++) {
            mapPowers[i] = powers.get(i).getPower();
        }
        Map map = new Map(mapPowers, new Province[0]);
        return map;
    }

}
