//	
//	@(#)Result.java		4/2002
//	
//	Copyright 2002 Zachary DelProposto. All rights reserved.
//	Use is subject to license terms.
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
//  Or from http://www.gnu.org/package dip.order.result;
//
package dip.order.result;

import dip.order.OrderFormatOptions;
import dip.world.Power;

import java.io.Serializable;


public class Result implements Serializable, Comparable<Result> {
    private static final OrderFormatOptions DEFAULT_OFO = OrderFormatOptions.createDefault();

    protected Power power = null;

    protected String message = "";    // message is never null

    public int compareTo(Result o) {

        // first: compare powers
        int compareResult = 0;
        if (o.power == null && this.power == null) {
            compareResult = 0;
        } else if (this.power == null && o.power != null) {
            return -1;
        } else if (this.power != null && o.power == null) {
            return +1;
        } else {
            // if these are equal, could be 0
            compareResult = this.power.compareTo(o.power);
        }

        // finally: compare messages
        return ((compareResult != 0) ? compareResult : message.compareTo(o.message));
    }// compareTo()


}// class Result
