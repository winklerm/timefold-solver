package ai.timefold.solver.core.impl.score.buildin;

import java.util.Arrays;

import ai.timefold.solver.core.api.score.buildin.hardmediumsoftlong.HardMediumSoftLongScore;
import ai.timefold.solver.core.config.score.trend.InitializingScoreTrendLevel;
import ai.timefold.solver.core.impl.score.definition.AbstractScoreDefinition;
import ai.timefold.solver.core.impl.score.trend.InitializingScoreTrend;

public class HardMediumSoftLongScoreDefinition extends AbstractScoreDefinition<HardMediumSoftLongScore> {

    public HardMediumSoftLongScoreDefinition() {
        super(new String[] { "hard score", "medium score", "soft score" });
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    public int getLevelsSize() {
        return 3;
    }

    @Override
    public int getFeasibleLevelsSize() {
        return 1;
    }

    @Override
    public Class<HardMediumSoftLongScore> getScoreClass() {
        return HardMediumSoftLongScore.class;
    }

    @Override
    public HardMediumSoftLongScore getZeroScore() {
        return HardMediumSoftLongScore.ZERO;
    }

    @Override
    public HardMediumSoftLongScore getOneSoftestScore() {
        return HardMediumSoftLongScore.ONE_SOFT;
    }

    @Override
    public HardMediumSoftLongScore parseScore(String scoreString) {
        return HardMediumSoftLongScore.parseScore(scoreString);
    }

    @Override
    public HardMediumSoftLongScore fromLevelNumbers(int initScore, Number[] levelNumbers) {
        if (levelNumbers.length != getLevelsSize()) {
            throw new IllegalStateException("The levelNumbers (" + Arrays.toString(levelNumbers)
                    + ")'s length (" + levelNumbers.length + ") must equal the levelSize (" + getLevelsSize() + ").");
        }
        return HardMediumSoftLongScore.ofUninitialized(initScore, (Long) levelNumbers[0], (Long) levelNumbers[1],
                (Long) levelNumbers[2]);
    }

    @Override
    public HardMediumSoftLongScore buildOptimisticBound(InitializingScoreTrend initializingScoreTrend,
            HardMediumSoftLongScore score) {
        InitializingScoreTrendLevel[] trendLevels = initializingScoreTrend.trendLevels();
        return HardMediumSoftLongScore.ofUninitialized(0,
                trendLevels[0] == InitializingScoreTrendLevel.ONLY_DOWN ? score.hardScore() : Long.MAX_VALUE,
                trendLevels[1] == InitializingScoreTrendLevel.ONLY_DOWN ? score.mediumScore() : Long.MAX_VALUE,
                trendLevels[2] == InitializingScoreTrendLevel.ONLY_DOWN ? score.softScore() : Long.MAX_VALUE);
    }

    @Override
    public HardMediumSoftLongScore buildPessimisticBound(InitializingScoreTrend initializingScoreTrend,
            HardMediumSoftLongScore score) {
        InitializingScoreTrendLevel[] trendLevels = initializingScoreTrend.trendLevels();
        return HardMediumSoftLongScore.ofUninitialized(0,
                trendLevels[0] == InitializingScoreTrendLevel.ONLY_UP ? score.hardScore() : Long.MIN_VALUE,
                trendLevels[1] == InitializingScoreTrendLevel.ONLY_UP ? score.mediumScore() : Long.MIN_VALUE,
                trendLevels[2] == InitializingScoreTrendLevel.ONLY_UP ? score.softScore() : Long.MIN_VALUE);
    }

    @Override
    public HardMediumSoftLongScore divideBySanitizedDivisor(HardMediumSoftLongScore dividend,
            HardMediumSoftLongScore divisor) {
        int dividendInitScore = dividend.initScore();
        int divisorInitScore = sanitize(divisor.initScore());
        long dividendHardScore = dividend.hardScore();
        long divisorHardScore = sanitize(divisor.hardScore());
        long dividendMediumScore = dividend.mediumScore();
        long divisorMediumScore = sanitize(divisor.mediumScore());
        long dividendSoftScore = dividend.softScore();
        long divisorSoftScore = sanitize(divisor.softScore());
        return fromLevelNumbers(
                divide(dividendInitScore, divisorInitScore),
                new Number[] {
                        divide(dividendHardScore, divisorHardScore),
                        divide(dividendMediumScore, divisorMediumScore),
                        divide(dividendSoftScore, divisorSoftScore)
                });
    }

    @Override
    public Class<?> getNumericType() {
        return long.class;
    }
}
