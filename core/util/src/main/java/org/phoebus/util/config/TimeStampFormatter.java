/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.util.config;

import java.time.format.DateTimeFormatter;

/***
 * <p>
 * Providing changeable time-stamp formatter. If setting "timeStampFormattingPattern" is provided, this pattern will be used for formatting the
 * time-stamp. The default pattern is "yyyy/MM/dd HH:mm:ss.SSS".
 *
 * @author Borut Terpinc - borut.terpinc@cosylab.com
 *
 */
public class TimeStampFormatter {

    private TimeStampFormatter() {
    };

    public final static DateTimeFormatter TIMESTAMP_FORMAT = createFormatter();

    private static DateTimeFormatter createFormatter() {

        String pattern = "yyyy/MM/dd HH:mm:ss.SSS";
        String patternFromSettings = SettingsProvider.getSetting("timeStampFormattingPattern");

        if (patternFromSettings != null)
            pattern = patternFromSettings;

        return DateTimeFormatter.ofPattern(pattern);
    }
}
