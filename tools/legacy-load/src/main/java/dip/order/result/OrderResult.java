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

public class OrderResult extends Result {

    protected ResultType resultType = null;

    protected Orderable order = null;


    public static class ResultType implements Serializable, Comparable<ResultType> {
        private static final String KEY_VALIDATION_FAILURE = "VALIDATION_FAILURE";

        public static final ResultType VALIDATION_FAILURE = new ResultType(KEY_VALIDATION_FAILURE, 10);
        private static final String KEY_SUCCESS = "SUCCESS";

        public static final ResultType SUCCESS = new ResultType(KEY_SUCCESS, 20);
        private static final String KEY_FAILURE = "FAILURE";

        public static final ResultType FAILURE = new ResultType(KEY_FAILURE, 30);
        private static final String KEY_DISLODGED = "DISLODGED";

        public static final ResultType DISLODGED = new ResultType(KEY_DISLODGED, 40);
        private static final String KEY_CONVOY_PATH_TAKEN = "CONVOY_PATH_TAKEN";

        public static final ResultType CONVOY_PATH_TAKEN = new ResultType(KEY_CONVOY_PATH_TAKEN, 50);
        private static final String KEY_TEXT = "TEXT";

        public static final ResultType TEXT = new ResultType(KEY_TEXT, 60);        // text message only
        private static final String KEY_SUBSTITUTED = "SUBSTITUTED";

        public static final ResultType SUBSTITUTED = new ResultType(KEY_SUBSTITUTED, 70);

        private final String key;
        private final int ordering;

        protected ResultType(String key, int ordering) {
            if (key == null) {
                throw new IllegalArgumentException("null key");
            }

            this.ordering = ordering;
            this.key = key;
        }

        public int compareTo(ResultType obj) {
            return (ordering - obj.ordering);
        }// compareTo()


    }

}
