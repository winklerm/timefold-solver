package ai.timefold.solver.jsonb.api.score.buildin.hardsoftbigdecimal;

import java.math.BigDecimal;

import jakarta.json.bind.annotation.JsonbTypeAdapter;

import ai.timefold.solver.core.api.score.buildin.hardsoftbigdecimal.HardSoftBigDecimalScore;
import ai.timefold.solver.jsonb.api.score.AbstractScoreJsonbAdapterTest;

import org.junit.jupiter.api.Test;

class HardSoftBigDecimalScoreJsonbAdapterTest extends AbstractScoreJsonbAdapterTest {

    @Test
    void serializeAndDeserialize() {
        assertSerializeAndDeserialize(null, new TestHardSoftBigDecimalScoreWrapper(null));
        HardSoftBigDecimalScore score = HardSoftBigDecimalScore.of(new BigDecimal("1200.0021"), new BigDecimal("34.4300"));
        assertSerializeAndDeserialize(score, new TestHardSoftBigDecimalScoreWrapper(score));
    }

    public static class TestHardSoftBigDecimalScoreWrapper extends TestScoreWrapper<HardSoftBigDecimalScore> {

        @JsonbTypeAdapter(HardSoftBigDecimalScoreJsonbAdapter.class)
        private HardSoftBigDecimalScore score;

        // Empty constructor required by JSON-B
        @SuppressWarnings("unused")
        public TestHardSoftBigDecimalScoreWrapper() {
        }

        public TestHardSoftBigDecimalScoreWrapper(HardSoftBigDecimalScore score) {
            this.score = score;
        }

        @Override
        public HardSoftBigDecimalScore getScore() {
            return score;
        }

        @Override
        public void setScore(HardSoftBigDecimalScore score) {
            this.score = score;
        }

    }
}
