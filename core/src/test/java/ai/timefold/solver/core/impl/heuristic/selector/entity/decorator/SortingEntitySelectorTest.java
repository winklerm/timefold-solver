package ai.timefold.solver.core.impl.heuristic.selector.entity.decorator;

import static ai.timefold.solver.core.testutil.PlannerAssert.assertAllCodesOfEntitySelector;
import static ai.timefold.solver.core.testutil.PlannerAssert.verifyPhaseLifecycle;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Comparator;

import ai.timefold.solver.core.config.heuristic.selector.common.SelectionCacheType;
import ai.timefold.solver.core.impl.heuristic.selector.SelectorTestUtils;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionSorter;
import ai.timefold.solver.core.impl.heuristic.selector.entity.EntitySelector;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataObject;
import ai.timefold.solver.core.testdomain.TestdataSolution;

import org.junit.jupiter.api.Test;

class SortingEntitySelectorTest {

    @Test
    void cacheTypeSolver() {
        runCacheType(SelectionCacheType.SOLVER, 1);
    }

    @Test
    void cacheTypePhase() {
        runCacheType(SelectionCacheType.PHASE, 2);
    }

    @Test
    void cacheTypeStep() {
        runCacheType(SelectionCacheType.STEP, 5);
    }

    @Test
    void cacheTypeJustInTime() {
        assertThatIllegalArgumentException().isThrownBy(() -> runCacheType(SelectionCacheType.JUST_IN_TIME, 5));
    }

    public void runCacheType(SelectionCacheType cacheType, int timesCalled) {
        EntitySelector childEntitySelector = SelectorTestUtils.mockEntitySelector(TestdataEntity.class,
                new TestdataEntity("jan"), new TestdataEntity("feb"), new TestdataEntity("mar"),
                new TestdataEntity("apr"), new TestdataEntity("may"), new TestdataEntity("jun"));

        SelectionSorter<TestdataSolution, TestdataEntity> sorter = (scoreDirector, selectionList) -> selectionList
                .sort(Comparator.comparing(TestdataObject::getCode));
        EntitySelector entitySelector = new SortingEntitySelector(childEntitySelector, cacheType, sorter);

        SolverScope solverScope = mock(SolverScope.class);
        entitySelector.solvingStarted(solverScope);

        AbstractPhaseScope phaseScopeA = mock(AbstractPhaseScope.class);
        when(phaseScopeA.getSolverScope()).thenReturn(solverScope);
        entitySelector.phaseStarted(phaseScopeA);

        AbstractStepScope stepScopeA1 = mock(AbstractStepScope.class);
        when(stepScopeA1.getPhaseScope()).thenReturn(phaseScopeA);
        entitySelector.stepStarted(stepScopeA1);
        assertAllCodesOfEntitySelector(entitySelector, "apr", "feb", "jan", "jun", "mar", "may");
        entitySelector.stepEnded(stepScopeA1);

        AbstractStepScope stepScopeA2 = mock(AbstractStepScope.class);
        when(stepScopeA2.getPhaseScope()).thenReturn(phaseScopeA);
        entitySelector.stepStarted(stepScopeA2);
        assertAllCodesOfEntitySelector(entitySelector, "apr", "feb", "jan", "jun", "mar", "may");
        entitySelector.stepEnded(stepScopeA2);

        entitySelector.phaseEnded(phaseScopeA);

        AbstractPhaseScope phaseScopeB = mock(AbstractPhaseScope.class);
        when(phaseScopeB.getSolverScope()).thenReturn(solverScope);
        entitySelector.phaseStarted(phaseScopeB);

        AbstractStepScope stepScopeB1 = mock(AbstractStepScope.class);
        when(stepScopeB1.getPhaseScope()).thenReturn(phaseScopeB);
        entitySelector.stepStarted(stepScopeB1);
        assertAllCodesOfEntitySelector(entitySelector, "apr", "feb", "jan", "jun", "mar", "may");
        entitySelector.stepEnded(stepScopeB1);

        AbstractStepScope stepScopeB2 = mock(AbstractStepScope.class);
        when(stepScopeB2.getPhaseScope()).thenReturn(phaseScopeB);
        entitySelector.stepStarted(stepScopeB2);
        assertAllCodesOfEntitySelector(entitySelector, "apr", "feb", "jan", "jun", "mar", "may");
        entitySelector.stepEnded(stepScopeB2);

        AbstractStepScope stepScopeB3 = mock(AbstractStepScope.class);
        when(stepScopeB3.getPhaseScope()).thenReturn(phaseScopeB);
        entitySelector.stepStarted(stepScopeB3);
        assertAllCodesOfEntitySelector(entitySelector, "apr", "feb", "jan", "jun", "mar", "may");
        entitySelector.stepEnded(stepScopeB3);

        entitySelector.phaseEnded(phaseScopeB);

        entitySelector.solvingEnded(solverScope);

        verifyPhaseLifecycle(childEntitySelector, 1, 2, 5);
        verify(childEntitySelector, times(timesCalled)).iterator();
        verify(childEntitySelector, times(timesCalled)).getSize();
    }

    @Test
    void isNeverEnding() {
        EntitySelector entitySelector = new SortingEntitySelector(mock(EntitySelector.class), SelectionCacheType.PHASE,
                mock(SelectionSorter.class));
        assertThat(entitySelector.isNeverEnding()).isFalse();
    }

    @Test
    void isCountable() {
        EntitySelector entitySelector = new SortingEntitySelector(mock(EntitySelector.class), SelectionCacheType.PHASE,
                mock(SelectionSorter.class));
        assertThat(entitySelector.isCountable()).isTrue();
    }

}
