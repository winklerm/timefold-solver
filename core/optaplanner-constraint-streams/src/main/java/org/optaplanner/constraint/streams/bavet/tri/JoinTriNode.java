/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.optaplanner.constraint.streams.bavet.tri;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import org.optaplanner.constraint.streams.bavet.bi.BiTuple;
import org.optaplanner.constraint.streams.bavet.common.AbstractNode;
import org.optaplanner.constraint.streams.bavet.common.BavetTupleState;
import org.optaplanner.constraint.streams.bavet.common.index.IndexProperties;
import org.optaplanner.constraint.streams.bavet.common.index.Indexer;
import org.optaplanner.constraint.streams.bavet.uni.UniTuple;

public final class JoinTriNode<A, B, C> extends AbstractNode {

    private final BiFunction<A, B, IndexProperties> mappingAB;
    private final Function<C, IndexProperties> mappingC;
    private final int inputStoreIndexAB;
    private final int inputStoreIndexC;
    /**
     * Calls for example {@link TriScorer#insert(TriTuple)} and/or ...
     */
    private final Consumer<TriTuple<A, B, C>> nextNodesInsert;
    /**
     * Calls for example {@link TriScorer#retract(TriTuple)} and/or ...
     */
    private final Consumer<TriTuple<A, B, C>> nextNodesRetract;
    private final int outputStoreSize;

    private final Indexer<BiTuple<A, B>, Map<UniTuple<C>, TriTuple<A, B, C>>> indexerAB;
    private final Indexer<UniTuple<C>, Map<BiTuple<A, B>, TriTuple<A, B, C>>> indexerC;
    private final Queue<TriTuple<A, B, C>> dirtyTupleQueue;

    public JoinTriNode(BiFunction<A, B, IndexProperties> mappingAB, Function<C, IndexProperties> mappingC,
            int inputStoreIndexAB, int inputStoreIndexC,
            Consumer<TriTuple<A, B, C>> nextNodesInsert, Consumer<TriTuple<A, B, C>> nextNodesRetract,
            int outputStoreSize,
            Indexer<BiTuple<A, B>, Map<UniTuple<C>, TriTuple<A, B, C>>> indexerAB,
            Indexer<UniTuple<C>, Map<BiTuple<A, B>, TriTuple<A, B, C>>> indexerC) {
        this.mappingAB = mappingAB;
        this.mappingC = mappingC;
        this.inputStoreIndexAB = inputStoreIndexAB;
        this.inputStoreIndexC = inputStoreIndexC;
        this.nextNodesInsert = nextNodesInsert;
        this.nextNodesRetract = nextNodesRetract;
        this.outputStoreSize = outputStoreSize;
        this.indexerAB = indexerAB;
        this.indexerC = indexerC;
        dirtyTupleQueue = new ArrayDeque<>(1000);
    }

    public void insertAB(BiTuple<A, B> tupleAB) {
        if (tupleAB.store[inputStoreIndexAB] != null) {
            throw new IllegalStateException("Impossible state: the input for the tuple (" + tupleAB
                    + ") was already added in the tupleStore.");
        }
        IndexProperties indexProperties = mappingAB.apply(tupleAB.factA, tupleAB.factB);
        tupleAB.store[inputStoreIndexAB] = indexProperties;

        Map<UniTuple<C>, TriTuple<A, B, C>> tupleABCMapAB = new HashMap<>();
        indexerAB.put(indexProperties, tupleAB, tupleABCMapAB);
        indexerC.visit(indexProperties, (tupleC, tupleABCMapC) -> {
            TriTuple<A, B, C> tupleABC = new TriTuple<>(tupleAB.factA, tupleAB.factB, tupleC.factA,
                    outputStoreSize);
            tupleABC.state = BavetTupleState.CREATING;
            tupleABCMapAB.put(tupleC, tupleABC);
            tupleABCMapC.put(tupleAB, tupleABC);
            dirtyTupleQueue.add(tupleABC);
        });
    }

    public void retractAB(BiTuple<A, B> tupleAB) {
        IndexProperties indexProperties = (IndexProperties) tupleAB.store[inputStoreIndexAB];
        if (indexProperties == null) {
            // No fail fast if null because we don't track which tuples made it through the filter predicate(s)
            return;
        }
        tupleAB.store[inputStoreIndexAB] = null;

        indexerAB.remove(indexProperties, tupleAB);
        // Remove tupleABCs from the other side
        indexerC.visit(indexProperties, (tupleC, tupleABCMapC) -> {
            TriTuple<A, B, C> tupleABC = tupleABCMapC.remove(tupleAB);
            if (tupleABC == null) {
                throw new IllegalStateException("Impossible state: the tuple (" + tupleAB
                        + ") with indexProperties (" + indexProperties
                        + ") has tuples on the AB side that didn't exist on the C side.");
            }
            killTuple(tupleABC);
        });
    }

    public void insertC(UniTuple<C> tupleC) {
        if (tupleC.store[inputStoreIndexC] != null) {
            throw new IllegalStateException("Impossible state: the input for the tuple (" + tupleC
                    + ") was already added in the tupleStore.");
        }
        IndexProperties indexProperties = mappingC.apply(tupleC.factA);
        tupleC.store[inputStoreIndexC] = indexProperties;

        Map<BiTuple<A, B>, TriTuple<A, B, C>> tupleABCMapC = new HashMap<>();
        indexerC.put(indexProperties, tupleC, tupleABCMapC);
        indexerAB.visit(indexProperties, (tupleAB, tupleABCMapAB) -> {
            TriTuple<A, B, C> tupleABC = new TriTuple<>(tupleAB.factA, tupleAB.factB, tupleC.factA,
                    outputStoreSize);
            tupleABC.state = BavetTupleState.CREATING;
            tupleABCMapC.put(tupleAB, tupleABC);
            tupleABCMapAB.put(tupleC, tupleABC);
            dirtyTupleQueue.add(tupleABC);
        });
    }

    public void retractC(UniTuple<C> tupleC) {
        IndexProperties indexProperties = (IndexProperties) tupleC.store[inputStoreIndexC];
        if (indexProperties == null) {
            // No fail fast if null because we don't track which tuples made it through the filter predicate(s)
            return;
        }
        tupleC.store[inputStoreIndexC] = null;

        indexerC.remove(indexProperties, tupleC);
        // Remove tupleABCs from the other side
        indexerAB.visit(indexProperties, (tupleAB, tupleABCMapAB) -> {
            TriTuple<A, B, C> tupleABC = tupleABCMapAB.remove(tupleC);
            if (tupleABC == null) {
                throw new IllegalStateException("Impossible state: the tuple (" + tupleAB
                        + ") with indexProperties (" + indexProperties
                        + ") has tuples on the C side that didn't exist on the AB side.");
            }
            killTuple(tupleABC);
        });
    }

    private void killTuple(TriTuple<A, B, C> tupleABC) {
        // Don't add the tuple to the dirtyTupleQueue twice
        if (tupleABC.state.isDirty()) {
            switch (tupleABC.state) {
                case CREATING:
                    // Kill it before it propagates
                    tupleABC.state = BavetTupleState.ABORTING;
                    break;
                case UPDATING:
                    // Kill the original propagation
                    tupleABC.state = BavetTupleState.DYING;
                    break;
                case DYING:
                    break;
                default:
                    throw new IllegalStateException("Impossible state: The tuple (" + tupleABC.factA
                            + ") has the dirty state (" + tupleABC.state + ").");
            }
        } else {
            tupleABC.state = BavetTupleState.DYING;
            dirtyTupleQueue.add(tupleABC);
        }
    }

    @Override
    public void calculateScore() {
        dirtyTupleQueue.forEach(tuple -> {
            // Retract
            if (tuple.state == BavetTupleState.UPDATING || tuple.state == BavetTupleState.DYING) {
                nextNodesRetract.accept(tuple);
            }
            // Insert
            if (tuple.state == BavetTupleState.CREATING || tuple.state == BavetTupleState.UPDATING) {
                nextNodesInsert.accept(tuple);
            }
            switch (tuple.state) {
                case CREATING:
                case UPDATING:
                    tuple.state = BavetTupleState.OK;
                    return;
                case DYING:
                case ABORTING:
                    tuple.state = BavetTupleState.DEAD;
                    return;
                case DEAD:
                    throw new IllegalStateException("Impossible state: The tuple (" + tuple + ") in node (" +
                            this + ") is already in the dead state (" + tuple.state + ").");
                default:
                    throw new IllegalStateException("Impossible state: The tuple (" + tuple + ") in node (" +
                            this + ") is in an unexpected state (" + tuple.state + ").");
            }
        });
        dirtyTupleQueue.clear();
    }

    @Override
    public String toString() {
        return "JoinTriNode";
    }

}