//
//  @(#)UndoResolve.java	8/2002
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

import dip.order.result.Result;
import dip.world.TurnState;

import java.util.List;


/**
 * UndoResolve is created when orders are resolved (adjudicated).
 * <p>
 * Note: we clear all results; we may (in the future) want to save Edit results.
 */
public class UndoResolve extends XAbstractUndoableEdit {
    // instance variables
    private final static String PRESENTATION_NAME_PREFIX = "Undo.resolve";
    private TurnState resolvedTS;
    private TurnState nextTS;
    private List<Result> resolvedTSResults;


    /**
     * Create an UndoResolve object.
     */
    public UndoResolve(UndoRedoManager urm, TurnState resolved, TurnState next) {
        super(urm);

        if (resolved == null) {
            throw new IllegalArgumentException();
        }

        this.resolvedTS = resolved;
        this.nextTS = next;        // this may be null (e.g., if game has been won)
        this.resolvedTSResults = resolvedTS.getResultList();
    }// UndoResolve


}// class UndoResolve

