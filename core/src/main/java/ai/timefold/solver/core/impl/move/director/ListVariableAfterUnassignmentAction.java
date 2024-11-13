package ai.timefold.solver.core.impl.move.director;

import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.score.director.VariableDescriptorAwareScoreDirector;
import ai.timefold.solver.core.preview.api.move.Rebaser;

record ListVariableAfterUnassignmentAction<Solution_>(Object element,
        ListVariableDescriptor<Solution_> variableDescriptor) implements ChangeAction<Solution_> {

    @Override
    public void undo(VariableDescriptorAwareScoreDirector<Solution_> scoreDirector) {
        scoreDirector.beforeListVariableElementAssigned(variableDescriptor, element);
    }

    @Override
    public ChangeAction<Solution_> rebase(Rebaser rebaser) {
        return new ListVariableAfterUnassignmentAction<>(rebaser.rebase(element), variableDescriptor);
    }

}