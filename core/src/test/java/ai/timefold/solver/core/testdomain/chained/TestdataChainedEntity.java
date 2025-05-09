package ai.timefold.solver.core.testdomain.chained;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.api.domain.variable.PlanningVariableGraphType;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.testdomain.TestdataObject;
import ai.timefold.solver.core.testdomain.TestdataValue;

@PlanningEntity
public class TestdataChainedEntity extends TestdataObject implements TestdataChainedObject {

    public static EntityDescriptor<TestdataChainedSolution> buildEntityDescriptor() {
        return TestdataChainedSolution.buildSolutionDescriptor()
                .findEntityDescriptorOrFail(TestdataChainedEntity.class);
    }

    public static GenuineVariableDescriptor<TestdataChainedSolution> buildVariableDescriptorForChainedObject() {
        return buildEntityDescriptor().getGenuineVariableDescriptor("chainedObject");
    }

    public static GenuineVariableDescriptor<TestdataChainedSolution> buildVariableDescriptorForUnchainedValue() {
        return buildEntityDescriptor().getGenuineVariableDescriptor("unchainedValue");
    }

    private TestdataChainedObject chainedObject;
    private TestdataValue unchainedValue;

    public TestdataChainedEntity() {
    }

    public TestdataChainedEntity(String code) {
        super(code);
    }

    public TestdataChainedEntity(String code, TestdataChainedObject chainedObject) {
        this(code);
        this.chainedObject = chainedObject;
    }

    @PlanningVariable(valueRangeProviderRefs = { "chainedAnchorRange",
            "chainedEntityRange" }, graphType = PlanningVariableGraphType.CHAINED)
    public TestdataChainedObject getChainedObject() {
        return chainedObject;
    }

    public void setChainedObject(TestdataChainedObject chainedObject) {
        this.chainedObject = chainedObject;
    }

    @PlanningVariable(valueRangeProviderRefs = { "unchainedRange" })
    public TestdataValue getUnchainedValue() {
        return unchainedValue;
    }

    public void setUnchainedValue(TestdataValue unchainedValue) {
        this.unchainedValue = unchainedValue;
    }

    public void getUnchainedObject(TestdataChainedObject chainedObject) {
        this.chainedObject = chainedObject;
    }

    // ************************************************************************
    // Complex methods
    // ************************************************************************

}
