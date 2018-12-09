//
//  @(#)GUIWaive.java	12/2003
//
//  Copyright 2003 Zachary DelProposto. All rights reserved.
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

import dip.order.Waive;
import dip.world.Location;
import dip.world.Power;
import info.jdip.test.loading.IgnoreComparisonResult;
import org.w3c.dom.svg.SVGGElement;

import java.awt.geom.Point2D;

/**
 * GUIOrder implementation of the Waive order.
 */
public class GUIWaive extends Waive implements GUIOrder {

    // i18n keys
    @IgnoreComparisonResult
    private static final String NOWAIVE_MUST_BE_AN_OWNED_SC = "GUIWaive.bad.must_own_sc";
    @IgnoreComparisonResult
    private static final String NOWAIVE_NOT_OWNED_HOME_SC = "GUIWaive.bad.now_owned_home_sc";
    @IgnoreComparisonResult
    private static final String NOWAIVE_NEED_ONE_OWNED_SC = "GUIWaive.bad.need_one_owned_sc";
    @IgnoreComparisonResult
    private static final String NOWAIVE_NO_BUILDS_AVAILABLE = "GUIWaive.bad.no_builds_available";
    @IgnoreComparisonResult
    private static final String NOWAIVE_SC_NOT_CONTROLLED = "GUIWaive.bad.sc_not_controlled";
    @IgnoreComparisonResult
    private static final String NOWAIVE_UNIT_PRESENT = "GUIWaive.bad.unit_already_present";
    @IgnoreComparisonResult
    private static final String NOWAIVE_UNOWNED_SC = "GUIWaive.bad.unowned_sc";

    // instance variables
    private transient final static int REQ_LOC = 1;
    private transient int currentLocNum = 0;
    private transient Point2D.Float failPt = null;
    private transient SVGGElement group = null;

    /**
     * Creates a GUIWaive
     */
    protected GUIWaive() {
        super();
    }// GUIWaive()

    /**
     * Creates a GUIWaive
     */
    protected GUIWaive(Power power, Location source) {
        super(power, source);
    }// GUIWaive()


}// class GUIWaive
