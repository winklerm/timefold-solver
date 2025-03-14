package ai.timefold.solver.core.impl.bavet.quad.joiner;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.api.score.stream.quad.QuadJoiner;
import ai.timefold.solver.core.impl.bavet.common.joiner.AbstractJoiner;
import ai.timefold.solver.core.impl.bavet.common.joiner.JoinerType;

import org.jspecify.annotations.NonNull;

public final class DefaultQuadJoiner<A, B, C, D> extends AbstractJoiner<D> implements QuadJoiner<A, B, C, D> {

    private static final DefaultQuadJoiner NONE =
            new DefaultQuadJoiner(new TriFunction[0], new JoinerType[0], new Function[0]);

    private final TriFunction<A, B, C, ?>[] leftMappings;

    public <Property_> DefaultQuadJoiner(TriFunction<A, B, C, Property_> leftMapping, JoinerType joinerType,
            Function<D, Property_> rightMapping) {
        super(rightMapping, joinerType);
        this.leftMappings = new TriFunction[] { leftMapping };
    }

    private <Property_> DefaultQuadJoiner(TriFunction<A, B, C, Property_>[] leftMappings, JoinerType[] joinerTypes,
            Function<D, Property_>[] rightMappings) {
        super(rightMappings, joinerTypes);
        this.leftMappings = leftMappings;
    }

    public static <A, B, C, D> DefaultQuadJoiner<A, B, C, D> merge(List<DefaultQuadJoiner<A, B, C, D>> joinerList) {
        if (joinerList.size() == 1) {
            return joinerList.get(0);
        }
        return joinerList.stream().reduce(NONE, DefaultQuadJoiner::and);
    }

    @Override
    public @NonNull DefaultQuadJoiner<A, B, C, D> and(@NonNull QuadJoiner<A, B, C, D> otherJoiner) {
        DefaultQuadJoiner<A, B, C, D> castJoiner = (DefaultQuadJoiner<A, B, C, D>) otherJoiner;
        int joinerCount = getJoinerCount();
        int castJoinerCount = castJoiner.getJoinerCount();
        int newJoinerCount = joinerCount + castJoinerCount;
        JoinerType[] newJoinerTypes = Arrays.copyOf(this.joinerTypes, newJoinerCount);
        TriFunction[] newLeftMappings = Arrays.copyOf(this.leftMappings, newJoinerCount);
        Function[] newRightMappings = Arrays.copyOf(this.rightMappings, newJoinerCount);
        for (int i = 0; i < castJoiner.getJoinerCount(); i++) {
            int newJoinerIndex = i + joinerCount;
            newJoinerTypes[newJoinerIndex] = castJoiner.getJoinerType(i);
            newLeftMappings[newJoinerIndex] = castJoiner.getLeftMapping(i);
            newRightMappings[newJoinerIndex] = castJoiner.getRightMapping(i);
        }
        return new DefaultQuadJoiner<>(newLeftMappings, newJoinerTypes, newRightMappings);
    }

    public TriFunction<A, B, C, Object> getLeftMapping(int index) {
        return (TriFunction<A, B, C, Object>) leftMappings[index];
    }

    public boolean matches(A a, B b, C c, D d) {
        int joinerCount = getJoinerCount();
        for (int i = 0; i < joinerCount; i++) {
            JoinerType joinerType = getJoinerType(i);
            Object leftMapping = getLeftMapping(i).apply(a, b, c);
            Object rightMapping = getRightMapping(i).apply(d);
            if (!joinerType.matches(leftMapping, rightMapping)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof DefaultQuadJoiner<?, ?, ?, ?> other) {
            return Arrays.equals(joinerTypes, other.joinerTypes)
                    && Arrays.equals(leftMappings, other.leftMappings)
                    && Arrays.equals(rightMappings, other.rightMappings);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(Arrays.hashCode(joinerTypes), Arrays.hashCode(leftMappings), Arrays.hashCode(rightMappings));
    }

}
