//
//  @(#)GUISupport.java		12/2002
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

import dip.order.Support;
import info.jdip.test.loading.IgnoreComparisonResult;
import org.w3c.dom.svg.SVGGElement;

import java.awt.geom.Point2D;

public class GUISupport extends Support implements GUIOrder {

    @IgnoreComparisonResult
    private final static String CLICK_TO_SUPPORT_UNIT = "GUISupport.click_to_sup";
    @IgnoreComparisonResult
    private final static String NO_UNIT_TO_SUPPORT = "GUISupport.no_unit_to_sup";
    @IgnoreComparisonResult
    private final static String CANNOT_SUPPORT_SELF = "GUISupport.no_self_sup";
    @IgnoreComparisonResult
    private final static String CLICK_TO_SUPPORT_FROM = "GUISupport.click_to_sup_from";
    @IgnoreComparisonResult
    private final static String CLICK_TO_SUPPORT_HOLD = "GUISupport.click_to_sup_hold";
    @IgnoreComparisonResult
    private final static String SUP_DEST_NOT_ADJACENT = "GUISupport.sup_dest_not_adj";
    @IgnoreComparisonResult
    private final static String SUPPORTING_THIS_UNIT = "GUISupport.sup_this_unit";
    @IgnoreComparisonResult
    private final static String CLICK_TO_SUPPORT_MOVE = "GUISupport.click_to_sup_move";
    @IgnoreComparisonResult
    private final static String CLICK_TO_SUPPORT_CONVOYED_MOVE = "GUISupport.click_to_sup_conv_move";
    @IgnoreComparisonResult
    private final static String CANNOT_SUPPORT_MOVE_NONADJACENT = "GUISupport.move_nonadj";
    @IgnoreComparisonResult
    private final static String CANNOT_SUPPORT_MOVE_GENERAL = "GUISupport.move_bad";
    @IgnoreComparisonResult
    private final static String CANNOT_SUPPORT_ACROSS_DPB = "GUISupport.over_dpb";


    private transient static final int REQ_LOC = 3;
    private transient int currentLocNum = 0;
    private transient boolean dependentFound = false;    // true associated Move or Support order found
    private transient Point2D.Float failPt = null;
    private transient SVGGElement group = null;



}
