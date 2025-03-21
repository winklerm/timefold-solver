package ai.timefold.solver.core.api.score.stream.common;

import java.util.Collection;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Contains info regarding the consecutive sequences and breaks in a collection of points.
 *
 * @param <Value_> The type of value in the sequence.
 * @param <Difference_> The type of difference between values in the sequence.
 */
public interface SequenceChain<Value_, Difference_ extends Comparable<Difference_>> {

    /**
     * @return the sequences contained in the collection in ascending order.
     */
    @NonNull
    Collection<Sequence<Value_, Difference_>> getConsecutiveSequences();

    /**
     * @return the breaks contained in the collection in ascending order.
     */
    @NonNull
    Collection<Break<Value_, Difference_>> getBreaks();

    /**
     * Returns the first sequence of consecutive values.
     *
     * @return null if there are no sequences
     */
    @Nullable
    Sequence<Value_, Difference_> getFirstSequence();

    /**
     * Returns the last sequence of consecutive values.
     *
     * @return null if there are no sequences
     */
    @Nullable
    Sequence<Value_, Difference_> getLastSequence();

    /**
     * Returns the first break between two consecutive sequences of values.
     *
     * @return null if there are less than two sequences
     */
    @Nullable
    Break<Value_, Difference_> getFirstBreak();

    /**
     * Returns the last break between two consecutive sequences of values.
     *
     * @return null if there are less than two sequences
     */
    @Nullable
    Break<Value_, Difference_> getLastBreak();

}
