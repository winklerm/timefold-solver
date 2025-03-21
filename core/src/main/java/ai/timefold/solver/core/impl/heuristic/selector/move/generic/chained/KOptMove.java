package ai.timefold.solver.core.impl.heuristic.selector.move.generic.chained;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.domain.variable.anchor.AnchorVariableSupply;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.inverserelation.SingletonInverseVariableSupply;
import ai.timefold.solver.core.impl.heuristic.move.AbstractMove;
import ai.timefold.solver.core.impl.score.director.VariableDescriptorAwareScoreDirector;

/**
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public class KOptMove<Solution_> extends AbstractMove<Solution_> {

    protected final GenuineVariableDescriptor<Solution_> variableDescriptor;
    // TODO remove me to enable multithreaded solving, but first fix https://issues.redhat.com/browse/PLANNER-1250
    protected final SingletonInverseVariableSupply inverseVariableSupply;
    protected final AnchorVariableSupply anchorVariableSupply;

    protected final Object entity;
    protected final Object[] values;

    public KOptMove(GenuineVariableDescriptor<Solution_> variableDescriptor,
            SingletonInverseVariableSupply inverseVariableSupply, AnchorVariableSupply anchorVariableSupply,
            Object entity, Object[] values) {
        this.variableDescriptor = variableDescriptor;
        this.inverseVariableSupply = inverseVariableSupply;
        this.anchorVariableSupply = anchorVariableSupply;
        this.entity = entity;
        this.values = values;
    }

    public String getVariableName() {
        return variableDescriptor.getVariableName();
    }

    public Object getEntity() {
        return entity;
    }

    public Object[] getValues() {
        return values;
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    public int getK() {
        return 1 + values.length;
    }

    @Override
    public boolean isMoveDoable(ScoreDirector<Solution_> scoreDirector) {
        Object firstAnchor = anchorVariableSupply.getAnchor(entity);
        Object firstValue = variableDescriptor.getValue(entity);
        Object formerAnchor = firstAnchor;
        Object formerValue = firstValue;
        for (Object value : values) {
            Object anchor = variableDescriptor.isValuePotentialAnchor(value)
                    ? value
                    : anchorVariableSupply.getAnchor(value);
            if (anchor == formerAnchor && compareValuesInSameChain(formerValue, value) >= 0) {
                return false;
            }
            formerAnchor = anchor;
            formerValue = value;
        }
        if (firstAnchor == formerAnchor && compareValuesInSameChain(formerValue, firstValue) >= 0) {
            return false;
        }
        return true;
    }

    protected int compareValuesInSameChain(Object a, Object b) {
        if (a == b) {
            return 0;
        }
        Object afterA = inverseVariableSupply.getInverseSingleton(a);
        while (afterA != null) {
            if (afterA == b) {
                return 1;
            }
            afterA = inverseVariableSupply.getInverseSingleton(afterA);
        }
        return -1;
    }

    @Override
    protected void doMoveOnGenuineVariables(ScoreDirector<Solution_> scoreDirector) {
        var castScoreDirector = (VariableDescriptorAwareScoreDirector<Solution_>) scoreDirector;
        var firstValue = variableDescriptor.getValue(entity);
        var formerEntity = entity;
        for (Object value : values) {
            if (formerEntity != null) {
                castScoreDirector.changeVariableFacade(variableDescriptor, formerEntity, value);
            }
            formerEntity = inverseVariableSupply.getInverseSingleton(value);
        }
        if (formerEntity != null) {
            castScoreDirector.changeVariableFacade(variableDescriptor, formerEntity, firstValue);
        }
    }

    @Override
    public KOptMove<Solution_> rebase(ScoreDirector<Solution_> destinationScoreDirector) {
        throw new UnsupportedOperationException("https://issues.redhat.com/browse/PLANNER-1250"); // TODO test also disabled
        //        return new KOptMove<>(variableDescriptor, inverseVariableSupply, anchorVariableSupply,
        //                destinationScoreDirector.lookUpWorkingObject(entity),
        //                rebaseArray(values, destinationScoreDirector));
    }

    // ************************************************************************
    // Introspection methods
    // ************************************************************************

    @Override
    public String getSimpleMoveTypeDescription() {
        return getClass().getSimpleName() + "(" + variableDescriptor.getSimpleEntityAndVariableName() + ")";
    }

    @Override
    public Collection<?> getPlanningEntities() {
        List<Object> allEntityList = new ArrayList<>(values.length + 1);
        allEntityList.add(entity);
        for (int i = 0; i < values.length; i++) {
            Object value = values[i];
            allEntityList.add(inverseVariableSupply.getInverseSingleton(value));
        }
        return allEntityList;
    }

    @Override
    public Collection<?> getPlanningValues() {
        List<Object> allValueList = new ArrayList<>(values.length + 1);
        allValueList.add(variableDescriptor.getValue(entity));
        Collections.addAll(allValueList, values);
        return allValueList;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final KOptMove<?> kOptMove = (KOptMove<?>) o;
        return Objects.equals(entity, kOptMove.entity) &&
                Arrays.equals(values, kOptMove.values);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entity, Arrays.hashCode(values));
    }

    @Override
    public String toString() {
        Object leftValue = variableDescriptor.getValue(entity);
        StringBuilder builder = new StringBuilder(80 * values.length);
        builder.append(entity).append(" {").append(leftValue);
        for (int i = 0; i < values.length; i++) {
            Object value = values[i];
            Object oldEntity = inverseVariableSupply.getInverseSingleton(value);
            builder.append("} -kOpt-> ").append(oldEntity).append(" {").append(value);
        }
        builder.append("}");
        return builder.toString();
    }

}
