//
// @(#)Support.java		4/2002
//
// Copyright 2002 Zachary DelProposto. All rights reserved.
// Use is subject to license terms.
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

import dip.world.Location;
import dip.world.Power;
import dip.world.Unit;


/**
 * Implementation of the Support order.
 * <p>
 * While the ability to specify a narrowing order exists, it is
 * not currently used. A narrowing order would be used to support
 * a specific type of support/hold/convoy order [typically of another power].
 */
public class Support extends Order {
    // il8n constants
    private static final String SUPPORT_VAL_NOSELF = "SUPPORT_VAL_NOSELF";
    private static final String SUPPORT_VAL_NOMOVE = "SUPPORT_VAL_NOMOVE";
    private static final String SUPPORT_VAL_INPLACEMOVE = "SUPPORT_VAL_INPLACEMOVE";
    private static final String SUPPORT_VER_FAILTEXT = "SUPPORT_VER_FAILTEXT";
    private static final String SUPPORT_VER_MOVE_ERR = "SUPPORT_VER_MOVE_ERR";
    private static final String SUPPORT_VER_NOMATCH = "SUPPORT_VER_NOMATCH";
    private static final String SUPPORT_VER_MOVE_BADCOAST = "SUPPORT_VER_MOVE_BADCOAST";
    private static final String SUPPORT_EVAL_CUT = "SUPPORT_EVAL_CUT";
    private static final String SUPPORT_DIFF_PASS = "SUPPORT_DIFF_PASS";

    private static final String SUPPORT_FORMAT_MOVE = "SUPPORT_FORMAT_MOVE";
    private static final String SUPPORT_FORMAT_NONMOVE = "SUPPORT_FORMAT_NONMOVE";

    // constants: names
    private static final String orderNameBrief = "S";
    private static final String orderNameFull = "Support";
    private static final transient String orderFormatString_move = (SUPPORT_FORMAT_MOVE);
    private static final transient String orderFormatString_nonmove = (SUPPORT_FORMAT_NONMOVE);

    // instance variables
    protected Location supSrc = null;
    protected Location supDest = null;
    protected Unit.Type supUnitType = null;
    protected Order narrowingOrder = null;
    protected Power supPower = null;


    /**
     * Creates a Support order, for supporting a Hold or other <b>non</b>-movement order.
     */
    protected Support(Power power, Location src, Unit.Type srcUnit,
                      Location supSrc, Power supPower, Unit.Type supUnit) {
        this(power, src, srcUnit, supSrc, supPower, supUnit, null);
    }// Support()

    /**
     * Creates a Support order, for Supporting a Move order.
     * <p>
     * If supDest == null, a Hold support will be generated. Note that If supSrc == supDest,
     * (or even if provinces are equal), this will be a Supported Move to the same location.
     * Note that a supported Move to the same location will fail, since a Move to the same
     * location is not a valid order.
     * <p>
     */
    protected Support(Power power, Location src, Unit.Type srcUnit,
                      Location supSrc, Power supPower, Unit.Type supUnit, Location supDest) {
        super(power, src, srcUnit);

        if (supSrc == null || supUnit == null) {
            throw new IllegalArgumentException("null argument(s)");
        }

        this.supPower = supPower;
        this.supSrc = supSrc;
        this.supUnitType = supUnit;
        this.supDest = supDest;
    }// Support()


    /**
     * Creates a Support order
     */
    protected Support() {
        super();
    }// Support()

    /**
     * Returns the Location of the Unit we are Supporting
     */
    public Location getSupportedSrc() {
        return supSrc;
    }

    /**
     * Returns the Unit Type of the Unit we are Supporting
     * <b>Warning:</b> this can be null, if no unit type was set, and
     * no strict validation was performed (via <code>validate()</code>).
     */
    public Unit.Type getSupportedUnitType() {
        return supUnitType;
    }

    /**
     * Returns the Narrowing order, or null if none was specified.
     */
    public Order getNarrowingOrder() {
        return narrowingOrder;
    }

    /**
     * A narrowing order only applies to non-move Supports,
     * to make it more specific.
     * <p>
     * <b>Note:</b> this can be set, but narrowing order usage is
     * not currently implemented.
     *
     * @throws IllegalArgumentException if this is a Move support.
     */
    public void setNarrowingOrder(Order o) {
        if (!isSupportingHold()) {
            throw new IllegalArgumentException("Cannot narrow a supported move order.");
        }

        narrowingOrder = o;
    }// setNarrowingOrder()

    /**
     * Returns the Power of the Unit we are Supporting.
     * <b>Warning:</b> this can be null, if no unit type was set, and
     * no strict validation was performed (via <code>validate()</code>).
     * <p>
     * <b>Important Note:</b> This also may be null only when a saved game
     * from 1.5.1 or prior versions are loaded into a recent version,
     * since prior versions did not support this field.
     */
    public Power getSupportedPower() {
        return supPower;
    }

    /**
     * Returns true if we are supporting a non-Move order.
     * This is the preferred method of determining if we are truly
     * supporting a Move order verses a non-Move (Hold) order.
     */
    public final boolean isSupportingHold() {
        return (supDest == null);
    }

    /**
     * Returns true if we are supporting a non-Move order.
     * <p>
     * Note: isSupportingHold() should be deprecated. There is no
     * difference (other than name) between this method and
     * isSupportingHold()).
     */
    public final boolean isNonMoveSupport() {
        return (supDest == null);
    }


    /**
     * Returns the Location that we are Supporting into;
     * if this is a non-move Support, this will return
     * the same (referentially!) location as getSupportedSrc().
     * It will not return null.
     */
    public Location getSupportedDest() {
        if (isSupportingHold()) {
            return supSrc;
        }

        return supDest;
    }// getSupportedDest()


    public String getFullName() {
        return orderNameFull;
    }// getName()

    public String getBriefName() {
        return orderNameBrief;
    }// getBriefName()


    public String getDefaultFormat() {
        if (isSupportingHold()) {
            return orderFormatString_nonmove;
        }

        return orderFormatString_move;
    }// getFormatBrief()


    public String toBriefString() {
        StringBuffer sb = new StringBuffer(64);

        super.appendBrief(sb);
        sb.append(' ');
        sb.append(orderNameBrief);
        sb.append(' ');
        sb.append(supUnitType.getShortName());
        sb.append(' ');
        supSrc.appendBrief(sb);

        if (!isSupportingHold()) {
            sb.append('-');
            supDest.appendBrief(sb);
        }

        return sb.toString();
    }// toBriefString()


    public String toFullString() {
        StringBuffer sb = new StringBuffer(128);

        super.appendFull(sb);
        sb.append(' ');
        sb.append(orderNameFull);
        sb.append(' ');
        sb.append(supUnitType.getFullName());
        sb.append(' ');
        supSrc.appendFull(sb);

        if (!isSupportingHold()) {
            sb.append(" -> ");
            supDest.appendFull(sb);
        }

        return sb.toString();
    }// toFullString()


    public boolean equals(Object obj) {
        if (obj instanceof Support) {
            final Support support = (Support) obj;
            return super.equals(support)
                    && supUnitType.equals(support.supUnitType)
                    && supSrc.equals(support.supSrc)
                    && supPower.equals(support.supPower)
                    && ((supDest == support.supDest) || ((supDest != null) && (supDest.equals(support.supDest))));
        }
        return false;
    }// equals()
}// class Support
