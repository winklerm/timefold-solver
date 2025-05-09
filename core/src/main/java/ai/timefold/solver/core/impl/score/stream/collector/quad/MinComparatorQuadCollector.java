package ai.timefold.solver.core.impl.score.stream.collector.quad;

import java.util.Comparator;
import java.util.Objects;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.impl.score.stream.collector.MinMaxUndoableActionable;

import org.jspecify.annotations.NonNull;

final class MinComparatorQuadCollector<A, B, C, D, Result_>
        extends UndoableActionableQuadCollector<A, B, C, D, Result_, Result_, MinMaxUndoableActionable<Result_, Result_>> {
    private final Comparator<? super Result_> comparator;

    MinComparatorQuadCollector(QuadFunction<? super A, ? super B, ? super C, ? super D, ? extends Result_> mapper,
            Comparator<? super Result_> comparator) {
        super(mapper);
        this.comparator = comparator;
    }

    @Override
    public @NonNull Supplier<MinMaxUndoableActionable<Result_, Result_>> supplier() {
        return () -> MinMaxUndoableActionable.minCalculator(comparator);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (object == null || getClass() != object.getClass())
            return false;
        if (!super.equals(object))
            return false;
        MinComparatorQuadCollector<?, ?, ?, ?, ?> that = (MinComparatorQuadCollector<?, ?, ?, ?, ?>) object;
        return Objects.equals(comparator, that.comparator);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), comparator);
    }
}
