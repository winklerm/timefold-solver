package ai.timefold.solver.core.testdomain.list;

import ai.timefold.solver.core.impl.heuristic.selector.common.nearby.NearbyDistanceMeter;
import ai.timefold.solver.core.testdomain.TestdataObject;

/**
 * For the sake of test readability, planning values (list variable elements) are placed in a 1-dimensional space.
 * An element's coordinate is represented by its ({@link TestdataObject#getCode() code}. If the code is not a number,
 * it is interpreted as zero.
 */
public class TestDistanceMeter implements NearbyDistanceMeter<TestdataListValue, TestdataObject> {

    @Override
    public double getNearbyDistance(TestdataListValue origin, TestdataObject destination) {
        return Math.abs(coordinate(destination) - coordinate(origin));
    }

    static int coordinate(TestdataObject o) {
        try {
            return Integer.parseInt(o.getCode());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
