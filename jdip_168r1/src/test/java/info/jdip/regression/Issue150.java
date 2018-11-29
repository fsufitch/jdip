package info.jdip.regression;

import info.jdip.misc.Case;
import info.jdip.misc.TestCaseRunner;
import info.jdip.test.builder.standard.StandardLocation;
import info.jdip.test.builder.standard.StandardPower;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static info.jdip.test.builder.TestCaseBuilder.crowdedMilan;
import static info.jdip.test.builder.TestCaseBuilder.milan;
import static info.jdip.test.builder.TestCaseBuilder.standard;
import static info.jdip.test.builder.milan.CrowdedMilanPower.GERMANY;
import static info.jdip.test.builder.milan.CrowdedMilanPower.RUSSIA;
import static info.jdip.test.builder.milan.MilanLocation.PRUSSIA;
import static info.jdip.test.builder.milan.MilanLocation.SILESIA;
import static info.jdip.test.builder.milan.MilanLocation.WARSAW;
import static info.jdip.world.Phase.PhaseType.MOVEMENT;
import static info.jdip.world.Phase.SeasonType.SPRING;

public class Issue150 {
    @Test
    @DisplayName("Gitlab Issue 150: Should be possible to move from Prussia to Silesia in crowded Milan")
    public void shouldMoveFromPrussiaToSilesiaInCrowdedMilan() throws Exception {
        Case testCase = crowdedMilan(SPRING, 1901, MOVEMENT)
                .army(GERMANY, PRUSSIA)
                .order(PRUSSIA).moveTo(SILESIA)
                .expectArmy(GERMANY, SILESIA)
                .build();

        TestCaseRunner.runCase(testCase);
    }

    @Test
    @DisplayName("Gitlab Issue 150: Should be possible to move from Prussia to Warsaw in crowded Milan")
    public void shouldMoveFromPrussiaToWarsawInCrowdedMilan() throws Exception {
        Case testCase = crowdedMilan(SPRING, 1901, MOVEMENT)
                .army(GERMANY, PRUSSIA)
                .order(PRUSSIA).moveTo(WARSAW)
                .expectArmy(GERMANY, WARSAW)
                .build();

        TestCaseRunner.runCase(testCase);
    }

    @Test
    @DisplayName("Gitlab Issue 150: Should be possible to move from Warsaw to Prussia in crowded Milan")
    public void shouldMoveFromWarsawToPrussiaInCrowdedMilan() throws Exception {
        Case testCase = crowdedMilan(SPRING, 1901, MOVEMENT)
                .army(RUSSIA, WARSAW)
                .order(WARSAW).moveTo(PRUSSIA)
                .expectArmy(RUSSIA, PRUSSIA)
                .build();

        TestCaseRunner.runCase(testCase);
    }


    @Test
    @DisplayName("Gitlab Issue 150: Should be possible to move from Prussia to Silesia in Standard")
    public void shouldMoveFromPrussiaToSilesiaInStandard() throws Exception {
        Case testCase = standard(SPRING, 1901, MOVEMENT)
                .army(StandardPower.GERMANY, StandardLocation.PRUSSIA)
                .order(StandardLocation.PRUSSIA).moveTo(StandardLocation.SILESIA)
                .expectArmy(StandardPower.GERMANY, StandardLocation.SILESIA)
                .build();

        TestCaseRunner.runCase(testCase);
    }

    @Test
    @DisplayName("Gitlab Issue 150: Should be possible to move from Prussia to Warsaw in Standard")
    public void shouldMoveFromPrussiaToWarsawInStandard() throws Exception {
        Case testCase = standard(SPRING, 1901, MOVEMENT)
                .army(StandardPower.GERMANY, StandardLocation.PRUSSIA)
                .order(StandardLocation.PRUSSIA).moveTo(StandardLocation.WARSAW)
                .expectArmy(StandardPower.GERMANY, StandardLocation.WARSAW)
                .build();

        TestCaseRunner.runCase(testCase);
    }

    @Test
    @DisplayName("Gitlab Issue 150: Should be possible to move from Warsaw to Prussia in Standard")
    public void shouldMoveFromWarsawToPrussiaInStandard() throws Exception {
        Case testCase = standard(SPRING, 1901, MOVEMENT)
                .army(StandardPower.RUSSIA, StandardLocation.WARSAW)
                .order(StandardLocation.WARSAW).moveTo(StandardLocation.PRUSSIA)
                .expectArmy(StandardPower.RUSSIA, StandardLocation.PRUSSIA)
                .build();

        TestCaseRunner.runCase(testCase);
    }

    @Test
    @DisplayName("Gitlab Issue 150: Should be possible to move from Prussia to Silesia in Milan")
    public void shouldMoveFromPrussiaToSilesiaInMilan() throws Exception {
        Case testCase = milan(SPRING, 1901, MOVEMENT)
                .army(StandardPower.GERMANY, PRUSSIA)
                .order(PRUSSIA).moveTo(SILESIA)
                .expectArmy(StandardPower.GERMANY, SILESIA)
                .build();

        TestCaseRunner.runCase(testCase);
    }

    @Test
    @DisplayName("Gitlab Issue 150: Should be possible to move from Prussia to Warsaw in Milan")
    public void shouldMoveFromPrussiaToWarsawInMilan() throws Exception {
        Case testCase = milan(SPRING, 1901, MOVEMENT)
                .army(StandardPower.GERMANY, PRUSSIA)
                .order(PRUSSIA).moveTo(WARSAW)
                .expectArmy(StandardPower.GERMANY, WARSAW)
                .build();

        TestCaseRunner.runCase(testCase);
    }

    @Test
    @DisplayName("Gitlab Issue 150: Should be possible to move from Warsaw to Prussia in Milan")
    public void shouldMoveFromWarsawToPrussiaInMilan() throws Exception {
        Case testCase = milan(SPRING, 1901, MOVEMENT)
                .army(StandardPower.RUSSIA, WARSAW)
                .order(WARSAW).moveTo(PRUSSIA)
                .expectArmy(StandardPower.RUSSIA, PRUSSIA)
                .build();

        TestCaseRunner.runCase(testCase);
    }
}
