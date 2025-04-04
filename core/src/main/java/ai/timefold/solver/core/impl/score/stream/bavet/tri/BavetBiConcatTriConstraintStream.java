package ai.timefold.solver.core.impl.score.stream.bavet.tri;

import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.bavet.common.AbstractConcatNode;
import ai.timefold.solver.core.impl.bavet.common.BavetAbstractConstraintStream;
import ai.timefold.solver.core.impl.bavet.common.tuple.TriTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.TupleLifecycle;
import ai.timefold.solver.core.impl.bavet.tri.ConcatBiTriNode;
import ai.timefold.solver.core.impl.bavet.tri.ConcatTriBiNode;
import ai.timefold.solver.core.impl.score.stream.bavet.BavetConstraintFactory;
import ai.timefold.solver.core.impl.score.stream.bavet.common.BavetConcatConstraintStream;
import ai.timefold.solver.core.impl.score.stream.bavet.common.ConstraintNodeBuildHelper;
import ai.timefold.solver.core.impl.score.stream.bavet.common.bridge.BavetForeBridgeBiConstraintStream;
import ai.timefold.solver.core.impl.score.stream.bavet.common.bridge.BavetForeBridgeTriConstraintStream;

public final class BavetBiConcatTriConstraintStream<Solution_, A, B, C>
        extends BavetAbstractTriConstraintStream<Solution_, A, B, C>
        implements BavetConcatConstraintStream<Solution_> {

    private final BavetAbstractConstraintStream<Solution_> leftParent;
    private final BavetAbstractConstraintStream<Solution_> rightParent;
    private final BiFunction<A, B, C> paddingFunction;
    private final ConcatNodeConstructor<A, B, C> nodeConstructor;

    public BavetBiConcatTriConstraintStream(BavetConstraintFactory<Solution_> constraintFactory,
            BavetForeBridgeBiConstraintStream<Solution_, A, B> leftParent,
            BavetForeBridgeTriConstraintStream<Solution_, A, B, C> rightParent,
            BiFunction<A, B, C> paddingFunction) {
        super(constraintFactory, leftParent.getRetrievalSemantics());
        this.leftParent = leftParent;
        this.rightParent = rightParent;
        this.paddingFunction = paddingFunction;
        this.nodeConstructor = ConcatBiTriNode::new;
    }

    public BavetBiConcatTriConstraintStream(BavetConstraintFactory<Solution_> constraintFactory,
            BavetForeBridgeTriConstraintStream<Solution_, A, B, C> leftParent,
            BavetForeBridgeBiConstraintStream<Solution_, A, B> rightParent,
            BiFunction<A, B, C> paddingFunction) {
        super(constraintFactory, leftParent.getRetrievalSemantics());
        this.leftParent = leftParent;
        this.rightParent = rightParent;
        this.paddingFunction = paddingFunction;
        this.nodeConstructor = ConcatTriBiNode::new;
    }

    @Override
    public boolean guaranteesDistinct() {
        /*
         * Since one of the two parents is increasing in cardinality,
         * it means its tuples must be distinct from the other parent's tuples.
         * Therefore, the guarantee can be given is both of the parents give it.
         */
        return leftParent.guaranteesDistinct() && rightParent.guaranteesDistinct();
    }

    // ************************************************************************
    // Node creation
    // ************************************************************************

    @Override
    public void collectActiveConstraintStreams(Set<BavetAbstractConstraintStream<Solution_>> constraintStreamSet) {
        leftParent.collectActiveConstraintStreams(constraintStreamSet);
        rightParent.collectActiveConstraintStreams(constraintStreamSet);
        constraintStreamSet.add(this);
    }

    @Override
    public <Score_ extends Score<Score_>> void buildNode(ConstraintNodeBuildHelper<Solution_, Score_> buildHelper) {
        TupleLifecycle<TriTuple<A, B, C>> downstream = buildHelper.getAggregatedTupleLifecycle(childStreamList);
        var leftCloneStoreIndex = buildHelper.reserveTupleStoreIndex(leftParent.getTupleSource());
        var rightCloneStoreIndex = buildHelper.reserveTupleStoreIndex(rightParent.getTupleSource());
        var outputStoreSize = buildHelper.extractTupleStoreSize(this);
        var node =
                nodeConstructor.apply(paddingFunction, downstream, leftCloneStoreIndex, rightCloneStoreIndex, outputStoreSize);
        buildHelper.addNode(node, this, leftParent, rightParent);
    }

    // ************************************************************************
    // Equality for node sharing
    // ************************************************************************

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        var other = (BavetBiConcatTriConstraintStream<?, ?, ?, ?>) o;
        /*
         * Bridge streams do not implement equality because their equals() would have to point back to this stream,
         * resulting in StackOverflowError.
         * Therefore we need to check bridge parents to see where this concat node comes from.
         */
        return Objects.equals(leftParent.getParent(), other.leftParent.getParent())
                && Objects.equals(rightParent.getParent(), other.rightParent.getParent());
    }

    @Override
    public int hashCode() {
        return Objects.hash(leftParent.getParent(), rightParent.getParent());
    }

    @Override
    public String toString() {
        return "BiConcat() with " + childStreamList.size() + " children";
    }

    // ************************************************************************
    // Getters/setters
    // ************************************************************************

    @Override
    public BavetAbstractConstraintStream<Solution_> getLeftParent() {
        return leftParent;
    }

    @Override
    public BavetAbstractConstraintStream<Solution_> getRightParent() {
        return rightParent;
    }

    private interface ConcatNodeConstructor<A, B, C> {

        AbstractConcatNode<?, ?, ?> apply(BiFunction<A, B, C> paddingFunction,
                TupleLifecycle<TriTuple<A, B, C>> nextNodesTupleLifecycle,
                int leftCloneStoreIndex, int rightCloneStoreIndex, int outputStoreSize);

    }

}
