/*******************************************************************************
 * Copyright (c) 2010-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.ui.plot;

import java.io.File;
import java.time.Instant;
import java.util.List;

import org.csstudio.trends.databrowser3.model.AnnotationInfo;
import org.csstudio.trends.databrowser3.model.ArchiveDataSource;
import org.phoebus.core.types.ProcessVariable;

/** Interface used by Plot to send events in response to user input:
 *  Zoom changed, scrolling turned on/off
 *  @author Kay Kasemir
 *
 *  Add events necessary in response of GRAPH settings changed by user
 *  ADD events link to add/remove annotation
 *  @author Laurent PHILIPPE (GANIL)
 */
public interface PlotListener
{
    /** Called when the user requests time config dialog. */
    public void timeConfigRequested();

    /** Called when the user changes time axis, includes turning scrolling on/off
     *  @param scroll Scrolling?
     *  @param start New time axis start time
     *  @param end ... end time ...
     */
    public void timeAxisChanged(boolean scroll, Instant start, Instant end);

    /** Called when the user changed a value (Y) axis
     *  @param index Value axis index 0, 1, ...
     *  @param lower Lower range limit
     *  @param upper Upper range limit
     */
    public void valueAxisChanged(int index, double lower, double upper);

    /** Received names, presumably for PVs, via drag & drop
     *  @param name PV(?) names
     */
    public void droppedNames(List<String> name);

    /** Received PV names and/or archive data sources via drag & drop
     *
     *  <p>If names with archive are received, the name and archive
     *  arrays will have the same size.
     *
     *  @param name PV names or <code>null</code>
     *  @param archive Archive data sources or <code>null</code>
     */
    public void droppedPVNames(List<ProcessVariable> name, List<ArchiveDataSource> archive);

    /** Received a file name */
    public void droppedFilename(File file_name);

    /** Received updated annotations */
    public void changedAnnotations(List<AnnotationInfo> annotations);

    /** ModelItems have new selected sample */
    public void selectedSamplesChanged();

    /** Plot tool bar displayed or hidden */
    public void changedToolbar(boolean visible);

    /** Plot legend displayed or hidden */
    public void changedLegend(boolean visible);

    /** Auto scale modified by user interaction with plot */
    public void autoScaleChanged(int index, boolean autoScale);

    /** Grid modified by user interaction with plot
     *  @param index Index of Y axis, -1 for X axis
     */
    public void gridChanged(int index, boolean show_grid);

    /** Log mode modified by user interaction with plot */
    public void logarithmicChanged(int index, boolean use_log);
}
