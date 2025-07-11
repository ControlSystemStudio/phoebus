package org.phoebus.applications.queueserver;

import org.phoebus.framework.nls.NLS;

/** Externalised strings for Queue-Monitor plug-in */
@SuppressWarnings("nls")
public final class Messages {

    // ---------- keep alphabetically sorted ----------
    public static String QueueServer;           // display name
    public static String QueueServerMenuPath;   // menu path
    // -----------------------------------------------

    static { NLS.initializeMessages(Messages.class); }

    private Messages() { /* no-instantiation */ }
}
