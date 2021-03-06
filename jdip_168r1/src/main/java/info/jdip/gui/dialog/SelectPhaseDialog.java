//
//  @(#)SelectPhaseDialog.java	1.00	4/1/2002
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

package info.jdip.gui.dialog;

import info.jdip.gui.ClientFrame;
import info.jdip.gui.swing.XJScrollPane;
import info.jdip.misc.Utils;
import info.jdip.world.Phase;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Shows a list of Phases, for a given world, so that the user
 * may select a Phase.
 */
public class SelectPhaseDialog extends HeaderDialog {
    // i18n constants
    private static final String TITLE = "SPD.title";
    private static final String HEADER_LOCATION = "SPD.location.header";

    // instance variables
    private final ClientFrame clientFrame;
    private JScrollPane phaseScrollPane = null;
    private JList<ListRow> list = null;


    private SelectPhaseDialog(ClientFrame clientFrame) {
        super(clientFrame, Utils.getLocalString(TITLE), true);
        this.clientFrame = clientFrame;

        makePhaseList();

        setHeaderText(Utils.getText(Utils.getLocalString(HEADER_LOCATION)));

        // if we don't put the scroller in a JPanel, the scroller's border
        // isn't drawn.
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.add(phaseScrollPane, BorderLayout.CENTER);
        createDefaultContentBorder(contentPanel);
        setContentPane(contentPanel);

        addTwoButtons(makeCancelButton(), makeOKButton(), false, true);
        setHelpID(info.jdip.misc.Help.HelpID.Dialog_PhaseSelect);
    }// SelectPhaseDialog()

    /**
     * Displays the Phases for the current World object.
     * <p>
     * Returns the Phase selected, or <code>null</code> if no
     * Phase was selected, or dialog was cancelled.
     */
    public static Phase displayDialog(ClientFrame cf) {
        SelectPhaseDialog spd = new SelectPhaseDialog(cf);
        spd.pack();
        spd.setSize(new Dimension(500, 450));
        Utils.centerInScreen(spd);
        spd.setVisible(true);
        return spd.getSelectedPhase();
    }// displayDialog()

    private Phase getSelectedPhase() {
        if (getReturnedActionCommand().equals(ACTION_OK)) {
            ListRow lr = list.getSelectedValue();
            if (lr != null) {
                return lr.getPhase();
            }
        }

        return null;
    }// getSelectedPhase()


    private void makePhaseList() {
        // create ListRows
        List<ListRow> lrList = new LinkedList<>();
        Set<Phase> phaseSet = clientFrame.getWorld().getPhaseSet();
        int idx = 1;
        for (Phase phase : phaseSet) {
            lrList.add(new ListRow(phase, idx++));
        }

        // create & populate JList
        list = new JList<>(lrList.toArray(new ListRow[lrList.size()]));
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        phaseScrollPane = new XJScrollPane(list);
    }// makePhaseList()


    private class ListRow {
        private final Phase phase;
        private final int num;

        public ListRow(Phase phase, int n) {
            this.phase = phase;
            this.num = n;
        }// ListRow()

        public Phase getPhase() {
            return phase;
        }// getPhase()

        public String toString() {
            StringBuilder sb = new StringBuilder(64);
            sb.append(String.valueOf(num));
            sb.append(".  ");
            sb.append(phase);
            return sb.toString();
        }// toString()

    }// inner class ListRow


}// class SelectPhaseDialog
