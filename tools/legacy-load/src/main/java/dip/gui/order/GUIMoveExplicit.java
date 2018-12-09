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
import dip.world.Province;
import org.w3c.dom.svg.SVGGElement;

import java.awt.geom.Point2D;
import java.util.LinkedList;

/**
 * GUIOrder subclass of Move order.
 * <p>
 * This differs from GUIMove in that Explicit convoy routes are <b>always</b>
 * created. Implicit convoy routes <b>cannot</b> be created via GUI entry.
 * <p>
 * This should be used instead of GUIMove for games with RuleOptions that
 * enforce explicit convoy routes (such as Judge-based games).
 */
public class GUIMoveExplicit extends Move implements GUIOrder {

    // i18n keys
    private final static String CLICK_TO_SET_DEST = "GUIMove.set.dest";
    private final static String CANNOT_MOVE_TO_ORIGIN = "GUIMove.cannot_to_origin";
    private final static String NO_CONVOY_ROUTE = "GUIMove.no_convoy_route";
    private final static String CANNOT_MOVE_HERE = "GUIMove.cannot_move_here";

    // i18n keys for convoys
    private final static String CANNOT_BACKTRACK = "GUIMoveExplicit.convoy.backtrack";
    private final static String FINAL_DESTINATION = "GUIMoveExplicit.convoy.location.destination";
    private final static String OK_CONVOY_LOCATION = "GUIMoveExplicit.convoy.location.ok";
    private final static String BAD_CONVOY_LOCATION = "GUIMoveExplicit.convoy.location.bad";
    private final static String NONADJACENT_CONVOY_LOCATION = "GUIMoveExplicit.convoy.location.nonadjacent";
    private final static String ADDED_CONVOY_LOCATION = "GUIMoveExplicit.convoy.location.added";

    // instance variables
    private transient static final int REQ_LOC = 2;
    private transient boolean isConvoyableArmy = false;
    private transient boolean isComplete = false;
    private transient LinkedList<Province> tmpConvoyPath = null;
    private transient int currentLocNum = 0;
    private transient int numSupports = -9999;
    private transient Point2D.Float failPt = null;
    private transient SVGGElement group = null;


    /**
     * Creates a GUIMoveExplicit
     */
    protected GUIMoveExplicit() {
        super();
    }// GUIMoveExplicit()


}// class GUIMoveExplicit
