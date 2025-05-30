package ai.timefold.solver.spring.boot.autoconfigure.inheritance.solution;

import ai.timefold.solver.core.testconstraint.DummyConstraintProvider;
import ai.timefold.solver.core.testdomain.inheritance.solution.baseannotated.replacemember.TestdataReplaceMemberExtendedSolution;

import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@AutoConfigurationPackage
@EntityScan(basePackageClasses = { TestdataReplaceMemberExtendedSolution.class, DummyConstraintProvider.class })
public class ReplaceAnnotatedMemberSpringTestConfiguration {

}
