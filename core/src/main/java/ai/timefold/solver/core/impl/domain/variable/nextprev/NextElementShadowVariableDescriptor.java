package ai.timefold.solver.core.impl.domain.variable.nextprev;

import java.util.Collection;

import ai.timefold.solver.core.api.domain.variable.AbstractVariableListener;
import ai.timefold.solver.core.api.domain.variable.NextElementShadowVariable;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessor;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.variable.ListVariableStateSupply;
import ai.timefold.solver.core.impl.domain.variable.listener.VariableListenerWithSources;
import ai.timefold.solver.core.impl.domain.variable.supply.SupplyManager;

public final class NextElementShadowVariableDescriptor<Solution_>
        extends AbstractNextPrevElementShadowVariableDescriptor<Solution_> {

    public NextElementShadowVariableDescriptor(int ordinal, EntityDescriptor<Solution_> entityDescriptor,
            MemberAccessor variableMemberAccessor) {
        super(ordinal, entityDescriptor, variableMemberAccessor);
    }

    @Override
    String getSourceVariableName() {
        return variableMemberAccessor.getAnnotation(NextElementShadowVariable.class).sourceVariableName();
    }

    @Override
    String getAnnotationName() {
        return NextElementShadowVariable.class.getSimpleName();
    }

    @Override
    public Collection<Class<? extends AbstractVariableListener>> getVariableListenerClasses() {
        throw new UnsupportedOperationException("Impossible state: Handled by %s."
                .formatted(ListVariableStateSupply.class.getSimpleName()));
    }

    @Override
    public Iterable<VariableListenerWithSources<Solution_>> buildVariableListeners(SupplyManager supplyManager) {
        throw new UnsupportedOperationException("Impossible state: Handled by %s."
                .formatted(ListVariableStateSupply.class.getSimpleName()));
    }
}
