//
//  @(#)JudgeImport.java	1.00	6/2002
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
package info.jdip.judge.parser;

import info.jdip.misc.Utils;
import info.jdip.order.OrderException;
import info.jdip.order.OrderFactory;
import info.jdip.world.InvalidWorldException;
import info.jdip.world.Location;
import info.jdip.world.Phase;
import info.jdip.world.Position;
import info.jdip.world.Power;
import info.jdip.world.Province;
import info.jdip.world.RuleOptions;
import info.jdip.world.TurnState;
import info.jdip.world.Unit;
import info.jdip.world.World;
import info.jdip.world.WorldFactory;
import info.jdip.world.metadata.GameMetadata;
import info.jdip.world.metadata.PlayerMetadata;
import info.jdip.world.variant.VariantManager;
import info.jdip.world.variant.data.Variant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.regex.PatternSyntaxException;

/**
 * Imports text or reads a file, that is a Judge history file or listing.
 * A World object is created from that file, if successful.
 * <p>
 * Todo:<p>
 * <br>
 * <br>
 */
public class JudgeImport {
    private static final Logger logger = LoggerFactory.getLogger(JudgeImport.class);
    // import result constants
    public static final String JI_RESULT_NEWWORLD = "JP.import.newworld";
    public static final String JI_RESULT_TRYREWIND = "JP.import.tryrewind";
    public static final String JI_RESULT_LOADOTHER = "JP.import.loadother";
    public static final String JI_RESULT_THISWORLD = "JP.import.thisworld";
    // resource constants
    private static final String JI_VARIANT_NOTFOUND = "JP.import.novariant";
    private static final String JI_NO_SUPPLY_INFO = "JP.import.nosupplyinfo";
    private static final String JI_NO_UNIT_INFO = "JP.import.nounitinfo";
    private static final String JI_UNKNOWN_POWER = "JP.import.powernotfound";
    private static final String JI_UNKNOWN_PROVINCE = "JP.import.provincenotfound";
    private static final String JI_BAD_POSITION = "JP.import.badposition";
    private static final String JI_UNKNOWN_TYPE = "JP.import.unknowntype";
    // Instance variables
    private OrderFactory orderFactory = null;
    private JudgeParser jp = null;
    private World world = null;
    private World currentWorld = null;
    private String importResult = JI_RESULT_NEWWORLD;
    private String gameInfo; // e.g. "Game: test  Judge: USCA  Variant: Standard S1901M"

    /**
     * Creates a JudgeImport object from a File
     */
    public JudgeImport(OrderFactory orderFactory, File file, World currentWorld)
            throws IOException, PatternSyntaxException {
        this(orderFactory, new FileReader(file), currentWorld);
    }// JudgeImport()

    /**
     * Creates a JudgeImport object from a String
     */
    public JudgeImport(OrderFactory orderFactory, String input)
            throws IOException, PatternSyntaxException {
        this(orderFactory, new StringReader(input), null); // TODO: submit a currentWorld
    }// JudgeImport()


    /**
     * Creates a JudgeImport object from a generic Reader
     */
    public JudgeImport(OrderFactory orderFactory, Reader reader, World currentWorld)
            throws IOException, PatternSyntaxException {
        this.orderFactory = orderFactory;
        this.currentWorld = currentWorld;
        jp = new JudgeParser(orderFactory, reader);
        procJudgeInput();
    }// JudgeImport()


    /**
     * Returns the World object after successful parsing, or null if unsuccessfull.
     */
    public World getWorld() {
        return world;
    }// getWorld()

    /**
     * Returns if the creator of this JudgeImport object needs to create a new world,
     * or if the current world is modified.
     */
    public String getResult() {
        return importResult;
    }

    public String getGameInfo() {
        return gameInfo;
    }

    /**
     * Processing that is common to both History and Listing files
     */
    private void procJudgeInput()
            throws IOException, PatternSyntaxException {
        // determine if we can load the variant
        Variant variant = VariantManager.getVariant(jp.getVariantName(), VariantManager.VERSION_NEWEST);
        if (variant == null) {
            throw new IOException(Utils.getLocalString(JI_VARIANT_NOTFOUND, jp.getVariantName()));
        }

        // create the world
        try {
            world = WorldFactory.getInstance().createWorld(variant);

            // essential! create the default rules
            world.setRuleOptions(RuleOptions.createFromVariant(variant));

        } catch (InvalidWorldException e) {
            throw new IOException(e.getMessage());
        }

        // set the 'explicit convoy' rule option (all nJudge games require this)
        logger.trace( "RuleOptions.VALUE_PATHS_EXPLICIT set");
        RuleOptions ruleOpts = world.getRuleOptions();
        ruleOpts.setOption(RuleOptions.OPTION_CONVOYED_MOVES, RuleOptions.VALUE_PATHS_EXPLICIT);
        world.setRuleOptions(ruleOpts);

        // eliminate all existing TurnStates; we will create our own from parsed values
        // we need the Position, though, since it has home-supply-center information
        Position position = null;
        for (Phase phase : world.getPhaseSet()) {
            TurnState ts = world.getTurnState(phase);
            if (position == null) {
                position = ts.getPosition();
            }
            world.removeTurnState(ts);
        }


        // set essential world data (variant name, map graphic to use)
        World.VariantInfo variantInfo = world.getVariantInfo();
        variantInfo.setVariantName(variant.getName());
        variantInfo.setVariantVersion(variant.getVersion());
        variantInfo.setMapName(variant.getDefaultMapGraphic().getName());

        // set general metadata
        GameMetadata gmd = world.getGameMetadata();
        gmd.setJudgeName(jp.getJudgeName());
        gmd.setGameName(jp.getGameName());

        // set player metadata (email address)
        String[] pPowerNames = jp.getPlayerPowerNames();
        String[] pPowerEmail = jp.getPlayerEmails();

        info.jdip.world.Map map = world.getMap();
        for (int i = 0; i < pPowerNames.length; i++) {
            Power power = map.getPowerMatching(pPowerNames[i]);
            if (power != null) {
                PlayerMetadata pmd = world.getPlayerMetadata(power);
                pmd.setEmailAddresses(new String[]{pPowerEmail[i]});
            } else if (pPowerNames[i].equalsIgnoreCase("master")) {
                gmd.setModeratorEmail(pPowerEmail[i]);
            }
        }

        // activate listing or history parsing
        if (JudgeParser.JP_TYPE_LISTING.equals(jp.getType())) {
            procListing(position);
        } else if (JudgeParser.JP_TYPE_HISTORY.equals(jp.getType())) {
            JudgeImportHistory jih = new JudgeImportHistory(orderFactory, world, jp, position);
            world = jih.getWorld();
        } else if (JudgeParser.JP_TYPE_RESULTS.equals(jp.getType())) {
            procResults(jp, variant.getName());
        } else if (JudgeParser.JP_TYPE_GAMESTART.equals(jp.getType())) {
            jp.prependText("Subject: " + jp.getJudgeName() + ":" + jp.getGameName() + " - " +
                    jp.getPhase().getBriefName() + " Game Starting\n");
            JudgeImportHistory jih = new JudgeImportHistory(orderFactory, world, jp, position);
            world = jih.getWorld();
        } else {
            // unknown judge input
            throw new IOException(Utils.getLocalString(JI_UNKNOWN_TYPE));
        }
    }// procJudgeInput()

    /**
     * Process a Game Result
     */
    private void procResults(JudgeParser jp, String variantName)
            throws IOException, PatternSyntaxException {
        // set game info
        gameInfo = "Judge: " + jp.getJudgeName() + "  Game: " + jp.getGameName() + "  Variant: " + variantName +
                ", " + jp.getPhase().toString();
        // check, if currentWorld matches judge, game, variant and phase of these results
        if (currentWorld == null) {
            importResult = JI_RESULT_LOADOTHER;
            return;
        }

        GameMetadata gmd = currentWorld.getGameMetadata();

        if ((gmd.getJudgeName() == null) || (!gmd.getJudgeName().equalsIgnoreCase(jp.getJudgeName())) ||
                (gmd.getGameName() == null) || (!gmd.getGameName().equalsIgnoreCase(jp.getGameName())) ||
                (!currentWorld.getVariantInfo().getVariantName().equalsIgnoreCase(variantName))) {
            // wrong game
            importResult = JI_RESULT_LOADOTHER;
            return;
        } else {
            // right game, check phase
            if (currentWorld.getLastTurnState().getPhase().compareTo(jp.getPhase()) != 0) {
                if (currentWorld.getLastTurnState().getPhase().compareTo(jp.getPhase()) > 0) {
                    importResult = JI_RESULT_TRYREWIND;
                } else {
                    importResult = JI_RESULT_LOADOTHER;
                }
                return;
            }
        }

        TurnParser.Turn turn = new TurnParser.Turn();
        turn.setPhase(jp.getPhase());
        turn.setText(jp.getText());
        JudgeImportHistory jih = new JudgeImportHistory(orderFactory, currentWorld, jp, turn);
        importResult = JI_RESULT_THISWORLD;
    }

    /**
     * Process a Game Listing
     */
    private void procListing(Position oldPosition)
            throws IOException, PatternSyntaxException {
        // Remember, for listings, we use jp.getText(), not jp.getInitialText()
        //
        //
        // parse position information
        PositionParser pp = new PositionParser(jp.getText());
        Phase phase = pp.getPhase();
        PositionParser.PositionInfo[] posInfo = pp.getPositionInfo();

        // parse ownership / adjustment information
        AdjustmentParser ap = new AdjustmentParser(world.getMap(), jp.getText());
        AdjustmentParser.OwnerInfo[] ownerInfo = ap.getOwnership();

        // ERROR if no positions, or no owner information.
        if (posInfo.length == 0) {
            throw new IOException(Utils.getLocalString(JI_NO_UNIT_INFO));
        }

        if (ownerInfo.length == 0) {
            throw new IOException(Utils.getLocalString(JI_NO_SUPPLY_INFO));
        }


        // Create a TurnState
        TurnState ts = new TurnState(phase);
        ts.setWorld(world);

        // Create position information, and add to TurnState
        Position position = new Position(world.getMap());
        ts.setPosition(position);

        // get world map information
        info.jdip.world.Map map = world.getMap();

        // reset home supply centers
        Province[] provinces = map.getProvinces();
        for (Province province : provinces) {
            Power power = oldPosition.getSupplyCenterHomePower(province);
            if (power != null) {
                position.setSupplyCenterHomePower(province, power);
            }
        }

        // set SC ownership information
        for (AdjustmentParser.OwnerInfo anOwnerInfo : ownerInfo) {
            Power power = map.getPowerMatching(anOwnerInfo.getPowerName());
            if (power == null) {
                throw new IOException(Utils.getLocalString(JI_UNKNOWN_POWER, anOwnerInfo.getPowerName()));
            }

            String[] ownedProvNames = anOwnerInfo.getProvinces();
            for (String ownedProvName : ownedProvNames) {
                Province province = map.getProvinceMatching(ownedProvName);
                if (province == null) {
                    throw new IOException(Utils.getLocalString(JI_UNKNOWN_PROVINCE, anOwnerInfo.getPowerName()));
                }

                position.setSupplyCenterOwner(province, power);
            }
        }

        // create units & positions on the map
        for (PositionParser.PositionInfo aPosInfo : posInfo) {
            Power power = map.getPowerMatching(aPosInfo.getPowerName());
            Unit.Type unitType = Unit.Type.parse(aPosInfo.getUnitName());
            Location location = map.parseLocation(aPosInfo.getLocationName());

            // check
            if (power == null || location == null || unitType.equals(Unit.Type.UNDEFINED)) {
                throw new IOException(
                        Utils.getLocalString(JI_BAD_POSITION,
                                aPosInfo.getPowerName(),
                                aPosInfo.getUnitName(),
                                aPosInfo.getLocationName()));
            }

            // validate location
            try {
                location = location.getValidated(unitType);
            } catch (OrderException e) {
                throw new IOException(e.getMessage());
            }

            // create unit, and add to Position
            Unit unit = new Unit(power, unitType);
            unit.setCoast(location.getCoast());
            position.setUnit(location.getProvince(), unit);
        }

        // although we parse adjustment info, we should not require it. We can just
        // detect supply-center differences.

        // add TurnState to World.
        world.setTurnState(ts);
    }// procListing()


}// class JudgeImport
