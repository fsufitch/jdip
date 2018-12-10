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
import info.jdip.test.loading.IgnoreComparisonResult;
import org.w3c.dom.svg.SVGGElement;

import java.awt.geom.Point2D;


public class GUIBuild extends Build implements GUIOrder {

    public transient static final BuildParameter BUILD_UNIT = new BuildParameter("BUILD_UNIT");

    @IgnoreComparisonResult
    private static final String BUILD_FLEET_OK = "GUIBuild.ok.fleet";
    @IgnoreComparisonResult
    private static final String BUILD_ARMY_OK = "GUIBuild.ok.army";
    @IgnoreComparisonResult
    private static final String BUILD_WING_OK = "GUIBuild.ok.wing";
    @IgnoreComparisonResult
    private static final String NOBUILD_FLEET_LANDLOCKED = "GUIBuild.bad.fleet.landlocked";
    @IgnoreComparisonResult
    private static final String NOBUILD_NO_ARMY_IN_SEA = "GUIBuild.bad.army_in_sea";
    @IgnoreComparisonResult
    private static final String NOBUILD_NO_UNIT_SELECTED = "GUIBuild.bad.no_unit_selected";
    @IgnoreComparisonResult
    private static final String NOBUILD_MUST_BE_AN_OWNED_SC = "GUIBuild.bad.must_own_sc";
    @IgnoreComparisonResult
    private static final String NOBUILD_NOT_OWNED_HOME_SC = "GUIBuild.bad.now_owned_home_sc";
    @IgnoreComparisonResult
    private static final String NOBUILD_NEED_ONE_OWNED_SC = "GUIBuild.bad.need_one_owned_sc";
    @IgnoreComparisonResult
    private static final String NOBUILD_NO_BUILDS_AVAILABLE = "GUIBuild.bad.no_builds_available";
    @IgnoreComparisonResult
    private static final String NOBUILD_SC_NOT_CONTROLLED = "GUIBuild.bad.sc_not_controlled";
    @IgnoreComparisonResult
    private static final String NOBUILD_UNIT_PRESENT = "GUIBuild.bad.unit_already_present";
    @IgnoreComparisonResult
    private static final String NOBUILD_UNOWNED_SC = "GUIBuild.bad.unowned_sc";

    private transient final static int REQ_LOC = 1;
    private transient int currentLocNum = 0;
    private transient Point2D.Float failPt = null;
    private transient SVGGElement group = null;



    protected static class BuildParameter extends Parameter {

        public BuildParameter(String name) {
            super(name);
        }
    }


}
