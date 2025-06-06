package ai.timefold.solver.core.impl.domain.variable.listener.support;

import java.util.Objects;

import ai.timefold.solver.core.api.domain.variable.VariableListener;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ShadowVariableDescriptor;

abstract class AbstractNotification {

    protected final Object entity;

    protected AbstractNotification(Object entity) {
        this.entity = entity;
    }

    public Object getEntity() {
        return entity;
    }

    /**
     * Warning: do not test equality of {@link AbstractNotification}s for different {@link VariableListener}s
     * (so {@link ShadowVariableDescriptor}s) because equality does not take those into account (for performance)!
     *
     * @param o sometimes null
     * @return true if same entity instance and the same type
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AbstractNotification that = (AbstractNotification) o;
        return entity.equals(that.entity);
    }

    @Override
    public int hashCode() {
        return Objects.hash(System.identityHashCode(entity), getClass());
    }
}
