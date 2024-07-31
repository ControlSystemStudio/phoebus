package org.phoebus.util.text;

import java.util.Objects;

public class Strings {

    /**
     * Based on the the google String.isNullOrEmpty.
     *
     * @param str
     * @return true if the string is null or empty
     */
    public static boolean isNullOrEmpty(String str) {
        if(Objects.isNull(str) || str.isBlank()) {
            return true;
        } else {
            return false;
        }
    }
}
