package info.jdip.regression;

import info.jdip.misc.Case;
import info.jdip.misc.TestCaseRunner;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static info.jdip.test.builder.TestCaseBuilder.standard;
import static info.jdip.test.builder.standard.StandardLocation.AEGEAN_SEA;
import static info.jdip.test.builder.standard.StandardLocation.ANKARA;
import static info.jdip.test.builder.standard.StandardLocation.BULGARIA;
import static info.jdip.test.builder.standard.StandardLocation.CONSTANTINOPLE;
import static info.jdip.test.builder.standard.StandardLocation.EDINBURGH;
import static info.jdip.test.builder.standard.StandardPower.ENGLAND;
import static info.jdip.test.builder.standard.StandardPower.RUSSIA;
import static info.jdip.test.builder.standard.StandardPower.TURKEY;
import static info.jdip.world.Phase.PhaseType.MOVEMENT;
import static info.jdip.world.Phase.SeasonType.SPRING;

public class Issue137 {

    @Test
    @Disabled
    @DisplayName("Gitlab Issue 137: Fleet cannot support hold if fleet cannot move into supported space")
    public void fleetCannotSupportLongDistance() throws Exception {
        Case testCase = standard(SPRING, 1901, MOVEMENT)
                .fleet(ENGLAND, EDINBURGH)
                .army(TURKEY, CONSTANTINOPLE)
                .order(EDINBURGH).supportHold(CONSTANTINOPLE)
                .army(RUSSIA, BULGARIA)
                .army(RUSSIA, ANKARA)
                .order(BULGARIA).moveTo(ANKARA)
                .order(ANKARA).supportMove(BULGARIA,CONSTANTINOPLE)
                .expectFleet(ENGLAND, EDINBURGH)
                .expectArmy(RUSSIA, CONSTANTINOPLE)
                .expectArmy(RUSSIA, ANKARA)
                .expectDislodgedArmy(TURKEY, CONSTANTINOPLE)
                .build();

        TestCaseRunner.runCase(testCase);
    }

    @Test
    @DisplayName("Gitlab Issue 137: Fleet can support hold if fleet can move into supported space")
    public void fleetCanSupportShortDistance() throws Exception {
        Case testCase = standard(SPRING, 1901, MOVEMENT)
                .fleet(ENGLAND, AEGEAN_SEA )
                .army(TURKEY, CONSTANTINOPLE)
                .order(AEGEAN_SEA).supportHold(CONSTANTINOPLE)
                .army(RUSSIA, BULGARIA)
                .army(RUSSIA, ANKARA)
                .order(BULGARIA).moveTo(ANKARA)
                .order(ANKARA).supportMove(BULGARIA,CONSTANTINOPLE)
                .expectFleet(ENGLAND, AEGEAN_SEA)
                .expectArmy(RUSSIA, BULGARIA)
                .expectArmy(RUSSIA, ANKARA)
                .expectArmy(TURKEY, CONSTANTINOPLE)
                .build();

        TestCaseRunner.runCase(testCase);
    }
}
