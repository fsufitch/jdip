//
//  @(#)VictoryConditions.java		4/2002
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
package dip.world;

import dip.order.result.Result;
import dip.world.Phase.PhaseType;
import dip.world.Phase.SeasonType;

import java.util.List;

/**
 * Establishes the conditions required to determine who wins a game, and contains
 * methods to evaluate if these condtions are met during adjudication.
 * <p>
 */
public class VictoryConditions implements java.io.Serializable {
    // il8n
    private static final String VC_MAX_GAME_TIME = "VC_MAX_GAME_TIME";
    private static final String VC_DRAW = "VC_DRAW";
    private static final String VC_WIN_SINGLE = "VC_WIN_SINGLE";
    private static final String VC_MAX_NO_SC_CHANGE = "VC_MAX_NO_SC_CHANGE";

    // class variables
    protected final int numSCForVictory;        // SCs required for victory; 0 if ignored
    protected final int maxYearsNoSCChange;        // max years w/o supply-center chaning hands; 0 if ignored
    protected final int maxGameTimeYears;        // max time, in years, a game may last
    protected final int initialYear;            // starting game year

    // transient variables
    protected transient List<Result> evalResults = null;

    /**
     * VictoryConditions constructor
     */
    public VictoryConditions(int numSCForVictory, int maxYearsNoSCChange, int maxGameTimeYears,
                             Phase initialPhase) {
        if (maxGameTimeYears < 0 || numSCForVictory < 0 || maxYearsNoSCChange < 0) {
            throw new IllegalArgumentException("arg: < 0; use 0 to disable");
        }

        if (initialPhase == null) {
            throw new IllegalArgumentException("args invalid");
        }


        if (maxGameTimeYears == 0 && numSCForVictory == 0 && maxYearsNoSCChange == 0) {
            throw new IllegalArgumentException("no conditions set!");
        }

        this.numSCForVictory = numSCForVictory;
        this.maxYearsNoSCChange = maxYearsNoSCChange;
        this.maxGameTimeYears = maxGameTimeYears;
        this.initialYear = initialPhase.getYear();
    }// VictoryConditions()


    /**
     * Returns the number of Supply Centers required for victory.
     */
    public int getSCsRequiredForVictory() {
        return numSCForVictory;
    }

    /**
     * Returns number of Years without any Supply Center being captured for the game to end.
     */
    public int getYearsWithoutSCChange() {
        return maxYearsNoSCChange;
    }

    /**
     * Returns number maximum game duration, in years.
     */
    public int getMaxGameDurationYears() {
        return maxGameTimeYears;
    }


    /**
     * Returns the Result(s) of evaluate(). This will return an empty list if
     * evaluate() has not been called or returned false.
     */
    public List<Result> getEvaluationResults() {
        return evalResults;
    }// getEvaluationResults()


    /**
     * Given a year, finds the phase in which supply-center-changes could occur.
     * <p>
     * the adjudicator marks the NEXT phase, indicating that supply center changes
     * have occured. Thus, the next phase will be either RETREAT (if a change occured
     * during movement) or ADJUSTMENT (if a change occured during retreat)
     * <p>
     * where supply-center-changes could occur. We actually will check both;
     */
    private boolean getIfSCChangeOccured(World world, int year) {
        boolean value = false;

        TurnState tsRetreat = world.getTurnState(new Phase(SeasonType.FALL, year, PhaseType.RETREAT));
        TurnState tsAdjustment = world.getTurnState(new Phase(SeasonType.FALL, year, PhaseType.ADJUSTMENT));

        if (tsRetreat != null) {
            value |= tsRetreat.getSCOwnerChanged();
        }

        if (tsAdjustment != null) {
            value |= tsAdjustment.getSCOwnerChanged();
        }

        return value;
    }// getLastSCChangePhase()


}// class VictoryConditions
