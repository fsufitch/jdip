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

import com.fasterxml.jackson.annotation.JsonProperty;
import info.jdip.world.World;

/**
 *
 * @author Uwe Plonus
 */
public class JsonWorld {

    public JsonWorld() {
    }

    public JsonWorld(World world) {
        map = new JsonMap(world.getMap());
    }

    @JsonProperty
    private JsonMap map;

    public JsonMap getMap() {
        return map;
    }

}
