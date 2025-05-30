package ai.timefold.solver.core.testdomain.clone.lookup;

import ai.timefold.solver.core.api.domain.lookup.PlanningId;

public class TestdataObjectIntegerId {

    @PlanningId
    private final Integer id;

    public TestdataObjectIntegerId(Integer id) {
        this.id = id;
    }

    public Integer getId() {
        return id;
    }

}
