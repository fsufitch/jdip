/*
 *  @(#)Hold.java	1.00	4/1/2002
 *
 *  Copyright 2002 Zachary DelProposto. All rights reserved.
 *  Use is subject to license terms.
 */
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
 * Implementation of the Hold order.
 */
public class Hold extends Order {
    // il8n
    private static final String HOLD_FORMAT = "HOLD_FORMAT";

    // constants: names
    private static final String orderNameBrief = "H";
    private static final String orderNameFull = "Hold";
    private static final transient String orderFormatString = (HOLD_FORMAT);


    /**
     * Creates a Hold order
     */
    protected Hold(Power power, Location src, Unit.Type srcUnit) {
        super(power, src, srcUnit);
    }// Hold()

    /**
     * Creates a Hold order
     */
    protected Hold() {
        super();
    }// Hold()


    public String getFullName() {
        return orderNameFull;
    }// getName()

    public String getBriefName() {
        return orderNameBrief;
    }// getBriefName()


    // format-strings for orders
    public String getDefaultFormat() {
        return orderFormatString;
    }// getFormatBrief()


    public String toBriefString() {
        StringBuffer sb = new StringBuffer(64);

        super.appendBrief(sb);
        sb.append(' ');
        sb.append(orderNameBrief);

        return sb.toString();
    }// toBriefString()


    public String toFullString() {
        StringBuffer sb = new StringBuffer(128);

        super.appendFull(sb);
        sb.append(' ');
        sb.append(orderNameFull);

        return sb.toString();
    }// toFullString()


    public boolean equals(Object obj) {
        return obj instanceof Hold && super.equals(obj);
    }// equals()

}// class Hold
