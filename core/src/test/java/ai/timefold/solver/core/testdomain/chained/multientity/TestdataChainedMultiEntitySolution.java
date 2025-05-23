package ai.timefold.solver.core.testdomain.chained.multientity;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;

@PlanningSolution
public class TestdataChainedMultiEntitySolution {

    private List<TestdataChainedBrownEntity> brownEntities;
    private List<TestdataChainedGreenEntity> greenEntities;
    private List<TestdataChainedMultiEntityAnchor> anchors;
    private SimpleScore score;

    public TestdataChainedMultiEntitySolution() {
    }

    public TestdataChainedMultiEntitySolution(
            List<TestdataChainedBrownEntity> brownEntities,
            List<TestdataChainedGreenEntity> greenEntities,
            List<TestdataChainedMultiEntityAnchor> anchors) {
        this.brownEntities = brownEntities;
        this.greenEntities = greenEntities;
        this.anchors = anchors;
    }

    @PlanningEntityCollectionProperty
    @ValueRangeProvider(id = "brownRange")
    public List<TestdataChainedBrownEntity> getBrownEntities() {
        return brownEntities;
    }

    @PlanningEntityCollectionProperty
    @ValueRangeProvider(id = "greenRange")
    public List<TestdataChainedGreenEntity> getGreenEntities() {
        return greenEntities;
    }

    @ProblemFactCollectionProperty
    @ValueRangeProvider(id = "anchorRange")
    public List<TestdataChainedMultiEntityAnchor> getAnchors() {
        return anchors;
    }

    @PlanningScore
    public SimpleScore getScore() {
        return score;
    }

    public void setScore(SimpleScore score) {
        this.score = score;
    }
}
