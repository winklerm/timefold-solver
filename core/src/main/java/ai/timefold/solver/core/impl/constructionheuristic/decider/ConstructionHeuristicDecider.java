package ai.timefold.solver.core.impl.constructionheuristic.decider;

import java.util.Iterator;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.constructionheuristic.decider.forager.ConstructionHeuristicForager;
import ai.timefold.solver.core.impl.constructionheuristic.scope.ConstructionHeuristicMoveScope;
import ai.timefold.solver.core.impl.constructionheuristic.scope.ConstructionHeuristicPhaseScope;
import ai.timefold.solver.core.impl.constructionheuristic.scope.ConstructionHeuristicStepScope;
import ai.timefold.solver.core.impl.heuristic.move.LegacyMoveAdapter;
import ai.timefold.solver.core.impl.move.generic.NoChangeMove;
import ai.timefold.solver.core.impl.phase.scope.SolverLifecyclePoint;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.solver.termination.PhaseTermination;
import ai.timefold.solver.core.preview.api.move.Move;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public class ConstructionHeuristicDecider<Solution_> {

    protected final transient Logger logger = LoggerFactory.getLogger(getClass());

    protected final String logIndentation;
    protected final PhaseTermination<Solution_> termination;
    protected final ConstructionHeuristicForager<Solution_> forager;

    protected boolean assertMoveScoreFromScratch = false;
    protected boolean assertExpectedUndoMoveScore = false;

    public ConstructionHeuristicDecider(String logIndentation, PhaseTermination<Solution_> termination,
            ConstructionHeuristicForager<Solution_> forager) {
        this.logIndentation = logIndentation;
        this.termination = termination;
        this.forager = forager;
    }

    public boolean isLoggingEnabled() {
        return true;
    }

    public ConstructionHeuristicForager<Solution_> getForager() {
        return forager;
    }

    public void enableAssertions(EnvironmentMode environmentMode) {
        this.assertMoveScoreFromScratch = environmentMode.isFullyAsserted();
        this.assertExpectedUndoMoveScore = environmentMode.isIntrusivelyAsserted();
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    public void solvingStarted(SolverScope<Solution_> solverScope) {
        forager.solvingStarted(solverScope);
    }

    public void phaseStarted(ConstructionHeuristicPhaseScope<Solution_> phaseScope) {
        forager.phaseStarted(phaseScope);
    }

    public void stepStarted(ConstructionHeuristicStepScope<Solution_> stepScope) {
        forager.stepStarted(stepScope);
    }

    public void stepEnded(ConstructionHeuristicStepScope<Solution_> stepScope) {
        forager.stepEnded(stepScope);
    }

    public void phaseEnded(ConstructionHeuristicPhaseScope<Solution_> phaseScope) {
        forager.phaseEnded(phaseScope);
    }

    public void solvingEnded(SolverScope<Solution_> solverScope) {
        forager.solvingEnded(solverScope);
    }

    public void solvingError(SolverScope<Solution_> solverScope, Exception exception) {
        // Overridable by a subclass.
    }

    public void decideNextStep(ConstructionHeuristicStepScope<Solution_> stepScope, Iterator<Move<Solution_>> moveIterator) {
        var moveIndex = 0;
        var terminatedPrematurely = false;
        while (moveIterator.hasNext()) {
            var move = moveIterator.next();
            var allowedNonDoableMove = isAllowedNonDoableMove(move);
            if (!allowedNonDoableMove) {
                var moveDirector = stepScope.getMoveDirector();
                if (!LegacyMoveAdapter.isDoable(moveDirector, move)) {
                    // Construction Heuristic should not do non-doable moves, but in some cases, it has to.
                    // Specifically:
                    //      1/ NoChangeMove for list variable; means "try to not assign that value".
                    //      2/ ChangeMove for basic variable; move from null to null means "try to not assign that value".
                    //      3/ Technically also ChainedChangeMove, but chained doesn't support unassigned values.
                    // Every other non-doable move must not be executed, as it may cause all sorts of issues.
                    // Example: ListChangeMove from a[0] to a[1] when the list of 'a' only has 1 element.
                    //      This move is correctly non-doable,
                    //      but it may be generated by the placer, and must therefore be ignored.
                    // Note: CH will only ever see change moves, as its purpose is to assign a variable to a value.
                    //      It will never do anything more complex than that.
                    continue;
                }
            }
            var moveScope = new ConstructionHeuristicMoveScope<>(stepScope, moveIndex, move);
            moveIndex++;
            doMove(moveScope);
            if (forager.isQuitEarly()) {
                break;
            }
            stepScope.getPhaseScope().getSolverScope().checkYielding();
            if (termination.isPhaseTerminated(stepScope.getPhaseScope())) {
                terminatedPrematurely = true;
                break;
            }
        }
        // Only pick a move when CH has finished all moves within the step, or when pick early was enabled.
        // If CH terminated prematurely, it means a move could have been picked which makes the solution worse,
        // while there were moves still to be evaluated that could have been better.
        // This typically happens for list variable with allowsUnassignedValues=true,
        // where most moves make the score worse and the only move that doesn't is the move which assigns null.
        // This move typically comes last, and therefore if the phase terminates early, it will not be attempted.
        if (!terminatedPrematurely) {
            pickMove(stepScope);
        }
    }

    private static <Solution_> boolean isAllowedNonDoableMove(Move<Solution_> move) {
        if (move instanceof LegacyMoveAdapter<Solution_> legacyMove) {
            var adaptedMove = legacyMove.legacyMove();
            return adaptedMove instanceof ai.timefold.solver.core.impl.heuristic.move.NoChangeMove<Solution_>
                    || adaptedMove instanceof ai.timefold.solver.core.impl.heuristic.selector.move.generic.ChangeMove<Solution_>;
        } else {
            return move instanceof NoChangeMove<Solution_>;
        }
    }

    protected void pickMove(ConstructionHeuristicStepScope<Solution_> stepScope) {
        var pickedMoveScope = forager.pickMove(stepScope);
        if (pickedMoveScope != null) {
            var step = pickedMoveScope.getMove();
            stepScope.setStep(step);
            if (isLoggingEnabled() && logger.isDebugEnabled()) {
                stepScope.setStepString(step.toString());
            }
            stepScope.setScore(pickedMoveScope.getScore());
        }
    }

    protected void doMove(ConstructionHeuristicMoveScope<Solution_> moveScope) {
        var scoreDirector = moveScope.getScoreDirector();
        var score = scoreDirector.executeTemporaryMove(moveScope.getMove(), assertMoveScoreFromScratch);
        moveScope.setScore(score);
        forager.addMove(moveScope);
        if (assertExpectedUndoMoveScore) {
            scoreDirector.assertExpectedUndoMoveScore(moveScope.getMove(),
                    moveScope.getStepScope().getPhaseScope().getLastCompletedStepScope().getScore(),
                    SolverLifecyclePoint.of(moveScope));
        }
        if (isLoggingEnabled()) {
            logger.trace("{}        Move index ({}), score ({}), move ({}).",
                    logIndentation, moveScope.getMoveIndex(), moveScope.getScore().raw(), moveScope.getMove());
        }
    }

}
