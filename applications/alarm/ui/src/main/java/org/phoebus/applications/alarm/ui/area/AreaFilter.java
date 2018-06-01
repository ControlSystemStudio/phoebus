/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.ui.area;

import static org.phoebus.applications.alarm.AlarmSystem.logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import org.phoebus.applications.alarm.model.AlarmTreeItem;
import org.phoebus.applications.alarm.model.AlarmTreePath;
import org.phoebus.applications.alarm.model.SeverityLevel;

/** Filter for Alarm Areas. Filters out messages not pertaining to the specified level.
 *  Maintains state information for the areas on the specified level.
 *  @author Evan Smith
 */
@SuppressWarnings("nls")
public class AreaFilter
{
    private final int level;

    /** Associate item name with the item severity level. */
    private final ConcurrentHashMap<String, SeverityLevel> itemSeverity = new ConcurrentHashMap<>();

    public AreaFilter(final int level)
    {
        // Set level for levelCheck.
        this.level = level;
    }

    // Check the level of the message by examining the number of elements in its path.
    private boolean levelCheck(final String path)
    {
        final String[] path_elements = AlarmTreePath.splitPath(path);
        return (path_elements.length == level);
    }

    /** Filter out messages not pertaining to the set level.
     *
     *  <p>If a message is related to an alarm area of interest,
     *  note its severity. Otherwise ignore.
     *
     *  @param message Item received from alarm client
     *  @return Name of affected area, <code>null</code> if message was for different level
     */
    public String filter(final AlarmTreeItem<?> message)
    {
        if (! levelCheck(message.getPathName()))
            return null;

        final String name = message.getName();
        final SeverityLevel severity = message.getState().getSeverity();
        itemSeverity.put(name, severity);

        return name;
    }

    /** @return List of all currently known items */
    public List<String> getItems()
    {
        final List<String> items = new ArrayList<>(itemSeverity.keySet());
        items.sort(String::compareTo);
        return items;
    }

    /** @param item_name Name of item
     *  @return Severity of the item. UNDEFINED when item is not known.
     */
    public SeverityLevel getSeverity(final String item_name)
    {
        final SeverityLevel severity = itemSeverity.get(item_name);
        if (severity == null)
        {
            logger.log(Level.WARNING, "Unknown alarm area " + item_name);
            return SeverityLevel.UNDEFINED;
        }
        return severity;
    }

    /** @param item_name Item to remove. This is safe to call even if item is not in itemSeverity map. */
    public void removeItem(final String item_name)
    {
        itemSeverity.remove(item_name);
    }
}
