package ai.timefold.solver.core.config.heuristic.selector.move.generic;

import java.util.List;
import java.util.function.Consumer;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlType;

import ai.timefold.solver.core.config.heuristic.selector.entity.pillar.PillarSelectorConfig;
import ai.timefold.solver.core.config.util.ConfigUtils;

@XmlType(propOrder = {
        "secondaryPillarSelectorConfig",
        "variableNameIncludeList"
})
public class PillarSwapMoveSelectorConfig extends AbstractPillarMoveSelectorConfig<PillarSwapMoveSelectorConfig> {

    public static final String XML_ELEMENT_NAME = "pillarSwapMoveSelector";

    @XmlElement(name = "secondaryPillarSelector")
    private PillarSelectorConfig secondaryPillarSelectorConfig = null;

    @XmlElementWrapper(name = "variableNameIncludes")
    @XmlElement(name = "variableNameInclude")
    private List<String> variableNameIncludeList = null;

    public PillarSelectorConfig getSecondaryPillarSelectorConfig() {
        return secondaryPillarSelectorConfig;
    }

    public void setSecondaryPillarSelectorConfig(PillarSelectorConfig secondaryPillarSelectorConfig) {
        this.secondaryPillarSelectorConfig = secondaryPillarSelectorConfig;
    }

    public List<String> getVariableNameIncludeList() {
        return variableNameIncludeList;
    }

    public void setVariableNameIncludeList(List<String> variableNameIncludeList) {
        this.variableNameIncludeList = variableNameIncludeList;
    }

    // ************************************************************************
    // With methods
    // ************************************************************************

    public PillarSwapMoveSelectorConfig withSecondaryPillarSelectorConfig(PillarSelectorConfig pillarSelectorConfig) {
        this.setSecondaryPillarSelectorConfig(pillarSelectorConfig);
        return this;
    }

    public PillarSwapMoveSelectorConfig withVariableNameIncludeList(List<String> variableNameIncludeList) {
        this.setVariableNameIncludeList(variableNameIncludeList);
        return this;
    }

    public PillarSwapMoveSelectorConfig withVariableNameIncludes(String... variableNameIncludes) {
        this.setVariableNameIncludeList(List.of(variableNameIncludes));
        return this;
    }

    @Override
    public PillarSwapMoveSelectorConfig inherit(PillarSwapMoveSelectorConfig inheritedConfig) {
        super.inherit(inheritedConfig);
        secondaryPillarSelectorConfig = ConfigUtils.inheritConfig(secondaryPillarSelectorConfig,
                inheritedConfig.getSecondaryPillarSelectorConfig());
        variableNameIncludeList = ConfigUtils.inheritMergeableListProperty(
                variableNameIncludeList, inheritedConfig.getVariableNameIncludeList());
        return this;
    }

    @Override
    public PillarSwapMoveSelectorConfig copyConfig() {
        return new PillarSwapMoveSelectorConfig().inherit(this);
    }

    @Override
    public void visitReferencedClasses(Consumer<Class<?>> classVisitor) {
        visitCommonReferencedClasses(classVisitor);
        if (secondaryPillarSelectorConfig != null) {
            secondaryPillarSelectorConfig.visitReferencedClasses(classVisitor);
        }
    }

    @Override
    public boolean hasNearbySelectionConfig() {
        return (pillarSelectorConfig != null && pillarSelectorConfig.hasNearbySelectionConfig())
                || (secondaryPillarSelectorConfig != null && secondaryPillarSelectorConfig.hasNearbySelectionConfig());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + pillarSelectorConfig
                + (secondaryPillarSelectorConfig == null ? "" : ", " + secondaryPillarSelectorConfig) + ")";
    }

}
