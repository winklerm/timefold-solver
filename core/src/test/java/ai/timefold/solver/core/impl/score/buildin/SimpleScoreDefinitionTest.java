package ai.timefold.solver.core.impl.score.buildin;

import static org.assertj.core.api.Assertions.assertThat;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.config.score.trend.InitializingScoreTrendLevel;
import ai.timefold.solver.core.impl.score.trend.InitializingScoreTrend;

import org.junit.jupiter.api.Test;

class SimpleScoreDefinitionTest {

    @Test
    void getZeroScore() {
        var score = new SimpleScoreDefinition().getZeroScore();
        assertThat(score).isEqualTo(SimpleScore.ZERO);
    }

    @Test
    void getSoftestOneScore() {
        var score = new SimpleScoreDefinition().getOneSoftestScore();
        assertThat(score).isEqualTo(SimpleScore.ONE);
    }

    @Test
    void getLevelsSize() {
        assertThat(new SimpleScoreDefinition().getLevelsSize()).isEqualTo(1);
    }

    @Test
    void getLevelLabels() {
        assertThat(new SimpleScoreDefinition().getLevelLabels()).containsExactly("score");
    }

    @Test
    void buildOptimisticBoundOnlyUp() {
        var scoreDefinition = new SimpleScoreDefinition();
        var optimisticBound = scoreDefinition.buildOptimisticBound(
                InitializingScoreTrend.buildUniformTrend(InitializingScoreTrendLevel.ONLY_UP, 1),
                SimpleScore.of(-1));
        assertThat(optimisticBound.score()).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    void buildOptimisticBoundOnlyDown() {
        var scoreDefinition = new SimpleScoreDefinition();
        var optimisticBound = scoreDefinition.buildOptimisticBound(
                InitializingScoreTrend.buildUniformTrend(InitializingScoreTrendLevel.ONLY_DOWN, 1),
                SimpleScore.of(-1));
        assertThat(optimisticBound.score()).isEqualTo(-1);
    }

    @Test
    void buildPessimisticBoundOnlyUp() {
        var scoreDefinition = new SimpleScoreDefinition();
        var pessimisticBound = scoreDefinition.buildPessimisticBound(
                InitializingScoreTrend.buildUniformTrend(InitializingScoreTrendLevel.ONLY_UP, 1),
                SimpleScore.of(-1));
        assertThat(pessimisticBound.score()).isEqualTo(-1);
    }

    @Test
    void buildPessimisticBoundOnlyDown() {
        var scoreDefinition = new SimpleScoreDefinition();
        var pessimisticBound = scoreDefinition.buildPessimisticBound(
                InitializingScoreTrend.buildUniformTrend(InitializingScoreTrendLevel.ONLY_DOWN, 1),
                SimpleScore.of(-1));
        assertThat(pessimisticBound.score()).isEqualTo(Integer.MIN_VALUE);
    }

    @Test
    void divideBySanitizedDivisor() {
        var scoreDefinition = new SimpleScoreDefinition();
        var dividend = scoreDefinition.fromLevelNumbers(new Number[] { 10 });
        var zeroDivisor = scoreDefinition.getZeroScore();
        assertThat(scoreDefinition.divideBySanitizedDivisor(dividend, zeroDivisor))
                .isEqualTo(dividend);
        var oneDivisor = scoreDefinition.getOneSoftestScore();
        assertThat(scoreDefinition.divideBySanitizedDivisor(dividend, oneDivisor))
                .isEqualTo(dividend);
        var tenDivisor = scoreDefinition.fromLevelNumbers(new Number[] { 10 });
        assertThat(scoreDefinition.divideBySanitizedDivisor(dividend, tenDivisor))
                .isEqualTo(scoreDefinition.fromLevelNumbers(new Number[] { 1 }));
    }

}
