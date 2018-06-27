/**
 * 
 */
package org.phoebus.logging;

/**
 * @author Eric Berryman taken from shroffk
 * 
 */
public interface Logbook {

    public String getName();

    public String getOwner();

    public static Logbook of(String name, String owner) {
        return new LogbookImpl(name, owner);
    }

    public static Logbook of(String name) {
        return new LogbookImpl(name);
    }
}
