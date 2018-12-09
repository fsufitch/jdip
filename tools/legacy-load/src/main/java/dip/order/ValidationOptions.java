//	
//	@(#)ValidationOptions.java		4/2002
//	
//	Copyright 2002 Zachary DelProposto. All rights reserved.
//	Use is subject to license terms.
//	
//  This program is free software; you can redistribute it and/or modify
//  it under the terms of the GNU General Public License as published by
//  the Free Software Foundation; either version 2 of the License, or
//  (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU General Public License for more details.
//
//  You should have received a copy of the GNU General Public License
//  along with this program; if not, write to the Free Software
//  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
//  Or from http://www.gnu.org/package dip.order.result;
//
package dip.order;


import java.io.Serializable;
import java.util.Hashtable;

/**
 * Controls how orders are validated.
 * <p>
 * This is expandable and adaptable, both for GUI use
 * and for future non-standard adjudicator use.
 * <p>
 * Currently, there are 2 levels; LOOSE and STRICT.
 */

public class ValidationOptions implements Serializable, Cloneable {
    // Contants: never internationalized.

    // global: parsing strictness
    // NOTE: for il8n:
    //		NAME = key for il8n name
    //		NAME_description = description
    public static final String KEY_GLOBAL_PARSING = "KEY_GLOBAL_PARSING";
    public static final String VALUE_GLOBAL_PARSING_STRICT = "VALUE_GLOBAL_PARSING_STRICT";
    public static final String VALUE_GLOBAL_PARSING_LOOSE = "VALUE_GLOBAL_PARSING_LOOSE";
    public static final Option OPTION_GLOBAL_PARSING = new Option(KEY_GLOBAL_PARSING,
            new String[]{VALUE_GLOBAL_PARSING_STRICT, VALUE_GLOBAL_PARSING_LOOSE},
            VALUE_GLOBAL_PARSING_STRICT);


    // internal: list of all options (in base class)
    protected static final Option[] _OPTIONS = {OPTION_GLOBAL_PARSING};
    private static final String DESCRIPTION = "_description";


    // instance variables
    protected Hashtable<String, Object> map = new Hashtable<>(5);
    protected Option[] options = null;            // subclasses should modify as appropriate in constructor


    // constructor: init's all values to default.
    public ValidationOptions() {
        options = _OPTIONS;
        clearOptions();
    }// ValidationOptions()


    public ValidationOptions copy() {
        ValidationOptions copy = new ValidationOptions();
        copy.map = new Hashtable<>(this.map);
        return copy;
    }





    // Set/Get methods
    public void setOption(String key, Object value) {
        map.put(key, value);
    }// setOption()

    public Object getOption(String key) {
        return map.get(key);
    }// getOption()

    public boolean isOption(String key, Object value) {
        return value.equals(map.get(key));
    }// isOption()

    // set all options to default.
    public final void clearOptions() {
        map.clear();
        for (Option option : options) {
            map.put(option.getKey(), option.getDefaultValue());
        }
    }// clearOptions()


    // exposed classes
    // 'current' value of an option is in hashtable
    // perhaps these should be renamed: OptionInfo, then
    public static class Option {
        private final String key;                // actual name
        private final String values[];            // allowable values (to set)
        private final String defaultValue;        // default value

        protected Option(String key, String[] values, String defaultValue) {
            this.key = key;
            this.values = values;
            this.defaultValue = defaultValue;
        }// Option

        public String getKey() {
            return key;
        }

        public String[] getValues() {
            return values;
        }

        public String getDefaultValue() {
            return defaultValue;
        }
    }// class Option

    public static class DescriptiveOption extends Option {
        private String displayName;            // il8n name
        private String description;            // il8n description of name
        private String displayValues[];        // il8n value name
        private String valueDescriptions[]; // il8n value description (optional)

        protected DescriptiveOption(Option option) {
            super(option.key, option.values, option.defaultValue);
        }// DescriptiveOption

        public String getDisplayName() {
            return displayName;
        }

        protected void setDisplayName(String value) {
            displayName = value;
        }

        public String getDescription() {
            return description;
        }

        protected void setDescription(String value) {
            description = value;
        }

        public String[] getDisplayValues() {
            return displayValues;
        }

        protected void setDisplayValues(String[] value) {
            if (value.length != getValues().length) {
                throw new IllegalArgumentException("Number of value names != Number of values");
            }

            displayValues = value;
        }// setDisplayValues()

        public String[] getValueDescriptions() {
            return valueDescriptions;
        }

        protected void setValueDescriptions(String[] value) {
            if (value.length != getValues().length) {
                throw new IllegalArgumentException("Number of value descriptions != Number of values");
            }

            valueDescriptions = value;
        }// setValueDescriptions()
    }// inner class DescriptiveOption


}// class ValidationOptions
