package ai.timefold.solver.core.impl.solver.termination;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import ai.timefold.solver.core.api.score.buildin.bendable.BendableScore;
import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.buildin.hardsoftbigdecimal.HardSoftBigDecimalScore;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.buildin.simplebigdecimal.SimpleBigDecimalScore;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.score.buildin.SimpleScoreDefinition;
import ai.timefold.solver.core.impl.score.definition.ScoreDefinition;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.testdata.domain.TestdataSolution;

import org.junit.jupiter.api.Test;

class BestScoreTerminationTest {

    @Test
    void solveTermination() {
        ScoreDefinition<?> scoreDefinition = mock(ScoreDefinition.class);
        when(scoreDefinition.getLevelsSize()).thenReturn(1);
        SolverTermination<TestdataSolution> termination =
                new BestScoreTermination<>(scoreDefinition, SimpleScore.of(-1000), new double[] {});
        SolverScope<TestdataSolution> solverScope = mock(SolverScope.class);
        when(solverScope.getScoreDefinition()).thenReturn(new SimpleScoreDefinition());
        when(solverScope.isBestSolutionInitialized()).thenReturn(true);
        when(solverScope.getStartingInitializedScore()).thenReturn(SimpleScore.of(-1100));

        when(solverScope.getBestScore()).thenReturn(SimpleScore.of(-1100));
        assertThat(termination.isSolverTerminated(solverScope)).isFalse();
        assertThat(termination.calculateSolverTimeGradient(solverScope)).isEqualTo(0.0, offset(0.0));
        when(solverScope.getBestScore()).thenReturn(SimpleScore.of(-1100));
        assertThat(termination.isSolverTerminated(solverScope)).isFalse();
        assertThat(termination.calculateSolverTimeGradient(solverScope)).isEqualTo(0.0, offset(0.0));
        when(solverScope.getBestScore()).thenReturn(SimpleScore.of(-1040));
        assertThat(termination.isSolverTerminated(solverScope)).isFalse();
        assertThat(termination.calculateSolverTimeGradient(solverScope)).isEqualTo(0.6, offset(0.0));
        when(solverScope.getBestScore()).thenReturn(SimpleScore.of(-1040));
        assertThat(termination.isSolverTerminated(solverScope)).isFalse();
        assertThat(termination.calculateSolverTimeGradient(solverScope)).isEqualTo(0.6, offset(0.0));
        when(solverScope.getBestScore()).thenReturn(SimpleScore.of(-1000));
        assertThat(termination.isSolverTerminated(solverScope)).isTrue();
        assertThat(termination.calculateSolverTimeGradient(solverScope)).isEqualTo(1.0, offset(0.0));
        when(solverScope.getBestScore()).thenReturn(SimpleScore.of(-900));
        assertThat(termination.isSolverTerminated(solverScope)).isTrue();
        assertThat(termination.calculateSolverTimeGradient(solverScope)).isEqualTo(1.0, offset(0.0));
    }

    @Test
    void phaseTermination() {
        ScoreDefinition<?> scoreDefinition = mock(ScoreDefinition.class);
        when(scoreDefinition.getLevelsSize()).thenReturn(1);
        PhaseTermination<TestdataSolution> termination =
                new BestScoreTermination<>(scoreDefinition, SimpleScore.of(-1000), new double[] {});
        AbstractPhaseScope<TestdataSolution> phaseScope = mock(AbstractPhaseScope.class);
        when(phaseScope.isBestSolutionInitialized()).thenReturn(true);
        when(phaseScope.getStartingScore()).thenReturn(SimpleScore.of(-1100));

        when(phaseScope.getBestScore()).thenReturn(SimpleScore.of(-1100));
        assertThat(termination.isPhaseTerminated(phaseScope)).isFalse();
        assertThat(termination.calculatePhaseTimeGradient(phaseScope)).isEqualTo(0.0, offset(0.0));
        when(phaseScope.getBestScore()).thenReturn(SimpleScore.of(-1100));
        assertThat(termination.isPhaseTerminated(phaseScope)).isFalse();
        assertThat(termination.calculatePhaseTimeGradient(phaseScope)).isEqualTo(0.0, offset(0.0));
        when(phaseScope.getBestScore()).thenReturn(SimpleScore.of(-1040));
        assertThat(termination.isPhaseTerminated(phaseScope)).isFalse();
        assertThat(termination.calculatePhaseTimeGradient(phaseScope)).isEqualTo(0.6, offset(0.0));
        when(phaseScope.getBestScore()).thenReturn(SimpleScore.of(-1040));
        assertThat(termination.isPhaseTerminated(phaseScope)).isFalse();
        assertThat(termination.calculatePhaseTimeGradient(phaseScope)).isEqualTo(0.6, offset(0.0));
        when(phaseScope.getBestScore()).thenReturn(SimpleScore.of(-1000));
        assertThat(termination.isPhaseTerminated(phaseScope)).isTrue();
        assertThat(termination.calculatePhaseTimeGradient(phaseScope)).isEqualTo(1.0, offset(0.0));
        when(phaseScope.getBestScore()).thenReturn(SimpleScore.of(-900));
        assertThat(termination.isPhaseTerminated(phaseScope)).isTrue();
        assertThat(termination.calculatePhaseTimeGradient(phaseScope)).isEqualTo(1.0, offset(0.0));
    }

    @Test
    void calculateTimeGradientSimpleScore() {
        ScoreDefinition scoreDefinition = mock(ScoreDefinition.class);
        when(scoreDefinition.getLevelsSize()).thenReturn(1);
        BestScoreTermination<TestdataSolution> termination = new BestScoreTermination<>(scoreDefinition,
                SimpleScore.of(10), new double[] {});

        assertThat(termination.calculateTimeGradient(
                SimpleScore.of(0), SimpleScore.of(10), SimpleScore.of(0))).isEqualTo(0.0, offset(0.0));
        assertThat(termination.calculateTimeGradient(
                SimpleScore.of(0), SimpleScore.of(10), SimpleScore.of(6))).isEqualTo(0.6, offset(0.0));
        assertThat(termination.calculateTimeGradient(
                SimpleScore.of(0), SimpleScore.of(10), SimpleScore.of(10))).isEqualTo(1.0, offset(0.0));
        assertThat(termination.calculateTimeGradient(
                SimpleScore.of(0), SimpleScore.of(10), SimpleScore.of(11))).isEqualTo(1.0, offset(0.0));

        assertThat(termination.calculateTimeGradient(
                SimpleScore.of(-10), SimpleScore.of(30), SimpleScore.of(0))).isEqualTo(0.25, offset(0.0));
        assertThat(termination.calculateTimeGradient(
                SimpleScore.of(10), SimpleScore.of(40), SimpleScore.of(20))).isEqualTo(0.33333, offset(0.00001));
    }

    @Test
    void calculateTimeGradientSimpleBigDecimalScore() {
        ScoreDefinition scoreDefinition = mock(ScoreDefinition.class);
        when(scoreDefinition.getLevelsSize()).thenReturn(1);
        BestScoreTermination<TestdataSolution> termination = new BestScoreTermination<>(scoreDefinition,
                SimpleBigDecimalScore.of(new BigDecimal("10.00")), new double[] {});

        assertThat(termination.calculateTimeGradient(
                SimpleBigDecimalScore.of(new BigDecimal("0.00")), SimpleBigDecimalScore.of(new BigDecimal("10.00")),
                SimpleBigDecimalScore.of(new BigDecimal("0.00")))).isEqualTo(0.0, offset(0.0));
        assertThat(termination.calculateTimeGradient(
                SimpleBigDecimalScore.of(new BigDecimal("0.00")), SimpleBigDecimalScore.of(new BigDecimal("10.00")),
                SimpleBigDecimalScore.of(new BigDecimal("6.00")))).isEqualTo(0.6, offset(0.0));
        assertThat(termination.calculateTimeGradient(
                SimpleBigDecimalScore.of(new BigDecimal("0.00")), SimpleBigDecimalScore.of(new BigDecimal("10.00")),
                SimpleBigDecimalScore.of(new BigDecimal("10.00")))).isEqualTo(1.0, offset(0.0));
        assertThat(termination.calculateTimeGradient(
                SimpleBigDecimalScore.of(new BigDecimal("0.00")), SimpleBigDecimalScore.of(new BigDecimal("10.00")),
                SimpleBigDecimalScore.of(new BigDecimal("11.00")))).isEqualTo(1.0, offset(0.0));
        assertThat(termination.calculateTimeGradient(
                SimpleBigDecimalScore.of(new BigDecimal("-10.00")), SimpleBigDecimalScore.of(new BigDecimal("30.00")),
                SimpleBigDecimalScore.of(new BigDecimal("0.00")))).isEqualTo(0.25, offset(0.0));
        assertThat(termination.calculateTimeGradient(
                SimpleBigDecimalScore.of(new BigDecimal("10.00")), SimpleBigDecimalScore.of(new BigDecimal("40.00")),
                SimpleBigDecimalScore.of(new BigDecimal("20.00")))).isEqualTo(0.33333, offset(0.00001));
    }

    @Test
    void calculateTimeGradientHardSoftScore() {
        ScoreDefinition scoreDefinition = mock(ScoreDefinition.class);
        when(scoreDefinition.getLevelsSize()).thenReturn(2);
        BestScoreTermination termination = new BestScoreTermination(scoreDefinition,
                HardSoftScore.of(-10, -300), new double[] { 0.75 });

        // Normal cases
        // Smack in the middle
        assertThat(termination.calculateTimeGradient(
                HardSoftScore.of(-20, -400), HardSoftScore.of(-10, -300),
                HardSoftScore.of(-14, -340))).isEqualTo(0.6, offset(0.0));
        // No hard broken, total soft broken
        assertThat(termination.calculateTimeGradient(
                HardSoftScore.of(-20, -400), HardSoftScore.of(-10, -300),
                HardSoftScore.of(-10, -400))).isEqualTo(0.75, offset(0.0));
        // Total hard broken, no soft broken
        assertThat(termination.calculateTimeGradient(
                HardSoftScore.of(-20, -400), HardSoftScore.of(-10, -300),
                HardSoftScore.of(-20, -300))).isEqualTo(0.25, offset(0.0));
        // No hard broken, more than total soft broken
        assertThat(termination.calculateTimeGradient(
                HardSoftScore.of(-20, -400), HardSoftScore.of(-10, -300),
                HardSoftScore.of(-10, -900))).isEqualTo(0.75, offset(0.0));
        // More than total hard broken, no soft broken
        assertThat(termination.calculateTimeGradient(
                HardSoftScore.of(-20, -400), HardSoftScore.of(-10, -300),
                HardSoftScore.of(-90, -300))).isEqualTo(0.0, offset(0.0));

        // Perfect min/max cases
        assertThat(termination.calculateTimeGradient(
                HardSoftScore.of(-10, -300), HardSoftScore.of(-10, -300),
                HardSoftScore.of(-10, -300))).isEqualTo(1.0, offset(0.0));
        assertThat(termination.calculateTimeGradient(
                HardSoftScore.of(-20, -400), HardSoftScore.of(-10, -300),
                HardSoftScore.of(-20, -400))).isEqualTo(0.0, offset(0.0));
        assertThat(termination.calculateTimeGradient(
                HardSoftScore.of(-20, -400), HardSoftScore.of(-10, -300),
                HardSoftScore.of(-10, -300))).isEqualTo(1.0, offset(0.0));

        // Hard total delta is 0
        assertThat(termination.calculateTimeGradient(
                HardSoftScore.of(-10, -400), HardSoftScore.of(-10, -300),
                HardSoftScore.of(-10, -340))).isEqualTo(0.75 + (0.6 * 0.25), offset(0.0));
        assertThat(termination.calculateTimeGradient(
                HardSoftScore.of(-10, -400), HardSoftScore.of(-10, -300),
                HardSoftScore.of(-20, -340))).isEqualTo(0.0, offset(0.0));
        assertThat(termination.calculateTimeGradient(
                HardSoftScore.of(-10, -400), HardSoftScore.of(-10, -300),
                HardSoftScore.of(-0, -340))).isEqualTo(1.0, offset(0.0));

        // Soft total delta is 0
        assertThat(termination.calculateTimeGradient(
                HardSoftScore.of(-20, -300), HardSoftScore.of(-10, -300),
                HardSoftScore.of(-14, -300))).isEqualTo((0.6 * 0.75) + 0.25, offset(0.0));
        assertThat(termination.calculateTimeGradient(
                HardSoftScore.of(-20, -300), HardSoftScore.of(-10, -300),
                HardSoftScore.of(-14, -400))).isEqualTo(0.6 * 0.75, offset(0.0));
        assertThat(termination.calculateTimeGradient(
                HardSoftScore.of(-20, -300), HardSoftScore.of(-10, -300),
                HardSoftScore.of(-14, -0))).isEqualTo((0.6 * 0.75) + 0.25, offset(0.0));
    }

    @Test
    void calculateTimeGradientHardSoftBigDecimalScore() {
        ScoreDefinition scoreDefinition = mock(ScoreDefinition.class);
        when(scoreDefinition.getLevelsSize()).thenReturn(2);
        BestScoreTermination termination = new BestScoreTermination(scoreDefinition,
                HardSoftBigDecimalScore.of(new BigDecimal("10.00"), new BigDecimal("10.00")), new double[] { 0.75 });

        // hard == soft
        assertThat(termination.calculateTimeGradient(
                HardSoftBigDecimalScore.of(new BigDecimal("0.00"), new BigDecimal("0.00")),
                HardSoftBigDecimalScore.of(new BigDecimal("10.00"), new BigDecimal("10.00")),
                HardSoftBigDecimalScore.of(new BigDecimal("0.00"), new BigDecimal("0.00")))).isEqualTo(0.0, offset(0.0));
        assertThat(termination.calculateTimeGradient(
                HardSoftBigDecimalScore.of(new BigDecimal("0.00"), new BigDecimal("0.00")),
                HardSoftBigDecimalScore.of(new BigDecimal("10.00"), new BigDecimal("10.00")),
                HardSoftBigDecimalScore.of(new BigDecimal("6.00"), new BigDecimal("6.00")))).isEqualTo(0.6, offset(0.0));
        assertThat(termination.calculateTimeGradient(
                HardSoftBigDecimalScore.of(new BigDecimal("0.00"), new BigDecimal("0.00")),
                HardSoftBigDecimalScore.of(new BigDecimal("10.00"), new BigDecimal("10.00")),
                HardSoftBigDecimalScore.of(new BigDecimal("10.00"), new BigDecimal("10.00")))).isEqualTo(1.0, offset(0.0));
        assertThat(termination.calculateTimeGradient(
                HardSoftBigDecimalScore.of(new BigDecimal("0.00"), new BigDecimal("0.00")),
                HardSoftBigDecimalScore.of(new BigDecimal("10.00"), new BigDecimal("10.00")),
                HardSoftBigDecimalScore.of(new BigDecimal("11.00"), new BigDecimal("11.00")))).isEqualTo(1.0, offset(0.0));
        assertThat(termination.calculateTimeGradient(
                HardSoftBigDecimalScore.of(new BigDecimal("-10.00"), new BigDecimal("-10.00")),
                HardSoftBigDecimalScore.of(new BigDecimal("30.00"), new BigDecimal("30.00")),
                HardSoftBigDecimalScore.of(new BigDecimal("0.00"), new BigDecimal("0.00")))).isEqualTo(0.25, offset(0.0));
        assertThat(termination.calculateTimeGradient(
                HardSoftBigDecimalScore.of(new BigDecimal("10.00"), new BigDecimal("10.00")),
                HardSoftBigDecimalScore.of(new BigDecimal("40.00"), new BigDecimal("40.00")),
                HardSoftBigDecimalScore.of(new BigDecimal("20.00"), new BigDecimal("20.00")))).isEqualTo(0.33333,
                        offset(0.00001));
    }

    @Test
    void calculateTimeGradientBendableScoreHS() {
        ScoreDefinition scoreDefinition = mock(ScoreDefinition.class);
        when(scoreDefinition.getLevelsSize()).thenReturn(2);
        BestScoreTermination termination = new BestScoreTermination(scoreDefinition,
                BendableScore.of(new int[] { -10 }, new int[] { -300 }), new double[] { 0.75 });

        // Normal cases
        // Smack in the middle
        assertThat(termination.calculateTimeGradient(
                BendableScore.of(new int[] { -20 }, new int[] { -400 }),
                BendableScore.of(new int[] { -10 }, new int[] { -300 }),
                BendableScore.of(new int[] { -14 }, new int[] { -340 }))).isEqualTo(0.6, offset(0.0));
        // No hard broken, total soft broken
        assertThat(termination.calculateTimeGradient(
                BendableScore.of(new int[] { -20 }, new int[] { -400 }),
                BendableScore.of(new int[] { -10 }, new int[] { -300 }),
                BendableScore.of(new int[] { -10 }, new int[] { -400 }))).isEqualTo(0.75, offset(0.0));
        // Total hard broken, no soft broken
        assertThat(termination.calculateTimeGradient(
                BendableScore.of(new int[] { -20 }, new int[] { -400 }),
                BendableScore.of(new int[] { -10 }, new int[] { -300 }),
                BendableScore.of(new int[] { -20 }, new int[] { -300 }))).isEqualTo(0.25, offset(0.0));
        // No hard broken, more than total soft broken
        assertThat(termination.calculateTimeGradient(
                BendableScore.of(new int[] { -20 }, new int[] { -400 }),
                BendableScore.of(new int[] { -10 }, new int[] { -300 }),
                BendableScore.of(new int[] { -10 }, new int[] { -900 }))).isEqualTo(0.75, offset(0.0));
        // More than total hard broken, no soft broken
        assertThat(termination.calculateTimeGradient(
                BendableScore.of(new int[] { -20 }, new int[] { -400 }),
                BendableScore.of(new int[] { -10 }, new int[] { -300 }),
                BendableScore.of(new int[] { -90 }, new int[] { -300 }))).isEqualTo(0.0, offset(0.0));

        // Perfect min/max cases
        assertThat(termination.calculateTimeGradient(
                BendableScore.of(new int[] { -10 }, new int[] { -300 }),
                BendableScore.of(new int[] { -10 }, new int[] { -300 }),
                BendableScore.of(new int[] { -10 }, new int[] { -300 }))).isEqualTo(1.0, offset(0.0));
        assertThat(termination.calculateTimeGradient(
                BendableScore.of(new int[] { -20 }, new int[] { -400 }),
                BendableScore.of(new int[] { -10 }, new int[] { -300 }),
                BendableScore.of(new int[] { -20 }, new int[] { -400 }))).isEqualTo(0.0, offset(0.0));
        assertThat(termination.calculateTimeGradient(
                BendableScore.of(new int[] { -20 }, new int[] { -400 }),
                BendableScore.of(new int[] { -10 }, new int[] { -300 }),
                BendableScore.of(new int[] { -10 }, new int[] { -300 }))).isEqualTo(1.0, offset(0.0));

        // Hard total delta is 0
        assertThat(termination.calculateTimeGradient(
                BendableScore.of(new int[] { -10 }, new int[] { -400 }),
                BendableScore.of(new int[] { -10 }, new int[] { -300 }),
                BendableScore.of(new int[] { -10 }, new int[] { -340 }))).isEqualTo(0.75 + (0.6 * 0.25), offset(0.0));
        assertThat(termination.calculateTimeGradient(
                BendableScore.of(new int[] { -10 }, new int[] { -400 }),
                BendableScore.of(new int[] { -10 }, new int[] { -300 }),
                BendableScore.of(new int[] { -20 }, new int[] { -340 }))).isEqualTo(0.0, offset(0.0));
        assertThat(termination.calculateTimeGradient(
                BendableScore.of(new int[] { -10 }, new int[] { -400 }),
                BendableScore.of(new int[] { -10 }, new int[] { -300 }),
                BendableScore.of(new int[] { -0 }, new int[] { -340 }))).isEqualTo(1.0, offset(0.0));

        // Soft total delta is 0
        assertThat(termination.calculateTimeGradient(
                BendableScore.of(new int[] { -20 }, new int[] { -300 }),
                BendableScore.of(new int[] { -10 }, new int[] { -300 }),
                BendableScore.of(new int[] { -14 }, new int[] { -300 }))).isEqualTo((0.6 * 0.75) + 0.25, offset(0.0));
        assertThat(termination.calculateTimeGradient(
                BendableScore.of(new int[] { -20 }, new int[] { -300 }),
                BendableScore.of(new int[] { -10 }, new int[] { -300 }),
                BendableScore.of(new int[] { -14 }, new int[] { -400 }))).isEqualTo(0.6 * 0.75, offset(0.0));
        assertThat(termination.calculateTimeGradient(
                BendableScore.of(new int[] { -20 }, new int[] { -300 }),
                BendableScore.of(new int[] { -10 }, new int[] { -300 }),
                BendableScore.of(new int[] { -14 }, new int[] { -0 }))).isEqualTo((0.6 * 0.75) + 0.25, offset(0.0));
    }

    @Test
    void calculateTimeGradientBendableScoreHHSSS() {
        ScoreDefinition scoreDefinition = mock(ScoreDefinition.class);
        when(scoreDefinition.getLevelsSize()).thenReturn(5);
        BestScoreTermination termination = new BestScoreTermination(scoreDefinition,
                BendableScore.of(new int[] { 0, 0 }, new int[] { 0, 0, -10 }),
                new double[] { 0.75, 0.75, 0.75, 0.75 });

        // Normal cases
        // Smack in the middle
        assertThat(termination.calculateTimeGradient(
                BendableScore.of(new int[] { -10, -100 }, new int[] { -50, -60, -70 }),
                BendableScore.of(new int[] { 0, 0 }, new int[] { 0, 0, -10 }),
                BendableScore.of(new int[] { -4, -40 }, new int[] { -50, -60, -70 }))).isEqualTo(0.6 * 0.75 + 0.6 * 0.25 * 0.75,
                        offset(0.0));
    }

}
