package info.jdip.regression;

import info.jdip.misc.Case;
import info.jdip.misc.TestCaseRunner;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static info.jdip.test.builder.TestCaseBuilder.ancientMediterranean;
import static info.jdip.test.builder.ancient.AncientMediterraneanLocation.AEGEAN_SEA;
import static info.jdip.test.builder.ancient.AncientMediterraneanLocation.CRETE;
import static info.jdip.test.builder.ancient.AncientMediterraneanLocation.CYPRUS;
import static info.jdip.test.builder.ancient.AncientMediterraneanLocation.SIDON;
import static info.jdip.test.builder.ancient.AncientMediterraneanLocation.SPARTA;
import static info.jdip.test.builder.ancient.AncientMediterraneanLocation.SYRIAN_SEA;
import static info.jdip.test.builder.ancient.AncientMediterraneanPower.GREECE;
import static info.jdip.test.builder.ancient.AncientMediterraneanPower.PERSIA;
import static info.jdip.world.Phase.PhaseType.MOVEMENT;
import static info.jdip.world.Phase.SeasonType.SPRING;

public class Issue168 {
    @Test
    @Disabled
    @DisplayName("Issue 168: Army should be able to convoy to Crete in ancient mediterranean")
    public void shouldConvoyArmyOntoCrete() throws Exception {
        Case testCase = ancientMediterranean(SPRING, 1, MOVEMENT).
                fleet(GREECE, AEGEAN_SEA)
                .army(GREECE, SPARTA)
                .order(SPARTA).moveTo(CRETE)
                .order(AEGEAN_SEA).convoy(SPARTA,CRETE)
                .expectArmy(GREECE, CRETE)
                .expectFleet(GREECE, AEGEAN_SEA)
                .build();

        TestCaseRunner.runCase(testCase);
    }

    @Test
    @Disabled
    @DisplayName("Issue 168: Army should be able to convoy to Cyprus in ancient mediterranean")
    public void shouldConvoyArmyOntoCyprus() throws Exception {
        Case testCase = ancientMediterranean(SPRING, 1, MOVEMENT).
                fleet(PERSIA, SYRIAN_SEA)
                .army(PERSIA, SIDON)
                .order(SIDON).moveTo(CYPRUS)
                .order(SYRIAN_SEA).convoy(SIDON,CYPRUS)
                .expectArmy(PERSIA, CYPRUS)
                .expectFleet(PERSIA, SYRIAN_SEA)
                .build();

        TestCaseRunner.runCase(testCase);
    }
}
