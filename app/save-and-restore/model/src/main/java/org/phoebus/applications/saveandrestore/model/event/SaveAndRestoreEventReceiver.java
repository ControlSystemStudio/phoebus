/*
 * Copyright (C) 2020 European Spallation Source ERIC.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.phoebus.applications.saveandrestore.model.event;

import org.phoebus.applications.saveandrestore.model.Node;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Implementations registered over Java SPI will be called when:
 * <ul>
 *     <li>A new snapshot has been created and successfully saved by the remote save-and-restore service.</li>
 *     <li>A snapshot has been restored.</li>
 * </ul>
 */
public interface SaveAndRestoreEventReceiver {

    /**
     * Called when a new snapshot {@link Node} has been successfully created and saved by the remote
     * save-and-restore service.
     * @param node The {@link Node} representing the snapshot.
     * @param errorHandler An error handler callback.
     */
    void snapshotSaved(Node node, Consumer<String> errorHandler);

    /**
     * Called when a new snapshot {@link Node} has been restored.
     * @param node The {@link Node} representing the snapshot.
     * @param failedPVs List of PVs that for any reason could not be restored.
     * @param errorHandler An error handler callback.
     */
    void snapshotRestored(Node node, List<String> failedPVs, Consumer<String> errorHandler);
}
