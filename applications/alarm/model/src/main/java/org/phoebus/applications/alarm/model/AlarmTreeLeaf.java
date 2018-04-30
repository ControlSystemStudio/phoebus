/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.model;

/** Additional methods of an alarm tree leaf
 *
 *  <p>An alarm tree lead is always an {@link AlarmTreeItem},
 *  but with these additional methods.
 *
 *  @author Kay Kasemir
 */
public interface AlarmTreeLeaf
{
    /** @return Return description */
    public String getDescription();

    /** @param description Description
     *  @return <code>true</code> if this is a change
     */
    public boolean setDescription(final String description);

    /** @return <code>true</code> if alarms from PV are enabled */
    public boolean isEnabled();

    /** @param enable Enable the PV?
     *  @return <code>true</code> if this is a change
     */
    public boolean setEnabled(final boolean enable);

    /** @return <code>true</code> if alarms from PV are latched */
    public boolean isLatching();

    /** @param latch Latch alarms from the PV?
     *  @return <code>true</code> if this is a change
     */
    public boolean setLatching(final boolean latch);

    /** @return <code>true</code> if alarms from PV are annunciated */
    public boolean isAnnunciating();

    /** @param latch Annunciate alarms from the PV?
     *  @return <code>true</code> if this is a change
     */
    public boolean setAnnunciating(final boolean annunciate);

    /** @return Alarm delay in seconds. */
    public int getDelay();

    /** @param seconds Alarm delay
     *  @return <code>true</code> if this is a change
     */
    public boolean setDelay(final int seconds);

    /** @return Alarm count. Alarm needs to exceed this count within the delay */
    public int getCount();

    /** @param times Alarm when PV not OK more often than this count within delay
     *  @return <code>true</code> if this is a change
     */
    public boolean setCount(final int times);

    /** @return Enabling filter expression. */
    public String getFilter();

    /** @param expression Expression that enables the alarm
     *  @return <code>true</code> if this is a change
     */
    public boolean setFilter(final String expression);
}
