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

import dip.order.Orderable;
import dip.world.Province;


/**
 * Similar to an OrderResult, but allows the <b>optional</b> specification of:
 * <ul>
 * <li>the unit with which this unit bounces</li>
 * <li>the attack and defense strengths</li>
 * </ul>
 */
public class BouncedResult extends OrderResult {
    // instance fields
    private Province bouncer = null;
    private int atkStrength = -1;
    private int defStrength = -1;

    public BouncedResult(Orderable order) {
        super(order, ResultType.FAILURE, null);
    }// BouncedResult()


}// class BouncedResult
