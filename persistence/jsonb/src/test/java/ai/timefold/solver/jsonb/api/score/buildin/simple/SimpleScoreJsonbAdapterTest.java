package ai.timefold.solver.jsonb.api.score.buildin.simple;

import jakarta.json.bind.annotation.JsonbTypeAdapter;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.jsonb.api.score.AbstractScoreJsonbAdapterTest;

import org.junit.jupiter.api.Test;

class SimpleScoreJsonbAdapterTest extends AbstractScoreJsonbAdapterTest {

    @Test
    void serializeAndDeserialize() {
        assertSerializeAndDeserialize(null, new TestSimpleScoreWrapper(null));
        SimpleScore score = SimpleScore.of(1234);
        assertSerializeAndDeserialize(score, new TestSimpleScoreWrapper(score));
    }

    public static class TestSimpleScoreWrapper extends TestScoreWrapper<SimpleScore> {

        @JsonbTypeAdapter(SimpleScoreJsonbAdapter.class)
        private SimpleScore score;

        // Empty constructor required by JSON-B
        @SuppressWarnings("unused")
        public TestSimpleScoreWrapper() {
        }

        public TestSimpleScoreWrapper(SimpleScore score) {
            this.score = score;
        }

        @Override
        public SimpleScore getScore() {
            return score;
        }

        @Override
        public void setScore(SimpleScore score) {
            this.score = score;
        }

    }
}
