/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.model;

/** Alarm severity level
 *
 *  <p>Basic severities plus 'acknowledged' variants.
 *
 *  @author Kay Kasemir
 */
public enum SeverityLevel
{
    /** OK/NO_ALARM/normal/good */
    OK,

    /** Acknowledged minor issue */
    MINOR_ACK,

    /** Acknowledged major issue */
    MAJOR_ACK,

    /** Acknowledged invalid condition */
    INVALID_ACK,

    /** Acknowledged undefined condition */
    UNDEFINED_ACK,

    /** Minor issue */
    MINOR,

    /** Major issue */
    MAJOR,

    /** Invalid condition, potentially very bad */
    INVALID,

    /** Unknown states, potentially very bad */
    UNDEFINED
}
