package info.jdip.regression;

import info.jdip.misc.Case;
import info.jdip.misc.TestCaseRunner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static info.jdip.test.builder.TestCaseBuilder.standard;
import static info.jdip.test.builder.standard.StandardLocation.AEGEAN_SEA;
import static info.jdip.test.builder.standard.StandardLocation.BLACK_SEA;
import static info.jdip.test.builder.standard.StandardLocation.BULGARIA;
import static info.jdip.test.builder.standard.StandardLocation.CONSTANTINOPLE;
import static info.jdip.test.builder.standard.StandardLocation.GREECE;
import static info.jdip.test.builder.standard.StandardLocation.RUMANIA;
import static info.jdip.test.builder.standard.StandardLocation.SERBIA;
import static info.jdip.test.builder.standard.StandardPower.AUSTRIA;
import static info.jdip.test.builder.standard.StandardPower.RUSSIA;
import static info.jdip.test.builder.standard.StandardPower.TURKEY;
import static info.jdip.world.Phase.PhaseType.MOVEMENT;
import static info.jdip.world.Phase.SeasonType.SPRING;

public class Issue149 {
    @Test
    @DisplayName("Gitlab Issue 149: Should not crash on a complicated stalemate")
    public void shouldNotCrashOnComplicatedStalemate() throws Exception {
        Case testCase = standard(SPRING, 1901, MOVEMENT)
                .army(AUSTRIA, SERBIA)
                .order(SERBIA).moveTo(BULGARIA)
                .army(RUSSIA,RUMANIA)
                .order(RUMANIA).supportMove(SERBIA,BULGARIA)
                .fleet(AUSTRIA,GREECE)
                .order(GREECE).supportMove(SERBIA,BULGARIA)
                .army(TURKEY,CONSTANTINOPLE)
                .order(CONSTANTINOPLE).moveTo(BULGARIA)
                .army(AUSTRIA,BULGARIA)
                .order(BULGARIA).moveTo(CONSTANTINOPLE)
                .fleet(TURKEY,BLACK_SEA)
                .order(BLACK_SEA).supportMove(CONSTANTINOPLE,BULGARIA)
                .fleet(TURKEY,AEGEAN_SEA)
                .order(AEGEAN_SEA).supportMove(CONSTANTINOPLE,BULGARIA)
                .expectArmy(AUSTRIA, SERBIA)
                .expectArmy(RUSSIA,RUMANIA)
                .expectFleet(AUSTRIA,GREECE)
                .expectArmy(TURKEY,CONSTANTINOPLE)
                .expectArmy(AUSTRIA,BULGARIA)
                .expectFleet(TURKEY,BLACK_SEA)
                .expectFleet(TURKEY,AEGEAN_SEA)
                .build();

        TestCaseRunner.runCase(testCase);
    }
}
