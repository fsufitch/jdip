//
// 	@(#)Waive.java	1	2/2003
//
// 	Copyright 2003 Zachary DelProposto. All rights reserved.
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
 * A Waive order; a power may explicitly choose not to build a unit.
 * in that case, a Waive order may be issued.
 */
public class Waive extends Order {
    // il8n constants
    private static final String WAIVE_FORMAT = "WAIVE_FORMAT";

    // constants: names
    private static final String orderNameBrief = "W";
    private static final String orderNameFull = "Waive";


    /**
     * Creates a Waive order
     */
    protected Waive(Power power, Location src) {
        super(power, src, Unit.Type.UNDEFINED);
    }// Waive()

    /**
     * Creates a Waive order
     */
    protected Waive() {
        super();
        srcUnitType = Unit.Type.UNDEFINED;
    }// Waive()

    public String getFullName() {
        return orderNameFull;
    }// getName()

    public String getBriefName() {
        return orderNameBrief;
    }// getBriefName()


    public String toBriefString() {
        StringBuffer sb = new StringBuffer(64);

        sb.append(power);
        sb.append(": ");
        sb.append(orderNameBrief);
        sb.append(" build in ");
        src.appendBrief(sb);

        return sb.toString();
    }// toBriefString()


    public String toFullString() {
        StringBuffer sb = new StringBuffer(128);

        sb.append(power);
        sb.append(": ");
        sb.append(orderNameFull);
        sb.append(" build in ");
        src.appendFull(sb);

        return sb.toString();
    }// toFullString()


    public boolean equals(Object obj) {
        if (obj instanceof Waive) {
            return super.equals(obj);
        }
        return false;
    }// equals()


}// class Waive


