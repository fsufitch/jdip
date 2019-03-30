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
import info.jdip.world.Province;
import java.util.Arrays;
import java.util.UUID;

/**
 *
 * @author Uwe Plonus
 */
@JsonPropertyOrder({"$id", "fullName", "shortNames", "index", "isCOnvoyableCoast"})
@JsonIgnoreProperties({"province"})
public class JsonProvince {

    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class)
    @JsonProperty(value = "$id")
    private String id;

    @JsonProperty
    private String fullName;

    @JsonProperty
    private String shortNames[];

    @JsonProperty
    private int index;

    @JsonProperty
    private boolean isConvoyableCoast;

    public JsonProvince() {
        id = UUID.randomUUID().toString();
    }

    public JsonProvince(Province province) {
        this();
        this.fullName = province.getFullName();
        if (province.getShortNames() == null || province.getShortNames().length <= 0) {
            throw new IllegalArgumentException("The shortNames of a province must contain at least a single name");
        }
        this.shortNames = Arrays.copyOf(province.getShortNames(), province.getShortNames().length);
        this.index = province.getIndex();
        this.isConvoyableCoast = province.isConvoyableCoast();
    }

    public String getId() {
        return id;
    }

    public Province getProvince() {
        return new Province(fullName, shortNames == null ? null : Arrays.copyOf(shortNames, shortNames.length),
                index, isConvoyableCoast);
    }

}
