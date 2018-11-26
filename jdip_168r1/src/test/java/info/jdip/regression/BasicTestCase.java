package info.jdip.regression;

import info.jdip.misc.Case;
import info.jdip.misc.TestCaseRunner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static info.jdip.test.builder.standard.StandardLocation.BUDAPEST;
import static info.jdip.test.builder.standard.StandardLocation.GALACIA;
import static info.jdip.test.builder.standard.StandardLocation.RUMANIA;
import static info.jdip.test.builder.standard.StandardLocation.TRIESTE;
import static info.jdip.test.builder.standard.StandardPower.AUSTRIA;
import static info.jdip.test.builder.standard.StandardPower.RUSSIA;
import static info.jdip.test.builder.TestCaseBuilder.standard;
import static info.jdip.world.Phase.PhaseType.MOVEMENT;
import static info.jdip.world.Phase.SeasonType.SPRING;

public class BasicTestCase {

    @Test
    @DisplayName("CASE 6.A.3.fleet.support.inland")
    public void fleetCannotSupportInland() throws Exception {
        Case testCase = standard(SPRING, 1901, MOVEMENT).
                fleet(AUSTRIA, TRIESTE)
                .army(AUSTRIA, BUDAPEST)
                .army(RUSSIA, GALACIA)
                .army(RUSSIA, RUMANIA)
                .order(TRIESTE).supportHold(BUDAPEST)
                .order(BUDAPEST).hold()
                .order(GALACIA).moveTo(BUDAPEST)
                .order(RUMANIA).supportMove(GALACIA, BUDAPEST)
                .expectFleet(AUSTRIA, TRIESTE)
                .expectArmy(RUSSIA, BUDAPEST)
                .expectArmy(RUSSIA,RUMANIA)
                .expectDislodgedArmy(AUSTRIA, BUDAPEST)
                .build();

        TestCaseRunner.runCase(testCase);
    }
}
