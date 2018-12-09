//	
//	@(#)BouncedResult.java	5/2003
//	
//	Copyright 2003 Zachary DelProposto. All rights reserved.
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
import dip.order.Orderable;


/**
 * An OrderResult that applies specifically to Move orders that
 * fail because they depend upon another Move to succeed (and
 * that move did not succeed).
 * <p>
 * This will print out the Order that caused the failure.
 * <p>
 */
public class DependentMoveFailedResult extends OrderResult {
    // instance fields
    private Orderable dependentOrder = null;


    public DependentMoveFailedResult(Orderable order, Orderable dependentOrder) {
        super(order, ResultType.FAILURE, null);
        if (dependentOrder == null) {
            throw new IllegalArgumentException();
        }
        this.dependentOrder = dependentOrder;
    }// DependentMoveFailedResult()





    /**
     * Creates an appropriate internationalized text
     * message given the set and unset parameters.
     */
    public String getMessage(OrderFormatOptions ofo) {
		/*
		{0} : the dependent order, formatted with OrderFormat
		*/

        // return formatted message
        return "DependentMoveFailedResult.message"+
                dependentOrder.toBriefString();
    }// getMessage()


    /**
     * Primarily for debugging.
     */
    public String toString() {
        StringBuilder sb = new StringBuilder(256);
        sb.append(super.toString());

        sb.append("Dependent Order: ");
        sb.append(dependentOrder);

        return sb.toString();
    }// toString()


}// class DependentMoveFailedResult
