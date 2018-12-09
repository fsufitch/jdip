//
//  @(#)UndoClearSelected.java	1.00	4/1/2002
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
package dip.gui.undo;

import javax.swing.undo.CompoundEdit;

/**
 * UndoClearSelected
 * <p>
 * Just a fancy name for a compound edit.
 * <p>
 * If only one 'selected' edit, it will display the name of the edit. But if
 * there are multiple edits, then it will just display "Clear Selected".
 */

public class UndoClearSelected extends CompoundEdit implements java.io.Serializable {
    private final static String PRESENTATION_NAME = "Undo.order.clearselected";

    public UndoClearSelected() {
        super();
    }// UndoClearSelected


}// class UndoClearSelected
