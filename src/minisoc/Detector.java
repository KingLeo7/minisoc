package minisoc;

import java.util.List;

/**
 * A detector looks at one log line at a time and returns any alerts it
 * wants to raise. Lines a detector doesn't care about just return an
 * empty list, so every line can be handed to every detector.
 */
public interface Detector {

    String name();

    List<Alert> inspect(String line);

    default void reset() {}
}
