package org.phoebus.applications.alarm.ui.annunciator;

public interface Annunciator
{

    /**
     * Annunciate the message. Only returns once speaking finishes.
     * @param message Message text
     */
    void speak(final AnnunciatorMessage message);

    /**
     * Release resources that need to be cleaned on shutdown.
     */
    default void shutdown(){}
}
