// 	
//  @(#)Move.java	4/2002
// 	
//  Copyright 2002-2004 Zachary DelProposto. All rights reserved.
//  Use is subject to license terms.
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
package dip.order;

import dip.world.Location;
import dip.world.Power;
import dip.world.Province;
import dip.world.Unit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * Implementation of the Move order.
 * <p>
 * This has been updated to support the 2003-Dec-16 DATC, specifically,
 * section 4.A.3.
 */
public class Move extends Order {
    // il8n constants
	/*
	private static final String MOVE_VAL_CONVOY_WARNING = "MOVE_VAL_CONVOY_WARNING";
	private static final String MOVE_VAL_ARMY_CONVOY = "MOVE_VAL_ARMY_CONVOY";
	*/
    private static final String MOVE_VAL_SRC_EQ_DEST = "MOVE_VAL_SRC_EQ_DEST";
    private static final String MOVE_VAL_UNIT_ADJ = "MOVE_VAL_UNIT_ADJ";
    private static final String MOVE_VAL_ADJ_UNLESS_CONVOY = "MOVE_VAL_ADJ_UNLESS_CONVOY";
    private static final String MOVE_VAL_BAD_ROUTE_SRCDEST = "MOVE_VAL_BAD_ROUTE_SRCDEST";
    private static final String MOVE_VAL_BAD_ROUTE = "MOVE_VAL_BAD_ROUTE";
    private static final String MOVE_VER_NO_ROUTE = "MOVE_VER_NO_ROUTE";
    private static final String MOVE_VER_CONVOY_INTENT = "MOVE_VER_CONVOY_INTENT";
    private static final String MOVE_EVAL_BAD_ROUTE = "MOVE_EVAL_BAD_ROUTE";
    private static final String MOVE_FAILED = "MOVE_FAILED";
    private static final String MOVE_FAILED_NO_SELF_DISLODGE = "MOVE_FAILED_NO_SELF_DISLODGE";
    private static final String MOVE_FORMAT = "MOVE_FORMAT";
    private static final String MOVE_FORMAT_EXPLICIT_CONVOY = "MOVE_FORMAT_EXPLICIT_CONVOY";
    private static final String CONVOY_PATH_MUST_BE_EXPLICIT = "CONVOY_PATH_MUST_BE_EXPLICIT";
    private static final String CONVOY_PATH_MUST_BE_IMPLICIT = "CONVOY_PATH_MUST_BE_IMPLICIT";


    // constants: names
    private static final String orderNameBrief = "M";
    private static final String orderNameFull = "Move";
    private static final transient String orderFormatString = (MOVE_FORMAT);
    private static final transient String orderFormatExCon = (MOVE_FORMAT_EXPLICIT_CONVOY);    // explicit convoy format

    // instance variables
    protected Location dest = null;
    protected ArrayList<Province[]> convoyRoutes = null;    // contains *defined* convoy routes; null if none.
    protected boolean _isViaConvoy = false;                    // 'true' if army was explicitly ordered to convoy.
    protected boolean _isConvoyIntent = false;                // 'true' if we determine that intent is to convoy. MUST be set to same initial value as _isViaConvoy
    protected boolean _isAdjWithPossibleConvoy = false;        // 'true' if an army with an adjacent move has a possible convoy route move too
    protected boolean _fmtIsAdjWithConvoy = false;            // for OrderFormat ONLY. 'true' if explicit convoy AND has land route.
    protected boolean _hasLandRoute = false;                    // 'true' if move has an overland route.

    /**
     * Creates a Move order
     */
    protected Move() {
        super();
    }// Move()

    /**
     * Creates a Move order
     */
    protected Move(Power power, Location src, Unit.Type srcUnitType, Location dest) {
        this(power, src, srcUnitType, dest, false);
    }// Move()

    /**
     * Creates a Move order, with optional convoy preference.
     */
    protected Move(Power power, Location src, Unit.Type srcUnitType, Location dest, boolean isConvoying) {
        super(power, src, srcUnitType);

        if (dest == null) {
            throw new IllegalArgumentException("null argument(s)");
        }

        this.dest = dest;
        this._isViaConvoy = isConvoying;
        this._isConvoyIntent = this._isViaConvoy;        // intent: same initial value as _isViaConvoy
    }// Move()

    /**
     * Creates a Move order with an explicit convoy route.
     * The convoyRoute array must have a length of 3 or more, and not be null.
     */
    protected Move(Power power, Location src, Unit.Type srcUnitType, Location dest, Province[] convoyRoute) {
        this(power, src, srcUnitType, dest, true);

        if (convoyRoute == null || convoyRoute.length < 3) {
            throw new IllegalArgumentException("bad or missing route");
        }

        convoyRoutes = new ArrayList<>(1);
        convoyRoutes.add(convoyRoute);
    }// Move()


    /**
     * Creates a Move order with multiple explicit convoy routes.
     * Each entry in routes must be a single-dimensional Province array.
     */
    protected Move(Power power, Location src, Unit.Type srcUnitType, Location dest, List<Province[]> routes) {
        this(power, src, srcUnitType, dest, true);

        if (routes == null) {
            throw new IllegalArgumentException("null routes");
        }

        // TODO: we don't check the routes very strictly.
        convoyRoutes = new ArrayList<>(routes);
    }// Move()


    /**
     * Returns the destination Location of this Move
     */
    public Location getDest() {
        return dest;
    }


    /**
     * Returns true if this Move was explicitly ordered to be by convoy,
     * either by specifying "by convoy" or "via convoy" after the move
     * order, or, by giving an explicit convoy path.
     * <p>
     * Note that this is <b>not</b> always true for all convoyed moves;
     * to check if a move is convoyed, see isConvoying().
     * <p>
     * Note that explicitly ordering a convoy doesn't really matter
     * unless there are <b>both</b> a land route and a convoy route. See
     * Dec-16-2003 DATC 6.G.8.
     */
    public boolean isViaConvoy() {
        return _isViaConvoy;
    }// isExplicitConvoy()


    /**
     * Returns true if an Army can possibly Move to its destination with a convoy,
     * even though it is adjacent to its destination. This is only really important
     * when a Move to an adjacent province could occur by land or by convoy.
     * <p>
     * <b>Important Note:</b> This value will not be properly determined
     * until <code>validate()</code> has been called.
     */
    public boolean isAdjWithPossibleConvoy() {
        return _isAdjWithPossibleConvoy;
    }// isAdjWithPossibleConvoy()


    /**
     * Returns true if the Intent of this Move order is to Convoy.
     * This is true when:
     * <ul>
     * <li>isViaConvoy() is false, Source and Dest are not adjacent,
     * both coastal, and there is
     * a theoretical convoy path between them. <b>Note:</b> this can
     * only be determined after <code>validate()</code> has been
     * called.</li>
     * <li>hasDualRoute() is true, isViaConvoy() is false, and there is a
     * matching convoy path between source and dest with at least one
     * Convoying Fleet of the same Power as this Move (thus signalling
     * "intent to Convoy"). <b>Note:</b> this can only be determined
     * after <code>verify()</code> has been called.</li>
     * <li>isViaConvoy is true, and hasDualRoute() are true. This also can
     * only be determined after <code>verify()</code> has been called.</li>
     * </ul>
     * <b>Note:</b> if this method (or isConvoying()) is to be used during
     * the verify() stage by other orders, they <b>absolutely</b> must check that
     * the Move has already been verified, since move verification can change
     * the value of this method.
     */
    public boolean isConvoyIntent() {
        return _isConvoyIntent;
    }// isConvoyIntent()


    /**
     * This is implemented for compatibility; it is no different than
     * <code>isConvoyIntent()</code>.
     */
    public boolean isConvoying() {
        return isConvoyIntent();
    }// isConvoying()


    /**
     * Returns, if set, an explicit convoy route (or the first explicit
     * route if there are multiple routes). Returns null if not convoying
     * or no explicit route was defined.
     */
    public Province[] getConvoyRoute() {
        return (convoyRoutes != null) ? convoyRoutes.get(0) : null;
    }// getConvoyRoute()

    /**
     * Returns, if set, all explicit convoy routes as an unmodifiable List.
     * Returns null if not convoying or no explicit route(s) were defined.
     */
    public List<Province[]> getConvoyRoutes() {
        return (convoyRoutes != null) ? Collections.unmodifiableList(convoyRoutes) : null;
    }// getConvoyRoute()


    public String getFullName() {
        return orderNameFull;
    }// getFullName()

    public String getBriefName() {
        return orderNameBrief;
    }// getBriefName()


    // order formatting
    public String getDefaultFormat() {
        return (convoyRoutes == null) ? orderFormatString : orderFormatExCon;
    }// getDefaultFormat()


    public String toBriefString() {
        StringBuffer sb = new StringBuffer(64);


        if (convoyRoutes != null) {
            // print all explicit routes
            sb.append(power);
            sb.append(": ");
            sb.append(srcUnitType.getShortName());
            sb.append(' ');
            final int size = convoyRoutes.size();
            for (int i = 0; i < size; i++) {
                final Province[] path = convoyRoutes.get(i);
                sb.append(sb);

                // prepare for next path
                if (i < (size - 1)) {
                    sb.append(", ");
                }
            }
        } else {
            super.appendBrief(sb);
            sb.append('-');
            dest.appendBrief(sb);

            if (isViaConvoy()) {
                sb.append(" by convoy");
            }
        }

        return sb.toString();
    }// toBriefString()


    public String toFullString() {
        StringBuffer sb = new StringBuffer(128);

        if (convoyRoutes != null) {
            // print all explicit routes
            sb.append(power);
            sb.append(": ");
            sb.append(srcUnitType.getFullName());
            sb.append(' ');
            final int size = convoyRoutes.size();
            for (int i = 0; i < size; i++) {
                final Province[] path = convoyRoutes.get(i);
                sb.append(path);

                // prepare for next path
                if (i < (size - 1)) {
                    sb.append(", ");
                }
            }
        } else {
            super.appendFull(sb);
            sb.append(" -> ");
            dest.appendFull(sb);

            if (isViaConvoy()) {
                sb.append(" by convoy");
            }
        }

        return sb.toString();
    }// toFullString()


    public boolean equals(Object obj) {
        if (obj instanceof Move) {
            Move move = (Move) obj;
            return super.equals(move)
                    && this.dest.equals(move.dest)
                    && this.isViaConvoy() == move.isViaConvoy();
        }
        return false;
    }// equals()


}// class Move
