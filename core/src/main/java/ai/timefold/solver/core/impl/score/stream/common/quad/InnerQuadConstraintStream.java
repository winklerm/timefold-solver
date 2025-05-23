package ai.timefold.solver.core.impl.score.stream.common.quad;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;

import ai.timefold.solver.core.api.function.PentaFunction;
import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.api.function.ToIntQuadFunction;
import ai.timefold.solver.core.api.function.ToLongQuadFunction;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.DefaultConstraintJustification;
import ai.timefold.solver.core.api.score.stream.penta.PentaJoiner;
import ai.timefold.solver.core.api.score.stream.quad.QuadConstraintBuilder;
import ai.timefold.solver.core.api.score.stream.quad.QuadConstraintStream;
import ai.timefold.solver.core.impl.score.stream.common.RetrievalSemantics;
import ai.timefold.solver.core.impl.score.stream.common.ScoreImpactType;
import ai.timefold.solver.core.impl.util.ConstantLambdaUtils;

import org.jspecify.annotations.NonNull;

public interface InnerQuadConstraintStream<A, B, C, D> extends QuadConstraintStream<A, B, C, D> {

    static <A, B, C, D> PentaFunction<A, B, C, D, Score<?>, DefaultConstraintJustification>
            createDefaultJustificationMapping() {
        return (a, b, c, d, score) -> DefaultConstraintJustification.of(score, a, b, c, d);
    }

    static <A, B, C, D> QuadFunction<A, B, C, D, Collection<?>> createDefaultIndictedObjectsMapping() {
        return Arrays::asList;
    }

    RetrievalSemantics getRetrievalSemantics();

    /**
     * This method will return true if the constraint stream is guaranteed to only produce distinct tuples.
     * See {@link #distinct()} for details.
     *
     * @return true if the guarantee of distinct tuples is provided
     */
    boolean guaranteesDistinct();

    @Override
    default @NonNull <E> QuadConstraintStream<A, B, C, D> ifExists(@NonNull Class<E> otherClass,
            @NonNull PentaJoiner<A, B, C, D, E> @NonNull... joiners) {
        if (getRetrievalSemantics() == RetrievalSemantics.STANDARD) {
            return ifExists(getConstraintFactory().forEach(otherClass), joiners);
        } else {
            // Calls fromUnfiltered() for backward compatibility only
            return ifExists(getConstraintFactory().fromUnfiltered(otherClass), joiners);
        }
    }

    @Override
    default @NonNull <E> QuadConstraintStream<A, B, C, D> ifExistsIncludingUnassigned(@NonNull Class<E> otherClass,
            @NonNull PentaJoiner<A, B, C, D, E> @NonNull... joiners) {
        if (getRetrievalSemantics() == RetrievalSemantics.STANDARD) {
            return ifExists(getConstraintFactory().forEachIncludingUnassigned(otherClass), joiners);
        } else {
            return ifExists(getConstraintFactory().fromUnfiltered(otherClass), joiners);
        }
    }

    @Override
    default @NonNull <E> QuadConstraintStream<A, B, C, D> ifNotExists(@NonNull Class<E> otherClass,
            @NonNull PentaJoiner<A, B, C, D, E> @NonNull... joiners) {
        if (getRetrievalSemantics() == RetrievalSemantics.STANDARD) {
            return ifNotExists(getConstraintFactory().forEach(otherClass), joiners);
        } else {
            // Calls fromUnfiltered() for backward compatibility only
            return ifNotExists(getConstraintFactory().fromUnfiltered(otherClass), joiners);
        }
    }

    @Override
    default @NonNull <E> QuadConstraintStream<A, B, C, D> ifNotExistsIncludingUnassigned(@NonNull Class<E> otherClass,
            @NonNull PentaJoiner<A, B, C, D, E> @NonNull... joiners) {
        if (getRetrievalSemantics() == RetrievalSemantics.STANDARD) {
            return ifNotExists(getConstraintFactory().forEachIncludingUnassigned(otherClass), joiners);
        } else {
            return ifNotExists(getConstraintFactory().fromUnfiltered(otherClass), joiners);
        }
    }

    @Override
    default @NonNull QuadConstraintStream<A, B, C, D> distinct() {
        if (guaranteesDistinct()) {
            return this;
        } else {
            return groupBy(ConstantLambdaUtils.quadPickFirst(),
                    ConstantLambdaUtils.quadPickSecond(),
                    ConstantLambdaUtils.quadPickThird(),
                    ConstantLambdaUtils.quadPickFourth());
        }
    }

    @Override
    default @NonNull <Score_ extends Score<Score_>> QuadConstraintBuilder<A, B, C, D, Score_> penalize(
            @NonNull Score_ constraintWeight,
            @NonNull ToIntQuadFunction<A, B, C, D> matchWeigher) {
        return innerImpact(constraintWeight, matchWeigher, ScoreImpactType.PENALTY);
    }

    @Override
    default @NonNull <Score_ extends Score<Score_>> QuadConstraintBuilder<A, B, C, D, Score_> penalizeLong(
            @NonNull Score_ constraintWeight,
            @NonNull ToLongQuadFunction<A, B, C, D> matchWeigher) {
        return innerImpact(constraintWeight, matchWeigher, ScoreImpactType.PENALTY);
    }

    @Override
    default @NonNull <Score_ extends Score<Score_>> QuadConstraintBuilder<A, B, C, D, Score_> penalizeBigDecimal(
            @NonNull Score_ constraintWeight,
            @NonNull QuadFunction<A, B, C, D, BigDecimal> matchWeigher) {
        return innerImpact(constraintWeight, matchWeigher, ScoreImpactType.PENALTY);
    }

    @Override
    default QuadConstraintBuilder<A, B, C, D, ?> penalizeConfigurable(ToIntQuadFunction<A, B, C, D> matchWeigher) {
        return innerImpact(null, matchWeigher, ScoreImpactType.PENALTY);
    }

    @Override
    default QuadConstraintBuilder<A, B, C, D, ?> penalizeConfigurableLong(ToLongQuadFunction<A, B, C, D> matchWeigher) {
        return innerImpact(null, matchWeigher, ScoreImpactType.PENALTY);
    }

    @Override
    default QuadConstraintBuilder<A, B, C, D, ?>
            penalizeConfigurableBigDecimal(QuadFunction<A, B, C, D, BigDecimal> matchWeigher) {
        return innerImpact(null, matchWeigher, ScoreImpactType.PENALTY);
    }

    @Override
    default @NonNull <Score_ extends Score<Score_>> QuadConstraintBuilder<A, B, C, D, Score_> reward(
            @NonNull Score_ constraintWeight,
            @NonNull ToIntQuadFunction<A, B, C, D> matchWeigher) {
        return innerImpact(constraintWeight, matchWeigher, ScoreImpactType.REWARD);
    }

    @Override
    default @NonNull <Score_ extends Score<Score_>> QuadConstraintBuilder<A, B, C, D, Score_> rewardLong(
            @NonNull Score_ constraintWeight,
            @NonNull ToLongQuadFunction<A, B, C, D> matchWeigher) {
        return innerImpact(constraintWeight, matchWeigher, ScoreImpactType.REWARD);
    }

    @Override
    default @NonNull <Score_ extends Score<Score_>> QuadConstraintBuilder<A, B, C, D, Score_> rewardBigDecimal(
            @NonNull Score_ constraintWeight,
            @NonNull QuadFunction<A, B, C, D, BigDecimal> matchWeigher) {
        return innerImpact(constraintWeight, matchWeigher, ScoreImpactType.REWARD);
    }

    @Override
    default QuadConstraintBuilder<A, B, C, D, ?> rewardConfigurable(ToIntQuadFunction<A, B, C, D> matchWeigher) {
        return innerImpact(null, matchWeigher, ScoreImpactType.REWARD);
    }

    @Override
    default QuadConstraintBuilder<A, B, C, D, ?> rewardConfigurableLong(ToLongQuadFunction<A, B, C, D> matchWeigher) {
        return innerImpact(null, matchWeigher, ScoreImpactType.REWARD);
    }

    @Override
    default QuadConstraintBuilder<A, B, C, D, ?>
            rewardConfigurableBigDecimal(QuadFunction<A, B, C, D, BigDecimal> matchWeigher) {
        return innerImpact(null, matchWeigher, ScoreImpactType.REWARD);
    }

    @Override
    default @NonNull <Score_ extends Score<Score_>> QuadConstraintBuilder<A, B, C, D, Score_> impact(
            @NonNull Score_ constraintWeight,
            @NonNull ToIntQuadFunction<A, B, C, D> matchWeigher) {
        return innerImpact(constraintWeight, matchWeigher, ScoreImpactType.MIXED);
    }

    @Override
    default @NonNull <Score_ extends Score<Score_>> QuadConstraintBuilder<A, B, C, D, Score_> impactLong(
            @NonNull Score_ constraintWeight,
            @NonNull ToLongQuadFunction<A, B, C, D> matchWeigher) {
        return innerImpact(constraintWeight, matchWeigher, ScoreImpactType.MIXED);
    }

    @Override
    default @NonNull <Score_ extends Score<Score_>> QuadConstraintBuilder<A, B, C, D, Score_> impactBigDecimal(
            @NonNull Score_ constraintWeight,
            @NonNull QuadFunction<A, B, C, D, BigDecimal> matchWeigher) {
        return innerImpact(constraintWeight, matchWeigher, ScoreImpactType.MIXED);
    }

    @Override
    default QuadConstraintBuilder<A, B, C, D, ?> impactConfigurable(ToIntQuadFunction<A, B, C, D> matchWeigher) {
        return innerImpact(null, matchWeigher, ScoreImpactType.MIXED);
    }

    @Override
    default QuadConstraintBuilder<A, B, C, D, ?> impactConfigurableLong(ToLongQuadFunction<A, B, C, D> matchWeigher) {
        return innerImpact(null, matchWeigher, ScoreImpactType.MIXED);
    }

    @Override
    default QuadConstraintBuilder<A, B, C, D, ?>
            impactConfigurableBigDecimal(QuadFunction<A, B, C, D, BigDecimal> matchWeigher) {
        return innerImpact(null, matchWeigher, ScoreImpactType.MIXED);
    }

    <Score_ extends Score<Score_>> QuadConstraintBuilder<A, B, C, D, Score_> innerImpact(Score_ constraintWeight,
            ToIntQuadFunction<A, B, C, D> matchWeigher, ScoreImpactType scoreImpactType);

    <Score_ extends Score<Score_>> QuadConstraintBuilder<A, B, C, D, Score_> innerImpact(Score_ constraintWeight,
            ToLongQuadFunction<A, B, C, D> matchWeigher, ScoreImpactType scoreImpactType);

    <Score_ extends Score<Score_>> QuadConstraintBuilder<A, B, C, D, Score_> innerImpact(Score_ constraintWeight,
            QuadFunction<A, B, C, D, BigDecimal> matchWeigher, ScoreImpactType scoreImpactType);

    @Override
    default @NonNull Constraint penalize(@NonNull String constraintName, @NonNull Score<?> constraintWeight) {
        return penalize((Score) constraintWeight)
                .asConstraint(constraintName);
    }

    @Override
    default @NonNull Constraint penalize(@NonNull String constraintPackage, @NonNull String constraintName,
            @NonNull Score<?> constraintWeight) {
        return penalize((Score) constraintWeight)
                .asConstraint(constraintPackage, constraintName);
    }

    @Override
    default @NonNull Constraint penalizeConfigurable(@NonNull String constraintName) {
        return penalizeConfigurable()
                .asConstraint(constraintName);
    }

    @Override
    default @NonNull Constraint penalizeConfigurable(@NonNull String constraintPackage, @NonNull String constraintName) {
        return penalizeConfigurable()
                .asConstraint(constraintPackage, constraintName);
    }

    @Override
    default @NonNull Constraint reward(@NonNull String constraintName, @NonNull Score<?> constraintWeight) {
        return reward((Score) constraintWeight)
                .asConstraint(constraintName);
    }

    @Override
    default @NonNull Constraint reward(@NonNull String constraintPackage, @NonNull String constraintName,
            @NonNull Score<?> constraintWeight) {
        return reward((Score) constraintWeight)
                .asConstraint(constraintPackage, constraintName);
    }

    @Override
    default @NonNull Constraint rewardConfigurable(@NonNull String constraintName) {
        return rewardConfigurable()
                .asConstraint(constraintName);
    }

    @Override
    default @NonNull Constraint rewardConfigurable(@NonNull String constraintPackage, @NonNull String constraintName) {
        return penalizeConfigurable()
                .asConstraint(constraintPackage, constraintName);
    }

    @Override
    default @NonNull Constraint impact(@NonNull String constraintName, @NonNull Score<?> constraintWeight) {
        return impact((Score) constraintWeight)
                .asConstraint(constraintName);
    }

    @Override
    default @NonNull Constraint impact(@NonNull String constraintPackage, @NonNull String constraintName,
            @NonNull Score<?> constraintWeight) {
        return impact((Score) constraintWeight)
                .asConstraint(constraintPackage, constraintName);
    }

}
