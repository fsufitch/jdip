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



public interface GUIOrder extends Orderable {

    int LAYER_HIGHEST = 0;

    int LAYER_TYPICAL = 1;

    int LAYER_LOWEST = 2;


    String CLICK_TO_ISSUE = "GUIOrder.common.click";

    String NO_UNIT = "GUIOrder.common.nounit";

    String NO_DISLODGED_UNIT = "GUIOrder.common.nodislodgedunit";

    String NOT_OWNER = "GUIOrder.common.notowner";

    String COMPLETE = "GUIOrder.common.complete";

    String CANCELED = "GUIOrder.common.canceled";

    String CLICK_TO_CANCEL = "GUIOrder.common.clickcancel";

    String BORDER_INVALID = "GUIOrder.common.badborder";

    String NOT_IN_PROVINCE = "GUIOrder.common.notprovince";

    abstract class Parameter {
        private transient final String name;

        public Parameter(String name) {
            if (name == null) {
                throw new IllegalArgumentException();
            }

            this.name = name;
        }

        public String toString() {
            return name;
        }

        public int hashCode() {
            return name.hashCode();
        }

    }


}
