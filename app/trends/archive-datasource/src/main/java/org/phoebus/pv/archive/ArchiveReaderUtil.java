package org.phoebus.pv.archive;

import java.time.DateTimeException;
import java.time.Instant;

import static org.phoebus.util.time.TimestampFormats.DATETIME_FORMAT;
import static org.phoebus.util.time.TimestampFormats.FULL_FORMAT;
import static org.phoebus.util.time.TimestampFormats.MILLI_FORMAT;
import static org.phoebus.util.time.TimestampFormats.SECONDS_FORMAT;

public class ArchiveReaderUtil {

    /**
     * A utility method to parse a subset of supported time formats used by the archive datasources
     * Support formats include
     *     FULL_PATTERN = "yyyy-MM-dd HH:mm:ss.nnnnnnnnn";
     *     MILLI_PATTERN = "yyyy-MM-dd HH:mm:ss.SSS";
     *     SECONDS_PATTERN = "yyyy-MM-dd HH:mm:ss";
     *     DATETIME_PATTERN = "yyyy-MM-dd HH:mm";
     *
     * @param timeValue
     * @return
     */
    public static Instant parseSupportedTimeFormat(String timeValue) throws Exception {
        Instant time;
        try {
            switch (timeValue.length()) {
                case 16 -> time = Instant.from(DATETIME_FORMAT.parse(timeValue));
                case 19 -> time = Instant.from(SECONDS_FORMAT.parse(timeValue));
                case 23 -> time = Instant.from(MILLI_FORMAT.parse(timeValue));
                case 29 -> time = Instant.from(FULL_FORMAT.parse(timeValue));
                default -> throw new Exception("Time value defined in a unknown format, '" + timeValue + "'");
            }
        } catch (
                DateTimeException e) {
            throw new Exception("Time value defined in a unknown format, '" + timeValue + "'");
        }
        return time;
    }
}
