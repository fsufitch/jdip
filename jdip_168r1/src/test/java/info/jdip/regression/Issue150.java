package info.jdip.regression;

import info.jdip.misc.Case;
import info.jdip.misc.TestCaseRunner;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static info.jdip.test.builder.TestCaseBuilder.crowdedMilan;
import static info.jdip.test.builder.milan.CrowdedMilanPower.GERMANY;
import static info.jdip.test.builder.milan.CrowdedMilanPower.RUSSIA;
import static info.jdip.test.builder.milan.MilanLocation.PRUSSIA;
import static info.jdip.test.builder.milan.MilanLocation.SILESIA;
import static info.jdip.test.builder.milan.MilanLocation.WARSAW;
import static info.jdip.world.Phase.PhaseType.MOVEMENT;
import static info.jdip.world.Phase.SeasonType.SPRING;

public class Issue150 {
    @Test
    @Disabled("To not fail pipeline, this test has to be fix for the issue 150 to be closed")
    @DisplayName("Gitlab Issue 150: Should be possible to move from Prussia to Silesia")
    public void shouldMoveFromPrussiaToSilesia() throws Exception {
        Case testCase = crowdedMilan(SPRING, 1901, MOVEMENT)
                .army(GERMANY, PRUSSIA)
                .order(PRUSSIA).moveTo(SILESIA)
                .expectArmy(GERMANY, SILESIA)
                .build();

        TestCaseRunner.runCase(testCase);
    }

    @Test
    @Disabled("To not fail pipeline, this test has to be fix for the issue 150 to be closed")
    @DisplayName("Gitlab Issue 150: Should be possible to move from Prussia to Warsaw")
    public void shouldMoveFromPrussiaToWarsaw() throws Exception {
        Case testCase = crowdedMilan(SPRING, 1901, MOVEMENT)
                .army(GERMANY, PRUSSIA)
                .order(PRUSSIA).moveTo(WARSAW)
                .expectArmy(GERMANY, WARSAW)
                .build();

        TestCaseRunner.runCase(testCase);
    }

    @Test
    @DisplayName("Gitlab Issue 150: Should be possible to move from Warsaw to Prussia")
    public void shouldMoveFromWarsawToPrussia() throws Exception {
        Case testCase = crowdedMilan(SPRING, 1901, MOVEMENT)
                .army(RUSSIA, WARSAW)
                .order(WARSAW).moveTo(PRUSSIA)
                .expectArmy(RUSSIA, PRUSSIA)
                .build();

        TestCaseRunner.runCase(testCase);
    }
}
