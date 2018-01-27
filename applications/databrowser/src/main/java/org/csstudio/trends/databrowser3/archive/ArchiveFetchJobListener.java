/*******************************************************************************
 * Copyright (c) 2010-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.archive;

import java.util.List;

import org.csstudio.trends.databrowser3.model.ArchiveDataSource;

/** Listener to an ArchiveFetchJob
 *  @author Kay Kasemir
 *  @author Jaka Bobnar - Added channelNotFound logic
 */
public interface ArchiveFetchJobListener
{
    /** Invoked when the job completed successfully
     *  @param job Job that completed
     */
    void fetchCompleted(ArchiveFetchJob job);

    /** Invoked when the job failed to complete
     *  @param job Job that had error
     *  @param archive Archive that job was currently accessing
     *  @param error Error description
     */
    void archiveFetchFailed(ArchiveFetchJob job, ArchiveDataSource archive, Exception error);

    /** Invoked when the channel was not found in at least one of the archive sources, regardless of whether in the end
     *  the data were loaded or not.
     *
     *  @param job Job that had error
     *  @param channelFoundAtLeastOnce <code>true</code> if the channel was found in at least one data source
     *  @param archivesThatFailed archive sources in which the channel was not found
     */
    void channelNotFound(ArchiveFetchJob job, boolean channelFoundAtLeastOnce, List<ArchiveDataSource> archivesThatFailed);
}
