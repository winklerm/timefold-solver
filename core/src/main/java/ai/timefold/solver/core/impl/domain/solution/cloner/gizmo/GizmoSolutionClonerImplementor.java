package ai.timefold.solver.core.impl.domain.solution.cloner.gizmo;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import ai.timefold.solver.core.api.domain.solution.cloner.SolutionCloner;
import ai.timefold.solver.core.impl.domain.common.accessor.gizmo.GizmoClassLoader;
import ai.timefold.solver.core.impl.domain.common.accessor.gizmo.GizmoMemberDescriptor;
import ai.timefold.solver.core.impl.domain.solution.cloner.DeepCloningUtils;
import ai.timefold.solver.core.impl.domain.solution.cloner.FieldAccessingSolutionCloner;
import ai.timefold.solver.core.impl.domain.solution.cloner.PlanningCloneable;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.util.MutableReference;

import io.quarkus.gizmo.AssignableResultHandle;
import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;

public class GizmoSolutionClonerImplementor {
    private static final MethodDescriptor EQUALS_METHOD = MethodDescriptor.ofMethod(Object.class, "equals", boolean.class,
            Object.class);
    protected static final MethodDescriptor GET_METHOD = MethodDescriptor.ofMethod(Map.class, "get", Object.class,
            Object.class);
    private static final MethodDescriptor PUT_METHOD = MethodDescriptor.ofMethod(Map.class, "put", Object.class,
            Object.class, Object.class);
    private static final String FALLBACK_CLONER = "fallbackCloner";
    public static final boolean DEBUG = false;

    /**
     * Return a comparator that sorts classes into instanceof check order.
     * In particular, if x is a subclass of y, then x will appear earlier
     * than y in the list.
     *
     * @param deepClonedClassSet The set of classes to generate a comparator for
     * @return A comparator that sorts classes from deepClonedClassSet such that
     *         x &lt; y if x is assignable from y.
     */
    public static Comparator<Class<?>> getInstanceOfComparator(Set<Class<?>> deepClonedClassSet) {
        Map<Class<?>, Integer> classToSubclassLevel = new HashMap<>();
        deepClonedClassSet
                .forEach(clazz -> {
                    if (deepClonedClassSet.stream()
                            .allMatch(
                                    otherClazz -> clazz.isAssignableFrom(otherClazz) || !otherClazz.isAssignableFrom(clazz))) {
                        classToSubclassLevel.put(clazz, 0);
                    }
                });
        boolean isChanged = true;
        while (isChanged) {
            // Need to iterate over all classes
            // since maxSubclassLevel can change
            // (for instance, Tiger extends Cat (1) implements Animal (0))
            isChanged = false;
            for (Class<?> clazz : deepClonedClassSet) {
                Optional<Integer> maxParentSubclassLevel = classToSubclassLevel.keySet().stream()
                        .filter(otherClazz -> otherClazz != clazz && otherClazz.isAssignableFrom(clazz))
                        .map(classToSubclassLevel::get)
                        .max(Integer::compare);

                if (maxParentSubclassLevel.isPresent()) {
                    Integer oldVal = classToSubclassLevel.getOrDefault(clazz, -1);
                    Integer newVal = maxParentSubclassLevel.get() + 1;
                    if (newVal.compareTo(oldVal) > 0) {
                        isChanged = true;
                        classToSubclassLevel.put(clazz, newVal);
                    }
                }
            }
        }

        return Comparator.<Class<?>, Integer> comparing(classToSubclassLevel::get)
                .thenComparing(Class::getName).reversed();
    }

    protected void createFields(ClassCreator classCreator) {
        classCreator.getFieldCreator(FALLBACK_CLONER, FieldAccessingSolutionCloner.class)
                .setModifiers(Modifier.PRIVATE | Modifier.STATIC);
    }

    /**
     * Generates the constructor and implementations of SolutionCloner methods for the given SolutionDescriptor using the given
     * ClassCreator
     */
    public static void defineClonerFor(ClassCreator classCreator,
            SolutionDescriptor<?> solutionDescriptor,
            Set<Class<?>> solutionClassSet,
            Map<Class<?>, GizmoSolutionOrEntityDescriptor> memoizedSolutionOrEntityDescriptorMap,
            Set<Class<?>> deepClonedClassSet) {
        defineClonerFor(GizmoSolutionClonerImplementor::new, classCreator, solutionDescriptor, solutionClassSet,
                memoizedSolutionOrEntityDescriptorMap, deepClonedClassSet);
    }

    public static boolean isCloneableClass(Class<?> clazz) {
        return !clazz.isInterface() && !Modifier.isAbstract(clazz.getModifiers());
    }

    /**
     * Generates the constructor and implementations of SolutionCloner
     * methods for the given SolutionDescriptor using the given ClassCreator
     */
    public static void defineClonerFor(Supplier<GizmoSolutionClonerImplementor> implementorSupplier,
            ClassCreator classCreator,
            SolutionDescriptor<?> solutionDescriptor,
            Set<Class<?>> solutionClassSet,
            Map<Class<?>, GizmoSolutionOrEntityDescriptor> memoizedSolutionOrEntityDescriptorMap,
            Set<Class<?>> deepClonedClassSet) {
        GizmoSolutionClonerImplementor implementor = implementorSupplier.get();
        // Classes that are not instances of any other class in the collection
        // have a subclass level of 0.
        // Other classes subclass level is the maximum of the subclass level
        // of the classes it is a subclass of + 1
        Set<Class<?>> deepCloneClassesThatAreNotSolutionSet =
                deepClonedClassSet.stream()
                        .filter(clazz -> !solutionClassSet.contains(clazz) && !clazz.isArray())
                        .filter(GizmoSolutionClonerImplementor::isCloneableClass)
                        .collect(Collectors.toSet());

        Comparator<Class<?>> instanceOfComparator = getInstanceOfComparator(deepClonedClassSet);
        SortedSet<Class<?>> deepCloneClassesThatAreNotSolutionSortedSet = new TreeSet<>(instanceOfComparator);
        deepCloneClassesThatAreNotSolutionSortedSet.addAll(deepCloneClassesThatAreNotSolutionSet);

        implementor.createFields(classCreator);
        implementor.createConstructor(classCreator);
        implementor.createSetSolutionDescriptor(classCreator, solutionDescriptor);
        implementor.createCloneSolution(classCreator, solutionDescriptor);
        implementor.createCloneSolutionRun(classCreator, solutionDescriptor, solutionClassSet,
                memoizedSolutionOrEntityDescriptorMap,
                deepCloneClassesThatAreNotSolutionSortedSet, instanceOfComparator);

        for (Class<?> deepClonedClass : deepCloneClassesThatAreNotSolutionSortedSet) {
            implementor.createDeepCloneHelperMethod(classCreator, deepClonedClass, solutionDescriptor,
                    memoizedSolutionOrEntityDescriptorMap,
                    deepCloneClassesThatAreNotSolutionSortedSet);
        }

        Set<Class<?>> abstractDeepCloneClassSet =
                deepClonedClassSet.stream()
                        .filter(clazz -> !solutionClassSet.contains(clazz) && !clazz.isArray())
                        .filter(Predicate.not(GizmoSolutionClonerImplementor::isCloneableClass))
                        .collect(Collectors.toSet());

        for (Class<?> abstractDeepClonedClass : abstractDeepCloneClassSet) {
            implementor.createAbstractDeepCloneHelperMethod(classCreator, abstractDeepClonedClass, solutionDescriptor,
                    memoizedSolutionOrEntityDescriptorMap,
                    deepCloneClassesThatAreNotSolutionSortedSet);
        }
    }

    public static ClassOutput createClassOutputWithDebuggingCapability(MutableReference<byte[]> classBytecodeHolder) {
        return (path, byteCode) -> {
            classBytecodeHolder.setValue(byteCode);

            if (DEBUG) {
                Path debugRoot = Paths.get("target/timefold-solver-generated-classes");
                Path rest = Paths.get(path + ".class");
                Path destination = debugRoot.resolve(rest);

                try {
                    Files.createDirectories(destination.getParent());
                    Files.write(destination, byteCode);
                } catch (IOException e) {
                    throw new IllegalStateException("Fail to write debug class file " + destination + ".", e);
                }
            }
        };
    }

    static <T> SolutionCloner<T> createClonerFor(SolutionDescriptor<T> solutionDescriptor,
            GizmoClassLoader gizmoClassLoader) {
        GizmoSolutionClonerImplementor implementor = new GizmoSolutionClonerImplementor();
        String className = GizmoSolutionClonerFactory.getGeneratedClassName(solutionDescriptor);
        if (gizmoClassLoader.hasBytecodeFor(className)) {
            return implementor.createInstance(className, gizmoClassLoader, solutionDescriptor);
        }
        MutableReference<byte[]> classBytecodeHolder = new MutableReference<>(null);
        ClassCreator classCreator = ClassCreator.builder()
                .className(className)
                .interfaces(GizmoSolutionCloner.class)
                .superClass(Object.class)
                .classOutput(createClassOutputWithDebuggingCapability(classBytecodeHolder))
                .setFinal(true)
                .build();

        Set<Class<?>> deepClonedClassSet = GizmoCloningUtils.getDeepClonedClasses(solutionDescriptor, Collections.emptyList());

        defineClonerFor(() -> implementor, classCreator, solutionDescriptor,
                Collections.singleton(solutionDescriptor.getSolutionClass()),
                new HashMap<>(), deepClonedClassSet);

        classCreator.close();
        byte[] classBytecode = classBytecodeHolder.getValue();

        gizmoClassLoader.storeBytecode(className, classBytecode);
        return implementor.createInstance(className, gizmoClassLoader, solutionDescriptor);
    }

    private <T> SolutionCloner<T> createInstance(String className, ClassLoader gizmoClassLoader,
            SolutionDescriptor<T> solutionDescriptor) {
        try {
            @SuppressWarnings("unchecked")
            Class<? extends GizmoSolutionCloner<T>> outClass =
                    (Class<? extends GizmoSolutionCloner<T>>) gizmoClassLoader.loadClass(className);
            GizmoSolutionCloner<T> out = outClass.getConstructor().newInstance();
            out.setSolutionDescriptor(solutionDescriptor);
            return out;
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException | ClassNotFoundException
                | NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }

    private void createConstructor(ClassCreator classCreator) {
        MethodCreator methodCreator = classCreator.getMethodCreator(
                MethodDescriptor.ofConstructor(classCreator.getClassName()));
        ResultHandle thisObj = methodCreator.getThis();

        // Invoke Object's constructor
        methodCreator.invokeSpecialMethod(MethodDescriptor.ofConstructor(Object.class), thisObj);

        // Return this (it a constructor)
        methodCreator.returnValue(thisObj);
    }

    protected void createSetSolutionDescriptor(ClassCreator classCreator, SolutionDescriptor<?> solutionDescriptor) {
        MethodCreator methodCreator = classCreator.getMethodCreator(
                MethodDescriptor.ofMethod(GizmoSolutionCloner.class, "setSolutionDescriptor", void.class,
                        SolutionDescriptor.class));

        methodCreator.writeStaticField(FieldDescriptor.of(
                GizmoSolutionClonerFactory.getGeneratedClassName(solutionDescriptor),
                FALLBACK_CLONER, FieldAccessingSolutionCloner.class),
                methodCreator.newInstance(
                        MethodDescriptor.ofConstructor(FieldAccessingSolutionCloner.class, SolutionDescriptor.class),
                        methodCreator.getMethodParam(0)));

        methodCreator.returnValue(null);
    }

    private void createCloneSolution(ClassCreator classCreator, SolutionDescriptor<?> solutionDescriptor) {
        Class<?> solutionClass = solutionDescriptor.getSolutionClass();
        MethodCreator methodCreator =
                classCreator.getMethodCreator(MethodDescriptor.ofMethod(SolutionCloner.class,
                        "cloneSolution",
                        Object.class,
                        Object.class));

        ResultHandle thisObj = methodCreator.getMethodParam(0);

        ResultHandle clone = methodCreator.invokeStaticMethod(
                MethodDescriptor.ofMethod(
                        GizmoSolutionClonerFactory.getGeneratedClassName(solutionDescriptor),
                        "cloneSolutionRun", solutionClass, solutionClass, Map.class),
                thisObj,
                methodCreator.newInstance(MethodDescriptor.ofConstructor(IdentityHashMap.class)));
        methodCreator.returnValue(clone);
    }

    private void createCloneSolutionRun(ClassCreator classCreator, SolutionDescriptor<?> solutionDescriptor,
            Set<Class<?>> solutionClassSet,
            Map<Class<?>, GizmoSolutionOrEntityDescriptor> memoizedSolutionOrEntityDescriptorMap,
            SortedSet<Class<?>> deepClonedClassesSortedSet, Comparator<Class<?>> instanceOfComparator) {
        Class<?> solutionClass = solutionDescriptor.getSolutionClass();
        MethodCreator methodCreator =
                classCreator.getMethodCreator("cloneSolutionRun", solutionClass, solutionClass, Map.class);
        methodCreator.setModifiers(Modifier.STATIC | Modifier.PRIVATE);

        ResultHandle thisObj = methodCreator.getMethodParam(0);
        BranchResult solutionNullBranchResult = methodCreator.ifNull(thisObj);
        BytecodeCreator solutionIsNullBranch = solutionNullBranchResult.trueBranch();
        solutionIsNullBranch.returnValue(thisObj); // thisObj is null

        BytecodeCreator solutionIsNotNullBranch = solutionNullBranchResult.falseBranch();

        ResultHandle createdCloneMap = methodCreator.getMethodParam(1);

        ResultHandle maybeClone = solutionIsNotNullBranch.invokeInterfaceMethod(
                GET_METHOD, createdCloneMap, thisObj);
        BranchResult hasCloneBranchResult = solutionIsNotNullBranch.ifNotNull(maybeClone);
        BytecodeCreator hasCloneBranch = hasCloneBranchResult.trueBranch();
        hasCloneBranch.returnValue(maybeClone);

        BytecodeCreator noCloneBranch = hasCloneBranchResult.falseBranch();
        List<Class<?>> sortedSolutionClassList = new ArrayList<>(solutionClassSet);
        sortedSolutionClassList.sort(instanceOfComparator);

        BytecodeCreator currentBranch = noCloneBranch;
        ResultHandle thisObjClass =
                currentBranch.invokeVirtualMethod(MethodDescriptor.ofMethod(Object.class, "getClass", Class.class), thisObj);
        for (Class<?> solutionSubclass : sortedSolutionClassList) {
            ResultHandle solutionSubclassResultHandle = currentBranch.loadClass(solutionSubclass);
            ResultHandle isSubclass =
                    currentBranch.invokeVirtualMethod(EQUALS_METHOD, solutionSubclassResultHandle, thisObjClass);
            BranchResult isSubclassBranchResult = currentBranch.ifTrue(isSubclass);

            BytecodeCreator isSubclassBranch = isSubclassBranchResult.trueBranch();

            GizmoSolutionOrEntityDescriptor solutionSubclassDescriptor =
                    memoizedSolutionOrEntityDescriptorMap.computeIfAbsent(solutionSubclass,
                            key -> new GizmoSolutionOrEntityDescriptor(solutionDescriptor, solutionSubclass));

            ResultHandle clone;
            if (PlanningCloneable.class.isAssignableFrom(solutionSubclass)) {
                clone = isSubclassBranch.invokeInterfaceMethod(
                        MethodDescriptor.ofMethod(PlanningCloneable.class, "createNewInstance", Object.class),
                        thisObj);
                clone = isSubclassBranch.checkCast(clone, solutionSubclass);
            } else {
                clone = isSubclassBranch.newInstance(MethodDescriptor.ofConstructor(solutionSubclass));
            }

            isSubclassBranch.invokeInterfaceMethod(
                    MethodDescriptor.ofMethod(Map.class, "put", Object.class, Object.class, Object.class),
                    createdCloneMap, thisObj, clone);

            for (GizmoMemberDescriptor shallowlyClonedField : solutionSubclassDescriptor.getShallowClonedMemberDescriptors()) {
                writeShallowCloneInstructions(solutionSubclassDescriptor, isSubclassBranch, shallowlyClonedField, thisObj,
                        clone, createdCloneMap, deepClonedClassesSortedSet);
            }

            for (Field deeplyClonedField : solutionSubclassDescriptor.getDeepClonedFields()) {
                GizmoMemberDescriptor gizmoMemberDescriptor =
                        solutionSubclassDescriptor.getMemberDescriptorForField(deeplyClonedField);

                ResultHandle fieldValue = gizmoMemberDescriptor.readMemberValue(isSubclassBranch, thisObj);
                AssignableResultHandle cloneValue = isSubclassBranch.createVariable(deeplyClonedField.getType());
                writeDeepCloneInstructions(isSubclassBranch, solutionSubclassDescriptor, deeplyClonedField,
                        gizmoMemberDescriptor, fieldValue, cloneValue, createdCloneMap, deepClonedClassesSortedSet);

                if (!gizmoMemberDescriptor.writeMemberValue(isSubclassBranch, clone, cloneValue)) {
                    throw new IllegalStateException("The member (" + gizmoMemberDescriptor.getName() + ") of class (" +
                            gizmoMemberDescriptor.getDeclaringClassName() +
                            ") does not have a setter.");
                }
            }
            isSubclassBranch.returnValue(clone);

            currentBranch = isSubclassBranchResult.falseBranch();
        }
        ResultHandle errorBuilder = currentBranch.newInstance(MethodDescriptor.ofConstructor(StringBuilder.class, String.class),
                currentBranch.load("Failed to create clone: encountered ("));
        final MethodDescriptor APPEND =
                MethodDescriptor.ofMethod(StringBuilder.class, "append", StringBuilder.class, Object.class);

        currentBranch.invokeVirtualMethod(APPEND, errorBuilder, thisObjClass);
        currentBranch.invokeVirtualMethod(APPEND, errorBuilder, currentBranch.load(") which is not a known subclass of " +
                "the solution class (" + solutionDescriptor.getSolutionClass() + "). The known subclasses are " +
                solutionClassSet.stream().map(Class::getName).collect(Collectors.joining(", ", "[", "]")) + "." +
                "\nMaybe use DomainAccessType.REFLECTION?"));
        ResultHandle errorMsg = currentBranch
                .invokeVirtualMethod(MethodDescriptor.ofMethod(Object.class, "toString", String.class), errorBuilder);
        ResultHandle error = currentBranch
                .newInstance(MethodDescriptor.ofConstructor(IllegalArgumentException.class, String.class), errorMsg);
        currentBranch.throwException(error);
    }

    /**
     * Writes the following code:
     *
     * <pre>
     * // If getter a field
     * clone.member = original.member
     * // If getter a method (i.e. Quarkus)
     * clone.setMember(original.getMember());
     * </pre>
     *
     * @param methodCreator
     * @param shallowlyClonedField
     * @param thisObj
     * @param clone
     */
    private void writeShallowCloneInstructions(GizmoSolutionOrEntityDescriptor solutionInfo,
            BytecodeCreator methodCreator, GizmoMemberDescriptor shallowlyClonedField,
            ResultHandle thisObj, ResultHandle clone, ResultHandle createdCloneMap,
            SortedSet<Class<?>> deepClonedClassesSortedSet) {
        try {
            boolean isArray = shallowlyClonedField.getTypeName().endsWith("[]");
            Class<?> type = null;
            if (shallowlyClonedField.getType() instanceof Class) {
                type = (Class<?>) shallowlyClonedField.getType();
            }

            List<Class<?>> entitySubclasses = Collections.emptyList();
            if (type == null && !isArray) {
                type = Class.forName(shallowlyClonedField.getTypeName().replace('/', '.'), false,
                        Thread.currentThread().getContextClassLoader());
            }

            if (type != null && !isArray) {
                entitySubclasses =
                        deepClonedClassesSortedSet.stream().filter(type::isAssignableFrom).toList();
            }

            ResultHandle fieldValue = shallowlyClonedField.readMemberValue(methodCreator, thisObj);
            if (!entitySubclasses.isEmpty()) {
                AssignableResultHandle cloneResultHolder = methodCreator.createVariable(type);
                writeDeepCloneEntityOrFactInstructions(methodCreator, solutionInfo, type,
                        fieldValue, cloneResultHolder, createdCloneMap, deepClonedClassesSortedSet,
                        UnhandledCloneType.SHALLOW);
                fieldValue = cloneResultHolder;
            }
            if (!shallowlyClonedField.writeMemberValue(methodCreator, clone, fieldValue)) {
                throw new IllegalStateException("Field (" + shallowlyClonedField.getName() + ") of class (" +
                        shallowlyClonedField.getDeclaringClassName() +
                        ") does not have a setter.");
            }
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Error creating Gizmo Solution Cloner", e);
        }
    }

    /**
     * @see #writeDeepCloneInstructions(BytecodeCreator, GizmoSolutionOrEntityDescriptor, Class, Type, ResultHandle,
     *      AssignableResultHandle, ResultHandle, SortedSet)
     */
    private void writeDeepCloneInstructions(BytecodeCreator bytecodeCreator,
            GizmoSolutionOrEntityDescriptor solutionDescriptor, Field deeplyClonedField,
            GizmoMemberDescriptor gizmoMemberDescriptor, ResultHandle toClone, AssignableResultHandle cloneResultHolder,
            ResultHandle createdCloneMap, SortedSet<Class<?>> deepClonedClassesSortedSet) {
        BranchResult isNull = bytecodeCreator.ifNull(toClone);

        BytecodeCreator isNullBranch = isNull.trueBranch();
        isNullBranch.assign(cloneResultHolder, isNullBranch.loadNull());

        BytecodeCreator isNotNullBranch = isNull.falseBranch();

        Class<?> deeplyClonedFieldClass = deeplyClonedField.getType();
        Type type = gizmoMemberDescriptor.getType();
        if (solutionDescriptor.getSolutionDescriptor().getSolutionClass().isAssignableFrom(deeplyClonedFieldClass)) {
            writeDeepCloneSolutionInstructions(bytecodeCreator, solutionDescriptor, toClone, cloneResultHolder,
                    createdCloneMap);
        } else if (Collection.class.isAssignableFrom(deeplyClonedFieldClass)) {
            writeDeepCloneCollectionInstructions(isNotNullBranch, solutionDescriptor, deeplyClonedFieldClass, type,
                    toClone, cloneResultHolder, createdCloneMap, deepClonedClassesSortedSet);
        } else if (Map.class.isAssignableFrom(deeplyClonedFieldClass)) {
            writeDeepCloneMapInstructions(isNotNullBranch, solutionDescriptor, deeplyClonedFieldClass, type,
                    toClone, cloneResultHolder, createdCloneMap, deepClonedClassesSortedSet);
        } else if (deeplyClonedFieldClass.isArray()) {
            writeDeepCloneArrayInstructions(isNotNullBranch, solutionDescriptor, deeplyClonedFieldClass,
                    toClone, cloneResultHolder, createdCloneMap, deepClonedClassesSortedSet);
        } else {
            UnhandledCloneType unknownClassCloneType =
                    (DeepCloningUtils.isFieldDeepCloned(solutionDescriptor.solutionDescriptor,
                            deeplyClonedField, deeplyClonedField.getDeclaringClass()))
                                    ? UnhandledCloneType.DEEP
                                    : UnhandledCloneType.SHALLOW;
            writeDeepCloneEntityOrFactInstructions(isNotNullBranch, solutionDescriptor, deeplyClonedFieldClass,
                    toClone, cloneResultHolder, createdCloneMap, deepClonedClassesSortedSet, unknownClassCloneType);
        }
    }

    /**
     * Writes the following code:
     *
     * <pre>
     * // For a Collection
     * Collection original = field;
     * Collection clone = new ActualCollectionType();
     * Iterator iterator = original.iterator();
     * while (iterator.hasNext()) {
     *     Object nextClone = (result from recursion on iterator.next());
     *     clone.add(nextClone);
     * }
     *
     * // For a Map
     * Map original = field;
     * Map clone = new ActualMapType();
     * Iterator iterator = original.entrySet().iterator();
     * while (iterator.hasNext()) {
     *      Entry next = iterator.next();
     *      nextClone = (result from recursion on next.getValue());
     *      clone.put(next.getKey(), nextClone);
     * }
     *
     * // For an array
     * Object[] original = field;
     * Object[] clone = new Object[original.length];
     *
     * for (int i = 0; i < original.length; i++) {
     *     clone[i] = (result from recursion on original[i]);
     * }
     *
     * // For an entity
     * if (original instanceof SubclassOfEntity1) {
     *     SubclassOfEntity1 original = field;
     *     SubclassOfEntity1 clone = new SubclassOfEntity1();
     *
     *     // shallowly clone fields using writeShallowCloneInstructions()
     *     // for any deeply cloned field, do recursion on it
     * } else if (original instanceof SubclassOfEntity2) {
     *     // ...
     * }
     * </pre>
     *
     * @param bytecodeCreator
     * @param solutionDescriptor
     * @param deeplyClonedFieldClass
     * @param type
     * @param toClone
     * @param cloneResultHolder
     */
    private void writeDeepCloneInstructions(BytecodeCreator bytecodeCreator,
            GizmoSolutionOrEntityDescriptor solutionDescriptor,
            Class<?> deeplyClonedFieldClass, java.lang.reflect.Type type, ResultHandle toClone,
            AssignableResultHandle cloneResultHolder, ResultHandle createdCloneMap,
            SortedSet<Class<?>> deepClonedClassesSortedSet) {
        BranchResult isNull = bytecodeCreator.ifNull(toClone);

        BytecodeCreator isNullBranch = isNull.trueBranch();
        isNullBranch.assign(cloneResultHolder, isNullBranch.loadNull());

        BytecodeCreator isNotNullBranch = isNull.falseBranch();

        if (solutionDescriptor.getSolutionDescriptor().getSolutionClass().isAssignableFrom(deeplyClonedFieldClass)) {
            writeDeepCloneSolutionInstructions(bytecodeCreator, solutionDescriptor, toClone, cloneResultHolder,
                    createdCloneMap);
        } else if (Collection.class.isAssignableFrom(deeplyClonedFieldClass)) {
            // Clone collection
            writeDeepCloneCollectionInstructions(isNotNullBranch, solutionDescriptor, deeplyClonedFieldClass, type,
                    toClone, cloneResultHolder, createdCloneMap, deepClonedClassesSortedSet);
        } else if (Map.class.isAssignableFrom(deeplyClonedFieldClass)) {
            // Clone map
            writeDeepCloneMapInstructions(isNotNullBranch, solutionDescriptor, deeplyClonedFieldClass, type,
                    toClone, cloneResultHolder, createdCloneMap, deepClonedClassesSortedSet);
        } else if (deeplyClonedFieldClass.isArray()) {
            // Clone array
            writeDeepCloneArrayInstructions(isNotNullBranch, solutionDescriptor, deeplyClonedFieldClass,
                    toClone, cloneResultHolder, createdCloneMap, deepClonedClassesSortedSet);
        } else {
            // Clone entity
            UnhandledCloneType unknownClassCloneType =
                    (DeepCloningUtils.isClassDeepCloned(solutionDescriptor.solutionDescriptor, deeplyClonedFieldClass))
                            ? UnhandledCloneType.DEEP
                            : UnhandledCloneType.SHALLOW;
            writeDeepCloneEntityOrFactInstructions(isNotNullBranch, solutionDescriptor, deeplyClonedFieldClass,
                    toClone, cloneResultHolder, createdCloneMap, deepClonedClassesSortedSet, unknownClassCloneType);
        }
    }

    private void writeDeepCloneSolutionInstructions(BytecodeCreator bytecodeCreator,
            GizmoSolutionOrEntityDescriptor solutionDescriptor, ResultHandle toClone, AssignableResultHandle cloneResultHolder,
            ResultHandle createdCloneMap) {
        BranchResult isNull = bytecodeCreator.ifNull(toClone);

        BytecodeCreator isNullBranch = isNull.trueBranch();
        isNullBranch.assign(cloneResultHolder, isNullBranch.loadNull());

        BytecodeCreator isNotNullBranch = isNull.falseBranch();

        ResultHandle clone = isNotNullBranch.invokeStaticMethod(
                MethodDescriptor.ofMethod(
                        GizmoSolutionClonerFactory.getGeneratedClassName(solutionDescriptor.getSolutionDescriptor()),
                        "cloneSolutionRun", solutionDescriptor.getSolutionDescriptor().getSolutionClass(),
                        solutionDescriptor.getSolutionDescriptor().getSolutionClass(), Map.class),
                toClone,
                createdCloneMap);
        isNotNullBranch.assign(cloneResultHolder, clone);
    }

    /**
     * Writes the following code:
     *
     * <pre>
     * // For a Collection
     * Collection clone = new ActualCollectionType();
     * Iterator iterator = toClone.iterator();
     * while (iterator.hasNext()) {
     *     Object toCloneElement = iterator.next();
     *     Object nextClone = (result from recursion on toCloneElement);
     *     clone.add(nextClone);
     * }
     * cloneResultHolder = clone;
     * </pre>
     **/
    private void writeDeepCloneCollectionInstructions(BytecodeCreator bytecodeCreator,
            GizmoSolutionOrEntityDescriptor solutionDescriptor,
            Class<?> deeplyClonedFieldClass, java.lang.reflect.Type type, ResultHandle toClone,
            AssignableResultHandle cloneResultHolder, ResultHandle createdCloneMap,
            SortedSet<Class<?>> deepClonedClassesSortedSet) {
        // Clone collection
        AssignableResultHandle cloneCollection = bytecodeCreator.createVariable(deeplyClonedFieldClass);

        ResultHandle size = bytecodeCreator
                .invokeInterfaceMethod(MethodDescriptor.ofMethod(Collection.class, "size", int.class), toClone);

        if (PlanningCloneable.class.isAssignableFrom(deeplyClonedFieldClass)) {
            var emptyInstance = bytecodeCreator
                    .invokeInterfaceMethod(MethodDescriptor.ofMethod(PlanningCloneable.class, "createNewInstance",
                            Object.class), bytecodeCreator.checkCast(toClone, PlanningCloneable.class));
            bytecodeCreator.assign(cloneCollection,
                    bytecodeCreator.checkCast(emptyInstance,
                            Collection.class));
        } else if (List.class.isAssignableFrom(deeplyClonedFieldClass)) {
            bytecodeCreator.assign(cloneCollection,
                    bytecodeCreator.newInstance(MethodDescriptor.ofConstructor(ArrayList.class, int.class), size));
        } else if (Set.class.isAssignableFrom(deeplyClonedFieldClass)) {
            ResultHandle isSortedSet = bytecodeCreator.instanceOf(toClone, SortedSet.class);
            BranchResult isSortedSetBranchResult = bytecodeCreator.ifTrue(isSortedSet);
            BytecodeCreator isSortedSetBranch = isSortedSetBranchResult.trueBranch();
            ResultHandle setComparator = isSortedSetBranch
                    .invokeInterfaceMethod(MethodDescriptor.ofMethod(SortedSet.class,
                            "comparator", Comparator.class), toClone);
            isSortedSetBranch.assign(cloneCollection,
                    isSortedSetBranch.newInstance(MethodDescriptor.ofConstructor(TreeSet.class, Comparator.class),
                            setComparator));
            BytecodeCreator isNotSortedSetBranch = isSortedSetBranchResult.falseBranch();
            isNotSortedSetBranch.assign(cloneCollection,
                    isNotSortedSetBranch.newInstance(MethodDescriptor.ofConstructor(LinkedHashSet.class, int.class), size));
        } else {
            // field is probably of type collection
            ResultHandle isSet = bytecodeCreator.instanceOf(toClone, Set.class);
            BranchResult isSetBranchResult = bytecodeCreator.ifTrue(isSet);
            BytecodeCreator isSetBranch = isSetBranchResult.trueBranch();
            ResultHandle isSortedSet = isSetBranch.instanceOf(toClone, SortedSet.class);
            BranchResult isSortedSetBranchResult = isSetBranch.ifTrue(isSortedSet);
            BytecodeCreator isSortedSetBranch = isSortedSetBranchResult.trueBranch();
            ResultHandle setComparator = isSortedSetBranch
                    .invokeInterfaceMethod(MethodDescriptor.ofMethod(SortedSet.class,
                            "comparator", Comparator.class), toClone);
            isSortedSetBranch.assign(cloneCollection,
                    isSortedSetBranch.newInstance(MethodDescriptor.ofConstructor(TreeSet.class, Comparator.class),
                            setComparator));
            BytecodeCreator isNotSortedSetBranch = isSortedSetBranchResult.falseBranch();
            isNotSortedSetBranch.assign(cloneCollection,
                    isNotSortedSetBranch.newInstance(MethodDescriptor.ofConstructor(LinkedHashSet.class, int.class), size));
            // Default to ArrayList
            BytecodeCreator isNotSetBranch = isSetBranchResult.falseBranch();
            isNotSetBranch.assign(cloneCollection,
                    isNotSetBranch.newInstance(MethodDescriptor.ofConstructor(ArrayList.class, int.class), size));
        }
        ResultHandle iterator = bytecodeCreator
                .invokeInterfaceMethod(MethodDescriptor.ofMethod(Iterable.class, "iterator", Iterator.class), toClone);

        BytecodeCreator whileLoopBlock = bytecodeCreator.whileLoop(conditionBytecode -> {
            ResultHandle hasNext = conditionBytecode
                    .invokeInterfaceMethod(MethodDescriptor.ofMethod(Iterator.class, "hasNext", boolean.class), iterator);
            return conditionBytecode.ifTrue(hasNext);
        }).block();

        Class<?> elementClass;
        java.lang.reflect.Type elementClassType;
        if (type instanceof ParameterizedType parameterizedType) {
            // Assume Collection follow Collection<T> convention of first type argument = element class
            elementClassType = parameterizedType.getActualTypeArguments()[0];
            if (elementClassType instanceof Class<?> class1) {
                elementClass = class1;
            } else if (elementClassType instanceof ParameterizedType parameterizedElementClassType) {
                elementClass = (Class<?>) parameterizedElementClassType.getRawType();
            } else if (elementClassType instanceof WildcardType wildcardType) {
                elementClass = (Class<?>) wildcardType.getUpperBounds()[0];
            } else {
                throw new IllegalStateException("Unhandled type " + elementClassType + ".");
            }
        } else {
            throw new IllegalStateException("Cannot infer element type for Collection type (" + type + ").");
        }

        // Odd case of member get and set being on different classes; will work as we only
        // use get on the original and set on the clone.
        ResultHandle next =
                whileLoopBlock.invokeInterfaceMethod(MethodDescriptor.ofMethod(Iterator.class, "next", Object.class), iterator);
        final AssignableResultHandle clonedElement = whileLoopBlock.createVariable(elementClass);
        writeDeepCloneInstructions(whileLoopBlock, solutionDescriptor,
                elementClass, elementClassType, next, clonedElement, createdCloneMap, deepClonedClassesSortedSet);
        whileLoopBlock.invokeInterfaceMethod(MethodDescriptor.ofMethod(Collection.class, "add", boolean.class, Object.class),
                cloneCollection,
                clonedElement);
        bytecodeCreator.assign(cloneResultHolder, cloneCollection);
    }

    /**
     * Writes the following code:
     *
     * <pre>
     * // For a Map
     * Map clone = new ActualMapType();
     * Iterator iterator = toClone.entrySet().iterator();
     * while (iterator.hasNext()) {
     *      Entry next = iterator.next();
     *      Object toCloneValue = next.getValue();
     *      nextClone = (result from recursion on toCloneValue);
     *      clone.put(next.getKey(), nextClone);
     * }
     * cloneResultHolder = clone;
     * </pre>
     **/
    private void writeDeepCloneMapInstructions(BytecodeCreator bytecodeCreator,
            GizmoSolutionOrEntityDescriptor solutionDescriptor,
            Class<?> deeplyClonedFieldClass, java.lang.reflect.Type type, ResultHandle toClone,
            AssignableResultHandle cloneResultHolder, ResultHandle createdCloneMap,
            SortedSet<Class<?>> deepClonedClassesSortedSet) {
        ResultHandle cloneMap;
        if (PlanningCloneable.class.isAssignableFrom(deeplyClonedFieldClass)) {
            var emptyInstance = bytecodeCreator
                    .invokeInterfaceMethod(MethodDescriptor.ofMethod(PlanningCloneable.class, "createNewInstance",
                            Object.class), bytecodeCreator.checkCast(toClone, PlanningCloneable.class));
            cloneMap = bytecodeCreator.checkCast(emptyInstance, Map.class);
        } else {
            Class<?> holderClass = deeplyClonedFieldClass;
            try {
                holderClass.getConstructor();
            } catch (NoSuchMethodException e) {
                if (LinkedHashMap.class.isAssignableFrom(holderClass)) {
                    holderClass = LinkedHashMap.class;
                } else if (ConcurrentHashMap.class.isAssignableFrom(holderClass)) {
                    holderClass = ConcurrentHashMap.class;
                } else {
                    // Default to LinkedHashMap
                    holderClass = LinkedHashMap.class;
                }
            }

            ResultHandle size =
                    bytecodeCreator.invokeInterfaceMethod(MethodDescriptor.ofMethod(Map.class, "size", int.class), toClone);
            try {
                holderClass.getConstructor(int.class);
                cloneMap = bytecodeCreator.newInstance(MethodDescriptor.ofConstructor(holderClass, int.class), size);
            } catch (NoSuchMethodException e) {
                cloneMap = bytecodeCreator.newInstance(MethodDescriptor.ofConstructor(holderClass));
            }
        }

        ResultHandle entrySet = bytecodeCreator
                .invokeInterfaceMethod(MethodDescriptor.ofMethod(Map.class, "entrySet", Set.class), toClone);
        ResultHandle iterator = bytecodeCreator
                .invokeInterfaceMethod(MethodDescriptor.ofMethod(Iterable.class, "iterator", Iterator.class), entrySet);

        BytecodeCreator whileLoopBlock = bytecodeCreator.whileLoop(conditionBytecode -> {
            ResultHandle hasNext = conditionBytecode
                    .invokeInterfaceMethod(MethodDescriptor.ofMethod(Iterator.class, "hasNext", boolean.class), iterator);
            return conditionBytecode.ifTrue(hasNext);
        }).block();

        Class<?> keyClass;
        Class<?> elementClass;
        java.lang.reflect.Type keyType;
        java.lang.reflect.Type elementClassType;
        if (type instanceof ParameterizedType parameterizedType) {
            // Assume Map follow Map<K,V> convention of second type argument = value class
            keyType = parameterizedType.getActualTypeArguments()[0];
            elementClassType = parameterizedType.getActualTypeArguments()[1];
            if (elementClassType instanceof Class<?> class1) {
                elementClass = class1;
            } else if (elementClassType instanceof ParameterizedType parameterizedElementClassType) {
                elementClass = (Class<?>) parameterizedElementClassType.getRawType();
            } else {
                throw new IllegalStateException("Unhandled type " + elementClassType + ".");
            }

            if (keyType instanceof Class<?> class1) {
                keyClass = class1;
            } else if (keyType instanceof ParameterizedType parameterizedElementClassType) {
                keyClass = (Class<?>) parameterizedElementClassType.getRawType();
            } else {
                throw new IllegalStateException("Unhandled type " + keyType + ".");
            }
        } else {
            throw new IllegalStateException("Cannot infer element type for Map type (" + type + ").");
        }

        List<Class<?>> entitySubclasses = deepClonedClassesSortedSet.stream()
                .filter(keyClass::isAssignableFrom).toList();
        ResultHandle entry = whileLoopBlock
                .invokeInterfaceMethod(MethodDescriptor.ofMethod(Iterator.class, "next", Object.class), iterator);
        ResultHandle toCloneValue = whileLoopBlock
                .invokeInterfaceMethod(MethodDescriptor.ofMethod(Map.Entry.class, "getValue", Object.class), entry);

        final AssignableResultHandle clonedElement = whileLoopBlock.createVariable(elementClass);
        writeDeepCloneInstructions(whileLoopBlock, solutionDescriptor,
                elementClass, elementClassType, toCloneValue, clonedElement, createdCloneMap, deepClonedClassesSortedSet);

        ResultHandle key = whileLoopBlock
                .invokeInterfaceMethod(MethodDescriptor.ofMethod(Map.Entry.class, "getKey", Object.class), entry);
        if (!entitySubclasses.isEmpty()) {
            AssignableResultHandle keyCloneResultHolder = whileLoopBlock.createVariable(keyClass);
            writeDeepCloneEntityOrFactInstructions(whileLoopBlock, solutionDescriptor, keyClass,
                    key, keyCloneResultHolder, createdCloneMap, deepClonedClassesSortedSet, UnhandledCloneType.DEEP);
            whileLoopBlock.invokeInterfaceMethod(
                    PUT_METHOD,
                    cloneMap, keyCloneResultHolder, clonedElement);
        } else {
            whileLoopBlock.invokeInterfaceMethod(
                    PUT_METHOD,
                    cloneMap, key, clonedElement);
        }

        bytecodeCreator.assign(cloneResultHolder, cloneMap);
    }

    /**
     * Writes the following code:
     *
     * <pre>
     * // For an array
     * Object[] clone = new Object[toClone.length];
     *
     * for (int i = 0; i < original.length; i++) {
     *     clone[i] = (result from recursion on toClone[i]);
     * }
     * cloneResultHolder = clone;
     * </pre>
     **/
    private void writeDeepCloneArrayInstructions(BytecodeCreator bytecodeCreator,
            GizmoSolutionOrEntityDescriptor solutionDescriptor,
            Class<?> deeplyClonedFieldClass, ResultHandle toClone, AssignableResultHandle cloneResultHolder,
            ResultHandle createdCloneMap, SortedSet<Class<?>> deepClonedClassesSortedSet) {
        // Clone array
        Class<?> arrayComponent = deeplyClonedFieldClass.getComponentType();
        ResultHandle arrayLength = bytecodeCreator.arrayLength(toClone);
        ResultHandle arrayClone = bytecodeCreator.newArray(arrayComponent, arrayLength);
        AssignableResultHandle iterations = bytecodeCreator.createVariable(int.class);
        bytecodeCreator.assign(iterations, bytecodeCreator.load(0));
        BytecodeCreator whileLoopBlock = bytecodeCreator
                .whileLoop(conditionBytecode -> conditionBytecode.ifIntegerLessThan(iterations, arrayLength))
                .block();
        ResultHandle toCloneElement = whileLoopBlock.readArrayValue(toClone, iterations);
        AssignableResultHandle clonedElement = whileLoopBlock.createVariable(arrayComponent);

        writeDeepCloneInstructions(whileLoopBlock, solutionDescriptor, arrayComponent,
                arrayComponent, toCloneElement, clonedElement, createdCloneMap, deepClonedClassesSortedSet);
        whileLoopBlock.writeArrayValue(arrayClone, iterations, clonedElement);
        whileLoopBlock.assign(iterations, whileLoopBlock.increment(iterations));

        bytecodeCreator.assign(cloneResultHolder, arrayClone);
    }

    /**
     * Writes the following code:
     *
     * <pre>
     * // For an entity
     * if (toClone instanceof SubclassOfEntity1) {
     *     SubclassOfEntity1 clone = new SubclassOfEntity1();
     *
     *     // shallowly clone fields using writeShallowCloneInstructions()
     *     // for any deeply cloned field, do recursion on it
     *     cloneResultHolder = clone;
     * } else if (toClone instanceof SubclassOfEntity2) {
     *     // ...
     * }
     * // ...
     * else if (toClone instanceof SubclassOfEntityN) {
     *     // ...
     * } else {
     *     // shallow or deep clone based on whether deep cloning is forced
     * }
     * </pre>
     **/
    private void writeDeepCloneEntityOrFactInstructions(BytecodeCreator bytecodeCreator,
            GizmoSolutionOrEntityDescriptor solutionDescriptor, Class<?> deeplyClonedFieldClass, ResultHandle toClone,
            AssignableResultHandle cloneResultHolder, ResultHandle createdCloneMap,
            SortedSet<Class<?>> deepClonedClassesSortedSet, UnhandledCloneType unhandledCloneType) {
        List<Class<?>> deepClonedSubclasses = deepClonedClassesSortedSet.stream()
                .filter(deeplyClonedFieldClass::isAssignableFrom)
                .filter(type -> DeepCloningUtils.isClassDeepCloned(solutionDescriptor.getSolutionDescriptor(), type))
                .toList();
        BytecodeCreator currentBranch = bytecodeCreator;
        // If the field holds an instance of one of the field's declared type's subtypes, clone the subtype instead.
        for (Class<?> deepClonedSubclass : deepClonedSubclasses) {
            ResultHandle isInstance = currentBranch.instanceOf(toClone, deepClonedSubclass);
            BranchResult isInstanceBranchResult = currentBranch.ifTrue(isInstance);
            BytecodeCreator isInstanceBranch = isInstanceBranchResult.trueBranch();
            ResultHandle cloneObj = isInstanceBranch.invokeStaticMethod(
                    MethodDescriptor.ofMethod(
                            GizmoSolutionClonerFactory.getGeneratedClassName(solutionDescriptor.getSolutionDescriptor()),
                            getEntityHelperMethodName(deepClonedSubclass), deepClonedSubclass, deepClonedSubclass, Map.class),
                    toClone, createdCloneMap);
            isInstanceBranch.assign(cloneResultHolder, cloneObj);
            currentBranch = isInstanceBranchResult.falseBranch();
        }
        // We are certain that the instance is of the same type as the declared field type.
        // (Or is an undeclared subclass of the planning entity)
        switch (unhandledCloneType) {
            case SHALLOW -> currentBranch.assign(cloneResultHolder, toClone);
            case DEEP -> {
                ResultHandle cloneObj = currentBranch.invokeStaticMethod(
                        MethodDescriptor.ofMethod(
                                GizmoSolutionClonerFactory.getGeneratedClassName(solutionDescriptor.getSolutionDescriptor()),
                                getEntityHelperMethodName(deeplyClonedFieldClass), deeplyClonedFieldClass,
                                deeplyClonedFieldClass, Map.class),
                        toClone, createdCloneMap);
                currentBranch.assign(cloneResultHolder, cloneObj);
            }
        }
    }

    protected String getEntityHelperMethodName(Class<?> entityClass) {
        return "$clone" + entityClass.getName().replace('.', '_');
    }

    /**
     * Writes the following code:
     * <p>
     * In Quarkus: (nothing)
     * <p>
     * Outside Quarkus:
     *
     * <pre>
     * if (toClone.getClass() != entityClass) {
     *     Cloner.fallbackCloner.gizmoFallbackDeepClone(toClone, cloneMap);
     * } else {
     *     ...
     * }
     * </pre>
     *
     * @return The else branch {@link BytecodeCreator} outside of Quarkus, the original bytecodeCreator otherwise.
     */
    protected BytecodeCreator createUnknownClassHandler(BytecodeCreator bytecodeCreator,
            SolutionDescriptor<?> solutionDescriptor,
            Class<?> entityClass,
            ResultHandle toClone,
            ResultHandle cloneMap) {
        ResultHandle actualClass =
                bytecodeCreator.invokeVirtualMethod(MethodDescriptor.ofMethod(Object.class, "getClass", Class.class),
                        toClone);
        BranchResult branchResult = bytecodeCreator.ifReferencesNotEqual(actualClass,
                bytecodeCreator.loadClass(entityClass));

        BytecodeCreator currentBranch = branchResult.trueBranch();

        ResultHandle fallbackCloner =
                currentBranch.readStaticField(FieldDescriptor.of(
                        GizmoSolutionClonerFactory.getGeneratedClassName(solutionDescriptor),
                        FALLBACK_CLONER, FieldAccessingSolutionCloner.class));
        ResultHandle cloneObj =
                currentBranch.invokeVirtualMethod(MethodDescriptor.ofMethod(FieldAccessingSolutionCloner.class,
                        "gizmoFallbackDeepClone", Object.class, Object.class, Map.class),
                        fallbackCloner, toClone, cloneMap);
        currentBranch.returnValue(cloneObj);

        return branchResult.falseBranch();
    }

    // To prevent stack overflow on chained models
    private void createDeepCloneHelperMethod(ClassCreator classCreator,
            Class<?> entityClass,
            SolutionDescriptor<?> solutionDescriptor,
            Map<Class<?>, GizmoSolutionOrEntityDescriptor> memoizedSolutionOrEntityDescriptorMap,
            SortedSet<Class<?>> deepClonedClassesSortedSet) {
        MethodCreator methodCreator =
                classCreator.getMethodCreator(getEntityHelperMethodName(entityClass), entityClass, entityClass, Map.class);
        methodCreator.setModifiers(Modifier.STATIC | Modifier.PRIVATE);

        GizmoSolutionOrEntityDescriptor entityDescriptor = memoizedSolutionOrEntityDescriptorMap.computeIfAbsent(entityClass,
                key -> new GizmoSolutionOrEntityDescriptor(solutionDescriptor, entityClass));

        ResultHandle toClone = methodCreator.getMethodParam(0);
        ResultHandle cloneMap = methodCreator.getMethodParam(1);
        ResultHandle maybeClone = methodCreator.invokeInterfaceMethod(
                GET_METHOD, cloneMap, toClone);
        BranchResult hasCloneBranchResult = methodCreator.ifNotNull(maybeClone);
        BytecodeCreator hasCloneBranch = hasCloneBranchResult.trueBranch();
        hasCloneBranch.returnValue(maybeClone);

        BytecodeCreator noCloneBranch = hasCloneBranchResult.falseBranch();
        noCloneBranch = createUnknownClassHandler(noCloneBranch,
                solutionDescriptor,
                entityClass,
                toClone,
                cloneMap);

        ResultHandle cloneObj;
        if (PlanningCloneable.class.isAssignableFrom(entityClass)) {
            cloneObj = noCloneBranch.invokeInterfaceMethod(
                    MethodDescriptor.ofMethod(PlanningCloneable.class, "createNewInstance", Object.class),
                    toClone);
            cloneObj = noCloneBranch.checkCast(cloneObj, entityClass);
        } else {
            cloneObj = noCloneBranch.newInstance(MethodDescriptor.ofConstructor(entityClass));
        }
        noCloneBranch.invokeInterfaceMethod(
                MethodDescriptor.ofMethod(Map.class, "put", Object.class, Object.class, Object.class),
                cloneMap, toClone, cloneObj);

        for (GizmoMemberDescriptor shallowlyClonedField : entityDescriptor.getShallowClonedMemberDescriptors()) {
            writeShallowCloneInstructions(entityDescriptor, noCloneBranch, shallowlyClonedField, toClone, cloneObj, cloneMap,
                    deepClonedClassesSortedSet);
        }

        for (Field deeplyClonedField : entityDescriptor.getDeepClonedFields()) {
            GizmoMemberDescriptor gizmoMemberDescriptor =
                    entityDescriptor.getMemberDescriptorForField(deeplyClonedField);
            ResultHandle subfieldValue = gizmoMemberDescriptor.readMemberValue(noCloneBranch, toClone);

            AssignableResultHandle cloneValue = noCloneBranch.createVariable(deeplyClonedField.getType());
            writeDeepCloneInstructions(noCloneBranch, entityDescriptor, deeplyClonedField, gizmoMemberDescriptor, subfieldValue,
                    cloneValue, cloneMap, deepClonedClassesSortedSet);

            if (!gizmoMemberDescriptor.writeMemberValue(noCloneBranch, cloneObj, cloneValue)) {
                throw new IllegalStateException("The member (" + gizmoMemberDescriptor.getName() + ") of class (" +
                        gizmoMemberDescriptor.getDeclaringClassName() + ") does not have a setter.");
            }
        }

        noCloneBranch.returnValue(cloneObj);
    }

    protected void createAbstractDeepCloneHelperMethod(ClassCreator classCreator,
            Class<?> entityClass,
            SolutionDescriptor<?> solutionDescriptor,
            Map<Class<?>, GizmoSolutionOrEntityDescriptor> memoizedSolutionOrEntityDescriptorMap,
            SortedSet<Class<?>> deepClonedClassesSortedSet) {
        MethodCreator methodCreator =
                classCreator.getMethodCreator(getEntityHelperMethodName(entityClass), entityClass, entityClass, Map.class);
        methodCreator.setModifiers(Modifier.STATIC | Modifier.PRIVATE);

        ResultHandle toClone = methodCreator.getMethodParam(0);
        ResultHandle cloneMap = methodCreator.getMethodParam(1);
        ResultHandle maybeClone = methodCreator.invokeInterfaceMethod(
                GET_METHOD, cloneMap, toClone);
        BranchResult hasCloneBranchResult = methodCreator.ifNotNull(maybeClone);
        BytecodeCreator hasCloneBranch = hasCloneBranchResult.trueBranch();
        hasCloneBranch.returnValue(maybeClone);

        BytecodeCreator noCloneBranch = hasCloneBranchResult.falseBranch();
        ResultHandle fallbackCloner =
                noCloneBranch.readStaticField(FieldDescriptor.of(
                        GizmoSolutionClonerFactory.getGeneratedClassName(solutionDescriptor),
                        FALLBACK_CLONER, FieldAccessingSolutionCloner.class));
        ResultHandle cloneObj =
                noCloneBranch.invokeVirtualMethod(MethodDescriptor.ofMethod(FieldAccessingSolutionCloner.class,
                        "gizmoFallbackDeepClone", Object.class, Object.class, Map.class),
                        fallbackCloner, toClone, cloneMap);
        noCloneBranch.returnValue(cloneObj);
    }

    private enum UnhandledCloneType {
        SHALLOW,
        DEEP
    }
}
