package org.phoebus.applications.queueserver;

import org.phoebus.framework.nls.NLS;

/** Externalised strings for Queue Server plug-ins */
@SuppressWarnings("nls")
public final class Messages {

    // ---------- keep alphabetically sorted ----------
    public static String EditControlQueue;
    public static String EditControlQueueMenuPath;
    public static String QueueMonitor;
    public static String QueueMonitorMenuPath;
    // -----------------------------------------------

    static { NLS.initializeMessages(Messages.class); }

    private Messages() { /* no-instantiation */ }
}
