//
// 	@(#)Convoy.java		4/2002
//
// 	Copyright 2002 Zachary DelProposto. All rights reserved.
// 	Use is subject to license terms.
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
 * Implementation of the Convoy order.
 */

public class Convoy extends Order {
    // il8n constants
    private static final String CONVOY_SEA_FLEETS = "CONVOY_SEA_FLEETS";
    private static final String CONVOY_ONLY_ARMIES = "CONVOY_ONLY_ARMIES";
    private static final String CONVOY_NO_ROUTE = "CONVOY_NO_ROUTE";
    private static final String CONVOY_VER_NOMOVE = "CONVOY_VER_NOMOVE";
    private static final String CONVOY_FORMAT = "CONVOY_FORMAT";
    private static final String CONVOY_SELF_ILLEGAL = "CONVOY_SELF_ILLEGAL";
    private static final String CONVOY_TO_SAME_PROVINCE = "CONVOY_TO_SAME_PROVINCE";

    // constants: names
    private static final String orderNameBrief = "C";
    private static final String orderNameFull = "Convoy";
    private static final transient String orderFormatString = (CONVOY_FORMAT);

    // instance variables
    protected Location convoySrc = null;
    protected Location convoyDest = null;
    protected Unit.Type convoyUnitType = null;
    protected Power convoyPower = null;

    /**
     * Creates a Convoy order
     */
    protected Convoy(Power power, Location src, Unit.Type srcUnit,
                     Location convoySrc, Power convoyPower, Unit.Type convoyUnitType,
                     Location convoyDest) {
        super(power, src, srcUnit);

        if (convoySrc == null || convoyUnitType == null || convoyDest == null) {
            throw new IllegalArgumentException("null argument(s)");
        }

        this.convoySrc = convoySrc;
        this.convoyUnitType = convoyUnitType;
        this.convoyPower = convoyPower;
        this.convoyDest = convoyDest;
    }// Convoy()


    /**
     * Creates a Convoy order
     */
    protected Convoy() {
        super();
    }// Convoy()


    /**
     * Returns the Location of the Unit to be Convoyed
     */
    public Location getConvoySrc() {
        return convoySrc;
    }

    /**
     * Returns the Unit Type of the Unit to be Convoyed
     * <b>Warning:</b> this can be null, if no unit type was set, and
     * no strict validation was performed (via <code>validate()</code>).
     */
    public Unit.Type getConvoyUnitType() {
        return convoyUnitType;
    }

    /**
     * Returns the Power of the Unit we are Convoying.
     * <b>Warning:</b> this can be null, if no unit type was set, and
     * no strict validation was performed (via <code>validate()</code>).
     * <p>
     * <b>Important Note:</b> This also may be null only when a saved game
     * from 1.5.1 or prior versions are loaded into a recent version,
     * since prior versions did not support this field.
     */
    public Power getConvoyedPower() {
        return convoyPower;
    }


    /**
     * Returns the Location of the Convoy destination
     */
    public Location getConvoyDest() {
        return convoyDest;
    }


    public String getFullName() {
        return orderNameFull;
    }// getName()

    public String getBriefName() {
        return orderNameBrief;
    }// getBriefName()


    public String getDefaultFormat() {
        return orderFormatString;
    }// getFormatBrief()


    public String toBriefString() {
        StringBuffer sb = new StringBuffer(64);

        super.appendBrief(sb);
        sb.append(' ');
        sb.append(orderNameBrief);
        sb.append(' ');
        sb.append(convoyUnitType.getShortName());
        sb.append(' ');
        convoySrc.appendBrief(sb);
        sb.append('-');
        convoyDest.appendBrief(sb);

        return sb.toString();
    }// toBriefString()


    public String toFullString() {
        StringBuffer sb = new StringBuffer(128);

        super.appendFull(sb);
        sb.append(' ');
        sb.append(orderNameFull);
        sb.append(' ');
        sb.append(convoyUnitType.getFullName());
        sb.append(' ');
        convoySrc.appendFull(sb);
        sb.append(" -> ");
        convoyDest.appendFull(sb);

        return sb.toString();
    }// toFullString()


    public boolean equals(Object obj) {
        if (obj instanceof Convoy) {
            Convoy convoy = (Convoy) obj;
            return super.equals(convoy)
                    && this.convoySrc.equals(convoy.convoySrc)
                    && this.convoyUnitType.equals(convoy.convoyUnitType)
                    && this.convoyDest.equals(convoy.convoyDest);
        }
        return false;
    }// equals()


}// class Convoy
