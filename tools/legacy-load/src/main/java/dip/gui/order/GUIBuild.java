//
//  @(#)GUIBuild.java	12/2002
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

import dip.order.Build;
import dip.world.Location;
import dip.world.Power;
import dip.world.Unit;
import org.w3c.dom.svg.SVGGElement;

import java.awt.geom.Point2D;

/**
 * GUIOrder implementation of Build order.
 */
public class GUIBuild extends Build implements GUIOrder {
    // BuildParameter constants
    /**
     * Required. Used to set build Unit.Type. Associated value must be a Unit.Type
     */
    public transient static final BuildParameter BUILD_UNIT = new BuildParameter("BUILD_UNIT");

    // i18n keys
    private static final String BUILD_FLEET_OK = "GUIBuild.ok.fleet";
    private static final String BUILD_ARMY_OK = "GUIBuild.ok.army";
    private static final String BUILD_WING_OK = "GUIBuild.ok.wing";
    private static final String NOBUILD_FLEET_LANDLOCKED = "GUIBuild.bad.fleet.landlocked";
    private static final String NOBUILD_NO_ARMY_IN_SEA = "GUIBuild.bad.army_in_sea";
    private static final String NOBUILD_NO_UNIT_SELECTED = "GUIBuild.bad.no_unit_selected";
    private static final String NOBUILD_MUST_BE_AN_OWNED_SC = "GUIBuild.bad.must_own_sc";
    private static final String NOBUILD_NOT_OWNED_HOME_SC = "GUIBuild.bad.now_owned_home_sc";
    private static final String NOBUILD_NEED_ONE_OWNED_SC = "GUIBuild.bad.need_one_owned_sc";
    private static final String NOBUILD_NO_BUILDS_AVAILABLE = "GUIBuild.bad.no_builds_available";
    private static final String NOBUILD_SC_NOT_CONTROLLED = "GUIBuild.bad.sc_not_controlled";
    private static final String NOBUILD_UNIT_PRESENT = "GUIBuild.bad.unit_already_present";
    private static final String NOBUILD_UNOWNED_SC = "GUIBuild.bad.unowned_sc";

    // instance variables
    private transient final static int REQ_LOC = 1;
    private transient int currentLocNum = 0;
    private transient Point2D.Float failPt = null;
    private transient SVGGElement group = null;

    /**
     * Creates a GUIBuild
     */
    protected GUIBuild() {
        super();
    }// GUIBuild()

    /**
     * Creates a GUIBuild
     */
    protected GUIBuild(Power power, Location source, Unit.Type sourceUnitType) {
        super(power, source, sourceUnitType);
    }// GUIBuild()

    /**
     * Typesafe Enumerated Parameter class for setting
     * required Build parameters.
     */
    protected static class BuildParameter extends Parameter {
        /**
         * Creates a BuildParameter
         */
        public BuildParameter(String name) {
            super(name);
        }// BuildParameter()
    }// nested class BuildParameter


}// class GUIBuild
