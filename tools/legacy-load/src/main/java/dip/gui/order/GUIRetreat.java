//
//  @(#)GUIRetreat.java		12/2002
//
//  Copyright 2002 Zachary DelProposto. All rights reserved.
//  Use is subject to license terms.
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
//  Or from http://www.gnu.org/
//
package dip.gui.order;

import dip.order.Retreat;
import dip.world.Location;
import dip.world.Power;
import dip.world.Unit;
import info.jdip.test.loading.IgnoreComparisonResult;
import org.w3c.dom.svg.SVGGElement;

import java.awt.geom.Point2D;

/**
 * GUIOrder subclass of Retreat order.
 */
public class GUIRetreat extends Retreat implements GUIOrder {
    // i18n keys
    @IgnoreComparisonResult
    private final static String UNIT_MUST_DISBAND = "GUIRetreat.must_disband";
    @IgnoreComparisonResult
    private final static String CLICK_TO_SET_DEST = "GUIRetreat.set_dest";
    @IgnoreComparisonResult
    private final static String CANNOT_RETREAT_HERE = "GUIRetreat.bad_dest";
    @IgnoreComparisonResult
    private final static String VALID_RETREAT_LOCS = "GUIRetreat.valid_locs";

    // instance variables
    private transient static final int REQ_LOC = 2;
    private transient int currentLocNum = 0;
    private transient Point2D.Float failPt = null;
    private transient SVGGElement group = null;


    /**
     * Creates a GUIRetreat
     */
    protected GUIRetreat() {
        super();
    }// GUIRetreat()

    /**
     * Creates a GUIRetreat
     */
    protected GUIRetreat(Power power, Location source, Unit.Type sourceUnitType, Location dest) {
        super(power, source, sourceUnitType, dest);
    }// GUIRetreat()


}// class GUIRetreat
