/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.talk;

import javafx.beans.property.SimpleStringProperty;

public class Message
{
    private final SimpleStringProperty time;
    private final SimpleStringProperty severity;
    private final SimpleStringProperty description;
    
    public Message(final String time, final String severity, final String description)
    {
        this.time          = new SimpleStringProperty(time);
        this.severity      = new SimpleStringProperty(severity);
        this.description   = new SimpleStringProperty(description);
    }
    
    public String getTime()         { return time.get();        }
    public String getSeverity()     { return severity.get();    }
    public String getDescription()  { return description.get(); }
    
    public void setTime(String t)            { time.set(t);            }
    public void setSeverity(String sev)      { severity.set(sev);      } 
    public void setDescription(String descr) { description.set(descr); }
}
