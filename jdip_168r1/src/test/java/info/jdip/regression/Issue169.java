package info.jdip.regression;

import info.jdip.misc.Case;
import info.jdip.misc.TestCaseRunner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static info.jdip.test.builder.TestCaseBuilder.h32;
import static info.jdip.test.builder.h32.H32Location.ENGLISH_CHANNEL;
import static info.jdip.test.builder.h32.H32Location.LONDON;
import static info.jdip.test.builder.h32.H32Location.NORMANDY;
import static info.jdip.test.builder.h32.H32Location.PARIS;
import static info.jdip.test.builder.h32.H32Power.ENGLAND;
import static info.jdip.test.builder.h32.H32Power.FRANCE;
import static info.jdip.world.Phase.PhaseType.MOVEMENT;
import static info.jdip.world.Phase.SeasonType.SPRING;

public class Issue169 {
    @Test
    @DisplayName("Issue 169: London army should not bounce in normandy without convoy")
    public void londonToNormandyShouldNotBounceWithoutConvoy() throws Exception {
        Case testCase = h32(SPRING, 1426, MOVEMENT)
                .army(ENGLAND, LONDON)
                .army(FRANCE, PARIS)
                .fleet(ENGLAND, ENGLISH_CHANNEL)
                .order(LONDON).moveTo(NORMANDY)
                .order(PARIS).moveTo(NORMANDY)
                .expectArmy(ENGLAND, LONDON)
                .expectArmy(FRANCE, NORMANDY)
                .expectFleet(ENGLAND, ENGLISH_CHANNEL)
                .build();

        TestCaseRunner.runCase(testCase);
    }

    @Test
    @DisplayName("Issue 169: London army should bounce in normandy with convoy")
    public void londonToNormandyShouldBounceWithConvoy() throws Exception {
        Case testCase = h32(SPRING, 1426, MOVEMENT)
                .army(ENGLAND, LONDON)
                .army(FRANCE, PARIS)
                .fleet(ENGLAND, ENGLISH_CHANNEL)
                .order(LONDON).moveTo(NORMANDY)
                .order(PARIS).moveTo(NORMANDY)
                .order(ENGLISH_CHANNEL).convoy(LONDON,NORMANDY)
                .expectArmy(ENGLAND, LONDON)
                .expectArmy(FRANCE, PARIS)
                .expectFleet(ENGLAND, ENGLISH_CHANNEL)
                .build();

        TestCaseRunner.runCase(testCase);
    }
}
