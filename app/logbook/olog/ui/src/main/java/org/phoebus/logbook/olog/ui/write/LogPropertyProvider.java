package org.phoebus.logbook.olog.ui.write;

import org.phoebus.logbook.Property;

/**
 * An interface to subscribe property providers
 */
public interface LogPropertyProvider {

    public Property getProperty();
}
