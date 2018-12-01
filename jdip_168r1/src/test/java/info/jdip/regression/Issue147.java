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
import static info.jdip.test.builder.standard.StandardPower.AUSTRIA;
import static info.jdip.test.builder.standard.StandardPower.RUSSIA;
import static info.jdip.test.builder.standard.StandardPower.TURKEY;
import static info.jdip.world.Phase.PhaseType.MOVEMENT;
import static info.jdip.world.Phase.SeasonType.SPRING;

public class Issue147 {
    @Test
    @Disabled
    @DisplayName("Issue 147: should not create a convoy paradox. 2v2 should bounce")
    public void shouldNotCreateParadoxTwoVersusTwoShouldBounce() throws Exception {
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

    @Test
    @DisplayName("Issue 147: 2v1 in constantinople should dislodge")
    public void shouldDislodgeConstantinopleTwoVersusOne() throws Exception {
        Case testCase = standard(SPRING, 1901, MOVEMENT)
                .army(RUSSIA, BULGARIA)
                .army(RUSSIA, GREECE)
                .army(RUSSIA, ANKARA)
                .army(TURKEY, CONSTANTINOPLE)
                .order(CONSTANTINOPLE).moveTo(BULGARIA)
                .order(BULGARIA).moveTo(CONSTANTINOPLE)
//                .order(GREECE).supportMove(CONSTANTINOPLE, BULGARIA)
                .order(ANKARA).supportMove(BULGARIA, CONSTANTINOPLE)
                .expectArmy(RUSSIA, CONSTANTINOPLE)
                .expectArmy(RUSSIA, GREECE)
                .expectArmy(RUSSIA, ANKARA)
                .expectDislodgedArmy(TURKEY, CONSTANTINOPLE)
                .build();

        TestCaseRunner.runCase(testCase);
    }

    @Test
    @DisplayName("Issue 147: 2v1 in bulgaria should not dislodge - russia cannot help dislodge russian unit")
    public void shouldNotDislodgeBulgariaTwoVersusOne() throws Exception {
        Case testCase = standard(SPRING, 1901, MOVEMENT)
                .army(RUSSIA, BULGARIA)
                .army(RUSSIA, GREECE)
                .army(RUSSIA, ANKARA)
                .army(TURKEY, CONSTANTINOPLE)
                .order(CONSTANTINOPLE).moveTo(BULGARIA)
                .order(BULGARIA).moveTo(CONSTANTINOPLE)
                .order(GREECE).supportMove(CONSTANTINOPLE, BULGARIA)
//                .order(ANKARA).supportMove(BULGARIA, CONSTANTINOPLE)
                .expectArmy(RUSSIA, BULGARIA)
                .expectArmy(RUSSIA, GREECE)
                .expectArmy(RUSSIA, ANKARA)
                .expectArmy(TURKEY, CONSTANTINOPLE)
                .build();

        TestCaseRunner.runCase(testCase);
    }


    @Test
    @DisplayName("Issue 147: should not create a convoy paradox if one supporting unit is from austria")
    public void shouldNotCreateParadoxWithAustriaOwningOneUnit() throws Exception {
        Case testCase = standard(SPRING, 1901, MOVEMENT)
                .army(RUSSIA, BULGARIA)
                .army(AUSTRIA, GREECE)
                .army(RUSSIA, ANKARA)
                .army(TURKEY, CONSTANTINOPLE)
                .order(CONSTANTINOPLE).moveTo(BULGARIA)
                .order(BULGARIA).moveTo(CONSTANTINOPLE)
                .order(GREECE).supportMove(CONSTANTINOPLE, BULGARIA)
                .order(ANKARA).supportMove(BULGARIA, CONSTANTINOPLE)
                .expectArmy(RUSSIA, BULGARIA)
                .expectArmy(AUSTRIA, GREECE)
                .expectArmy(RUSSIA, ANKARA)
                .expectArmy(TURKEY, CONSTANTINOPLE)
                .build();

        TestCaseRunner.runCase(testCase);
    }

    @Test
    @Disabled
    @DisplayName("Issue 147: should not create a convoy paradox if the other supporting unit is from austria")
    public void shouldNotCreateParadoxWithAustriaOwningOtherUnit() throws Exception {
        Case testCase = standard(SPRING, 1901, MOVEMENT)
                .army(RUSSIA, BULGARIA)
                .army(RUSSIA, GREECE)
                .army(AUSTRIA, ANKARA)
                .army(TURKEY, CONSTANTINOPLE)
                .order(CONSTANTINOPLE).moveTo(BULGARIA)
                .order(BULGARIA).moveTo(CONSTANTINOPLE)
                .order(GREECE).supportMove(CONSTANTINOPLE, BULGARIA)
                .order(ANKARA).supportMove(BULGARIA, CONSTANTINOPLE)
                .expectArmy(RUSSIA, BULGARIA)
                .expectArmy(RUSSIA, GREECE)
                .expectArmy(AUSTRIA, ANKARA)
                .expectArmy(TURKEY, CONSTANTINOPLE)
                .build();

        TestCaseRunner.runCase(testCase);
    }
}
