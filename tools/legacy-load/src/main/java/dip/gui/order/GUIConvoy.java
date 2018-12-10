//
//  @(#)GUIConvoy.java		12/2002
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

import dip.order.Convoy;
import info.jdip.test.loading.IgnoreComparisonResult;
import org.w3c.dom.svg.SVGGElement;

import java.awt.geom.Point2D;


public class GUIConvoy extends Convoy implements GUIOrder {

    @IgnoreComparisonResult
    private final static String ONLY_SEA_OR_CC_FLEETS_CAN_CONVOY = "GUIConvoy.only_fleets_can_convoy";
    @IgnoreComparisonResult
    private final static String CLICK_TO_CONVOY = "GUIConvoy.click_to_convoy";
    @IgnoreComparisonResult
    private final static String NO_UNIT = "GUIConvoy.no_unit";
    @IgnoreComparisonResult
    private final static String CLICK_TO_CONVOY_ARMY = "GUIConvoy.click_to_convoy_army";
    @IgnoreComparisonResult
    private final static String CANNOT_CONVOY_LANDLOCKED = "GUIConvoy.no_convoy_landlocked";
    @IgnoreComparisonResult
    private final static String MUST_CONVOY_FROM_COAST = "GUIConvoy.must_convoy_from_coast";
    @IgnoreComparisonResult
    private final static String CLICK_TO_CONVOY_FROM = "GUIConvoy.click_to_convoy_from";
    @IgnoreComparisonResult
    private final static String NO_POSSIBLE_CONVOY_PATH = "GUIConvoy.no_path";
    @IgnoreComparisonResult
    private final static String MUST_CONVOY_TO_COAST = "GUIConvoy.must_convoy_to_coast";


    private transient static final int REQ_LOC = 3;
    private transient int currentLocNum = 0;
    private transient Point2D.Float failPt = null;
    private transient SVGGElement group = null;


}
