//
//  @(#)F2FOrderDisplayPanel.java		6/2003
//
//  Copyright 2003 Zachary DelProposto. All rights reserved.
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
package dip.gui;

import dip.world.Power;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.util.HashMap;
import java.util.Iterator;


/**
 * The F2FOrderDisplayPanel: displayer of orders for Face-to-Face (F2F) games.
 * <p>
 * This is a subclass of ODP that manages F2F games.
 */
public class F2FOrderDisplayPanel  {


    public static class F2FState {
        private final HashMap<Power, Boolean> submittedMap;
        private Power currentPower;

        /**
         * Create an F2FState object
         */
        public F2FState() {
            submittedMap = new HashMap<>(11);
        }// F2FState()

        /**
         * Create an F2FState object from an existing F2FState object
         */
        public F2FState(F2FState f2fs) {
            if (f2fs == null) {
                throw new IllegalArgumentException();
            }

            synchronized (f2fs) {
                currentPower = f2fs.getCurrentPower();
                submittedMap = new HashMap<>(f2fs.submittedMap);
            }
        }// F2FState()

        /**
         * The current power (or null) who is entering orders.
         */
        public synchronized Power getCurrentPower() {
            return currentPower;
        }// getCurrentPower()

        /**
         * Set the current power (or null) who is entering orders.
         */
        public synchronized void setCurrentPower(Power power) {
            currentPower = power;
        }// setCurrentPower()

        /**
         * Get if the Power has submitted orders.
         */
        public synchronized boolean getSubmitted(Power power) {
            if (power == null) {
                throw new IllegalArgumentException();
            }
            return Boolean.TRUE.equals(submittedMap.get(power));
        }// getSubmitted()

        /**
         * Set if a power has submitted orders
         */
        public synchronized void setSubmitted(Power power, boolean value) {
            if (power == null) {
                throw new IllegalArgumentException();
            }
            submittedMap.put(power, value);
        }// setSubmitted()

        /**
         * Reset all powers to "not submitted" state.
         */
        public synchronized void clearSubmitted() {
            submittedMap.clear();
        }// clearSubmitted()

        /**
         * Get an iterator. Note that this <b>always</b> returns an iterator
         * on a <b>copy</b> of the F2FState.
         */
        public synchronized Iterator iterator() {
            final F2FState copy = new F2FState(this);
            return copy.submittedMap.entrySet().iterator();
        }// iterator()
    }// nested class F2FState


    /**
     * Handle Tab Pane events
     */
    private class TabListener implements ChangeListener {
        private boolean isEnabled = true;


        @Override
        public void stateChanged(ChangeEvent e) {

        }
    }// inner class TabListener


}// class F2FOrderDisplayPanel

