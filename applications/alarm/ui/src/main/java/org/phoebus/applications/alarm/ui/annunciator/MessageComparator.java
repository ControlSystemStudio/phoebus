/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.ui.annunciator;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Message comparator for alarm system messages of the form 'SEVERITY Alarm: message...'
 * @author Evan Smith
 *
 */
public class MessageComparator implements Comparator<String>
{
    private final List<String> severities = Arrays.asList( "OK",  "MINOR_ACK",  "MAJOR_ACK", "INVALID_ACK", "UNDEFINED_ACK", "MINOR", "MAJOR", "INVALID", "UNDEFINED");
    @Override
    public int compare(String s1, String s2)
    {
        String[] s1tok = s1.split(" ");
        String[] s2tok = s2.split(" ");
        
        String s1sev = s1tok[0];
        String s2sev = s2tok[0];
        
        if (isSeverity(s1sev) || isSeverity(s2sev))
        {
            if (s1sev.equals(s2sev))
                return 0;
            
            if (severities.indexOf(s1sev) > severities.indexOf(s2sev))
                return -1;
            else 
                return 1;
        }
           
        // Put the non alarms at the back of the queue.
        return 1;
    }
    
    private boolean isSeverity(String sev)
    {
        return severities.contains(sev);
    }
    
}
