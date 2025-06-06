package ai.timefold.solver.core.impl.score.constraint;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.constraint.ConstraintMatch;
import ai.timefold.solver.core.api.score.constraint.ConstraintRef;
import ai.timefold.solver.core.api.score.stream.ConstraintJustification;
import ai.timefold.solver.core.api.score.stream.DefaultConstraintJustification;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testutil.PlannerAssert;

import org.junit.jupiter.api.Test;

class DefaultIndictmentTest {

    @Test
    void getScoreTotal() {
        TestdataEntity e1 = new TestdataEntity("e1");
        TestdataEntity e2 = new TestdataEntity("e2");
        TestdataEntity e3 = new TestdataEntity("e3");
        DefaultIndictment<SimpleScore> indictment = new DefaultIndictment<>(e1, SimpleScore.ZERO);
        assertThat(indictment.getScore()).isEqualTo(SimpleScore.ZERO);

        ConstraintMatch<SimpleScore> match1 = buildConstraintMatch("package1", "constraint1", SimpleScore.of(-1), e1);
        indictment.addConstraintMatch(match1);
        assertThat(indictment.getScore()).isEqualTo(SimpleScore.of(-1));
        // Different constraintName
        ConstraintMatch<SimpleScore> match2 = buildConstraintMatch("package1", "constraint2", SimpleScore.of(-20), e1);
        indictment.addConstraintMatch(match2);
        assertThat(indictment.getScore()).isEqualTo(SimpleScore.of(-21));
        indictment.addConstraintMatch(buildConstraintMatch("package1", "constraint3", SimpleScore.of(-300), e1, e2));
        assertThat(indictment.getScore()).isEqualTo(SimpleScore.of(-321));
        // Different justification
        indictment.addConstraintMatch(buildConstraintMatch("package1", "constraint3", SimpleScore.of(-4000), e1, e3));
        assertThat(indictment.getScore()).isEqualTo(SimpleScore.of(-4321));
        // Almost duplicate, but e2 and e1 are in reverse order, so different justification
        indictment.addConstraintMatch(buildConstraintMatch("package1", "constraint3", SimpleScore.of(-50000), e2, e1));
        assertThat(indictment.getScore()).isEqualTo(SimpleScore.of(-54321));

        indictment.removeConstraintMatch(match2);
        assertThat(indictment.getScore()).isEqualTo(SimpleScore.of(-54301));
        indictment.removeConstraintMatch(match1);
        assertThat(indictment.getScore()).isEqualTo(SimpleScore.of(-54300));
    }

    @Test
    void getJustificationList() {
        TestdataEntity e1 = new TestdataEntity("e1");
        TestdataEntity e2 = new TestdataEntity("e2");
        TestdataEntity e3 = new TestdataEntity("e3");
        DefaultIndictment<SimpleScore> indictment = new DefaultIndictment<>(e1, SimpleScore.ZERO);
        assertThat(indictment.getScore()).isEqualTo(SimpleScore.ZERO);

        // Add a constraint match with a default justification
        ConstraintMatch<SimpleScore> match1 = buildConstraintMatch("package1", "constraint1", SimpleScore.of(-1), e1, e2);
        indictment.addConstraintMatch(match1);

        assertThat(indictment.getJustificationList())
                .hasSize(1);
        DefaultConstraintJustification constraintJustification =
                (DefaultConstraintJustification) indictment.getJustificationList().get(0);
        assertThat(constraintJustification.getFacts())
                .containsExactly(e1, e2);

        assertThat(indictment.getJustificationList(DefaultConstraintJustification.class))
                .hasSize(1);
        constraintJustification = indictment.getJustificationList(DefaultConstraintJustification.class).get(0);
        assertThat(constraintJustification.getFacts())
                .containsExactly(e1, e2);

        // Add another constraint match with a custom justification
        ConstraintMatch<SimpleScore> match2 = buildConstraintMatch("package1", "constraint1",
                SimpleScore.of(-1), new TestConstraintJustification(e1, e3), e1, e3);
        indictment.addConstraintMatch(match2);

        assertThat(indictment.getJustificationList())
                .hasSize(2);
        DefaultConstraintJustification firstConstraintJustification =
                (DefaultConstraintJustification) indictment.getJustificationList().get(0);
        assertThat(firstConstraintJustification.getFacts())
                .containsExactly(e1, e2);
        TestConstraintJustification secondConstraintJustification =
                (TestConstraintJustification) indictment.getJustificationList().get(1);
        assertThat(secondConstraintJustification.getFacts())
                .containsExactly(e1, e3);

        assertThat(indictment.getJustificationList(DefaultConstraintJustification.class))
                .hasSize(1);
        firstConstraintJustification = indictment.getJustificationList(DefaultConstraintJustification.class).get(0);
        assertThat(firstConstraintJustification.getFacts())
                .containsExactly(e1, e2);

        assertThat(indictment.getJustificationList(TestConstraintJustification.class))
                .hasSize(1);
        secondConstraintJustification = indictment.getJustificationList(TestConstraintJustification.class).get(0);
        assertThat(secondConstraintJustification.getFacts())
                .containsExactly(e1, e3);
    }

    private <Score_ extends Score<Score_>> ConstraintMatch<Score_> buildConstraintMatch(String constraintPackage,
            String constraintName, Score_ score, Object... indictments) {
        return buildConstraintMatch(constraintPackage, constraintName, score,
                DefaultConstraintJustification.of(score, indictments), indictments);
    }

    private <Score_ extends Score<Score_>> ConstraintMatch<Score_> buildConstraintMatch(String constraintPackage,
            String constraintName, Score_ score, ConstraintJustification justification, Object... indictments) {
        return new ConstraintMatch<>(ConstraintRef.of(constraintPackage, constraintName), justification,
                Arrays.asList(indictments), score);
    }

    @Test
    void equalsAndHashCode() {
        PlannerAssert.assertObjectsAreEqual(
                new DefaultIndictment<>("e1", SimpleScore.ZERO),
                new DefaultIndictment<>("e1", SimpleScore.ZERO),
                new DefaultIndictment<>("e1", SimpleScore.of(-7)));
        PlannerAssert.assertObjectsAreNotEqual(
                new DefaultIndictment<>("a", SimpleScore.ZERO),
                new DefaultIndictment<>("aa", SimpleScore.ZERO),
                new DefaultIndictment<>("b", SimpleScore.ZERO),
                new DefaultIndictment<>("c", SimpleScore.ZERO));
    }

    private final class TestConstraintJustification implements ConstraintJustification {

        private final List<Object> facts;

        public TestConstraintJustification(Object... facts) {
            this.facts = Arrays.asList(facts);
        }

        public List<Object> getFacts() {
            return facts;
        }
    }

}
