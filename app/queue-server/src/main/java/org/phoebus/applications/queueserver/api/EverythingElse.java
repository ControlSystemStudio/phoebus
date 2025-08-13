package org.phoebus.applications.queueserver.api;

import java.util.Map;

/**
 * Marker types for endpoints whose payload is either
 *   • empty, or
 *   • an arbitrary JSON object we don’t inspect yet.
 *
 */
public interface EverythingElse {
    /** Server returns only {success,msg} */
    enum Empty implements EverythingElse { INSTANCE }

    /** Server returns some JSON object we keep as Map */
    record Arbitrary(Map<String,Object> value) implements EverythingElse {}
}
