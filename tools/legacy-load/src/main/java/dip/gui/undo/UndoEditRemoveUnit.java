//
//  @(#)UndoEditRemoveUnit.java	1.00	8/2002
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

import dip.world.Position;
import dip.world.Province;
import dip.world.Unit;


/**
 * UndoEditAddUnit is created any time a unit is added in Edit mode.
 */
public class UndoEditRemoveUnit extends XAbstractUndoableEdit {
    private static final String PRESENTATION_NAME = "Undo.edit.removeunit";

    // instance variables
    private final Position position;
    private final Province province;
    private final Unit unit;
    private final boolean isDislodged;


    public UndoEditRemoveUnit(UndoRedoManager urm, Position position, Province province, Unit unit, boolean isDislodged) {
        super(urm);
        this.position = position;
        this.unit = unit;
        this.province = province;
        this.isDislodged = isDislodged;
    }// UndoEditRemoveUnit


}// class UndoEditRemoveUnit
