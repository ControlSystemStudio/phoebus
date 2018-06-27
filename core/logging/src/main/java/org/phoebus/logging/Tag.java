package org.phoebus.logging;

public interface Tag {

    /**
     * @author berryman from shroffk
     *
     */

    public String getName();

    public String getState();

    public static Tag of(String name, String state) {
        return new TagImpl(name, state);
    }
}
