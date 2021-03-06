//
//	@(#)JudgeImport.java	1.00	6/2002
//
//	Copyright 2002 Zachary DelProposto. All rights reserved.
//	Use is subject to license terms.
//
//
//	This program is free software; you can redistribute it and/or modify
//	it under the terms of the GNU General Public License as published by
//	the Free Software Foundation; either version 2 of the License, or
//	(at your option) any later version.
//
//	This program is distributed in the hope that it will be useful,
//	but WITHOUT ANY WARRANTY; without even the implied warranty of
//	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//	GNU General Public License for more details.
//
//	You should have received a copy of the GNU General Public License
//	along with this program; if not, write to the Free Software
//	Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
//	Or from http://www.gnu.org/
//
package info.jdip.judge.parser;

import info.jdip.judge.parser.TurnParser.Turn;
import info.jdip.misc.Utils;
import info.jdip.order.Build;
import info.jdip.order.Disband;
import info.jdip.order.Move;
import info.jdip.order.NJudgeOrderParser.NJudgeOrder;
import info.jdip.order.OrderException;
import info.jdip.order.OrderFactory;
import info.jdip.order.Orderable;
import info.jdip.order.Remove;
import info.jdip.order.ValidationOptions;
import info.jdip.order.result.DislodgedResult;
import info.jdip.order.result.OrderResult;
import info.jdip.order.result.Result;
import info.jdip.order.result.SubstitutedResult;
import info.jdip.process.Adjustment;
import info.jdip.world.Location;
import info.jdip.world.Phase;
import info.jdip.world.Position;
import info.jdip.world.Power;
import info.jdip.world.Province;
import info.jdip.world.RuleOptions;
import info.jdip.world.TurnState;
import info.jdip.world.Unit;
import info.jdip.world.VictoryConditions;
import info.jdip.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Processes an entire game history to create a world.
 * <p>
 * TODO:
 * <br>positioning units with orders that failed parsing (e.g., a move to Switzerland (swi))
 */
final class JudgeImportHistory {
    private static final Logger logger = LoggerFactory.getLogger(JudgeImportHistory.class);
    /**
     * Regular expression for parsing what the Next phase is. This is only used to create the last
     * (final) phase.<p>
     * Capture groups: (1)Phase (2)Season (3)Year
     */
    public static final String PARSE_REGEX = "(?i)the\\snext\\sphase\\s.*will\\sbe\\s(\\p{Alpha}+)\\sfor\\s(\\p{Alpha}+)\\sof\\s((\\p{Digit}+))";
    public static final String END_FOF_GAME = "(?i)the game is over";
    public static final String START_POSITIONS = "(?i)Subject:\\s\\p{Alpha}+:\\p{Alnum}+\\s-\\s(\\p{Alpha})(\\p{Digit}+)(\\p{Alpha})";
    // constants
    private static final String STDADJ_MV_UNIT_DESTROYED = "STDADJ_MV_UNIT_DESTROYED";
    private static final String JIH_BAD_POSITION = "JP.import.badposition";
    private static final String JIH_NO_MOVEMENT_PHASE = "JP.history.nomovement";
    private static final String JIH_ORDER_PARSE_FAILURE = "JP.history.badorder";
    private static final String JIH_UNKNOWN_RESULT = "JP.history.unknownresult";

    // parsing parameters
    private static final String JIH_NO_DISLODGED_MATCH = "JP.history.dislodgedmatchfail";
    private static final String JIH_INVALID_RETREAT = "JP.history.badretreat";
    private static final String JIH_BAD_LAST_PHASE = "JP.history.badlastphase";
    // instance variables
    private final info.jdip.world.Map map;
    private final OrderFactory orderFactory;
    private final World world;
    private final ValidationOptions valOpts;
    private JudgeParser jp = null;
    private Position oldPosition = null;
    private HSCInfo[] homeSCInfo = null;
    private boolean finalTurn = false;

    /**
     * Create a JudgeImportHistory
     */
    protected JudgeImportHistory(OrderFactory orderFactory, World world, JudgeParser jp, Position oldPosition)
            throws IOException, PatternSyntaxException {
        this.orderFactory = orderFactory;
        this.world = world;
        this.jp = jp;
        this.oldPosition = oldPosition;
        this.map = world.getMap();

        // create a very strict validation object, loose seems to have some weird problems when importing.
        valOpts = new ValidationOptions();
        valOpts.setOption(ValidationOptions.KEY_GLOBAL_PARSING, ValidationOptions.VALUE_GLOBAL_PARSING_STRICT);

        processTurns();
    }// JudgeImportHistory()

    /**
     * Create a JudgeImportHistory and process a single turn
     */
    protected JudgeImportHistory(OrderFactory orderFactory, World world, JudgeParser jp, Turn turn)
            throws IOException, PatternSyntaxException {
        this.orderFactory = orderFactory;
        this.world = world;
        this.jp = jp;
        this.oldPosition = world.getLastTurnState().getPosition();
        this.map = world.getMap();

        // create a very strict validation object, loose seems to have some weird problems when importing.
        valOpts = new ValidationOptions();
        valOpts.setOption(ValidationOptions.KEY_GLOBAL_PARSING, ValidationOptions.VALUE_GLOBAL_PARSING_STRICT);

        processSingleTurn(turn);
    }// JudgeImportHistory()

    /**
     * Returns the World, with TurnStates & Positions added as appropriate.
     */
    protected World getWorld() {
        return world;
    }// getWorld()


    /**
     * Processes the Turn data, starting with the first Movement phase. An exception is
     * thrown if no Movement phase exists.
     */
    private void processTurns()
            throws IOException, PatternSyntaxException {
        // break data up into turns
        Turn[] turns = new TurnParser(jp.getText()).getTurns();
        //System.out.println("# of turns: "+turns.length);

        // find first movement phase, if any
        int firstMovePhase = -1;
        for (int i = 0; i < turns.length; i++) {
            if (turns[i].getPhase() != null) {
                if (turns[i].getPhase().getPhaseType() == Phase.PhaseType.MOVEMENT) {
                    firstMovePhase = i;
                    break;
                }
            }
        }

        // If we couldn't find the first movement phase... perhaps the game is just starting
        if (firstMovePhase == -1) {
            // Try to use the text info to create the game at its starting positions
            try {
                createStartingPositions();
                // Don't do the rest of this method, it will all fail.
                return;
            } catch (IOException e) {
                throw new IOException(Utils.getLocalString(JIH_NO_MOVEMENT_PHASE));
            }
        }

        //System.out.println("First move phase: "+firstMovePhase);

        // get home supply center information from the oldPosition object
        // and store it in HSCInfo object array, so that it can be set during each successive
        // turn.
        ArrayList<HSCInfo> hscList = new ArrayList<>(50);
        Province[] provinces = map.getProvinces();
        for (Province province : provinces) {
            Power power = oldPosition.getSupplyCenterHomePower(province);
            if (power != null) {
                hscList.add(new HSCInfo(province, power));
            }
        }
        homeSCInfo = hscList.toArray(new HSCInfo[hscList.size()]);

        // process all but the final phase
        for (int i = firstMovePhase; i < turns.length - 1; i++) {
            //System.out.println("processing turn: "+i+"; phase = "+turns[i].getPhase());
            if (i == 0) {
                procTurn(turns[i], null, null, false);
            } else if (i == 1) {
                procTurn(turns[i], turns[i - 1], null, false);
            } else {
                procTurn(turns[i], turns[i - 1], turns[i - 2], false);
            }
        }
        // process the last turn once more, but as the final turn, to allow proper positioning.
        finalTurn = true;
        if (turns.length == 1) {
            procTurn(turns[turns.length - 1], null, null, true);
        } else if (turns.length == 2) {
            procTurn(turns[turns.length - 1], turns[turns.length - 2], null, true);
        } else if (turns.length >= 3) {
            procTurn(turns[turns.length - 1], turns[turns.length - 2], turns[turns.length - 3], true);
        }

        Pattern endofgame = Pattern.compile(END_FOF_GAME);

        Matcher e = endofgame.matcher(turns[turns.length - 1].getText());

        if (!e.find()) {

            // create last (un-resolved) turnstate
            makeLastTurnState(turns[turns.length - 1]);

            // reprocess the last turn, again, not as final, so it looks right for viewing.
            finalTurn = false;
            if (turns.length == 1) {
                procTurn(turns[turns.length - 1], null, null, false);
            } else if (turns.length == 2) {
                procTurn(turns[turns.length - 1], turns[turns.length - 2], null, false);
            } else if (turns.length >= 3) {
                procTurn(turns[turns.length - 1], turns[turns.length - 2], turns[turns.length - 3], false);
            }
        } else {
            // The imported game has ended
            // Reprocess the last turn, again, not as final, so it looks right for viewing.
            finalTurn = false;
            procTurn(turns[turns.length - 1], turns[turns.length - 2], turns[turns.length - 3], false);
            // Set the game as ended.
            TurnState ts = world.getTurnState(turns[turns.length - 1].getPhase());
            VictoryConditions vc = world.getVictoryConditions();
            RuleOptions ruleOpts = world.getRuleOptions();
            Adjustment.AdjustmentInfoMap adjMap = Adjustment.getAdjustmentInfo(ts, ruleOpts, world.getMap().getPowers());
            vc.evaluate(ts, adjMap);
            List<Result> evalResults = ts.getResultList();
            evalResults.addAll(vc.getEvaluationResults());
            ts.setResultList(evalResults);
            ts.setEnded(true);
            ts.setResolved(true);
            world.setTurnState(ts);
        }

        // all phases have been processed; perform post-processing here.
    }// processTurns()

    /**
     * Processes a single turn.
     */
    private void processSingleTurn(Turn turn)
            throws IOException, PatternSyntaxException {
        // get home supply center information from the oldPosition object
        // and store it in HSCInfo object array, so that it can be set during each successive
        // turn.
        ArrayList<HSCInfo> hscList = new ArrayList<>(50);
        Province[] provinces = map.getProvinces();
        for (Province province : provinces) {
            Power power = oldPosition.getSupplyCenterHomePower(province);
            if (power != null) {
                hscList.add(new HSCInfo(province, power));
            }
        }
        homeSCInfo = hscList.toArray(new HSCInfo[hscList.size()]);

        // process the turn
        procTurn(turn, null, null, false);
        // save new turn state
        TurnState savedTS = world.getLastTurnState();

        // process the last turn once more, but as the final turn, to allow proper positioning.
        finalTurn = true;
        procTurn(turn, null, null, true);
        // create last (un-resolved) turnstate
        makeLastTurnState(turn);

        // inject the saved turnstate.
        world.setTurnState(savedTS);

        // TODO: do we have to check for victory conditions ?

    }// processSingleTurn()


    /**
     * Decides how to process the turn, based upon the phase information and past turns.
     * This is not the best way to process the turns, especially the adjustment phase,
     * but it works.
     */
    private void procTurn(Turn turn, Turn prevTurn, Turn thirdTurn, boolean positionPlacement)
            throws IOException {
        Phase phase = turn.getPhase();
        if (phase != null) {
            Phase.PhaseType phaseType = phase.getPhaseType();
            if (phaseType == Phase.PhaseType.MOVEMENT) {
                logger.trace( "MOVEMENT START");
                procMove(turn, positionPlacement);
                logger.trace( "MOVEMENT END");
            } else if (phaseType == Phase.PhaseType.RETREAT) {
                logger.trace( "RETREAT START");
                /*
                 * Set the proper positionPlacement value depending on if the turn being
                 * processed is the final turn. Set it back again when done.
                 */
                if (!finalTurn) {
                    procMove(prevTurn, !positionPlacement);
                } else {
                    procMove(prevTurn, positionPlacement);
                }

                procRetreat(turn, positionPlacement);

                if (!finalTurn) {
                    procMove(prevTurn, positionPlacement);
                } else {
                    procMove(prevTurn, !positionPlacement);
                }
                logger.trace( "RETREAT END");
            } else if (phaseType == Phase.PhaseType.ADJUSTMENT) {
                logger.trace( "ADJUSTMENT START");
                Phase.PhaseType prevPhaseType = Phase.PhaseType.MOVEMENT; // dummy
                if (prevTurn != null) {
                    Phase phase_p = prevTurn.getPhase();
                    prevPhaseType = phase_p.getPhaseType();
                }
                /*
                 * Much the same as above, set the proper positionPlacement value depending
                 * on the PhaseType and if the turn being processed is the final turn.
                 * Set it back again when done.
                 */
                if (prevPhaseType == Phase.PhaseType.MOVEMENT) {
                    if (!finalTurn) {
                        procMove(prevTurn, !positionPlacement);
                    } else {
                        procMove(prevTurn, positionPlacement);
                    }

                    procAdjust(turn, positionPlacement);

                    if (!finalTurn) {
                        procMove(prevTurn, positionPlacement);
                    } else {
                        procMove(prevTurn, !positionPlacement);
                    }

                } else {

                    if (!finalTurn) {
                        procMove(thirdTurn, !positionPlacement);
                        procRetreat(prevTurn, !positionPlacement);
                    } else {
                        procMove(thirdTurn, positionPlacement);
                        procRetreat(prevTurn, positionPlacement);
                    }

                    procAdjust(turn, positionPlacement);

                    if (!finalTurn) {
                        procRetreat(prevTurn, positionPlacement);
                        procMove(thirdTurn, positionPlacement);
                    } else {
                        procRetreat(prevTurn, !positionPlacement);
                        procMove(thirdTurn, !positionPlacement);
                    }
                }
                logger.trace( "ADJUSTMENT END");
            } else {
                throw new IllegalStateException("unknown phase type");
            }
        }
        logger.trace( "METHOD EXIT");
    }// procTurn()


    /**
     * Creates a TurnState object with the correct Phase, Position, and World information,
     * including setting things such as home supply centers and what not.
     * <p>
     * This method ensures that TurnState objects are properly (and consistently) initialized.
     */
    private TurnState makeTurnState(Turn turn, boolean positionPlacement) {
        // does the turnstate already exist?
        // it could, if we are importing orders into an already-existing game.
        //
        logger.debug("Phase: {}", turn.getPhase());

        // TODO: we can import judge games, but we cannot import judge games
        // into existing games successfully.
        //
        TurnState ts = new TurnState(turn.getPhase());
        ts.setWorld(world);
        ts.setPosition(new Position(world.getMap()));

        // note: we don't add the turnstate to the World object at this point (although we could), because
        // if a processing error occurs, we don't want a partial turnstate object in the World.

        // set Home Supply centers in position
        Position pos = ts.getPosition();
        for (HSCInfo aHomeSCInfo : homeSCInfo) {
            pos.setSupplyCenterHomePower(aHomeSCInfo.getProvince(), aHomeSCInfo.getPower());
        }

        return ts;
    }// makeTurnState()


    /**
     * Old method
     */
    private void procMove(Turn turn, final boolean positionPlacement)
            throws IOException {
        procMove(turn, positionPlacement, false);
    }// procMove()


    /**
     * Process a Movement phase turn
     */
    private void procMove(Turn turn, final boolean positionPlacement, final boolean isRetreatMoveProcessing)
            throws IOException {
        logger.debug("positionPlacement={}, isRetreatMoveProcessing={}", positionPlacement,isRetreatMoveProcessing);
        if (turn == null) {
            return;
        }

        // create TurnState
        logger.debug("Turn Phase: {}", turn.getPhase());

        TurnState ts = makeTurnState(turn, positionPlacement);
        List<Result> results = ts.getResultList();

        logger.debug("TurnState Phase: {}", ts.getPhase());

        // copy previous lastOccupier information into current turnstate.
        copyPreviousLastOccupierInfo(ts);

        // parse orders, and create orders for each unit
        final JudgeOrderParser jop = new JudgeOrderParser(map, orderFactory, turn.getText());
        final NJudgeOrder[] nJudgeOrders = jop.getNJudgeOrders();

        // get Position. Remember, this position contains no units.
        Position position = ts.getPosition();

        logger.info("NJudgeOrders: {}, results size: {} ", nJudgeOrders.length, results.size());

        // create units from start position
        for (final NJudgeOrder njo : nJudgeOrders) {
            final Orderable order = njo.getOrder();
            if (order == null) {
                logger.debug("Null order; njo: ", njo);
                throw new IOException("Internal error: null order in JudgeImportHistory::procMove()");
            }

            Location loc = order.getSource();
            final Unit.Type unitType = order.getSourceUnitType();
            final Power power = order.getPower();

            // validate location
            try {
                loc = loc.getValidated(unitType);
            } catch (OrderException e) {
                logger.error("There was a problem validation location. njo: {}, turn: {}", njo, turn.getText(),e);
                throw new IOException(e.getMessage());
            }

            // create unit, and add to Position
            // we may have to add the unit in a dislodged position.
            // We must first check for a 'dislodged' indicator.
            boolean isUnitDislodged = false;
            if (isRetreatMoveProcessing) {
                for (Result r : njo.getResults()) {
                    if (r instanceof OrderResult) {
                        if (((OrderResult) r).getResultType().equals(OrderResult.ResultType.DISLODGED)) {
                            isUnitDislodged = true;
                            break;
                        }
                    }
                }
            }

            Unit unit = new Unit(order.getPower(), unitType);
            unit.setCoast(loc.getCoast());
            position.setLastOccupier(loc.getProvince(), power);

            if (isUnitDislodged) {
                position.setDislodgedUnit(loc.getProvince(), unit);
                logger.info("Created a dislodged unit {} at {}.", unit, loc);
            } else {
                position.setUnit(loc.getProvince(), unit);
                logger.info("Created unit {} at {}. ", unit, loc);
            }

            // if we found a Wing unit, make sure Wing units are enabled.
            checkAndEnableWings(unitType);
        }


        // now, validate all order objects from the parsed order
        // also create result objects
        // create positions from successful orders...
        // note that we only need to set the last occupier for changing (moving)
        // units, but we will do it for all units for consistency
        //
        {
            // create orderMap, which maps powers to their respective order list
            Power[] powers = map.getPowers();

            logger.trace( "Created power->order mapping");

            HashMap<Power, List<Orderable>> orderMap = new HashMap<>(powers.length);
            for (Power power : powers) {
                orderMap.put(power, new LinkedList<>());
            }

            // process all orders
            final RuleOptions ruleOpts = world.getRuleOptions();

            for (final NJudgeOrder njo : nJudgeOrders) {
                final Orderable order = njo.getOrder();

                // first try to validate under strict settings; if fail, try
                // to validate under loose settings.
                try {
                    order.validate(ts, valOpts, ruleOpts);

                    List<Orderable> list = orderMap.get(order.getPower());
                    list.add(order);

                    results.addAll(njo.getResults());

                    logger.debug("Order ok: {}", order);
                } catch (OrderException e) {
                    logger.warn(" There was an error while processing order: {}", order, e);
                    logger.info( "      Retesting with loose validation");

                    //Try loosening the validation object
                    valOpts.setOption(ValidationOptions.KEY_GLOBAL_PARSING, ValidationOptions.VALUE_GLOBAL_PARSING_LOOSE);

                    /* Try the order once more.
                     * nJudge accepts illegal moves as valid as long as the syntax is valid.
                     * (Perhaps a few other things as well). jDip should accept these as well,
                     * even if the move is illegal.
                     */
                    try {
                        order.validate(ts, valOpts, ruleOpts);

                        List<Orderable> list = orderMap.get(order.getPower());
                        list.add(order);

                        results.addAll(njo.getResults());

                        logger.debug("Order ok: {}", order);
                    } catch (OrderException e1) {
                        // create a general result indicating failure if an order could not be validated.
                        results.add(new Result(Utils.getLocalString(JIH_ORDER_PARSE_FAILURE, order, e.getMessage())));
                        logger.error("Cannot validate order on second pass. Order: {}", order, e1);
                        throw new IOException("Cannot validate order on second pass.", e1);
                    }

                    // Back to strict!
                    valOpts.setOption(ValidationOptions.KEY_GLOBAL_PARSING, ValidationOptions.VALUE_GLOBAL_PARSING_STRICT);
                }
            }

            logger.trace( "ORDER PARSING COMPLETE");

            // clear all units (dislodged or not) from the board
            Province[] unitProv = position.getUnitProvinces();
            Province[] dislProv = position.getDislodgedUnitProvinces();
            for (Province province : unitProv) {
                position.setUnit(province, null);
            }
            for (Province province : dislProv) {
                position.setUnit(province, null);
            }

            // now that all orders are parsed, and all units are cleared, put
            // unit in the proper place.
            for (Result result : results) {
                if (result instanceof OrderResult) {
                    OrderResult ordResult = (OrderResult) result;
                    Orderable order = ordResult.getOrder();

                    if (ordResult.getResultType() == OrderResult.ResultType.DISLODGED) {
                        // dislodged orders create a unit in the source province, marked as dislodged,
                        // unless it was destroyed; if so, it will be destroyed later. Mark as dislodged for now.
                        Unit unit = new Unit(order.getPower(), order.getSourceUnitType());
                        /*
                         * Check for the positionPlacement flag, if not, we need to position the units
                         * in their source places for VIEWING. Otherwise the units need to be
                         * in their destination place for copying.
                         */
                        if (!positionPlacement) {
                            unit.setCoast(order.getSource().getCoast());
                            position.setUnit(order.getSource().getProvince(), unit);
                        } else {
                            unit.setCoast(order.getSource().getCoast());
                            position.setDislodgedUnit(order.getSource().getProvince(), unit);
                            logger.debug("Unit dislodged: {}", order.getSource().getProvince());
                        }
                    } else if (ordResult.getResultType() == OrderResult.ResultType.SUCCESS
                            && order instanceof Move) {
                        // successful moves create a unit in the destination province
                        Move move = (Move) order;
                        Unit unit = new Unit(move.getPower(), move.getSourceUnitType());
                        /*
                         * Check for the positionPlacement flag, if not, we need to position the units
                         * in their source places for VIEWING. Otherwise the units need to be
                         * in their destination place for copying.
                         */
                        if (!positionPlacement) {
                            unit.setCoast(move.getSource().getCoast());
                            position.setUnit(move.getSource().getProvince(), unit);
                            position.setLastOccupier(move.getSource().getProvince(), move.getPower());
                        } else {
                            unit.setCoast(move.getDest().getCoast());
                            position.setUnit(move.getDest().getProvince(), unit);
                            position.setLastOccupier(move.getDest().getProvince(), move.getPower());
                        }
                    } else {
                        // all other orders create a non-dislodged unit in the source province
                        Unit unit = new Unit(order.getPower(), order.getSourceUnitType());
                        /*
                         * Only add a unit if there is not a unit currently there, this stops
                         * powers further down in alpha. order from overriding powers before
                         * them. Eg. England dislodged Germany will be overriding if this isn't here.
                         */
                        if (!position.hasUnit(order.getSource().getProvince())) {
                            unit.setCoast(order.getSource().getCoast());
                            position.setUnit(order.getSource().getProvince(), unit);
                            position.setLastOccupier(order.getSource().getProvince(), order.getPower());
                        }
                    }
                }
            }

            // set orders in turnstate
            for (Power power : powers) {
                ts.setOrders(power, orderMap.get(power));
            }
        }

        // process dislodged unit info, to determine retreat paths
        // correct dislodged results are created here, and the old dislodged
        // results are removed
        DislodgedParser dislodgedParser = new DislodgedParser(ts.getPhase(), turn.getText());
        makeDislodgedResults(ts.getPhase(), results, position, dislodgedParser.getDislodgedInfo(), positionPlacement);

        // process adjustment info ownership info (if any)
        //
        AdjustmentParser adjParser = new AdjustmentParser(map, turn.getText());
        procAdjustmentBlock(adjParser.getOwnership(), ts, position);

        // check for elimination
        position.setEliminationStatus(map.getPowers());

        // set adjudicated flag
        ts.setResolved(true);

        // add to world
        world.setTurnState(ts);
        logger.trace( "METHOD EXIT");
    }// procMove()


    /**
     * Process a Retreat phase turn
     */
    private void procRetreat(Turn turn, boolean positionPlacement)
            throws IOException {
        logger.trace("Starting processing retreat.");
        if (turn == null) return;
        // create TurnState
        final TurnState ts = makeTurnState(turn, positionPlacement);
        final Position position = ts.getPosition();
        final List<Result> results = ts.getResultList();
        final RuleOptions ruleOpts = world.getRuleOptions();

        logger.info("Phase: {}, positionPlacement: {} ", ts.getPhase(),  positionPlacement);

        // parse orders, and create orders for each unit
        JudgeOrderParser jop = new JudgeOrderParser(map, orderFactory, turn.getText());
        NJudgeOrder[] nJudgeOrders = jop.getNJudgeOrders();

        // Copy previous phase positions
        copyPreviousPositions(ts);

        // process retreat orders (either moves or disbands)
        // if order failed, it counts as a disband
        // generate results
        // create units for all successfull move (retreat) orders in destination province
        {
            // create orderMap, which maps powers to their respective order list
            Power[] powers = map.getPowers();
            HashMap<Power, List<Orderable>> orderMap = new HashMap<>(powers.length);
            for (Power power : powers) {
                orderMap.put(power, new LinkedList<>());
            }

            // validate all parsed orders
            for (final NJudgeOrder njo : nJudgeOrders) {
                final Orderable order = njo.getOrder();
                if (order == null) {
                    logger.debug("Null order; njo: {}", njo);
                    throw new IOException("Internal error: null order in JudgeImportHistory::procRetreat()");
                }

                // if we found a Wing unit, make sure Wing units are enabled.
                checkAndEnableWings(order.getSourceUnitType());

                try {
                    order.validate(ts, valOpts, ruleOpts);

                    List<Orderable> list = orderMap.get(order.getPower());
                    list.add(order);

                    results.addAll(njo.getResults());

                    logger.info("Order ok: {}", order);
                } catch (OrderException e) {
                    results.add(new Result(Utils.getLocalString(JIH_ORDER_PARSE_FAILURE, order, e.getMessage())));
                    logger.error("Cannot validate retreat order: {} ", order, e);
                    throw new IOException("Cannot validate retreat order.\n", e);
                }
            }

            // clear all dislodged units from board
            if (positionPlacement) {
                Province[] dislProv = position.getDislodgedUnitProvinces();
                for (Province province : dislProv) {
                    position.setDislodgedUnit(province, null);
                }
            }

            // now that all orders are parsed, and all units are cleared, put
            // unit in the proper place.
            //
            for (Result result : results) {
                if (result instanceof OrderResult) {
                    OrderResult ordResult = (OrderResult) result;
                    Orderable order = ordResult.getOrder();

                    // successful moves create a unit in the destination province
                    // unsuccessful moves OR disbands create no unit
                    if (order instanceof Move)// && ordResult.getResultType() == OrderResult.ResultType.SUCCESS)
                    {
                        // success: unit retreat to destination
                        Move move = (Move) order;

                        Unit unit = new Unit(move.getPower(), move.getSourceUnitType());
                        /*
                         * Check for the positionPlacement flag, if not, we need to position the units
                         * in their source places for VIEWING. Otherwise the units need to be
                         * in their destination place for copying.
                         */
                        if (!positionPlacement) {
                            unit.setCoast(move.getSource().getCoast());
                            position.setDislodgedUnit(move.getSource().getProvince(), unit);
                            position.setLastOccupier(move.getSource().getProvince(), move.getPower());
                            logger.debug("Unit dislodged: {}", move.getSource().getProvince());
                        } else {
                            unit.setCoast(move.getDest().getCoast());
                            position.setUnit(move.getDest().getProvince(), unit);
                            position.setLastOccupier(move.getSource().getProvince(), move.getPower());
                        }
                    } else if (order instanceof Disband) {
                        /*
                         * Check for the positionPlacement flag, if not, we need to position the units
                         * in their source places for VIEWING. Otherwise the units should not be drawn.
                         */
                        Unit unit = new Unit(order.getPower(), order.getSourceUnitType());
                        if (!positionPlacement) {
                            unit.setCoast(order.getSource().getCoast());
                            position.setDislodgedUnit(order.getSource().getProvince(), unit);
                            logger.debug("Unit dislodged: ", order.getSource().getProvince());
                        }
                    }
                }
            }

            // set orders in turnstate
            for (Power power : powers) {
                ts.setOrders(power, orderMap.get(power));
            }
        }

        // process adjustment info ownership info (if any)
        AdjustmentParser adjParser = new AdjustmentParser(map, turn.getText());
        procAdjustmentBlock(adjParser.getOwnership(), ts, position);

        // check for elimination
        ts.getPosition().setEliminationStatus(map.getPowers());

        // set adjudicated flag
        ts.setResolved(true);

        // add to world
        world.setTurnState(ts);
        logger.trace( "METHOD EXIT");
    }// procRetreat()


    /**
     * Process an Adjustment phase turn
     */
    private void procAdjust(Turn turn, boolean positionPlacement)
            throws IOException {
        if (turn == null) {
            return;
        }

        // create TurnState
        final TurnState ts = makeTurnState(turn, positionPlacement);
        final List<Result> results = ts.getResultList();
        final RuleOptions ruleOpts = world.getRuleOptions();

        logger.debug("Phase: {}", ts.getPhase());

        // parse orders, and create orders for each unit
        final JudgeOrderParser jop = new JudgeOrderParser(map, orderFactory, turn.getText());
        final NJudgeOrder[] nJudgeOrders = jop.getNJudgeOrders();

        // Copy previous phase positions
        copyPreviousPositions(ts);

        // Copy previous SC info (we need proper ownership info before parsing orders)
        copyPreviousSCInfo(ts);

        // DEBUG: use Adjustment to check out WTF is going on
		/*
		System.out.println("info.jdip.process.Adjustment.getAdjustmentInfo()");
		for(int i=0; i<map.getPowers().length; i++)
		{
			Power power = map.getPowers()[i];
			System.out.println("   for power: "+power+"; "+info.jdip.process.Adjustment.getAdjustmentInfo(ts, power));
		}
		*/

        // process adjustment orders (either builds or removes)
        // create a unit, unless order failed
        {
            // get Position
            final Position position = ts.getPosition();

            // create orderMap, which maps powers to their respective order list
            Power[] powers = map.getPowers();
            HashMap<Power, List<Orderable>> orderMap = new HashMap<>(powers.length);
            for (Power power : powers) {
                orderMap.put(power, new LinkedList<>());
            }

            // parse all orders
            for (final NJudgeOrder njo : nJudgeOrders) {
                final Orderable order = njo.getOrder();

                // all adjustment orders produced by NJudgeOrderParser should
                // have only 1 result
                //
                if (njo.getResults().size() != 1) {
                    throw new IOException("Internal error: JIH:procAdjustments(): " +
                            "getResults() != 1");
                }

                final Result result = njo.getResults().get(0);

                // if result is a substituted result, the player defaulted,
                // and the Judge inserted a Disband order
                //
                final boolean isDefaulted = (result instanceof SubstitutedResult);

                if (order == null && !isDefaulted) {
                    // orders may be null; if they are, that is because
                    // it's a waive or unusable/pending order. These have
                    // results, but no associated order.
                    results.addAll(njo.getResults());
                } else {
                    // NOTE: everything in this block should use newOrder,
                    // not order, from here on!!
                    Orderable newOrder = order;

                    if (isDefaulted) {
                        newOrder = ((SubstitutedResult) result).getSubstitutedOrder();
                        assert (newOrder != null);
                    }

                    // if we found a Wing unit, make sure Wing units are enabled.
                    checkAndEnableWings(newOrder.getSourceUnitType());

                    try {
                        newOrder.validate(ts, valOpts, ruleOpts);

                        if (!isDefaulted) {
                            List<Orderable> list = orderMap.get(newOrder.getPower());
                            list.add(newOrder);
                        }

                        results.addAll(njo.getResults());

                        logger.debug("Order ok: {}", newOrder);

                        // create or remove units
                        // as far as I know, orders are always successful.
                        //
                        if (newOrder instanceof Build) {
                            if (positionPlacement) {
                                final Unit unit = new Unit(newOrder.getPower(), newOrder.getSourceUnitType());
                                unit.setCoast(newOrder.getSource().getCoast());
                                position.setUnit(newOrder.getSource().getProvince(), unit);
                                position.setLastOccupier(newOrder.getSource().getProvince(), newOrder.getPower());
                            }
                        } else if (newOrder instanceof Remove) {
                            if (positionPlacement) {
                                position.setUnit(newOrder.getSource().getProvince(), null);
                            }
                        } else {
                            throw new IllegalStateException("JIH::procAdjust(): type :" + newOrder + " not handled!");
                        }
                    } catch (OrderException e) {
                        results.add(new Result(Utils.getLocalString(JIH_ORDER_PARSE_FAILURE, newOrder, e.getMessage())));

                        logger.error( "OrderException (during validation) phase: {}, order: {}",ts.getPhase(),newOrder,e);

                        throw new IOException("Cannot validate adjustment order.\n" + e.getMessage());
                    }
                }
            }

            // set orders in turnstate
            for (Power power : powers) {
                ts.setOrders(power, orderMap.get(power));
            }
        }


        // check for elimination
        ts.getPosition().setEliminationStatus(map.getPowers());

        // set adjudicated flag
        ts.setResolved(true);

        // Since this is the adjustment phase, check for supply center change. Required for VictoryConditions
        // Otherwise, problems can arise and the game will end after importing due to no SC change.
        if (!positionPlacement) {
            TurnState previousTS = world.getPreviousTurnState(ts);
            while (previousTS.getPhase().getPhaseType() != Phase.PhaseType.MOVEMENT) {
                previousTS = world.getPreviousTurnState(previousTS);
            }
            //System.out.println(previousTS.getPhase());
            Position oldPosition = previousTS.getPosition();
            Position position = ts.getPosition();
            Province[] provinces = position.getProvinces();
            for (Province province : provinces) {
                if (province != null && province.hasSupplyCenter()) {
                    Unit unit = position.getUnit(province);
                    if (unit != null) {
                        // nextPosition still contains old ownership information
                        Power oldOwner = oldPosition.getSupplyCenterOwner(province);
                        Power newOwner = unit.getPower();
                        //System.out.println(oldOwner + " VS " + newOwner);

                        // change if ownership change, and not a wing unit
                        if (oldOwner != newOwner && unit.getType() != Unit.Type.WING) {
                            // set owner-changed flag in TurnState [req'd for certain victory conditions]
                            ts.setSCOwnerChanged(true);
                        }
                    }
                }
            }
        }

        // add to world
        world.setTurnState(ts);
    }// procAdjust()


    /**
     * Clones all non-dislodged units from previous phase TurnState
     * and inserts them into the current turnstate.
     * <p>
     * We also copy non-dislodged units, unless the CURRENT turnstate is
     * an Adjustment phase
     */
    private void copyPreviousPositions(TurnState current) {
        // get previous turnstate
        TurnState previousTS = current.getWorld().getPreviousTurnState(current);
        final boolean isCopyDislodged = (current.getPhase().getPhaseType() != Phase.PhaseType.ADJUSTMENT);

        // get position info
        Position newPos = current.getPosition();
        Position oldPos = null;
        if (previousTS == null) {
            oldPos = oldPosition;
        } else {
            oldPos = previousTS.getPosition();
        }

        logger.info("Copy previous positions from: {}", oldPos);

        // clone!
        final Province[] provinces = map.getProvinces();
        for (final Province p : provinces) {
            Unit unit = oldPos.getUnit(p);
            if (unit != null) {
                Unit newUnit = unit.copy();
                newPos.setUnit(p, newUnit);
                logger.info("Cloned unit from/into: {} - {} ", p, unit.getPower());
            }

            unit = oldPos.getDislodgedUnit(p);
            if (isCopyDislodged && unit != null) {
                Unit newUnit = unit.copy();
                newPos.setDislodgedUnit(p, newUnit);
                logger.info("  cloned dislodged unit from/into: {} - {} ", p, unit.getPower());
            }

            // clone any lastOccupied info as well.
            newPos.setLastOccupier(p, oldPos.getLastOccupier(p));
        }
    }// copyPreviousPositions()


    /**
     * Copies the previous TurnState (Position, really) home SC and SC info.
     * <p>
     * If no previous home supply center information is available (e.g.,
     * initial turn), the information from the initial board setup is
     * used.
     * <p>
     * This method should only be used if no AdjustmentInfo block has
     * been detected.
     */
    private void copyPreviousSCInfo(TurnState current) {
        logger.debug("Current phase: {}", current.getPhase());

        // get previous position information (or initial, if previous not available)
        final TurnState previousTS = current.getWorld().getPreviousTurnState(current);
        Position prevPos = (previousTS == null) ? oldPosition : previousTS.getPosition();

        // current position
        Position currentPos = current.getPosition();

        // copy!
        Province[] provinces = map.getProvinces();
        for (Province province : provinces) {
            Power power = prevPos.getSupplyCenterOwner(province);
            if (power != null) {
                currentPos.setSupplyCenterOwner(province, power);
                logger.info("Supply center {} is owned by {}", province, power);
            }
            power = prevPos.getSupplyCenterHomePower(province);
            if (power != null) {
                currentPos.setSupplyCenterHomePower(province, power);
                logger.info("Supply center {} home power is {}", province, power);
            }
        }
    }


    /**
     * Copies the Previous turnstate's lastOccupier information only
     */
    private void copyPreviousLastOccupierInfo(TurnState current) {
        TurnState previousTS = current.getWorld().getPreviousTurnState(current);
        Position newPos = current.getPosition();
        Position oldPos = (previousTS == null) ? oldPosition : previousTS.getPosition();

        final Province[] provinces = map.getProvinces();
        for (final Province p : provinces) {
            // clone any lastOccupied info as well.
            newPos.setLastOccupier(p, oldPos.getLastOccupier(p));
        }
    }// copyPreviousLastOccupierInfo()


    /**
     * Processes a block of adjustment info; this can occur during a
     * Move or Retreat phase. Only the Supply Center ownership is used;
     * the adjustment values are ignored, since they can be computed
     * based upon ownership information.
     * <p>
     * If no SC owner info exists, copyPreviousSCInfo() is used to
     * supply the appropriate information.
     */
    private void procAdjustmentBlock(AdjustmentParser.OwnerInfo[] ownerInfo, TurnState ts, Position position)
            throws IOException {
        logger.debug("Phase: {}", ts.getPhase());
        if (ownerInfo.length == 0) {
            logger.debug( "No adjustment block. Copying previous SC ownership info.");
            copyPreviousSCInfo(ts);
        } else {
            for (AdjustmentParser.OwnerInfo anOwnerInfo : ownerInfo) {
                Power power = map.getPowerMatching(anOwnerInfo.getPowerName());
                if (power != null) {
                    logger.debug("SC Owned by Power: ", power);
                    String[] provNames = anOwnerInfo.getProvinces();
                    for (String provName : provNames) {
                        Province province = map.getProvinceMatching(provName);
                        if (province == null) {
                            throw new IOException("Unknown Province in SC Ownership block: " + provName);
                        }

                        logger.debug("Province: {}", province);
                        position.setSupplyCenterOwner(province, power);
                    }
                } else {
                    logger.error("Unrecognized power: {}", anOwnerInfo.getPowerName());
                    throw new IOException("Unregognized power \"" + anOwnerInfo.getPowerName() + "\" in Ownership block.");
                }
            }
        }
    }// procAdjustmentBlock()


    /**
     * Creates correct dislodged results (with retreat information) by matching
     * DislodgedInfo with the previously generated Dislodged result.
     * <p>
     * Units with no retreat results are destroyed, and a message generated indicating so.
     * <p>
     * old Dislodged results are discarded.
     */
    private void makeDislodgedResults(Phase phase, List<Result> results, Position position,
                                      DislodgedParser.DislodgedInfo[] dislodgedInfo, boolean positionPlacement)
            throws IOException {
        logger.info("Creating dislodged results for phase [{}], results: {}", phase, results.size());
        ListIterator<Result> iter = results.listIterator();
        while (iter.hasNext()) {
            Result result = iter.next();
            if (result instanceof OrderResult) {
                OrderResult orderResult = (OrderResult) result;
                if (OrderResult.ResultType.DISLODGED.equals(orderResult.getResultType())) {
                    logger.debug("Failed order: {}", orderResult.getOrder());

                    String[] retreatLocNames = null;
                    for (DislodgedParser.DislodgedInfo aDislodgedInfo : dislodgedInfo) {
                        // find the province for this dislodgedInfo source
                        // remember, we use map.parseLocation() to auto-normalize coasts (see Coast.normalize())
                        Location location = map.parseLocation(aDislodgedInfo.getSourceName());
                        if (orderResult.getOrder().getSource().isProvinceEqual(location)) {
                            retreatLocNames = aDislodgedInfo.getRetreatLocationNames();
                            break;
                        }
                    }

                    // we didn't find a match!! note that, and don't delete old dislodged order
                    if (retreatLocNames == null) {
                        iter.add(new Result(Utils.getLocalString(JIH_NO_DISLODGED_MATCH, orderResult.getOrder())));

                        String message = "Could not match dislodged order: " + orderResult.getOrder() + "; phase: " + phase;
                        logger.warn( message);

                        // we are more strict with our errors
                        throw new IOException(message);
                    } else {
                        try {
                            // create objects from retreat location names
                            Location[] retreatLocations = new Location[retreatLocNames.length];
                            for (int i = 0; i < retreatLocNames.length; i++) {
                                retreatLocations[i] = map.parseLocation(retreatLocNames[i]);
                                retreatLocations[i] = retreatLocations[i].getValidated(orderResult.getOrder().getSourceUnitType());
                            }

                            logger.info("Possible retreats: {}",  (Object) retreatLocations);

                            // remove old dislodged result, replacing with the new dislodged result
                            iter.set(new DislodgedResult(orderResult.getOrder(), retreatLocations));

                            // if no retreat results, destroy unit
                            if (retreatLocations.length == 0) {
                                // destroy
                                Province province = orderResult.getOrder().getSource().getProvince();
                                Unit unit;

                                /*
                                 * Check for the positionPlacement flag. If so, go ahead and set the unit to the
                                 * dislodged one. If not, the unit that is dislodged is not SHOWN as dislodged
                                 * therefore get that one.
                                 */
                                if (positionPlacement) {
                                    unit = position.getDislodgedUnit(province);
                                } else {
                                    unit = position.getUnit(province);
                                }

                                position.setDislodgedUnit(province, null);

                                // send result
                                iter.add(new Result(unit.getPower(), Utils.getLocalString(STDADJ_MV_UNIT_DESTROYED, unit.getType().getFullName(), province)));
                            }
                        } catch (OrderException e) {
                            // couldn't validate!!
                            iter.add(new Result(Utils.getLocalString(JIH_INVALID_RETREAT, orderResult.getOrder())));
                            logger.error("exception for order: {}", orderResult.getOrder(),e);
                            throw new IOException(e.getMessage());
                        }
                    }
                }
            }
        }
    }// makeDislodgedResults()

    private void createStartingPositions() throws IOException {
        Phase phase = null;

        // determine the next phase by reading through the turn text.
        Pattern pattern = Pattern.compile(START_POSITIONS);
        Matcher m = pattern.matcher(jp.getText());

        if (m.find()) {
            StringBuilder sb = new StringBuilder(64);
            sb.append(m.group(1));
            sb.append(' ');
            sb.append(m.group(2));
            sb.append(' ');
            sb.append(m.group(3));
            phase = Phase.parse(sb.toString());
        }

        if (phase == null) {
            throw new IOException(Utils.getLocalString(JIH_BAD_LAST_PHASE));
        }

        // Create the new turnstate
        TurnState ts = new TurnState(phase);
        ts.setWorld(world);
        ts.setPosition(new Position(world.getMap()));

        // set Home Supply centers in position
        Position pos = oldPosition;
        for (int i = 0; i < oldPosition.getHomeSupplyCenters().length; i++) {
            pos.setSupplyCenterHomePower(oldPosition.getHomeSupplyCenters()[i], oldPosition.getSupplyCenterHomePower(oldPosition.getHomeSupplyCenters()[i]));
        }

        // Copy previous phase positions
        copyPreviousPositions(ts);

        // Copy previous SC info (we need proper ownership info before parsing orders)
        copyPreviousSCInfo(ts);

        // check for elimination
        ts.getPosition().setEliminationStatus(map.getPowers());

        // add to World
        world.setTurnState(ts);
    }// createStartingPositions()

    /**
     * Creates the last TurnState, which is always ready for adjudication.
     * <p>
     * If parsing fails, no last turnstate will be created.
     */
    private void makeLastTurnState(Turn lastTurn)
            throws IOException {
        Phase phase = null;

        // determine the next phase by reading through the turn text.
        Pattern pattern = Pattern.compile(PARSE_REGEX);

        Matcher m = pattern.matcher(lastTurn.getText());

        if (m.find()) {
            StringBuilder sb = new StringBuilder(64);
            sb.append(m.group(1));
            sb.append(' ');
            sb.append(m.group(2));
            sb.append(' ');
            sb.append(m.group(3));
            phase = Phase.parse(sb.toString());
        }

        if (phase == null) {
            throw new IOException(Utils.getLocalString(JIH_BAD_LAST_PHASE));
        }

        // Create the new turnstate
        TurnState ts = new TurnState(phase);
        ts.setWorld(world);
        ts.setPosition(new Position(world.getMap()));

        // set Home Supply centers in position
        Position pos = ts.getPosition();
        for (HSCInfo aHomeSCInfo : homeSCInfo) {
            pos.setSupplyCenterHomePower(aHomeSCInfo.getProvince(), aHomeSCInfo.getPower());
        }

        // Copy previous phase positions
        copyPreviousPositions(ts);

        // Copy previous SC info (we need proper ownership info before parsing orders)
        copyPreviousSCInfo(ts);

        // check for elimination
        ts.getPosition().setEliminationStatus(map.getPowers());

        // add to World
        world.setTurnState(ts);
    }// makeLastTurnState()


    /**
     * If a WING unit is detected, make sure we have the WING option
     * enabled; if it already is, do nothing.
     */
    private void checkAndEnableWings(Unit.Type unitType) {
        if (Unit.Type.WING.equals(unitType)) {
            RuleOptions ruleOpts = world.getRuleOptions();
            if (RuleOptions.VALUE_WINGS_DISABLED.equals(ruleOpts.getOptionValue(RuleOptions.OPTION_WINGS))) {
                ruleOpts.setOption(RuleOptions.OPTION_WINGS, RuleOptions.VALUE_WINGS_ENABLED);
                world.setRuleOptions(ruleOpts);
            }
        }
    }// enableWings()


    /**
     * Home Supply Center information
     */
    private class HSCInfo {
        private final Province province;
        private final Power power;

        public HSCInfo(Province province, Power power) {
            this.province = province;
            this.power = power;
        }// HSCInfo()

        public Province getProvince() {
            return province;
        }

        public Power getPower() {
            return power;
        }
    }// inner class HSCInfo

}// class JudgeImportHistory
