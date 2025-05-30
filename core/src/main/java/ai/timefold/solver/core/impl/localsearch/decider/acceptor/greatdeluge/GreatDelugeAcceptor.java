package ai.timefold.solver.core.impl.localsearch.decider.acceptor.greatdeluge;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.localsearch.decider.acceptor.AbstractAcceptor;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchMoveScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchPhaseScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchStepScope;

public class GreatDelugeAcceptor<Solution_> extends AbstractAcceptor<Solution_> {

    // Guaranteed inside local search, therefore no need for InnerScore.
    private Score initialWaterLevel;
    private Score waterLevelIncrementScore;
    private Double waterLevelIncrementRatio;
    private Score startingWaterLevel = null;
    private Score currentWaterLevel = null;
    private Double currentWaterLevelRatio = null;

    public Score getWaterLevelIncrementScore() {
        return this.waterLevelIncrementScore;
    }

    public void setWaterLevelIncrementScore(Score waterLevelIncrementScore) {
        this.waterLevelIncrementScore = waterLevelIncrementScore;
    }

    public Score getInitialWaterLevel() {
        return this.initialWaterLevel;
    }

    public void setInitialWaterLevel(Score initialLevel) {
        this.initialWaterLevel = initialLevel;
    }

    public Double getWaterLevelIncrementRatio() {
        return this.waterLevelIncrementRatio;
    }

    public void setWaterLevelIncrementRatio(Double waterLevelIncrementRatio) {
        this.waterLevelIncrementRatio = waterLevelIncrementRatio;
    }

    @Override
    public void phaseStarted(LocalSearchPhaseScope<Solution_> phaseScope) {
        super.phaseStarted(phaseScope);
        startingWaterLevel = initialWaterLevel != null ? initialWaterLevel : phaseScope.getBestScore().raw();
        if (waterLevelIncrementRatio != null) {
            currentWaterLevelRatio = 0.0;
        }
        currentWaterLevel = startingWaterLevel;
    }

    @Override
    public void phaseEnded(LocalSearchPhaseScope<Solution_> phaseScope) {
        super.phaseEnded(phaseScope);
        startingWaterLevel = null;
        if (waterLevelIncrementRatio != null) {
            currentWaterLevelRatio = null;
        }
        currentWaterLevel = null;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public boolean isAccepted(LocalSearchMoveScope moveScope) {
        var moveScore = moveScope.getScore().raw();
        if (moveScore.compareTo(currentWaterLevel) >= 0) {
            return true;
        }
        var lastStepScore = moveScope.getStepScope().getPhaseScope().getLastCompletedStepScope().getScore().raw();
        return moveScore.compareTo(lastStepScore) > 0; // Aspiration
    }

    @Override
    public void stepEnded(LocalSearchStepScope<Solution_> stepScope) {
        super.stepEnded(stepScope);
        if (waterLevelIncrementScore != null) {
            currentWaterLevel = currentWaterLevel.add(waterLevelIncrementScore);
        } else {
            // Avoid numerical instability: SimpleScore.of(500).multiply(0.000_001) underflows to zero
            currentWaterLevelRatio += waterLevelIncrementRatio;
            currentWaterLevel = startingWaterLevel.add(
                    // TODO targetWaterLevel.subtract(startingWaterLevel).multiply(waterLevelIncrementRatio);
                    // Use startingWaterLevel.abs() to keep the number being positive.
                    startingWaterLevel.abs().multiply(currentWaterLevelRatio));
        }
    }

}
