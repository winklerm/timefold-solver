package ai.timefold.solver.core.impl.heuristic.selector.move.generic;

import static ai.timefold.solver.core.impl.testdata.util.PlannerAssert.assertCode;
import static ai.timefold.solver.core.impl.testdata.util.PlannerTestUtils.mockRebasingScoreDirector;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.impl.score.constraint.ConstraintMatchPolicy;
import ai.timefold.solver.core.impl.score.director.ScoreDirectorFactory;
import ai.timefold.solver.core.impl.score.director.easy.EasyScoreDirectorFactory;
import ai.timefold.solver.core.impl.testdata.domain.TestdataEntity;
import ai.timefold.solver.core.impl.testdata.domain.TestdataSolution;
import ai.timefold.solver.core.impl.testdata.domain.TestdataValue;
import ai.timefold.solver.core.impl.testdata.domain.multivar.TestdataMultiVarEntity;
import ai.timefold.solver.core.impl.testdata.domain.multivar.TestdataMultiVarSolution;
import ai.timefold.solver.core.impl.testdata.domain.multivar.TestdataOtherValue;
import ai.timefold.solver.core.impl.testdata.domain.valuerange.entityproviding.TestdataEntityProvidingEntity;
import ai.timefold.solver.core.impl.testdata.domain.valuerange.entityproviding.TestdataEntityProvidingSolution;

import org.junit.jupiter.api.Test;

class SwapMoveTest {

    @Test
    void isMoveDoableValueRangeProviderOnEntity() {
        TestdataValue v1 = new TestdataValue("1");
        TestdataValue v2 = new TestdataValue("2");
        TestdataValue v3 = new TestdataValue("3");
        TestdataValue v4 = new TestdataValue("4");
        TestdataValue v5 = new TestdataValue("5");

        TestdataEntityProvidingEntity a = new TestdataEntityProvidingEntity("a", Arrays.asList(v1, v2, v3), null);
        TestdataEntityProvidingEntity b = new TestdataEntityProvidingEntity("b", Arrays.asList(v2, v3, v4, v5), null);
        TestdataEntityProvidingEntity c = new TestdataEntityProvidingEntity("c", Arrays.asList(v4, v5), null);

        ScoreDirector<TestdataEntityProvidingSolution> scoreDirector = mock(ScoreDirector.class);
        EntityDescriptor<TestdataEntityProvidingSolution> entityDescriptor = TestdataEntityProvidingEntity
                .buildEntityDescriptor();

        SwapMove<TestdataEntityProvidingSolution> abMove = new SwapMove<>(entityDescriptor.getGenuineVariableDescriptorList(),
                a, b);
        a.setValue(v1);
        b.setValue(v2);
        assertThat(abMove.isMoveDoable(scoreDirector)).isFalse();
        a.setValue(v2);
        b.setValue(v2);
        assertThat(abMove.isMoveDoable(scoreDirector)).isFalse();
        a.setValue(v2);
        b.setValue(v3);
        assertThat(abMove.isMoveDoable(scoreDirector)).isTrue();
        a.setValue(v3);
        b.setValue(v2);
        assertThat(abMove.isMoveDoable(scoreDirector)).isTrue();
        a.setValue(v3);
        b.setValue(v3);
        assertThat(abMove.isMoveDoable(scoreDirector)).isFalse();
        a.setValue(v2);
        b.setValue(v4);
        assertThat(abMove.isMoveDoable(scoreDirector)).isFalse();

        SwapMove<TestdataEntityProvidingSolution> acMove = new SwapMove<>(entityDescriptor.getGenuineVariableDescriptorList(),
                a, c);
        a.setValue(v1);
        c.setValue(v4);
        assertThat(acMove.isMoveDoable(scoreDirector)).isFalse();
        a.setValue(v2);
        c.setValue(v5);
        assertThat(acMove.isMoveDoable(scoreDirector)).isFalse();

        SwapMove<TestdataEntityProvidingSolution> bcMove = new SwapMove<>(entityDescriptor.getGenuineVariableDescriptorList(),
                b, c);
        b.setValue(v2);
        c.setValue(v4);
        assertThat(bcMove.isMoveDoable(scoreDirector)).isFalse();
        b.setValue(v4);
        c.setValue(v5);
        assertThat(bcMove.isMoveDoable(scoreDirector)).isTrue();
        b.setValue(v5);
        c.setValue(v4);
        assertThat(bcMove.isMoveDoable(scoreDirector)).isTrue();
        b.setValue(v5);
        c.setValue(v5);
        assertThat(bcMove.isMoveDoable(scoreDirector)).isFalse();
    }

    @Test
    void doMove() {
        TestdataValue v1 = new TestdataValue("1");
        TestdataValue v2 = new TestdataValue("2");
        TestdataValue v3 = new TestdataValue("3");
        TestdataValue v4 = new TestdataValue("4");

        TestdataEntityProvidingEntity a = new TestdataEntityProvidingEntity("a", Arrays.asList(v1, v2, v3), null);
        TestdataEntityProvidingEntity b = new TestdataEntityProvidingEntity("b", Arrays.asList(v1, v2, v3, v4), null);
        TestdataEntityProvidingEntity c = new TestdataEntityProvidingEntity("c", Arrays.asList(v2, v3, v4), null);

        ScoreDirectorFactory<TestdataEntityProvidingSolution> scoreDirectorFactory =
                new EasyScoreDirectorFactory<>(TestdataEntityProvidingSolution.buildSolutionDescriptor(),
                        solution -> SimpleScore.ZERO);
        ScoreDirector<TestdataEntityProvidingSolution> scoreDirector =
                scoreDirectorFactory.buildScoreDirector(false, ConstraintMatchPolicy.DISABLED);
        EntityDescriptor<TestdataEntityProvidingSolution> entityDescriptor = TestdataEntityProvidingEntity
                .buildEntityDescriptor();

        SwapMove<TestdataEntityProvidingSolution> abMove = new SwapMove<>(entityDescriptor.getGenuineVariableDescriptorList(),
                a, b);

        a.setValue(v1);
        b.setValue(v1);
        abMove.doMoveOnly(scoreDirector);
        assertThat(a.getValue()).isEqualTo(v1);
        assertThat(b.getValue()).isEqualTo(v1);

        a.setValue(v1);
        b.setValue(v2);
        abMove.doMoveOnly(scoreDirector);
        assertThat(a.getValue()).isEqualTo(v2);
        assertThat(b.getValue()).isEqualTo(v1);

        a.setValue(v2);
        b.setValue(v3);
        abMove.doMoveOnly(scoreDirector);
        assertThat(a.getValue()).isEqualTo(v3);
        assertThat(b.getValue()).isEqualTo(v2);
        abMove.doMoveOnly(scoreDirector);
        assertThat(a.getValue()).isEqualTo(v2);
        assertThat(b.getValue()).isEqualTo(v3);

        SwapMove<TestdataEntityProvidingSolution> acMove = new SwapMove<>(entityDescriptor.getGenuineVariableDescriptorList(),
                a, c);

        a.setValue(v2);
        c.setValue(v2);
        acMove.doMoveOnly(scoreDirector);
        assertThat(a.getValue()).isEqualTo(v2);
        assertThat(c.getValue()).isEqualTo(v2);

        a.setValue(v3);
        c.setValue(v2);
        acMove.doMoveOnly(scoreDirector);
        assertThat(a.getValue()).isEqualTo(v2);
        assertThat(c.getValue()).isEqualTo(v3);

        a.setValue(v3);
        c.setValue(v4);
        acMove.doMoveOnly(scoreDirector);
        assertThat(a.getValue()).isEqualTo(v4);
        assertThat(c.getValue()).isEqualTo(v3);
        acMove.doMoveOnly(scoreDirector);
        assertThat(a.getValue()).isEqualTo(v3);
        assertThat(c.getValue()).isEqualTo(v4);

        SwapMove<TestdataEntityProvidingSolution> bcMove = new SwapMove<>(entityDescriptor.getGenuineVariableDescriptorList(),
                b, c);

        b.setValue(v2);
        c.setValue(v2);
        bcMove.doMoveOnly(scoreDirector);
        assertThat(b.getValue()).isEqualTo(v2);
        assertThat(c.getValue()).isEqualTo(v2);

        b.setValue(v2);
        c.setValue(v3);
        bcMove.doMoveOnly(scoreDirector);
        assertThat(b.getValue()).isEqualTo(v3);
        assertThat(c.getValue()).isEqualTo(v2);

        b.setValue(v2);
        c.setValue(v3);
        bcMove.doMoveOnly(scoreDirector);
        assertThat(b.getValue()).isEqualTo(v3);
        assertThat(c.getValue()).isEqualTo(v2);
        bcMove.doMoveOnly(scoreDirector);
        assertThat(b.getValue()).isEqualTo(v2);
        assertThat(c.getValue()).isEqualTo(v3);
    }

    @Test
    void rebase() {
        EntityDescriptor<TestdataSolution> entityDescriptor = TestdataEntity.buildEntityDescriptor();
        List<GenuineVariableDescriptor<TestdataSolution>> variableDescriptorList = entityDescriptor
                .getGenuineVariableDescriptorList();

        TestdataValue v1 = new TestdataValue("v1");
        TestdataValue v2 = new TestdataValue("v2");
        TestdataEntity e1 = new TestdataEntity("e1", v1);
        TestdataEntity e2 = new TestdataEntity("e2", null);
        TestdataEntity e3 = new TestdataEntity("e3", v1);

        TestdataValue destinationV1 = new TestdataValue("v1");
        TestdataValue destinationV2 = new TestdataValue("v2");
        TestdataEntity destinationE1 = new TestdataEntity("e1", destinationV1);
        TestdataEntity destinationE2 = new TestdataEntity("e2", null);
        TestdataEntity destinationE3 = new TestdataEntity("e3", destinationV1);

        ScoreDirector<TestdataSolution> destinationScoreDirector = mockRebasingScoreDirector(
                entityDescriptor.getSolutionDescriptor(), new Object[][] {
                        { v1, destinationV1 },
                        { v2, destinationV2 },
                        { e1, destinationE1 },
                        { e2, destinationE2 },
                        { e3, destinationE3 },
                });

        assertSameProperties(destinationE1, destinationE2,
                new SwapMove<>(variableDescriptorList, e1, e2).rebase(destinationScoreDirector));
        assertSameProperties(destinationE1, destinationE3,
                new SwapMove<>(variableDescriptorList, e1, e3).rebase(destinationScoreDirector));
        assertSameProperties(destinationE2, destinationE3,
                new SwapMove<>(variableDescriptorList, e2, e3).rebase(destinationScoreDirector));
    }

    public void assertSameProperties(Object leftEntity, Object rightEntity, SwapMove<?> move) {
        assertThat(move.getLeftEntity()).isSameAs(leftEntity);
        assertThat(move.getRightEntity()).isSameAs(rightEntity);
    }

    @Test
    void getters() {
        GenuineVariableDescriptor<TestdataMultiVarSolution> primaryDescriptor = TestdataMultiVarEntity
                .buildVariableDescriptorForPrimaryValue();
        GenuineVariableDescriptor<TestdataMultiVarSolution> secondaryDescriptor = TestdataMultiVarEntity
                .buildVariableDescriptorForSecondaryValue();
        SwapMove move = new SwapMove<>(Collections.singletonList(primaryDescriptor),
                new TestdataMultiVarEntity("a"), new TestdataMultiVarEntity("b"));
        assertCode("a", move.getLeftEntity());
        assertCode("b", move.getRightEntity());

        move = new SwapMove<>(Arrays.asList(primaryDescriptor, secondaryDescriptor),
                new TestdataMultiVarEntity("c"), new TestdataMultiVarEntity("d"));
        assertCode("c", move.getLeftEntity());
        assertCode("d", move.getRightEntity());
    }

    @Test
    void toStringTest() {
        TestdataValue v1 = new TestdataValue("v1");
        TestdataValue v2 = new TestdataValue("v2");
        TestdataEntity a = new TestdataEntity("a", null);
        TestdataEntity b = new TestdataEntity("b", v1);
        TestdataEntity c = new TestdataEntity("c", v2);
        EntityDescriptor<TestdataSolution> entityDescriptor = TestdataEntity.buildEntityDescriptor();
        List<GenuineVariableDescriptor<TestdataSolution>> variableDescriptorList = entityDescriptor
                .getGenuineVariableDescriptorList();

        assertThat(new SwapMove<>(variableDescriptorList, a, a)).hasToString("a {null} <-> a {null}");
        assertThat(new SwapMove<>(variableDescriptorList, a, b)).hasToString("a {null} <-> b {v1}");
        assertThat(new SwapMove<>(variableDescriptorList, a, c)).hasToString("a {null} <-> c {v2}");
        assertThat(new SwapMove<>(variableDescriptorList, b, c)).hasToString("b {v1} <-> c {v2}");
        assertThat(new SwapMove<>(variableDescriptorList, c, b)).hasToString("c {v2} <-> b {v1}");
    }

    @Test
    void toStringTestMultiVar() {
        TestdataValue v1 = new TestdataValue("v1");
        TestdataValue v2 = new TestdataValue("v2");
        TestdataValue v3 = new TestdataValue("v3");
        TestdataValue v4 = new TestdataValue("v4");
        TestdataOtherValue w1 = new TestdataOtherValue("w1");
        TestdataOtherValue w2 = new TestdataOtherValue("w2");
        TestdataMultiVarEntity a = new TestdataMultiVarEntity("a", null, null, null);
        TestdataMultiVarEntity b = new TestdataMultiVarEntity("b", v1, v3, w1);
        TestdataMultiVarEntity c = new TestdataMultiVarEntity("c", v2, v4, w2);
        EntityDescriptor<TestdataMultiVarSolution> entityDescriptor = TestdataMultiVarEntity.buildEntityDescriptor();
        List<GenuineVariableDescriptor<TestdataMultiVarSolution>> variableDescriptorList = entityDescriptor
                .getGenuineVariableDescriptorList();

        assertThat(new SwapMove<>(variableDescriptorList, a, a)).hasToString("a {null, null, null} <-> a {null, null, null}");
        assertThat(new SwapMove<>(variableDescriptorList, a, b)).hasToString("a {null, null, null} <-> b {v1, v3, w1}");
        assertThat(new SwapMove<>(variableDescriptorList, a, c)).hasToString("a {null, null, null} <-> c {v2, v4, w2}");
        assertThat(new SwapMove<>(variableDescriptorList, b, c)).hasToString("b {v1, v3, w1} <-> c {v2, v4, w2}");
        assertThat(new SwapMove<>(variableDescriptorList, c, b)).hasToString("c {v2, v4, w2} <-> b {v1, v3, w1}");
    }

}
