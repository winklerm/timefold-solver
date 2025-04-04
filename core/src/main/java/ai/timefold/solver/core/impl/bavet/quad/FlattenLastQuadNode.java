package ai.timefold.solver.core.impl.bavet.quad;

import java.util.function.Function;

import ai.timefold.solver.core.impl.bavet.common.AbstractFlattenLastNode;
import ai.timefold.solver.core.impl.bavet.common.tuple.QuadTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;

public final class FlattenLastQuadNode<A, B, C, D, NewD>
        extends AbstractFlattenLastNode<QuadTuple<A, B, C, D>, QuadTuple<A, B, C, NewD>, D, NewD> {

    private final int outputStoreSize;

    public FlattenLastQuadNode(int flattenLastStoreIndex, Function<D, Iterable<NewD>> mappingFunction,
            TupleLifecycle<QuadTuple<A, B, C, NewD>> nextNodesTupleLifecycle, int outputStoreSize) {
        super(flattenLastStoreIndex, mappingFunction, nextNodesTupleLifecycle);
        this.outputStoreSize = outputStoreSize;
    }

    @Override
    protected QuadTuple<A, B, C, NewD> createTuple(QuadTuple<A, B, C, D> originalTuple, NewD newD) {
        return new QuadTuple<>(originalTuple.factA, originalTuple.factB, originalTuple.factC, newD,
                outputStoreSize);
    }

    @Override
    protected D getEffectiveFactIn(QuadTuple<A, B, C, D> tuple) {
        return tuple.factD;
    }

    @Override
    protected NewD getEffectiveFactOut(QuadTuple<A, B, C, NewD> outTuple) {
        return outTuple.factD;
    }
}
