package info.jdip.regression;

import info.jdip.misc.Case;
import info.jdip.misc.TestCaseRunner;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static info.jdip.test.builder.TestCaseBuilder.standard;
import static info.jdip.test.builder.standard.StandardLocation.ANKARA;
import static info.jdip.test.builder.standard.StandardLocation.BULGARIA;
import static info.jdip.test.builder.standard.StandardLocation.CONSTANTINOPLE;
import static info.jdip.test.builder.standard.StandardLocation.GREECE;
import static info.jdip.test.builder.standard.StandardPower.RUSSIA;
import static info.jdip.test.builder.standard.StandardPower.TURKEY;
import static info.jdip.world.Phase.PhaseType.MOVEMENT;
import static info.jdip.world.Phase.SeasonType.SPRING;

public class Issue147 {
    @Test
    @Disabled
    @DisplayName("Issue 147: should not create a convoy paradox")
    public void shouldNotCreateParadox() throws Exception {
        Case testCase = standard(SPRING, 1901, MOVEMENT)
                .army(RUSSIA, BULGARIA)
                .army(RUSSIA, GREECE)
                .army(RUSSIA, ANKARA)
                .army(TURKEY, CONSTANTINOPLE)
                .order(CONSTANTINOPLE).moveTo(BULGARIA)
                .order(BULGARIA).moveTo(CONSTANTINOPLE)
                .order(GREECE).supportMove(CONSTANTINOPLE, BULGARIA)
                .order(ANKARA).supportMove(BULGARIA, CONSTANTINOPLE)
                .expectArmy(RUSSIA, BULGARIA)
                .expectArmy(RUSSIA, GREECE)
                .expectArmy(RUSSIA, ANKARA)
                .expectArmy(TURKEY, CONSTANTINOPLE)
                .build();

        TestCaseRunner.runCase(testCase);
    }
}
