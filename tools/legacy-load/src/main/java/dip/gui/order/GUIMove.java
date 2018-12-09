//
//  @(#)GUIMove.java	12/2002
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

import dip.order.Move;
import dip.world.Location;
import dip.world.Power;
import dip.world.Province;
import dip.world.Unit;
import info.jdip.test.loading.IgnoreComparisonResult;
import org.w3c.dom.svg.SVGGElement;

import java.awt.geom.Point2D;
import java.util.List;

/**
 * GUIOrder subclass of Move order.
 * <p>
 * Note that GUIMove fully supports moves with explicit convoy routes,
 * but their GUI entry is not supported.
 */
public class GUIMove extends Move implements GUIOrder {
    // MoveParameter constants
    /**
     * Optional. Sets this Move to be by convoy. Value must be a Boolean object (Boolean.TRUE or Boolean.FALSE)
     */
    public transient static final MoveParameter BY_CONVOY = new MoveParameter("BY_CONVOY");

    // i18n keys
    @IgnoreComparisonResult
    private final static String CLICK_TO_SET_DEST = "GUIMove.set.dest";
    @IgnoreComparisonResult
    private final static String CANNOT_MOVE_TO_ORIGIN = "GUIMove.cannot_to_origin";
    @IgnoreComparisonResult
    private final static String NO_CONVOY_ROUTE = "GUIMove.no_convoy_route";
    @IgnoreComparisonResult
    private final static String CANNOT_MOVE_HERE = "GUIMove.cannot_move_here";

    // instance variables
    private transient static final int REQ_LOC = 2;
    private transient int currentLocNum = 0;
    private transient int numSupports = -9999;
    private transient Point2D.Float failPt = null;
    private transient SVGGElement group = null;

    /**
     * Creates a GUIMove
     */
    protected GUIMove() {
        super();
    }// GUIMove()

    /**
     * Creates a GUIMove
     */
    protected GUIMove(Power power, Location source, Unit.Type srcUnitType, Location dest, boolean isConvoying) {
        super(power, source, srcUnitType, dest, isConvoying);
    }// GUIMove()

    /**
     * Creates a GUIMove
     */
    protected GUIMove(Power power, Location src, Unit.Type srcUnitType, Location dest, Province[] convoyRoute) {
        super(power, src, srcUnitType, dest, convoyRoute);
    }// GUIMove()


    /**
     * Creates a GUIMove
     */
    protected GUIMove(Power power, Location src, Unit.Type srcUnitType, Location dest, List<Province[]> routes) {
        super(power, src, srcUnitType, dest, routes);
    }// GUIMove()


    /**
     * Typesafe Enumerated Parameter class for setting
     * optional Move parameters.
     */
    protected static class MoveParameter extends Parameter {
        /**
         * Creates a MoveParameter
         */
        public MoveParameter(String name) {
            super(name);
        }// MoveParameter()
    }// nested class MoveParameter


}// class GUIMove
