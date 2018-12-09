//
// 	@(#)OrderResult.java		4/2002
//
// 	Copyright 2002 Zachary DelProposto. All rights reserved.
// 	Use is subject to license terms.
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

import java.io.Serializable;

/**
 * A message sent to a specific Power that refers to a specific order.
 * The message is classified according to ResultType (see for details).
 * <p>
 * More than one OrderResult may exist for a single order.
 */
public class OrderResult extends Result {
    // instance fields
    /**
     * The ResultType
     */
    protected ResultType resultType = null;
    /**
     * The Order to which this Result refers
     */
    protected Orderable order = null;


    /**
     * no-arg constructor for subclasses
     */
    protected OrderResult() {
    }// OrderResult()

    /**
     * Create an OrderResult with the given Order and Message.
     * A null order is not permissable.
     */
    public OrderResult(Orderable order, String message) {
        this(order, ResultType.TEXT, message);
    }// OrderResult()


    /**
     * Create an OrderResult with the given Order, ResultType, and Message.
     * A null Order or ResultType is not permissable.
     */
    public OrderResult(Orderable order, ResultType type, String message) {
        super(order.getPower(), message);
        if (type == null || order == null) {
            throw new IllegalArgumentException("null type or order");
        }

        this.resultType = type;
        this.order = order;
    }// OrderResult()


    /**
     * Type-Safe enumerated categories of OrderResults.
     */
    public static class ResultType implements Serializable, Comparable<ResultType> {
        // key constants
        private static final String KEY_VALIDATION_FAILURE = "VALIDATION_FAILURE";
        /**
         * ResultType indicating that order validation failed
         */
        public static final ResultType VALIDATION_FAILURE = new ResultType(KEY_VALIDATION_FAILURE, 10);
        private static final String KEY_SUCCESS = "SUCCESS";
        /**
         * ResultType indicating the order was successful
         */
        public static final ResultType SUCCESS = new ResultType(KEY_SUCCESS, 20);
        private static final String KEY_FAILURE = "FAILURE";
        /**
         * ResultType indicating the order has failed
         */
        public static final ResultType FAILURE = new ResultType(KEY_FAILURE, 30);
        private static final String KEY_DISLODGED = "DISLODGED";

        // enumerated constants
        /**
         * ResultType indicating the order's source unit has been dislodged
         */
        public static final ResultType DISLODGED = new ResultType(KEY_DISLODGED, 40);
        private static final String KEY_CONVOY_PATH_TAKEN = "CONVOY_PATH_TAKEN";
        /**
         * ResultType indicating what convoy path a convoyed unit used
         */
        public static final ResultType CONVOY_PATH_TAKEN = new ResultType(KEY_CONVOY_PATH_TAKEN, 50);
        private static final String KEY_TEXT = "TEXT";
        /**
         * ResultType for a general (not otherwise specified) message
         */
        public static final ResultType TEXT = new ResultType(KEY_TEXT, 60);        // text message only
        private static final String KEY_SUBSTITUTED = "SUBSTITUTED";
        /**
         * ResultType indicating that the order was substituted with another order
         */
        public static final ResultType SUBSTITUTED = new ResultType(KEY_SUBSTITUTED, 70);

        // instance variables
        private final String key;
        private final int ordering;

        protected ResultType(String key, int ordering) {
            if (key == null) {
                throw new IllegalArgumentException("null key");
            }

            this.ordering = ordering;
            this.key = key;
        }// ResultType()
		

		/*
			equals():

			We use Object.equals(), which just does a test of
			referential equality.

		*/

        /**
         * For debugging: return the name
         */
        public String toString() {
            return key;
        }// toString()

        /**
         * Sorts the result type
         */
        public int compareTo(ResultType obj) {
            return (ordering - obj.ordering);
        }// compareTo()


        /**
         * Assigns serialized objects to a single constant reference
         */
        protected Object readResolve()
                throws java.io.ObjectStreamException {
            ResultType rt = null;

            if (key.equals(KEY_VALIDATION_FAILURE)) {
                rt = VALIDATION_FAILURE;
            } else if (key.equals(KEY_SUCCESS)) {
                rt = SUCCESS;
            } else if (key.equals(KEY_FAILURE)) {
                rt = FAILURE;
            } else if (key.equals(KEY_DISLODGED)) {
                rt = DISLODGED;
            } else if (key.equals(KEY_CONVOY_PATH_TAKEN)) {
                rt = CONVOY_PATH_TAKEN;
            } else if (key.equals(KEY_TEXT)) {
                rt = TEXT;
            } else if (key.equals(KEY_SUBSTITUTED)) {
                rt = SUBSTITUTED;
            } else {
                throw new java.io.InvalidObjectException("Unknown ResultType: " + key);
            }

            return rt;
        }// readResolve()
    }// nested class ResultType

}// class OrderResult
