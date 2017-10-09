/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.vtype;

import java.util.List;

/**
 * Extended alarm information. This supplements the alarm information
 * given with scalar and array pvs, by showing all the possible alarm
 * conditions specific to a device, and which ones are active
 * at the moment.
 * <p>
 * The data type on the wire will look significantly different. The name
 * and severity of each conditions don't typically change and are considered
 * metadata. The actual alarm state (which ones are on or off) and the error
 * message are what changes at each notification.
 *
 * @author carcassi
 */
public interface VExtendedAlarm extends VType {

    /**
     * A single alarm condition that can be on or off.
     */
    public interface AlarmCondition extends Alarm {

        /**
         * Whether the alarm is on.
         *
         * @return true if alarm condition is on
         */
        public boolean isActive();
    }

    /**
     * All the possible alarm conditions for the device, and
     * whether they are on or off.
     *
     * @return the alarm conditions
     */
    public List<AlarmCondition> getConditions();


    /**
     * A human readable error message associated to the error(s).
     *
     * @return a message or null
     */
    public String getMessage();
}
