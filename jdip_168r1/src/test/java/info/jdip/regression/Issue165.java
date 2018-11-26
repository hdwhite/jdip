package info.jdip.regression;

import info.jdip.misc.Case;
import info.jdip.misc.TestCaseRunner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static info.jdip.test.builder.standard.StandardLocation.DENMARK;
import static info.jdip.test.builder.standard.StandardLocation.HOLLAND;
import static info.jdip.test.builder.standard.StandardLocation.NORTH_SEA;
import static info.jdip.test.builder.standard.StandardLocation.NORWAY;
import static info.jdip.test.builder.standard.StandardLocation.NORWEGIAN_SEA;
import static info.jdip.test.builder.standard.StandardLocation.SWEDEN;
import static info.jdip.test.builder.standard.StandardPower.GERMANY;
import static info.jdip.test.builder.standard.StandardPower.RUSSIA;
import static info.jdip.test.builder.TestCaseBuilder.standard;
import static info.jdip.world.Phase.PhaseType.MOVEMENT;
import static info.jdip.world.Phase.SeasonType.SPRING;

public class Issue165 {
    @Test
    @DisplayName("Issue 165")
    public void fleetCannotSupportInland() throws Exception {
        Case testCase = standard(SPRING, 1901, MOVEMENT).
                fleet(GERMANY, NORTH_SEA)
                .army(GERMANY, HOLLAND)
                .order(NORTH_SEA).convoy(HOLLAND, DENMARK)
                .order(HOLLAND).moveTo(DENMARK)
                .army(RUSSIA, SWEDEN)
                .order(SWEDEN).moveTo(DENMARK)
                .fleet(RUSSIA, NORWEGIAN_SEA)
                .fleet(RUSSIA, NORWAY)
                .order(NORWEGIAN_SEA).supportMove(NORWAY, NORTH_SEA)
                .order(NORWAY).moveTo(NORTH_SEA)
                .expectArmy(GERMANY, HOLLAND)
                .expectArmy(RUSSIA, DENMARK)
                .expectFleet(RUSSIA, NORTH_SEA)
                .expectFleet(RUSSIA, NORWEGIAN_SEA)
                .expectDislodgedFleet(GERMANY, NORTH_SEA)
                .build();

        TestCaseRunner.runCase(testCase);
    }
}
