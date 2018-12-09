//
//  @(#)GUIOrder.java	12/2002
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

import dip.order.Orderable;


/**
 * GUI extension of Orderable that is used for GUI input/handling
 * and GUI order rendering.
 * <p>
 * It is recommended that toString() is not used for GUI orders, as
 * the formatting is not as precise (or controllable) as with
 * toFormattedString() (or using OrderFormat.format()).
 */
public interface GUIOrder extends Orderable {
    // public, numeric, Z-Order constants
    /**
     * Z-order layer for orders that are drawn over units
     */
    int LAYER_HIGHEST = 0;        // build, remove, disband
    /**
     * Typical Z-order layer; drawn under units
     */
    int LAYER_TYPICAL = 1;        // most orders
    /**
     * Lowest Z-order units; all other order layers drawn over this layer.
     */
    int LAYER_LOWEST = 2;        // convoy, supports


    //
    //	Common Message Constants
    //
    //
    /**
     * Message displayed when an order may be issued by a mouse click
     */
    String CLICK_TO_ISSUE = "GUIOrder.common.click";
    /**
     * Message displayed when there is no unit in given location
     */
    String NO_UNIT = "GUIOrder.common.nounit";
    /**
     * Message displayed when there is no dislodged unit in given location
     */
    String NO_DISLODGED_UNIT = "GUIOrder.common.nodislodgedunit";
    /**
     * Message displayed when a power (set by setLockedPower()) does not control the given unit
     */
    String NOT_OWNER = "GUIOrder.common.notowner";
    /**
     * Message displayed when order entry is complete.
     */
    String COMPLETE = "GUIOrder.common.complete";
    /**
     * Message displayed when order is cancelled.
     */
    String CANCELED = "GUIOrder.common.canceled";
    /**
     * Message indicating click to cancel order.
     */
    String CLICK_TO_CANCEL = "GUIOrder.common.clickcancel";
    /**
     * Message indicating cannot give order due to a Border constraint.
     */
    String BORDER_INVALID = "GUIOrder.common.badborder";
    /**
     * Message indicating that pointer is not over a province
     */
    String NOT_IN_PROVINCE = "GUIOrder.common.notprovince";

    //
    // Methods for factory class
    //
    //


    /**
     * Typesafe Enum base class for Order object parameters.	<br>
     * GUIOrders which require Parameters must subclass this.
     */
    abstract class Parameter {
        private transient final String name;

        /**
         * Constructor
         */
        public Parameter(String name) {
            if (name == null) {
                throw new IllegalArgumentException();
            }

            this.name = name;
        }// Parameter()

        /**
         * gets the name of Parameter
         */
        public String toString() {
            return name;
        }// toString()

        /**
         * hashCode implementation
         */
        public int hashCode() {
            return name.hashCode();
        }// hashCode()

    }// nested class Parameter


}// interface GUIOrder
