/*******************************************************************************
 * Copyright (c) 2010-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.model;

import java.util.Optional;

/** Listener interface for the Model
 *  @author Kay Kasemir
 */
public interface ModelListener
{
    /** @param save_changes Should UI ask to save changes to the model? */
    default void changedSaveChangesBehavior(final boolean save_changes) {}

    /** Title changed */
    default void changedTitle() {}

    /** The visbility for the toolbar and/or legend has changed */
    default void changedLayout() {}

    /** The update period or scroll step changed */
    default void changedTiming() {}

    /** The archive-rescale configuration has changed */
    default void changedArchiveRescale() {}

    /** One of the colors (background, ...) or overall fonts changed */
    default void changedColorsOrFonts() {}

    /** The time range (start/end time or span) was changed */
    default void changedTimerange() {}

    /** Time axis grid, .. changed */
    default void changedTimeAxisConfig() {}

    /** @param axis Axis that changed its color, range, ....
     *              If <code>null</code>, an axis was added or removed
     */
    default void changedAxis(Optional<AxisConfig> axis) {}

    /** @param item Item that was added to the model */
    default void itemAdded(ModelItem item) {}

    /** @param item Item that was removed from the model */
    default void itemRemoved(ModelItem item) {}

    /** @param item Item that turned visible/invisible */
    default void changedItemVisibility(ModelItem item) {}

    /** @param item Item that changed its visible attributes:
     *              color, line width, display name, ...
     */
    default void changedItemLook(ModelItem item) {}

    /** @param item Item that changed its units */
    default void changedItemUnits(ModelItem item) {}

    /** @param item Item that changed its data configuration:
     *              Archives, request method.
     *  @param archive_invalid Was a data source added, do we need to get new archived data?
     *                         Or does the change not affect archived data?
     */
    default void changedItemDataConfig(PVItem item, boolean archive_invalid) {}

    /** The annotation list changed */
    default void changedAnnotations() {}

    /**
     * The item requested to refresh its history.
     *
     * @param item the item to refresh the history data for
     */
    default void itemRefreshRequested(PVItem item) {}

    /** ModelItems have new selected sample */
    default void selectedSamplesChanged() {}
}
