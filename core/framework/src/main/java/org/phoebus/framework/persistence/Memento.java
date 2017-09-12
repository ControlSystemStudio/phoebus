/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.framework.persistence;

/** Memento used to save and later restore state
 *
 *  @author Kay Kasemir
 */
public interface Memento
{
    /** @param key Key that identifies a setting
     *  @param value String to write
     */
    public void setString(String key, String value);

    /** @param key Key that identifies a setting
     *  @param value Number to write
     */
    public void setNumber(String key, Number value);

    /** @param key Key that identifies a setting
     *  @param value Flag to write
     */
    public void setBoolean(String key, Boolean value);

    /** @param key Key that identifies a setting
     *  @return String value,
     *          <code>null</<code> if no string was written.
     */
    public String getString(String key);

    /** @param key Key that identifies a setting
     *  @return Number value,
     *          <code>null</<code> if no number was written.
     */
    public Number getNumber(String key);

    /** @param key Key that identifies a setting
     *  @return Boolean value,
     *          <code>null</<code> if no flag was written.
     */
    public Boolean getBoolean(String key);
}
