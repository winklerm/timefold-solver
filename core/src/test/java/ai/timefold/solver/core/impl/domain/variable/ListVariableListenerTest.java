package ai.timefold.solver.core.impl.domain.variable;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.stream.Collectors;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.ListAssignMove;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.ListChangeMove;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.ListSwapMove;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.ListUnassignMove;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.SubListChangeMove;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.SubListSwapMove;
import ai.timefold.solver.core.impl.score.director.InnerScore;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.testdomain.list.shadowhistory.TestdataListEntityWithShadowHistory;
import ai.timefold.solver.core.testdomain.list.shadowhistory.TestdataListSolutionWithShadowHistory;
import ai.timefold.solver.core.testdomain.list.shadowhistory.TestdataListValueWithShadowHistory;
import ai.timefold.solver.core.testutil.PlannerTestUtils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ListVariableListenerTest {

    private final ListVariableDescriptor<TestdataListSolutionWithShadowHistory> variableDescriptor =
            TestdataListEntityWithShadowHistory.buildVariableDescriptorForValueList();

    private final InnerScoreDirector<TestdataListSolutionWithShadowHistory, SimpleScore> scoreDirector =
            PlannerTestUtils.mockScoreDirector(variableDescriptor.getEntityDescriptor().getSolutionDescriptor());

    static TestdataListSolutionWithShadowHistory buildSolution(TestdataListEntityWithShadowHistory... entities) {
        var values =
                Arrays.stream(entities).flatMap(e -> e.getValueList().stream()).collect(Collectors.toList());
        var solution = new TestdataListSolutionWithShadowHistory();
        solution.setEntityList(Arrays.asList(entities));
        solution.setValueList(values);
        return solution;
    }

    static void assertIndexHistory(TestdataListValueWithShadowHistory element, Integer... indexHistory) {
        assertThat(element.getIndex()).isEqualTo(indexHistory[indexHistory.length - 1]);
        assertThat(element.getIndexHistory()).containsExactly(indexHistory);
    }

    static void assertEntityHistory(TestdataListValueWithShadowHistory element,
            TestdataListEntityWithShadowHistory... entityHistory) {
        assertThat(element.getEntity()).isEqualTo(entityHistory[entityHistory.length - 1]);
        assertThat(element.getEntityHistory()).containsExactly(entityHistory);
    }

    static void assertEmptyPreviousHistory(TestdataListValueWithShadowHistory element) {
        assertThat(element.getPrevious()).isNull();
        assertThat(element.getPreviousHistory()).isEmpty();
    }

    static void assertPreviousHistory(TestdataListValueWithShadowHistory element,
            TestdataListValueWithShadowHistory... previousHistory) {
        assertThat(element.getPrevious())
                .as("Previous is incorrect")
                .isEqualTo(previousHistory[previousHistory.length - 1]);
        assertThat(element.getPreviousHistory())
                .as("History is incorrect")
                .containsExactly(previousHistory);
    }

    static void assertEmptyNextHistory(TestdataListValueWithShadowHistory element) {
        assertThat(element.getNext()).isNull();
        assertThat(element.getNextHistory()).isEmpty();
    }

    static void assertNextHistory(TestdataListValueWithShadowHistory element,
            TestdataListValueWithShadowHistory... nextHistory) {
        assertThat(element.getNext())
                .as("Next is incorrect")
                .isEqualTo(nextHistory[nextHistory.length - 1]);
        assertThat(element.getNextHistory())
                .as("History is incorrect")
                .containsExactly(nextHistory);
    }

    void doChangeMove(TestdataListEntityWithShadowHistory sourceEntity, int sourceIndex,
            TestdataListEntityWithShadowHistory destinationEntity, int destinationIndex) {
        new ListChangeMove<>(variableDescriptor, sourceEntity, sourceIndex, destinationEntity, destinationIndex)
                .doMoveOnly(scoreDirector);
    }

    void doSwapMove(TestdataListEntityWithShadowHistory leftEntity, int leftIndex,
            TestdataListEntityWithShadowHistory rightEntity, int rightIndex) {
        new ListSwapMove<>(variableDescriptor, leftEntity, leftIndex, rightEntity, rightIndex)
                .doMoveOnly(scoreDirector);
    }

    void doSubListChangeMove(TestdataListEntityWithShadowHistory sourceEntity, int fromIndex, int toIndex,
            TestdataListEntityWithShadowHistory destinationEntity, int destinationIndex, boolean reversing) {
        new SubListChangeMove<>(
                variableDescriptor,
                sourceEntity, fromIndex, toIndex - fromIndex,
                destinationEntity, destinationIndex,
                reversing)
                .doMoveOnly(scoreDirector);
    }

    void doSubListSwapMove(
            TestdataListEntityWithShadowHistory leftEntity, int leftFromIndex, int leftToIndex,
            TestdataListEntityWithShadowHistory rightEntity, int rightFromIndex, int rightToIndex, boolean reversing) {
        new SubListSwapMove<>(
                variableDescriptor,
                leftEntity, leftFromIndex, leftToIndex,
                rightEntity, rightFromIndex, rightToIndex,
                reversing)
                .doMoveOnly(scoreDirector);
    }

    @Test
    void addAndRemoveEntity() {
        var a = new TestdataListValueWithShadowHistory("A");
        var b = new TestdataListValueWithShadowHistory("B");
        var c = new TestdataListValueWithShadowHistory("C");
        var ann = new TestdataListEntityWithShadowHistory("Ann", a, b, c);

        scoreDirector.setWorkingSolution(buildSolution(ann));
        scoreDirector.forceTriggerVariableListeners();

        // Assert inverse entity.
        assertEntityHistory(a, ann);
        assertEntityHistory(b, ann);
        assertEntityHistory(c, ann);

        // Assert index.
        assertIndexHistory(a, 0);
        assertIndexHistory(b, 1);
        assertIndexHistory(c, 2);

        // Assert previous.
        assertEmptyPreviousHistory(a);
        assertPreviousHistory(b, a);
        assertPreviousHistory(c, b);

        // Assert next.
        assertNextHistory(a, b);
        assertNextHistory(b, c);
        assertEmptyNextHistory(c);

        // Remove Ann.
        scoreDirector.beforeEntityRemoved(ann);
        scoreDirector.afterEntityRemoved(ann);
        scoreDirector.triggerVariableListeners();

        // Assert inverse entity.
        assertEntityHistory(a, ann, null);
        assertEntityHistory(b, ann, null);
        assertEntityHistory(c, ann, null);

        // Assert index.
        assertIndexHistory(a, 0, null);
        assertIndexHistory(b, 1, null);
        assertIndexHistory(c, 2, null);

        // Assert previous.
        assertEmptyPreviousHistory(a);
        assertPreviousHistory(b, a, null);
        assertPreviousHistory(c, b, null);

        // Assert next.
        assertNextHistory(a, b, null);
        assertNextHistory(b, c, null);
        assertEmptyNextHistory(c);
    }

    @Test
    void addAndRemoveElement() {
        var a = new TestdataListValueWithShadowHistory("A");
        var b = new TestdataListValueWithShadowHistory("B");
        var c = new TestdataListValueWithShadowHistory("C");
        var x = new TestdataListValueWithShadowHistory("X");
        var ann = TestdataListEntityWithShadowHistory.createWithValues("Ann", a, b, c);
        var solution = buildSolution(ann);
        solution.getValueList().add(x);

        scoreDirector.setWorkingSolution(solution);
        assertThat(scoreDirector.calculateScore()).isEqualTo(InnerScore.withUnassignedCount(SimpleScore.ZERO, 1));

        new ListAssignMove<>(variableDescriptor, x, ann, 2).doMoveOnly(scoreDirector);

        assertThat(ann.getValueList()).containsExactly(a, b, x, c);

        assertEntityHistory(a, ann);
        assertEntityHistory(b, ann);
        assertEntityHistory(x, ann);
        assertEntityHistory(c, ann);

        assertIndexHistory(a, 0);
        assertIndexHistory(b, 1);
        assertIndexHistory(x, 2);
        assertIndexHistory(c, 2, 3);

        assertEmptyPreviousHistory(a);
        assertPreviousHistory(b, a);
        assertPreviousHistory(x, b);
        assertPreviousHistory(c, b, x);

        assertNextHistory(a, b);
        assertNextHistory(b, c, x);
        assertNextHistory(x, c);
        assertEmptyNextHistory(c);

        new ListUnassignMove<>(variableDescriptor, ann, 1).doMoveOnly(scoreDirector);

        assertThat(ann.getValueList()).containsExactly(a, x, c);

        assertEntityHistory(a, ann);
        assertEntityHistory(b, ann, null);
        assertEntityHistory(x, ann);
        assertEntityHistory(c, ann);

        assertIndexHistory(a, 0);
        assertIndexHistory(b, 1, null);
        assertIndexHistory(x, 2, 1);
        assertIndexHistory(c, 2, 3, 2);

        assertEmptyPreviousHistory(a);
        assertPreviousHistory(b, a, null);
        assertPreviousHistory(x, b, a);
        assertPreviousHistory(c, b, x);

        assertNextHistory(a, b, x);
        assertNextHistory(b, c, x, null);
        assertNextHistory(x, c);
        assertEmptyNextHistory(c);
    }

    @Test
    @DisplayName("M1: Ann[3]→Ann[1]")
    void moveElementToLowerIndexSameEntity() {
        var a = new TestdataListValueWithShadowHistory("A");
        var b = new TestdataListValueWithShadowHistory("B");
        var c = new TestdataListValueWithShadowHistory("C");
        var d = new TestdataListValueWithShadowHistory("D");
        var e = new TestdataListValueWithShadowHistory("E");
        var ann = TestdataListEntityWithShadowHistory.createWithValues("Ann", a, b, c, d, e);

        scoreDirector.setWorkingSolution(buildSolution(ann));

        doChangeMove(ann, 3, ann, 1);

        assertThat(ann.getValueList()).containsExactly(a, d, b, c, e);

        assertEntityHistory(a, ann);
        assertEntityHistory(b, ann);
        assertEntityHistory(c, ann);
        assertEntityHistory(d, ann);
        assertEntityHistory(e, ann);

        assertIndexHistory(a, 0);
        assertIndexHistory(b, 1, 2);
        assertIndexHistory(c, 2, 3);
        assertIndexHistory(d, 3, 1);
        assertIndexHistory(e, 4);

        assertEmptyPreviousHistory(a);
        assertPreviousHistory(b, a, d);
        assertPreviousHistory(c, b);
        assertPreviousHistory(d, c, a);
        assertPreviousHistory(e, d, c);

        assertNextHistory(a, b, d);
        assertNextHistory(b, c);
        assertNextHistory(c, d, e);
        assertNextHistory(d, e, b);
        assertEmptyNextHistory(e);
    }

    @Test
    @DisplayName("M2: Ann[0]→Ann[2]")
    void moveElementToHigherIndexSameEntity() {
        var a = new TestdataListValueWithShadowHistory("A");
        var b = new TestdataListValueWithShadowHistory("B");
        var c = new TestdataListValueWithShadowHistory("C");
        var d = new TestdataListValueWithShadowHistory("D");
        var e = new TestdataListValueWithShadowHistory("E");
        var ann = TestdataListEntityWithShadowHistory.createWithValues("Ann", a, b, c, d, e);

        scoreDirector.setWorkingSolution(buildSolution(ann));

        doChangeMove(ann, 0, ann, 2);

        assertThat(ann.getValueList()).containsExactly(b, c, a, d, e);

        assertEntityHistory(a, ann);
        assertEntityHistory(b, ann);
        assertEntityHistory(c, ann);
        assertEntityHistory(d, ann);
        assertEntityHistory(e, ann);

        assertIndexHistory(a, 0, 2);
        assertIndexHistory(b, 1, 0);
        assertIndexHistory(c, 2, 1);
        assertIndexHistory(d, 3);
        assertIndexHistory(e, 4);

        assertPreviousHistory(a, c);
        assertPreviousHistory(b, a, null);
        assertPreviousHistory(c, b);
        assertPreviousHistory(d, c, a);
        assertPreviousHistory(e, d);

        assertNextHistory(a, b, d);
        assertNextHistory(b, c);
        assertNextHistory(c, d, a);
        assertNextHistory(d, e);
        assertEmptyNextHistory(e);
    }

    @Test
    @DisplayName("M3: Ann[0]→Bob[1]")
    void moveElementToAnotherEntityChangeIndex() {
        var a = new TestdataListValueWithShadowHistory("A");
        var b = new TestdataListValueWithShadowHistory("B");
        var c = new TestdataListValueWithShadowHistory("C");
        var x = new TestdataListValueWithShadowHistory("X");
        var y = new TestdataListValueWithShadowHistory("Y");
        var ann = TestdataListEntityWithShadowHistory.createWithValues("Ann", a, b, c);
        var bob = TestdataListEntityWithShadowHistory.createWithValues("Bob", x, y);

        scoreDirector.setWorkingSolution(buildSolution(ann, bob));

        new ListChangeMove<>(variableDescriptor, ann, 0, bob, 1).doMoveOnly(scoreDirector);

        assertThat(ann.getValueList()).containsExactly(b, c);
        assertThat(bob.getValueList()).containsExactly(x, a, y);

        assertEntityHistory(a, ann, bob);
        assertEntityHistory(b, ann);
        assertEntityHistory(c, ann);
        assertEntityHistory(x, bob);
        assertEntityHistory(y, bob);

        assertIndexHistory(a, 0, 1);
        assertIndexHistory(b, 1, 0);
        assertIndexHistory(c, 2, 1);
        assertIndexHistory(x, 0);
        assertIndexHistory(y, 1, 2);

        assertPreviousHistory(a, x);
        assertPreviousHistory(b, a, null);
        assertPreviousHistory(c, b);
        assertEmptyPreviousHistory(x);
        assertPreviousHistory(y, x, a);

        assertNextHistory(a, b, y);
        assertNextHistory(b, c);
        assertEmptyNextHistory(c);
        assertNextHistory(x, y, a);
        assertEmptyNextHistory(y);
    }

    @Test
    @DisplayName("M4: Ann[1]→Bob[1]")
    void moveElementToAnotherEntitySameIndex() {
        var a = new TestdataListValueWithShadowHistory("A");
        var b = new TestdataListValueWithShadowHistory("B");
        var c = new TestdataListValueWithShadowHistory("C");
        var x = new TestdataListValueWithShadowHistory("X");
        var y = new TestdataListValueWithShadowHistory("Y");
        var ann = TestdataListEntityWithShadowHistory.createWithValues("Ann", a, b, c);
        var bob = TestdataListEntityWithShadowHistory.createWithValues("Bob", x, y);

        scoreDirector.setWorkingSolution(buildSolution(ann, bob));

        doChangeMove(ann, 1, bob, 1);

        assertThat(ann.getValueList()).containsExactly(a, c);
        assertThat(bob.getValueList()).containsExactly(x, b, y);

        assertEntityHistory(a, ann);
        assertEntityHistory(b, ann, bob);
        assertEntityHistory(c, ann);
        assertEntityHistory(x, bob);
        assertEntityHistory(y, bob);

        assertIndexHistory(a, 0);
        assertIndexHistory(b, 1);
        assertIndexHistory(c, 2, 1);
        assertIndexHistory(x, 0);
        assertIndexHistory(y, 1, 2);

        assertEmptyPreviousHistory(a);
        assertPreviousHistory(b, a, x);
        assertPreviousHistory(c, b, a);
        assertEmptyPreviousHistory(x);
        assertPreviousHistory(y, x, b);

        assertNextHistory(a, b, c);
        assertNextHistory(b, c, y);
        assertEmptyNextHistory(c);
        assertNextHistory(x, y, b);
        assertEmptyNextHistory(y);
    }

    @Test
    @DisplayName("M5: Ann[1]→Ann[2]")
    void moveOneUpToEnd() {
        var a = new TestdataListValueWithShadowHistory("A");
        var b = new TestdataListValueWithShadowHistory("B");
        var c = new TestdataListValueWithShadowHistory("C");
        var ann = TestdataListEntityWithShadowHistory.createWithValues("Ann", a, b, c);

        scoreDirector.setWorkingSolution(buildSolution(ann));

        doChangeMove(ann, 1, ann, 2);

        assertThat(ann.getValueList()).containsExactly(a, c, b);

        assertEntityHistory(a, ann);
        assertEntityHistory(b, ann);
        assertEntityHistory(c, ann);

        assertIndexHistory(a, 0);
        assertIndexHistory(b, 1, 2);
        assertIndexHistory(c, 2, 1);

        assertEmptyPreviousHistory(a);
        assertPreviousHistory(b, a, c);
        assertPreviousHistory(c, b, a);

        assertNextHistory(a, b, c);
        assertNextHistory(b, c, null);
        assertNextHistory(c, b);
    }

    @Test
    @DisplayName("M6: Ann[1]→Ann[0]")
    void moveOneDownToStart() {
        var a = new TestdataListValueWithShadowHistory("A");
        var b = new TestdataListValueWithShadowHistory("B");
        var c = new TestdataListValueWithShadowHistory("C");
        var ann = TestdataListEntityWithShadowHistory.createWithValues("Ann", a, b, c);

        scoreDirector.setWorkingSolution(buildSolution(ann));

        doChangeMove(ann, 1, ann, 0);

        assertThat(ann.getValueList()).containsExactly(b, a, c);

        assertEntityHistory(a, ann);
        assertEntityHistory(b, ann);
        assertEntityHistory(c, ann);

        assertIndexHistory(a, 0, 1);
        assertIndexHistory(b, 1, 0);
        assertIndexHistory(c, 2);

        assertPreviousHistory(a, b);
        assertPreviousHistory(b, a, null);
        assertPreviousHistory(c, b, a);

        assertNextHistory(a, b, c);
        assertNextHistory(b, c, a);
        assertEmptyNextHistory(c);
    }

    @Test
    @DisplayName("S1: Ann[1]↔Ann[3]")
    void swapElementsSameEntity() {
        var a = new TestdataListValueWithShadowHistory("A");
        var b = new TestdataListValueWithShadowHistory("B");
        var c = new TestdataListValueWithShadowHistory("C");
        var d = new TestdataListValueWithShadowHistory("D");
        var e = new TestdataListValueWithShadowHistory("E");
        var ann = TestdataListEntityWithShadowHistory.createWithValues("Ann", a, b, c, d, e);

        scoreDirector.setWorkingSolution(buildSolution(ann));

        doSwapMove(ann, 1, ann, 3);

        assertThat(ann.getValueList()).containsExactly(a, d, c, b, e);

        assertEntityHistory(a, ann);
        assertEntityHistory(b, ann);
        assertEntityHistory(c, ann);
        assertEntityHistory(d, ann);
        assertEntityHistory(e, ann);

        assertIndexHistory(a, 0);
        assertIndexHistory(b, 1, 3);
        assertIndexHistory(c, 2);
        assertIndexHistory(d, 3, 1);
        assertIndexHistory(e, 4);

        assertEmptyPreviousHistory(a);
        assertPreviousHistory(b, a, c);
        assertPreviousHistory(c, b, d);
        assertPreviousHistory(d, c, a);
        assertPreviousHistory(e, d, b);

        assertNextHistory(a, b, d);
        assertNextHistory(b, c, e);
        assertNextHistory(c, d, b);
        assertNextHistory(d, e, c);
        assertEmptyNextHistory(e);
    }

    @Test
    @DisplayName("S2: Ann[0]↔Bob[1]")
    void swapElementsAnotherEntityChangeIndex() {
        var a = new TestdataListValueWithShadowHistory("A");
        var b = new TestdataListValueWithShadowHistory("B");
        var c = new TestdataListValueWithShadowHistory("C");
        var x = new TestdataListValueWithShadowHistory("X");
        var y = new TestdataListValueWithShadowHistory("Y");
        var ann = TestdataListEntityWithShadowHistory.createWithValues("Ann", a, b, c);
        var bob = TestdataListEntityWithShadowHistory.createWithValues("Bob", x, y);

        scoreDirector.setWorkingSolution(buildSolution(ann, bob));

        doSwapMove(ann, 0, bob, 1);

        assertThat(ann.getValueList()).containsExactly(y, b, c);
        assertThat(bob.getValueList()).containsExactly(x, a);

        assertEntityHistory(a, ann, bob);
        assertEntityHistory(b, ann);
        assertEntityHistory(c, ann);
        assertEntityHistory(x, bob);
        assertEntityHistory(y, bob, ann);

        assertIndexHistory(a, 0, 1);
        assertIndexHistory(b, 1);
        assertIndexHistory(c, 2);
        assertIndexHistory(x, 0);
        assertIndexHistory(y, 1, 0);

        assertPreviousHistory(a, x);
        assertPreviousHistory(b, a, y);
        assertPreviousHistory(c, b);
        assertEmptyPreviousHistory(x);
        assertPreviousHistory(y, x, null);

        assertNextHistory(a, b, null);
        assertNextHistory(b, c);
        assertEmptyNextHistory(c);
        assertNextHistory(x, y, a);
        assertNextHistory(y, b);
    }

    @Test
    @DisplayName("S3: Ann[1]↔Bob[1]")
    void swapElementsAnotherEntitySameIndex() {
        var a = new TestdataListValueWithShadowHistory("A");
        var b = new TestdataListValueWithShadowHistory("B");
        var c = new TestdataListValueWithShadowHistory("C");
        var x = new TestdataListValueWithShadowHistory("X");
        var y = new TestdataListValueWithShadowHistory("Y");
        var ann = TestdataListEntityWithShadowHistory.createWithValues("Ann", a, b, c);
        var bob = TestdataListEntityWithShadowHistory.createWithValues("Bob", x, y);

        scoreDirector.setWorkingSolution(buildSolution(ann, bob));

        doSwapMove(ann, 1, bob, 1);

        assertThat(ann.getValueList()).containsExactly(a, y, c);
        assertThat(bob.getValueList()).containsExactly(x, b);

        assertEntityHistory(a, ann);
        assertEntityHistory(b, ann, bob);
        assertEntityHistory(c, ann);
        assertEntityHistory(x, bob);
        assertEntityHistory(y, bob, ann);

        assertIndexHistory(a, 0);
        assertIndexHistory(b, 1);
        assertIndexHistory(c, 2);
        assertIndexHistory(x, 0);
        assertIndexHistory(y, 1);

        assertEmptyPreviousHistory(a);
        assertPreviousHistory(b, a, x);
        assertPreviousHistory(c, b, y);
        assertEmptyPreviousHistory(x);
        assertPreviousHistory(y, x, a);

        assertNextHistory(a, b, y);
        assertNextHistory(b, c, null);
        assertEmptyNextHistory(c);
        assertNextHistory(x, y, b);
        assertNextHistory(y, c);
    }

    /**
     * The point of this test case is not only to verify that swapped neighbors end up with the expected indices, but also to
     * verify that listeners do not receive excessive notifications. Excessive (redundant, duplicate) notifications might cause
     * troubles for custom listeners that do incremental changes on a shadow variable.
     */
    @Test
    @DisplayName("S4: Ann[1]↔Ann[2]")
    void swapNeighbors() {
        var a = new TestdataListValueWithShadowHistory("A");
        var b = new TestdataListValueWithShadowHistory("B");
        var c = new TestdataListValueWithShadowHistory("C");
        var d = new TestdataListValueWithShadowHistory("D");
        var ann = TestdataListEntityWithShadowHistory.createWithValues("Ann", a, b, c, d);

        scoreDirector.setWorkingSolution(buildSolution(ann));

        doSwapMove(ann, 1, ann, 2);

        assertThat(ann.getValueList()).containsExactly(a, c, b, d);

        assertEntityHistory(a, ann);
        assertEntityHistory(b, ann);
        assertEntityHistory(c, ann);
        assertEntityHistory(d, ann);

        assertIndexHistory(a, 0);
        assertIndexHistory(b, 1, 2);
        assertIndexHistory(c, 2, 1);
        assertIndexHistory(d, 3);

        assertEmptyPreviousHistory(a);
        assertPreviousHistory(b, a, c);
        assertPreviousHistory(c, b, a);
        assertPreviousHistory(d, c, b);

        assertNextHistory(a, b, c);
        assertNextHistory(b, c, d);
        assertNextHistory(c, d, b);
        assertEmptyNextHistory(d);
    }

    @Test
    @DisplayName("subC1: Ann[1..3]→Ann[3]")
    void moveSubListToHigherIndexSameEntity() {
        var a = new TestdataListValueWithShadowHistory("A");
        var b = new TestdataListValueWithShadowHistory("B");
        var c = new TestdataListValueWithShadowHistory("C");
        var d = new TestdataListValueWithShadowHistory("D");
        var e = new TestdataListValueWithShadowHistory("E");
        var ann = TestdataListEntityWithShadowHistory.createWithValues("Ann", a, b, c, d, e);

        scoreDirector.setWorkingSolution(buildSolution(ann));

        doSubListChangeMove(ann, 1, 3, ann, 3, false);

        assertThat(ann.getValueList()).containsExactly(a, d, e, b, c);

        assertEntityHistory(a, ann);
        assertEntityHistory(b, ann);
        assertEntityHistory(c, ann);
        assertEntityHistory(d, ann);
        assertEntityHistory(e, ann);

        assertIndexHistory(a, 0);
        assertIndexHistory(b, 1, 3);
        assertIndexHistory(c, 2, 4);
        assertIndexHistory(d, 3, 1);
        assertIndexHistory(e, 4, 2);

        assertEmptyPreviousHistory(a);
        assertPreviousHistory(b, a, e);
        assertPreviousHistory(c, b);
        assertPreviousHistory(d, c, a);
        assertPreviousHistory(e, d);

        assertNextHistory(a, b, d);
        assertNextHistory(b, c);
        assertNextHistory(c, d, null);
        assertNextHistory(d, e);
        assertNextHistory(e, b);
    }

    @Test
    @DisplayName("subC2: Ann[1..4]→Ann[0]")
    void moveSubListToLowerIndexSameEntity() {
        var a = new TestdataListValueWithShadowHistory("A");
        var b = new TestdataListValueWithShadowHistory("B");
        var c = new TestdataListValueWithShadowHistory("C");
        var d = new TestdataListValueWithShadowHistory("D");
        var e = new TestdataListValueWithShadowHistory("E");
        var ann = TestdataListEntityWithShadowHistory.createWithValues("Ann", a, b, c, d, e);

        scoreDirector.setWorkingSolution(buildSolution(ann));

        doSubListChangeMove(ann, 1, 4, ann, 0, false);

        assertThat(ann.getValueList()).containsExactly(b, c, d, a, e);

        assertEntityHistory(a, ann);
        assertEntityHistory(b, ann);
        assertEntityHistory(c, ann);
        assertEntityHistory(d, ann);
        assertEntityHistory(e, ann);

        assertIndexHistory(a, 0, 3);
        assertIndexHistory(b, 1, 0);
        assertIndexHistory(c, 2, 1);
        assertIndexHistory(d, 3, 2);
        assertIndexHistory(e, 4);

        assertPreviousHistory(a, d);
        assertPreviousHistory(b, a, null);
        assertPreviousHistory(c, b);
        assertPreviousHistory(d, c);
        assertPreviousHistory(e, d, a);

        assertNextHistory(a, b, e);
        assertNextHistory(b, c);
        assertNextHistory(c, d);
        assertNextHistory(d, e, a);
        assertEmptyNextHistory(e);
    }

    @Test
    @DisplayName("subC3: Ann[1..4]→Bob[2]")
    void moveSubListToAnotherEntity() {
        var a = new TestdataListValueWithShadowHistory("A");
        var b = new TestdataListValueWithShadowHistory("B");
        var c = new TestdataListValueWithShadowHistory("C");
        var d = new TestdataListValueWithShadowHistory("D");
        var e = new TestdataListValueWithShadowHistory("E");
        var x = new TestdataListValueWithShadowHistory("X");
        var y = new TestdataListValueWithShadowHistory("Y");
        var z = new TestdataListValueWithShadowHistory("Z");
        var ann = TestdataListEntityWithShadowHistory.createWithValues("Ann", a, b, c, d, e);
        var bob = TestdataListEntityWithShadowHistory.createWithValues("Bob", x, y, z);

        scoreDirector.setWorkingSolution(buildSolution(ann, bob));

        doSubListChangeMove(ann, 1, 4, bob, 2, false);

        assertThat(ann.getValueList()).containsExactly(a, e);
        assertThat(bob.getValueList()).containsExactly(x, y, b, c, d, z);

        assertEntityHistory(a, ann);
        assertEntityHistory(b, ann, bob);
        assertEntityHistory(c, ann, bob);
        assertEntityHistory(d, ann, bob);
        assertEntityHistory(e, ann);
        assertEntityHistory(x, bob);
        assertEntityHistory(y, bob);
        assertEntityHistory(z, bob);

        assertIndexHistory(a, 0);
        assertIndexHistory(b, 1, 2);
        assertIndexHistory(c, 2, 3);
        assertIndexHistory(d, 3, 4);
        assertIndexHistory(e, 4, 1);
        assertIndexHistory(x, 0);
        assertIndexHistory(y, 1);
        assertIndexHistory(z, 2, 5);

        assertEmptyPreviousHistory(a);
        assertPreviousHistory(b, a, y);
        assertPreviousHistory(c, b);
        assertPreviousHistory(d, c);
        assertPreviousHistory(e, d, a);
        assertEmptyPreviousHistory(x);
        assertPreviousHistory(y, x);
        assertPreviousHistory(z, y, d);

        assertNextHistory(a, b, e);
        assertNextHistory(b, c);
        assertNextHistory(c, d);
        assertNextHistory(d, e, z);
        assertEmptyNextHistory(e);
        assertNextHistory(x, y);
        assertNextHistory(y, z, b);
        assertEmptyNextHistory(z);
    }

    @Test
    @DisplayName("subC4: Ann[1..3]→Bob[1]")
    void moveTailSubList() {
        var a = new TestdataListValueWithShadowHistory("A");
        var b = new TestdataListValueWithShadowHistory("B");
        var c = new TestdataListValueWithShadowHistory("C");
        var x = new TestdataListValueWithShadowHistory("X");
        var ann = TestdataListEntityWithShadowHistory.createWithValues("Ann", a, b, c);
        var bob = TestdataListEntityWithShadowHistory.createWithValues("Bob", x);

        scoreDirector.setWorkingSolution(buildSolution(ann, bob));

        doSubListChangeMove(ann, 1, 3, bob, 1, false);

        assertThat(ann.getValueList()).containsExactly(a);
        assertThat(bob.getValueList()).containsExactly(x, b, c);

        assertEntityHistory(a, ann);
        assertEntityHistory(b, ann, bob);
        assertEntityHistory(c, ann, bob);

        assertIndexHistory(a, 0);
        assertIndexHistory(b, 1);
        assertIndexHistory(c, 2);

        assertEmptyPreviousHistory(a);
        assertPreviousHistory(b, a, x);
        assertPreviousHistory(c, b);
        assertEmptyPreviousHistory(x);

        assertNextHistory(a, b, null);
        assertNextHistory(b, c);
        assertEmptyNextHistory(c);
        assertNextHistory(x, b);
    }

    @Test
    @DisplayName("subC5: Ann[0..3]→Bob[0]")
    void moveWholeListReversing() {
        var a = new TestdataListValueWithShadowHistory("A");
        var b = new TestdataListValueWithShadowHistory("B");
        var c = new TestdataListValueWithShadowHistory("C");
        var ann = TestdataListEntityWithShadowHistory.createWithValues("Ann", a, b, c);
        var bob = TestdataListEntityWithShadowHistory.createWithValues("Bob");

        scoreDirector.setWorkingSolution(buildSolution(ann, bob));

        doSubListChangeMove(ann, 0, 3, bob, 0, true);

        assertThat(ann.getValueList()).isEmpty();
        assertThat(bob.getValueList()).containsExactly(c, b, a);

        assertEntityHistory(a, ann, bob);
        assertEntityHistory(b, ann, bob);
        assertEntityHistory(c, ann, bob);

        assertIndexHistory(a, 0, 2);
        assertIndexHistory(b, 1);
        assertIndexHistory(c, 2, 0);

        assertPreviousHistory(a, b);
        assertPreviousHistory(b, a, c);
        assertPreviousHistory(c, b, null);

        assertNextHistory(a, b, null);
        assertNextHistory(b, c, a);
        assertNextHistory(c, b);
    }

    @Test
    @DisplayName("subS1: Ann[0..2]↔Ann[4..7]")
    void swapSubListsSameEntity() {
        var a = new TestdataListValueWithShadowHistory("A");
        var b = new TestdataListValueWithShadowHistory("B");
        var c = new TestdataListValueWithShadowHistory("C");
        var d = new TestdataListValueWithShadowHistory("D");
        var e = new TestdataListValueWithShadowHistory("E");
        var f = new TestdataListValueWithShadowHistory("F");
        var g = new TestdataListValueWithShadowHistory("G");
        var h = new TestdataListValueWithShadowHistory("H");
        var ann =
                TestdataListEntityWithShadowHistory.createWithValues("Ann", a, b, c, d, e, f, g, h);

        scoreDirector.setWorkingSolution(buildSolution(ann));

        doSubListSwapMove(ann, 0, 2, ann, 4, 7, false);

        assertThat(ann.getValueList()).containsExactly(e, f, g, c, d, a, b, h);

        assertEntityHistory(a, ann);
        assertEntityHistory(b, ann);
        assertEntityHistory(c, ann);
        assertEntityHistory(d, ann);
        assertEntityHistory(e, ann);
        assertEntityHistory(f, ann);
        assertEntityHistory(g, ann);
        assertEntityHistory(h, ann);

        assertIndexHistory(a, 0, 5);
        assertIndexHistory(b, 1, 6);
        assertIndexHistory(c, 2, 3);
        assertIndexHistory(d, 3, 4);
        assertIndexHistory(e, 4, 0);
        assertIndexHistory(f, 5, 1);
        assertIndexHistory(g, 6, 2);
        assertIndexHistory(h, 7);

        assertPreviousHistory(a, d);
        assertPreviousHistory(b, a);
        assertPreviousHistory(c, b, g);
        assertPreviousHistory(d, c);
        assertPreviousHistory(e, d, null);
        assertPreviousHistory(f, e);
        assertPreviousHistory(g, f);
        assertPreviousHistory(h, g, b);

        assertNextHistory(a, b);
        assertNextHistory(b, c, h);
        assertNextHistory(c, d);
        assertNextHistory(d, e, a);
        assertNextHistory(e, f);
        assertNextHistory(f, g);
        assertNextHistory(g, h, c);
        assertEmptyNextHistory(h);
    }

    @Test
    @DisplayName("subS2: Ann[0..2]↔Ann[4..6]")
    void swapSubListsSameSizeSameEntity() {
        var a = new TestdataListValueWithShadowHistory("A");
        var b = new TestdataListValueWithShadowHistory("B");
        var c = new TestdataListValueWithShadowHistory("C");
        var d = new TestdataListValueWithShadowHistory("D");
        var e = new TestdataListValueWithShadowHistory("E");
        var f = new TestdataListValueWithShadowHistory("F");
        var g = new TestdataListValueWithShadowHistory("G");
        var h = new TestdataListValueWithShadowHistory("H");
        var ann =
                TestdataListEntityWithShadowHistory.createWithValues("Ann", a, b, c, d, e, f, g, h);

        scoreDirector.setWorkingSolution(buildSolution(ann));

        doSubListSwapMove(ann, 0, 2, ann, 4, 6, false);

        assertThat(ann.getValueList()).containsExactly(e, f, c, d, a, b, g, h);

        assertEntityHistory(a, ann);
        assertEntityHistory(b, ann);
        assertEntityHistory(c, ann);
        assertEntityHistory(d, ann);
        assertEntityHistory(e, ann);
        assertEntityHistory(f, ann);
        assertEntityHistory(g, ann);
        assertEntityHistory(h, ann);

        assertIndexHistory(a, 0, 4);
        assertIndexHistory(b, 1, 5);
        assertIndexHistory(c, 2);
        assertIndexHistory(d, 3);
        assertIndexHistory(e, 4, 0);
        assertIndexHistory(f, 5, 1);
        assertIndexHistory(g, 6);
        assertIndexHistory(h, 7);

        assertPreviousHistory(a, d);
        assertPreviousHistory(b, a);
        assertPreviousHistory(c, b, f);
        assertPreviousHistory(d, c);
        assertPreviousHistory(e, d, null);
        assertPreviousHistory(f, e);
        assertPreviousHistory(g, f, b);
        assertPreviousHistory(h, g);

        assertNextHistory(a, b);
        assertNextHistory(b, c, g);
        assertNextHistory(c, d);
        assertNextHistory(d, e, a);
        assertNextHistory(e, f);
        assertNextHistory(f, g, c);
        assertNextHistory(g, h);
        assertEmptyNextHistory(h);
    }

    @Test
    @DisplayName("subSR1: Ann[3..5]↔Ann[0..2]")
    void swapSubListsSameEntityReversing() {
        var a = new TestdataListValueWithShadowHistory("A");
        var b = new TestdataListValueWithShadowHistory("B");
        var c = new TestdataListValueWithShadowHistory("C");
        var d = new TestdataListValueWithShadowHistory("D");
        var e = new TestdataListValueWithShadowHistory("E");
        var ann = TestdataListEntityWithShadowHistory.createWithValues("Ann", a, b, c, d, e);

        scoreDirector.setWorkingSolution(buildSolution(ann));

        doSubListSwapMove(ann, 3, 5, ann, 0, 2, true);

        assertThat(ann.getValueList()).containsExactly(e, d, c, b, a);

        assertEntityHistory(a, ann);
        assertEntityHistory(b, ann);
        assertEntityHistory(c, ann);
        assertEntityHistory(d, ann);
        assertEntityHistory(e, ann);

        assertIndexHistory(a, 0, 4);
        assertIndexHistory(b, 1, 3);
        assertIndexHistory(c, 2);
        assertIndexHistory(d, 3, 1);
        assertIndexHistory(e, 4, 0);

        assertPreviousHistory(a, b);
        assertPreviousHistory(b, a, c);
        assertPreviousHistory(c, b, d);
        assertPreviousHistory(d, c, e);
        assertPreviousHistory(e, d, null);

        assertNextHistory(a, b, null);
        assertNextHistory(b, c, a);
        assertNextHistory(c, d, b);
        assertNextHistory(d, e, c);
        assertNextHistory(e, d);
    }
}
