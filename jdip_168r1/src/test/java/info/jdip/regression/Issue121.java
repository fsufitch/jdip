package info.jdip.regression;

import info.jdip.misc.Case;
import info.jdip.misc.TestCaseRunner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static info.jdip.test.builder.TestCaseBuilder.standard;
import static info.jdip.test.builder.standard.StandardLocation.BELGIUM;
import static info.jdip.test.builder.standard.StandardLocation.HOLLAND;
import static info.jdip.test.builder.standard.StandardLocation.KIEL;
import static info.jdip.test.builder.standard.StandardLocation.NORTH_SEA;
import static info.jdip.test.builder.standard.StandardLocation.NORWAY;
import static info.jdip.test.builder.standard.StandardLocation.RUHR;
import static info.jdip.test.builder.standard.StandardPower.ENGLAND;
import static info.jdip.test.builder.standard.StandardPower.FRANCE;
import static info.jdip.test.builder.standard.StandardPower.GERMANY;
import static info.jdip.world.Phase.PhaseType.MOVEMENT;
import static info.jdip.world.Phase.SeasonType.SPRING;

public class Issue121 {
    @Test
    @DisplayName("Gitlab Issue 121: Convoy should cut a support")
    public void fleetCannotSupportInland() throws Exception {
        Case testCase = standard(SPRING, 1901, MOVEMENT)
                .fleet(ENGLAND, NORTH_SEA)
                .army(ENGLAND, NORWAY)
                .order(NORWAY).moveTo(BELGIUM)
                .order(NORTH_SEA).convoy(NORWAY, BELGIUM)
                .fleet(FRANCE, HOLLAND)
                .army(FRANCE, BELGIUM)
                .order(HOLLAND).supportHold(BELGIUM)
                .order(BELGIUM).supportHold(HOLLAND)
                .army(GERMANY, RUHR)
                .fleet(GERMANY, KIEL)
                .order(RUHR).moveTo(HOLLAND)
                .order(KIEL).supportMove(RUHR, HOLLAND)
                .expectFleet(ENGLAND, NORTH_SEA)
                .expectArmy(ENGLAND, NORWAY)
                .expectDislodgedFleet(FRANCE, HOLLAND)
                .expectArmy(FRANCE, BELGIUM)
                .expectArmy(GERMANY, HOLLAND)
                .expectFleet(GERMANY, KIEL)
                .build();

        TestCaseRunner.runCase(testCase);
    }
}
