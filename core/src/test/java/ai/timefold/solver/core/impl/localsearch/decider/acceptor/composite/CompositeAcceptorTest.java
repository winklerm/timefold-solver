package ai.timefold.solver.core.impl.localsearch.decider.acceptor.composite;

import static ai.timefold.solver.core.testutil.PlannerAssert.verifyPhaseLifecycle;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;

import ai.timefold.solver.core.impl.localsearch.decider.acceptor.Acceptor;
import ai.timefold.solver.core.impl.localsearch.decider.acceptor.CompositeAcceptor;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchMoveScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchPhaseScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchStepScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.testdomain.TestdataSolution;

import org.junit.jupiter.api.Test;

class CompositeAcceptorTest {

    @Test
    void phaseLifecycle() {
        SolverScope<TestdataSolution> solverScope = mock(SolverScope.class);
        LocalSearchPhaseScope<TestdataSolution> phaseScope = mock(LocalSearchPhaseScope.class);
        LocalSearchStepScope<TestdataSolution> stepScope = mock(LocalSearchStepScope.class);

        Acceptor<TestdataSolution> acceptor1 = mock(Acceptor.class);
        Acceptor<TestdataSolution> acceptor2 = mock(Acceptor.class);
        Acceptor<TestdataSolution> acceptor3 = mock(Acceptor.class);
        var compositeAcceptor = new CompositeAcceptor<>(acceptor1, acceptor2, acceptor3);

        compositeAcceptor.solvingStarted(solverScope);
        compositeAcceptor.phaseStarted(phaseScope);
        compositeAcceptor.stepStarted(stepScope);
        compositeAcceptor.stepEnded(stepScope);
        compositeAcceptor.stepStarted(stepScope);
        compositeAcceptor.stepEnded(stepScope);
        compositeAcceptor.phaseEnded(phaseScope);
        compositeAcceptor.phaseStarted(phaseScope);
        compositeAcceptor.stepStarted(stepScope);
        compositeAcceptor.stepEnded(stepScope);
        compositeAcceptor.phaseEnded(phaseScope);
        compositeAcceptor.solvingEnded(solverScope);

        verifyPhaseLifecycle(acceptor1, 1, 2, 3);
        verifyPhaseLifecycle(acceptor2, 1, 2, 3);
        verifyPhaseLifecycle(acceptor3, 1, 2, 3);
    }

    @Test
    void isAccepted() {
        assertThat(isCompositeAccepted(true, true, true)).isTrue();
        assertThat(isCompositeAccepted(false, true, true)).isFalse();
        assertThat(isCompositeAccepted(true, false, true)).isFalse();
        assertThat(isCompositeAccepted(true, true, false)).isFalse();
        assertThat(isCompositeAccepted(false, false, false)).isFalse();
    }

    private boolean isCompositeAccepted(boolean... childAccepts) {
        var acceptorList = new ArrayList<Acceptor<TestdataSolution>>(childAccepts.length);
        for (var childAccept : childAccepts) {
            Acceptor<TestdataSolution> acceptor = mock(Acceptor.class);
            when(acceptor.isAccepted(any(LocalSearchMoveScope.class))).thenReturn(childAccept);
            acceptorList.add(acceptor);
        }
        var acceptor = new CompositeAcceptor<>(acceptorList);
        return acceptor.isAccepted(mock(LocalSearchMoveScope.class));
    }
}
