//
//  @(#)GUIRemove.java	12/2002
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

import dip.order.Remove;
import dip.world.Location;
import dip.world.Power;
import dip.world.Unit;
import org.w3c.dom.svg.SVGGElement;

import java.awt.geom.Point2D;

/**
 * GUIOrder implementation of Remove order.
 */
public class GUIRemove extends Remove implements GUIOrder {
    // i18n
    public static final String NO_UNITS_TO_REMOVE = "GUIRemove.no_removes";

    // instance variables
    private transient final static int REQ_LOC = 1;
    private transient int currentLocNum = 0;
    private transient Point2D.Float failPt = null;
    private transient SVGGElement group = null;


    /**
     * Creates a GUIRemove
     */
    protected GUIRemove() {
        super();
    }// GUIRemove()

    /**
     * Creates a GUIRemove
     */
    protected GUIRemove(Power power, Location source, Unit.Type sourceUnitType) {
        super(power, source, sourceUnitType);
    }// GUIRemove()


}// class GUIRemove