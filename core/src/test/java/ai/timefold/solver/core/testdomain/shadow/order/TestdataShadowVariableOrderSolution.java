package ai.timefold.solver.core.testdomain.shadow.order;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.testdomain.TestdataObject;
import ai.timefold.solver.core.testdomain.TestdataValue;

@PlanningSolution
public class TestdataShadowVariableOrderSolution extends TestdataObject {

    public static SolutionDescriptor<TestdataShadowVariableOrderSolution> buildSolutionDescriptor() {
        return SolutionDescriptor.buildSolutionDescriptor(TestdataShadowVariableOrderSolution.class,
                TestdataShadowVariableOrderEntity.class);
    }

    private List<TestdataValue> valueList;
    private List<TestdataShadowVariableOrderEntity> entityList;

    private SimpleScore score;

    public TestdataShadowVariableOrderSolution() {
    }

    public TestdataShadowVariableOrderSolution(String code) {
        super(code);
    }

    @ValueRangeProvider(id = "valueRange")
    @ProblemFactCollectionProperty
    public List<TestdataValue> getValueList() {
        return valueList;
    }

    public void setValueList(List<TestdataValue> valueList) {
        this.valueList = valueList;
    }

    @PlanningEntityCollectionProperty
    public List<TestdataShadowVariableOrderEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestdataShadowVariableOrderEntity> entityList) {
        this.entityList = entityList;
    }

    @PlanningScore
    public SimpleScore getScore() {
        return score;
    }

    public void setScore(SimpleScore score) {
        this.score = score;
    }
}
