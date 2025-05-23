package ai.timefold.solver.core.impl.constructionheuristic;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.impl.phase.AbstractPhase;
import ai.timefold.solver.core.impl.phase.Phase;
import ai.timefold.solver.core.impl.phase.PossiblyInitializingPhase;

/**
 * A {@link ConstructionHeuristicPhase} is a {@link Phase} which uses a construction heuristic algorithm,
 * such as First Fit, First Fit Decreasing, Cheapest Insertion, ...
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 * @see Phase
 * @see AbstractPhase
 * @see DefaultConstructionHeuristicPhase
 */
public interface ConstructionHeuristicPhase<Solution_>
        extends PossiblyInitializingPhase<Solution_> {

}
