package ai.timefold.solver.core.impl.solver.change;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ai.timefold.solver.core.api.solver.change.ProblemChange;
import ai.timefold.solver.core.api.solver.change.ProblemChangeDirector;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.score.lavish.TestdataLavishEntity;
import ai.timefold.solver.core.testdomain.score.lavish.TestdataLavishEntityGroup;
import ai.timefold.solver.core.testdomain.score.lavish.TestdataLavishSolution;
import ai.timefold.solver.core.testdomain.score.lavish.TestdataLavishValue;
import ai.timefold.solver.core.testdomain.score.lavish.TestdataLavishValueGroup;

import org.junit.jupiter.api.Test;

class DefaultProblemChangeDirectorTest {

    @Test
    void complexProblemChange_correctlyNotifiesScoreDirector() {
        final TestdataLavishEntityGroup entityGroupOne = new TestdataLavishEntityGroup("entityGroupOne");
        final TestdataLavishValueGroup valueGroupOne = new TestdataLavishValueGroup("valueGroupOne");
        final TestdataLavishEntity addedEntity = new TestdataLavishEntity("newly added entity", entityGroupOne);
        final TestdataLavishEntity removedEntity = new TestdataLavishEntity("entity to remove", entityGroupOne);
        final TestdataLavishValue addedFact = new TestdataLavishValue("newly added fact", valueGroupOne);
        final TestdataLavishValue removedFact = new TestdataLavishValue("fact to remove", valueGroupOne);
        final TestdataLavishEntity changedEntity = new TestdataLavishEntity("changed entity", entityGroupOne);
        final TestdataLavishValue changedEntityValue = new TestdataLavishValue("changed entity value", valueGroupOne);

        InnerScoreDirector<TestdataSolution, ?> scoreDirectorMock = mock(InnerScoreDirector.class);
        when(scoreDirectorMock.lookUpWorkingObject(removedEntity)).thenReturn(removedEntity);
        when(scoreDirectorMock.lookUpWorkingObject(changedEntity)).thenReturn(changedEntity);
        when(scoreDirectorMock.lookUpWorkingObject(removedFact)).thenReturn(removedFact);
        ProblemChangeDirector defaultProblemChangeDirector = new DefaultProblemChangeDirector<>(scoreDirectorMock);

        ProblemChange<TestdataLavishSolution> problemChange = ((workingSolution, problemChangeDirector) -> {
            // Add an entity.
            problemChangeDirector.addEntity(addedEntity, workingSolution.getEntityList()::add);
            // Remove an entity.
            problemChangeDirector.removeEntity(removedEntity, workingSolution.getEntityList()::remove);
            // Change a planning variable.
            problemChangeDirector.changeVariable(changedEntity, TestdataLavishEntity.VALUE_FIELD,
                    testdataEntity -> testdataEntity.setValue(changedEntityValue));
            // Change a property
            problemChangeDirector.changeProblemProperty(changedEntity,
                    workingEntity -> workingEntity.setEntityGroup(null));
            // Add a problem fact.
            problemChangeDirector.addProblemFact(addedFact, workingSolution.getValueList()::add);
            // Remove a problem fact.
            problemChangeDirector.removeProblemFact(removedFact, workingSolution.getValueList()::remove);
        });

        TestdataLavishSolution testdataSolution = TestdataLavishSolution.generateSolution();
        testdataSolution.getEntityList().add(removedEntity);
        testdataSolution.getEntityList().add(changedEntity);
        testdataSolution.getValueList().add(removedFact);
        testdataSolution.getValueList().add(changedEntityValue);

        problemChange.doChange(testdataSolution, defaultProblemChangeDirector);
        verify(scoreDirectorMock, times(1)).beforeEntityAdded(addedEntity);
        verify(scoreDirectorMock, times(1)).afterEntityAdded(addedEntity);

        verify(scoreDirectorMock, times(1)).beforeEntityRemoved(removedEntity);
        verify(scoreDirectorMock, times(1)).afterEntityRemoved(removedEntity);

        verify(scoreDirectorMock, times(1))
                .beforeVariableChanged(changedEntity, TestdataEntity.VALUE_FIELD);
        verify(scoreDirectorMock, times(1))
                .afterVariableChanged(changedEntity, TestdataEntity.VALUE_FIELD);

        verify(scoreDirectorMock, times(1)).beforeProblemPropertyChanged(changedEntity);
        verify(scoreDirectorMock, times(1)).afterProblemPropertyChanged(changedEntity);

        verify(scoreDirectorMock, times(1)).beforeProblemFactAdded(addedFact);
        verify(scoreDirectorMock, times(1)).afterProblemFactAdded(addedFact);

        verify(scoreDirectorMock, times(1)).beforeProblemFactRemoved(removedFact);
        verify(scoreDirectorMock, times(1)).afterProblemFactRemoved(removedFact);
    }
}
