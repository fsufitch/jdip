//	@(#)OrderFormatOptions.java
//	
//	Copyright 2004 Zachary DelProposto. All rights reserved.
//	Use is subject to license terms.
//	
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
//  Or from http://www.gnu.org/package info.jdip.order.result;
//
package info.jdip.order;

import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * This object controls, in detail, how orders are formatted.
 * <p>
 * Several static methods for default order formats are supported.
 * Remember that the static methods create mutable objects that can
 * be modified.
 * <p>
 * <b>Styles</b> define how an order appears (e.g., upper case,
 * lower case, none). Styles are applied after all formatting.
 * <p>
 * <b>Formats</b> define how an order is displayed.
 * <p>
 * <b>Possessive Powers</b> are the powers in Support and Convoy orders
 * that can be displayed if supporting a unit that is of a different power.
 * <p>
 * <b>Debug Mode</b> displays full coasts, and null items with their type.
 * <p>
 */
public class OrderFormatOptions implements Cloneable {
    private static final Logger logger = getLogger(OrderFormatOptions.class);


    // constants
    //
    // base styles
    /**
     * Default style (no modification)
     */
    public static final int STYLE_NONE = 0;
    /**
     * Lower case
     */
    public static final int STYLE_LOWER = 1;
    /**
     * Upper case
     */
    public static final int STYLE_UPPER = 2;
    /**
     * Title case (first word only)
     */
    public static final int STYLE_TITLE = 3;
    /**
     * Title case (all words)
     */
    public static final int STYLE_TITLE_ALL = 4;

    // plural styles (base style + 10)
    /**
     * Plural (adds an 's') Default style (no modification)
     */
    public static final int STYLE_PLURAL_NONE = 10;
    /**
     * Plural (adds an 's') Lower case
     */
    public static final int STYLE_PLURAL_LOWER = 11;
    /**
     * Plural (adds an 's') Upper case
     */
    public static final int STYLE_PLURAL_UPPER = 12;
    /**
     * Plural (adds an 's') Title case (first word only)
     */
    public static final int STYLE_PLURAL_TITLE = 13;
    /**
     * Plural (adds an 's') Title case (all words)
     */
    public static final int STYLE_PLURAL_TITLE_ALL = 14;

    // formats: general
    /**
     * Brief (abbreviated) format
     */
    public static final int FORMAT_BRIEF = 1;
    /**
     * Full (non-abbreviated) format
     */
    public static final int FORMAT_FULL = 2;

    // formats: specific
    /**
     * For coasts only: abbreviated, in parentheses
     */
    public static final int FORMAT_COAST_PAREN_BRIEF = 11;
    /**
     * For coasts only: <i>non</i>-abbreviated, in parentheses
     */
    public static final int FORMAT_COAST_PAREN_FULL = 12;

    // arrows
    /**
     * The Default Movement arrow
     */
    public static final String ARROW_DEFAULT = "->";
    /**
     * A hyphen (instead of an arrow)
     */
    public static final String ARROW_HYPHEN = "-";
    /**
     * A fancier Unicode Movement arrow
     */
    public static final String ARROW_UNICODE = "\u2192";

    /**
     * A list of all Movement arrows
     */
    public static final String[] ARROWS = {
            ARROW_DEFAULT, ARROW_HYPHEN, ARROW_UNICODE
    };

    // coast separators
    /**
     * Coast Separator: forward slash
     */
    public static final char COAST_SEP_SLASH = '/';
    /**
     * Coast Separator: hyphen
     */
    public static final char COAST_SEP_HYPEN = '-';
    /**
     * Coast Separator: none (a space; used for parenthetical coasts)
     */
    public static final char COAST_SEP_NONE = ' ';


    // instance fields
    private int styleProvince = STYLE_NONE;
    private int styleCoast = STYLE_NONE;
    private int stylePower = STYLE_NONE;
    private int styleUnit = STYLE_NONE;
    private int styleOrderName = STYLE_NONE;

    private int formatProvince = FORMAT_BRIEF;
    private int formatCoast = FORMAT_BRIEF;
    private int formatPower = FORMAT_BRIEF;
    private int formatUnit = FORMAT_BRIEF;
    private int formatOrderName = FORMAT_BRIEF;

    private String arrow = ARROW_DEFAULT;
    private boolean showPossessivePower = false;
    private boolean endWithDot = false;    // end order with period
    private boolean isDebug = false;
    private char coastSep = COAST_SEP_SLASH;

    /**
     * Create an OrderFormatOptions object.
     */
    public OrderFormatOptions() {
    }// OrderFormatOptions()


    /**
     * Create an OrderFormatOptions object with
     * the default settings.
     */
    public static OrderFormatOptions createDefault() {
        final OrderFormatOptions ofo = new OrderFormatOptions();

        ofo.styleProvince = STYLE_NONE;
        ofo.styleCoast = STYLE_NONE;
        ofo.stylePower = STYLE_NONE;
        ofo.styleUnit = STYLE_NONE;
        ofo.styleOrderName = STYLE_PLURAL_NONE;

        ofo.formatProvince = FORMAT_BRIEF;
        ofo.formatCoast = FORMAT_BRIEF;
        ofo.formatPower = FORMAT_FULL;
        ofo.formatUnit = FORMAT_BRIEF;
        ofo.formatOrderName = FORMAT_FULL;

        ofo.arrow = ARROW_DEFAULT;
        ofo.showPossessivePower = false;
        ofo.endWithDot = false;
        ofo.isDebug = false;

        return ofo;
    }// createDefault()

    /**
     * Create an OrderFormatOptions object with
     * the terse settings.
     */
    public static OrderFormatOptions createTerse() {
        final OrderFormatOptions ofo = new OrderFormatOptions();

        ofo.styleProvince = STYLE_NONE;
        ofo.styleCoast = STYLE_NONE;
        ofo.stylePower = STYLE_NONE;
        ofo.styleUnit = STYLE_NONE;
        ofo.styleOrderName = STYLE_NONE;

        ofo.formatProvince = FORMAT_BRIEF;
        ofo.formatCoast = FORMAT_BRIEF;
        ofo.formatPower = FORMAT_BRIEF;
        ofo.formatUnit = FORMAT_BRIEF;
        ofo.formatOrderName = FORMAT_BRIEF;

        ofo.arrow = ARROW_DEFAULT;
        ofo.showPossessivePower = false;
        ofo.endWithDot = false;
        ofo.isDebug = false;

        return ofo;
    }// createTerse()

    /**
     * Create an OrderFormatOptions object with
     * the verbose settings.
     */
    public static OrderFormatOptions createVerbose() {
        final OrderFormatOptions ofo = new OrderFormatOptions();

        ofo.styleProvince = STYLE_NONE;
        ofo.styleCoast = STYLE_NONE;
        ofo.stylePower = STYLE_NONE;
        ofo.styleUnit = STYLE_NONE;
        ofo.styleOrderName = STYLE_NONE;

        ofo.formatProvince = FORMAT_FULL;
        ofo.formatCoast = FORMAT_FULL;
        ofo.formatPower = FORMAT_FULL;
        ofo.formatUnit = FORMAT_FULL;
        ofo.formatOrderName = FORMAT_FULL;

        ofo.arrow = ARROW_DEFAULT;
        ofo.showPossessivePower = false;
        ofo.endWithDot = false;
        ofo.isDebug = false;

        return ofo;
    }// createVerbose()

    /**
     * Create an OrderFormatOptions object with
     * the NJudge settings.
     */
    public static OrderFormatOptions createNJudge() {
        final OrderFormatOptions ofo = new OrderFormatOptions();

        ofo.styleProvince = STYLE_TITLE;
        ofo.styleCoast = STYLE_LOWER;
        ofo.stylePower = STYLE_TITLE;
        ofo.styleUnit = STYLE_TITLE;
        ofo.styleOrderName = STYLE_UPPER;

        ofo.formatProvince = FORMAT_FULL;
        ofo.formatCoast = FORMAT_COAST_PAREN_FULL;
        ofo.formatPower = FORMAT_FULL;
        ofo.formatUnit = FORMAT_FULL;
        ofo.formatOrderName = FORMAT_FULL;

        ofo.arrow = ARROW_DEFAULT;
        ofo.showPossessivePower = true;
        ofo.endWithDot = true;
        ofo.isDebug = false;

        return ofo;
    }// createNJudge()


    /**
     * Create an OrderFormatOptions object with
     * the debug settings. This is the createDefault()
     * with setDebug() set to true..
     */
    public static OrderFormatOptions createDebug() {
        final OrderFormatOptions ofo = createDefault();
        ofo.setDebug(true);
        return ofo;
    }// createDebug()

    /**
     * Decode an encoded String into the appropriate OrderFormatOptions
     */
    public static OrderFormatOptions decode(final byte[] in,
                                            final OrderFormatOptions defaultOptions) {
        try {
            defaultOptions.styleProvince = -1;
            defaultOptions.styleCoast = -1;
            defaultOptions.stylePower = -1;
            defaultOptions.styleUnit = -1;
            defaultOptions.styleOrderName = -1;
            defaultOptions.formatProvince = -1;
            defaultOptions.formatCoast = -1;
            defaultOptions.formatPower = -1;
            defaultOptions.formatUnit = -1;
            defaultOptions.formatOrderName = -1;

            ByteArrayInputStream is = new ByteArrayInputStream(in);
            DataInputStream d = new DataInputStream(is);

            defaultOptions.setProvinceStyle(d.readInt());
            defaultOptions.setCoastStyle(d.readInt());
            defaultOptions.setPowerStyle(d.readInt());
            defaultOptions.setUnitStyle(d.readInt());
            defaultOptions.setOrderNameStyle(d.readInt());

            defaultOptions.setProvinceFormat(d.readInt());
            defaultOptions.setCoastFormat(d.readInt());
            defaultOptions.setPowerFormat(d.readInt());
            defaultOptions.setUnitFormat(d.readInt());
            defaultOptions.setOrderNameFormat(d.readInt());

            defaultOptions.showPossessivePower = d.readBoolean();
            defaultOptions.endWithDot = d.readBoolean();
            defaultOptions.isDebug = d.readBoolean();

            defaultOptions.coastSep = d.readChar();
            defaultOptions.arrow = d.readUTF();

            d.close();
        } catch (IOException e) {
            logger.error("OrderFormatOptions error", e);
        }

        return defaultOptions;
    }// decode()

    /**
     * Gets the Province Style
     */
    public int getProvinceStyle() {
        return styleProvince;
    }

    /**
     * Sets the Province Style
     */
    public void setProvinceStyle(int style) {
        checkStyle(style);
        styleProvince = style;
    }// ()

    /**
     * Gets the Coast Style
     */
    public int getCoastStyle() {
        return styleCoast;
    }

    /**
     * Sets the Coast Style
     */
    public void setCoastStyle(int style) {
        checkStyle(style);
        styleCoast = style;
    }// ()

    /**
     * Gets the Power Style
     */
    public int getPowerStyle() {
        return stylePower;
    }

    /**
     * Sets the Power Style
     */
    public void setPowerStyle(int style) {
        checkStyle(style);
        stylePower = style;
    }// ()

    /**
     * Gets the Unit Style
     */
    public int getUnitStyle() {
        return styleUnit;
    }

    /**
     * Sets the Unit Style
     */
    public void setUnitStyle(int style) {
        checkStyle(style);
        styleUnit = style;
    }// ()

    /**
     * Gets the Order Name Style
     */
    public int getOrderNameStyle() {
        return styleOrderName;
    }

    /**
     * Sets the Order Name Style
     */
    public void setOrderNameStyle(int style) {
        checkStyle(style);
        styleOrderName = style;
    }// ()

    /**
     * Gets the Province Format
     */
    public int getProvinceFormat() {
        return formatProvince;
    }

    /**
     * Sets the Province Format
     */
    public void setProvinceFormat(int fmt) {
        checkFormat(fmt);
        formatProvince = fmt;
    }// ()

    /**
     * Gets the Coast Format
     */
    public int getCoastFormat() {
        return formatCoast;
    }

    /**
     * Sets the Coast Format
     */
    public void setCoastFormat(int fmt) {
        checkCoastFormat(fmt);
        formatCoast = fmt;
    }// ()

    /**
     * Gets the Power Format
     */
    public int getPowerFormat() {
        return formatPower;
    }

    /**
     * Sets the Power Format
     */
    public void setPowerFormat(int fmt) {
        checkFormat(fmt);
        formatPower = fmt;
    }// ()

    /**
     * Gets the Unit Format
     */
    public int getUnitFormat() {
        return formatUnit;
    }

    /**
     * Sets the Unit Format
     */
    public void setUnitFormat(int fmt) {
        checkFormat(fmt);
        formatUnit = fmt;
    }// ()

    /**
     * Gets the Order Name Format
     */
    public int getOrderNameFormat() {
        return formatOrderName;
    }

    /**
     * Sets the Order Name Format
     */
    public void setOrderNameFormat(int fmt) {
        checkFormat(fmt);
        formatOrderName = fmt;
    }// ()

    /**
     * Get the Movement Arrow
     */
    public String getArrow() {
        return arrow;
    }

    /**
     * Sets the Movement Arrow
     */
    public void setArrow(String value) {
        if (value == null) {
            throw new IllegalArgumentException();
        }

        arrow = value;
    }// setArrow()

    /**
     * Indicates if Posessive Powers should be shown
     */
    public boolean getShowPossessivePower() {
        return showPossessivePower;
    }

    /**
     * Sets if Possessive Powers are displayed
     */
    public void setShowPossessivePower(boolean value) {
        showPossessivePower = value;
    }// setShowOptionalPower()

    /**
     * Indicates if orders should end with a period
     */
    public boolean getEndWithDot() {
        return endWithDot;
    }

    /**
     * Sets if order should end with a period.
     */
    public void setEndWithDot(boolean value) {
        endWithDot = value;
    }// setEndWithDot()

    /**
     * Indicates if debug mode is enabled
     */
    public boolean isDebug() {
        return isDebug;
    }

    /**
     * When <code>true</code>, the Coast of a Location is always printed
     * (even if, for example, it is a "/mv" coast). Furthermore, if a
     * variable is null, it will be output as null and followed by its
     * type (in brackets); e.g.: "null[Location]", instead of being
     * ignored.
     */
    public void setDebug(boolean value) {
        isDebug = value;
    }// setDebug()

    /**
     * Returns the Coast Separator, or returns COAST_SEP_NONE
     * (a space), if coasts are to be formatted in parentheses.
     * (e.g., FORMAT_COAST_PAREN_BRIEF).
     */
    public char getCoastSeparator() {
        if (getCoastFormat() == FORMAT_COAST_PAREN_BRIEF
                || getCoastFormat() == FORMAT_COAST_PAREN_FULL) {
            return COAST_SEP_NONE;
        }

        return coastSep;
    }// getCoastSeparator()

    /**
     * Set the character that separates the Province from the Coast
     */
    public void setCoastSeparator(char value) {
        coastSep = value;
    }

    /**
     * Encode this object as a compact String
     */
    public byte[] encode() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(256);
            DataOutputStream d = new DataOutputStream(baos);

            d.writeInt(styleProvince);
            d.writeInt(styleCoast);
            d.writeInt(stylePower);
            d.writeInt(styleUnit);
            d.writeInt(styleOrderName);

            d.writeInt(formatProvince);
            d.writeInt(formatCoast);
            d.writeInt(formatPower);
            d.writeInt(formatUnit);
            d.writeInt(formatOrderName);

            d.writeBoolean(showPossessivePower);
            d.writeBoolean(endWithDot);
            d.writeBoolean(isDebug);

            d.writeChar(coastSep);
            d.writeUTF(arrow);

            d.close();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("internal error");
        }
    }// encode()

    /**
     * Check style constant value
     */
    private void checkStyle(int value) {
        value = (value >= 10) ? (value - 10) : value;
        if (value < 0 || value > 4) {
            throw new IllegalArgumentException(String.valueOf(value));
        }
    }// checkStyle()

    /**
     * Check non-coast format value
     */
    private void checkFormat(int value) {
        if (value != FORMAT_BRIEF && value != FORMAT_FULL) {
            throw new IllegalArgumentException(String.valueOf(value));
        }
    }// checkFormat()

    /**
     * Check coast formats
     */
    private void checkCoastFormat(int value) {
        if (value != FORMAT_COAST_PAREN_BRIEF
                && value != FORMAT_COAST_PAREN_FULL
                && value != FORMAT_BRIEF
                && value != FORMAT_FULL) {
            throw new IllegalArgumentException(String.valueOf(value));
        }
    }// checkCoastFormat()

    /**
     * Clone
     */
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            logger.error("There was a problem with cloning", e);
            throw new RuntimeException(e);
        }
    }
}// class OrderFormatOptions
