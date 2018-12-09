//	
// 	@(#)Order.java	12/2002
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
package dip.order;

import dip.world.Location;
import dip.world.Phase;
import dip.world.Position;
import dip.world.Power;
import dip.world.Province;
import dip.world.TurnState;
import dip.world.Unit;

/**
 * This is the base class for all Order objects.
 * <p>
 * <b>When referring to an Order subclass, it is best to refer to an object
 * as an "Orderable" object rather than an "Order" object.
 * </b>
 * <br>
 * For example:
 * <pre>
 * 		GUIOrderFactory gof = new GUIOrderFactory();
 *
 * 		Order order = OrderFactory.getDefault().createHold(a,b,c);			// ok
 * 		Orderable order = OrderFactory.getDefault().createHold(a,b,c);		// dangerous
 *
 * 		Order order = gof.createHold(a,b,c);			// this may fail!!
 * 		Orderable order = gof.createHold(a,b,c);		// ok
 * 	</pre>
 * <p>
 * This class provides default implementations for many Order interface
 * methods, as well as protected convenience methods.
 * <p>
 * A note on serialization: provided are several internal fields
 * which can be used to 'upgrade' objects as needed. These fields
 * are for future use, but their presence enables future upgradibility.
 */
public abstract class Order implements Orderable, java.io.Serializable {
    protected static final String ORD_VAL_BORDER = "ORD_VAL_BORDER";
    // resource keys
    private static final String ORD_VAL_NOUNIT = "ORD_VAL_NOUNIT";
    private static final String ORD_VAL_BADPOWER = "ORD_VAL_BADPOWER";
    private static final String ORD_VAL_BADTYPE = "ORD_VAL_BADTYPE";
    private static final String ORD_VAL_SZ_RETREAT = "ORD_VAL_SZ_RETREAT";
    private static final String ORD_VAL_SZ_ADJUST = "ORD_VAL_SZ_ADJUST";
    private static final String ORD_VAL_SZ_MOVEMENT = "ORD_VAL_SZ_MOVEMENT";
    //private static final String ORD_VAL_SZ_SETUP = "ORD_VAL_SZ_SETUP";
    //private static final String ORD_VAL_PWR_DISORDER = "ORD_VAL_PWR_DISORDER";
    private static final String ORD_VAL_PWR_ELIMINATED = "ORD_VAL_PWR_ELIMINATED";
    private static final String ORD_VAL_PWR_INACTIVE = "ORD_VAL_PWR_INACTIVE";
    /**
     * Power who gave the order to the unit
     */
    protected Power power = null;

    /**
     * Location of the ordered unit
     */
    protected Location src = null;

    /**
     * Type of the ordered unit
     */
    protected Unit.Type srcUnitType = null;


    /**
     * No-arg constructor
     */
    protected Order() {
    }// Order()


    /**
     * Constructor for the Order object
     *
     * @param power   Power giving the Order
     * @param src     Location of the ordered unit
     * @param srcUnit Unit type
     * @since
     */
    protected Order(Power power, Location src, Unit.Type srcUnit) {

        if (power == null || src == null || srcUnit == null) {
            throw new IllegalArgumentException("null parameter(s)");
        }

        this.power = power;
        this.src = src;
        this.srcUnitType = srcUnit;
    }// Order()


    public final Location getSource() {
        return src;
    }// getSource()


    public final Unit.Type getSourceUnitType() {
        return srcUnitType;
    }// getSourceUnitType()


    public final Power getPower() {
        return power;
    }// getPower()


    /**
     * Convenience method for matching unit types.
     * <p>
     * If a type is undefined, the type is derived from the existing unit. If the
     * existing unit is not found (or mismatched), an exception is thrown.
     */
    protected final Unit.Type getValidatedUnitType(Province province, Unit.Type unitType, Unit unit) {
        if (unit == null) {
            throw new RuntimeException(ORD_VAL_NOUNIT + province);
        }

        if (unitType.equals(Unit.Type.UNDEFINED)) {
            // make unitType correct.
            return unit.getType();
        } else {
            if (!unitType.equals(unit.getType())) {
                throw new RuntimeException(ORD_VAL_BADTYPE + province +
                        unit.getType().getFullNameWithArticle() + unitType.getFullNameWithArticle());
            }
        }

        return unitType;
    }// getValidatedUnitType()


    /**
     * Validates the given Power
     */
    protected final void checkPower(Power power, TurnState turnState, boolean checkIfActive) {
        Position position = turnState.getPosition();
        if (position.isEliminated(power)) {
            throw new RuntimeException(ORD_VAL_PWR_ELIMINATED + power);
        }
        if (!power.isActive() && checkIfActive) {
            throw new RuntimeException(ORD_VAL_PWR_INACTIVE + power);
        }
    }// checkPower()


    /**
     * Convenience method to check that we are in the Retreat phase
     */
    protected final void checkSeasonRetreat(TurnState state, String orderName) {
        if (state.getPhase().getPhaseType() != Phase.PhaseType.RETREAT) {
            throw new RuntimeException((ORD_VAL_SZ_RETREAT + orderName));
        }
    }// checkSeasonRetreat()


    /**
     * Convenience method to check that we are in the Adjustment phase
     */
    protected final void checkSeasonAdjustment(TurnState state, String orderName) {
        if (state.getPhase().getPhaseType() != Phase.PhaseType.ADJUSTMENT) {
            throw new RuntimeException(ORD_VAL_SZ_ADJUST + orderName);
        }
    }// checkSeasonAdjustment()


    /**
     * Convenience method to check that we are in the Movement phase
     */
    protected final void checkSeasonMovement(TurnState state, String orderName) {
        if (state.getPhase().getPhaseType() != Phase.PhaseType.MOVEMENT) {
            throw new RuntimeException((ORD_VAL_SZ_MOVEMENT + orderName));
        }
    }// checkSeasonMovement()


    /**
     * Convenience Method: prints the beginning of an order in a verbose format.
     * <br>
     * Example: France: Army Spain/sc
     */
    protected final void appendFull(StringBuffer sb) {
        sb.append(power);
        sb.append(": ");
        sb.append(srcUnitType.getFullName());
        sb.append(' ');
        src.appendFull(sb);
    }// appendFull()


    /**
     * Convenience Method: prints the beginning of an order in a brief format.
     * <br>
     * Example: France: Army spa/sc
     */
    protected final void appendBrief(StringBuffer sb) {
        sb.append(power);
        sb.append(": ");
        sb.append(srcUnitType.getShortName());
        sb.append(' ');
        src.appendBrief(sb);
    }// appendBrief()


    //
    //
    // java.lang.Object method implementations
    //
    //

    /**
     * For debugging: calls toBriefString(). Note this will fail if order is null.
     */
    public String toString() {
        return toBriefString();
    }// toString()


    /**
     * Determines if the orders are equal.
     * <p>
     * Note that full equality MUST be implemented for each
     * subclassed Order object! Subclasses are advised to call
     * the super method for assistance.
     */
    public boolean equals(Object obj) {
        // speedy reference check
        if (this == obj) {
            return true;
        } else if (obj instanceof Order) {
            Order o = (Order) obj;

            return power.equals(o.power) &&
                    src.equals(o.src) &&
                    srcUnitType.equals(o.srcUnitType);
        }

        return false;
    }// equals()

}// abstract class Order


