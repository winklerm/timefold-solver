package ai.timefold.solver.core.impl.heuristic.move;

import java.util.Collection;
import java.util.Collections;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testutil.CodeAssertable;

public class DummyMove extends AbstractMove<TestdataSolution> implements CodeAssertable {

    protected String code;

    public DummyMove() {
    }

    public DummyMove(String code) {
        this.code = code;
    }

    @Override
    public String getCode() {
        return code;
    }

    // ************************************************************************
    // Complex methods
    // ************************************************************************

    @Override
    public boolean isMoveDoable(ScoreDirector<TestdataSolution> scoreDirector) {
        return true;
    }

    @Override
    protected void doMoveOnGenuineVariables(ScoreDirector<TestdataSolution> scoreDirector) {
        // do nothing
    }

    @Override
    public Collection<? extends TestdataSolution> getPlanningEntities() {
        return Collections.emptyList();
    }

    @Override
    public Collection<? extends TestdataSolution> getPlanningValues() {
        return Collections.emptyList();
    }

    @Override
    public String toString() {
        return code;
    }

}
