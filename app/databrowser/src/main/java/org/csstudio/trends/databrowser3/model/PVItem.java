/*******************************************************************************
 * Copyright (c) 2010-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.model;

import static org.csstudio.trends.databrowser3.Activator.logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import javax.xml.stream.XMLStreamWriter;

import org.csstudio.trends.databrowser3.Messages;
import org.csstudio.trends.databrowser3.imports.ImportArchiveReaderFactory;
import org.csstudio.trends.databrowser3.persistence.XMLPersistence;
import org.csstudio.trends.databrowser3.preferences.Preferences;
import org.epics.vtype.Display;
import org.epics.vtype.VType;
import org.phoebus.archive.vtype.VTypeHelper;
import org.phoebus.framework.jobs.NamedThreadFactory;
import org.phoebus.framework.persistence.XMLUtil;
import org.phoebus.pv.PV;
import org.phoebus.pv.PVPool;
import org.w3c.dom.Element;

import io.reactivex.BackpressureStrategy;
import io.reactivex.disposables.Disposable;

/** Data Browser Model Item for 'live' PV.
 *  <p>
 *  Holds both historic and live data in PVSamples.
 *  Performs the periodic scans of a control system PV.
 *  <p>
 *  Also implements IProcessVariable so that context menus
 *  can link to related CSS tools.
 *
 *  @author Kay Kasemir
 *  @author Takashi Nakamoto changed PVItem to handle waveform index.
 */
@SuppressWarnings("nls")
public class PVItem extends ModelItem
{
    /** Waveform Index */
    final private AtomicInteger waveform_index = new AtomicInteger(0);

    /** Historic and 'live' samples for this PV */
    private PVSamples samples = new PVSamples(waveform_index);

    /** Where to get archived data for this item. */
    private List<ArchiveDataSource> archives = new ArrayList<>();

    /** Control system PV, set when running */
    private PV pv = null;

    private Disposable pv_flow;

    /** Most recently received value */
    private volatile VType current_value;

    /** Scan period in seconds, &le;0 to 'monitor' */
    private double period;

    /** Timer that was used to schedule the scanner */
    final private static ScheduledExecutorService scan_timer =
            Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("DataBrowserScanner"));

    /** For a period &gt;0, this timer task performs the scanning */
    private ScheduledFuture<?> scanner = null;

    /** Archive data request type */
    private RequestType request_type = RequestType.OPTIMIZED;

    /** Indicating if the history data is automatically refreshed, whenever
     * the live buffer is too small to show all the data */
    private boolean automaticRefresh = Preferences.automatic_history_refresh;

    /** Initialize
     *  @param name PV name
     *  @param period Scan period in seconds, &le;0 to 'monitor'
     *  @throws Exception on error
     */
    public PVItem(final String name, final double period)
    {
        super(name);
        this.period = period;
    }

    /** @return Waveform index */
    @Override
    public int getWaveformIndex()
    {
        return waveform_index.get();
    }

    /** @param index New waveform index */
    @Override
    public void setWaveformIndex(int index)
    {
        if (index < 0)
            index = 0;
        if (waveform_index.getAndSet(index) != index)
            fireItemDataConfigChanged(false);
    }

    /** Set new item name, which changes the underlying PV name
     *  {@inheritDoc}
     */
    @Override
    public boolean setName(final String new_name) throws Exception
    {
        if (! super.setName(new_name))
            return false;
        // Stop PV, clear samples
        final boolean running = (pv != null);
        if (running)
            stop();
        samples.clear();
        // Create new PV, maybe start it
        if (running)
            start();
        return true;
    }

    /** @return Scan period in seconds, &le;0 to 'monitor' */
    public double getScanPeriod()
    {
        return period;
    }

    /** Update scan period.
     *  <p>
     *  When called on a running item, this stops and re-starts the PV.
     *  @param period New scan period in seconds, &le;0 to 'monitor'
     *  @throws Exception On error re-starting a running PVItem
     */
    public void setScanPeriod(double period) throws Exception
    {
        // Don't 'scan' faster than 1 Hz. Instead switch to on-change.
        if (period < 0.1)
            period = 0.0;
        final boolean running = (pv != null);
        if (running)
            stop();
        this.period = period;
        if (running)
            start();
        fireItemDataConfigChanged(false);
    }

    /** {@inheritDoc} */
    @Override
    void setModel(final Model model)
    {
        super.setModel(model);
        // Dis-associated from model? Then refresh is no longer required
        if (model == null)
            automaticRefresh = false;
        else // Otherwise use preferences
            automaticRefresh = Preferences.automatic_history_refresh;
    }

    /** @return Maximum number of live samples in ring buffer */
    public int getLiveCapacity()
    {
        return samples.getLiveCapacity();
    }

    /** Set new capacity for live sample ring buffer
     *  <p>
     *  @param new_capacity New sample count capacity
     *  @throws Exception on out-of-memory error
     */
    public void setLiveCapacity(final int new_capacity) throws Exception
    {
        samples.setLiveCapacity(new_capacity);
        fireItemDataConfigChanged(false);
    }

    /** @return Archive data sources for this item */
    public Collection<ArchiveDataSource> getArchiveDataSources()
    {
        return archives;
    }

    /** Replace archives with settings from preferences */
    public void useDefaultArchiveDataSources()
    {
        archives.clear();
        for (ArchiveDataSource arch : Preferences.archives)
            archives.add(arch);
        fireItemDataConfigChanged(true);
    }

    /** @param archive Archive data source
     *  @return <code>true</code> if PV uses given data source
     */
    public boolean hasArchiveDataSource(final ArchiveDataSource archive)
    {
        for (ArchiveDataSource arch : archives)
            if (arch.equals(archive))
                return true;
        return false;
    }

    /** @param archive Archive to add as a source to this item
     *  @throws Error when archive is already used
     */
    public void addArchiveDataSource(final ArchiveDataSource archive)
    {
        if (hasArchiveDataSource(archive))
            throw new Error("Duplicate archive " + archive);
        archives.add(archive);
        fireItemDataConfigChanged(true);
    }

    /**
     * @param archs Archives to add as a source to this item. Duplicates are ignored
     */
    public void addArchiveDataSource(final ArchiveDataSource archs[])
    {
        boolean change = false;
        for (ArchiveDataSource archive : archs)
            if (! archives.contains(archive))
            {
                change = true;
                archives.add(archive);
            }
        if (change)
            fireItemDataConfigChanged(true);
    }

    /** @param archive Archive to remove as a source from this item. */
    public void removeArchiveDataSource(final ArchiveDataSource archive)
    {
        // Archive removed -> (Probably) no need to get new data
        if (archives.remove(archive))
            fireItemDataConfigChanged(false);
    }

    /** @param archs Archives to remove as a source from this item. Ignored when not used. */
    public void removeArchiveDataSource(final List<ArchiveDataSource> archs)
    {
        boolean change = false;
        for (ArchiveDataSource archive : archs)
            if (archives.remove(archive))
                change = true;
        if (change)
            fireItemDataConfigChanged(false);
    }

    /** Replace existing archive data sources with given archives
     *  @param archs ArchiveDataSources to use for this item
     */
    public void setArchiveDataSource(final ArchiveDataSource... archs)
    {
        // Check if they are the same, i.e. count AND order match
        if (archs.length == archives.size())
        {
            boolean same = true;
            for (int i=0; i<archs.length; ++i)
                if (! archs[i].equals(archives.get(i)))
                {
                    same = false;
                    break;
                }
            if (same)
                return;
        }
        // Different archives
        archives.clear();
        for (ArchiveDataSource arch : archs)
            archives.add(arch);
        fireItemDataConfigChanged(true);
    }

    /** @return Archive data request type */
    public RequestType getRequestType()
    {
        return request_type;
    }

    /** @param request_type New request type */
    public void setRequestType(final RequestType request_type)
    {
        if (this.request_type == request_type)
            return;
        this.request_type = request_type;
        fireItemDataConfigChanged(true);
    }

    /** Notify listeners
     *  @param archive_invalid Was a data source added, do we need to get new archived data?
     *                         Or does the change not affect archived data?
     */
    private void fireItemDataConfigChanged(final boolean archive_invalid)
    {
        if (model.isPresent())
            model.get().fireItemDataConfigChanged(this, archive_invalid);
    }

    /** Connect control system PV, start scanning, ...
     *  @throws Exception on error
     */
    public void start() throws Exception
    {
        if (pv != null)
            throw new RuntimeException("Already started " + getName());
        pv = PVPool.getPV(getResolvedName());
        pv_flow = pv.onValueEvent(BackpressureStrategy.BUFFER)
                    .subscribe(this::valueChanged);
        // Log every received value?
        if (period <= 0.0)
            return;
        // Start scanner for periodic log
        final long delay = (long) (period*1000);
        scanner = scan_timer.scheduleAtFixedRate(this::doScan, delay, delay, TimeUnit.MILLISECONDS);
    }

    /** Disconnect from control system PV, stop scanning, ... */
    public void stop()
    {
        if (pv == null)
        {   // Warn. Throwing exception would prevent closing when there was an error during start
            logger.log(Level.WARNING, "Data Browser PV closed while not running: " + getName());
            return;
        }

        pv_flow.dispose();
        if (scanner != null)
        {
            scanner.cancel(true);
            scanner = null;
        }
        PVPool.releasePV(pv);
        pv = null;
    }

    /** Called by PV's onValueEvent */
    private void valueChanged(final VType value)
    {
        boolean added = false;
        // Cache most recent for 'scanned' operation
        current_value = value;
        // In 'monitor' mode, add to live sample buffer
        if (period <= 0)
        {
            logger.log(Level.FINE, "PV {0} received {1}", new Object[] { getName(), value });
            if (PV.isDisconnected(value))
            {
                logDisconnected();
                return;
            }
            samples.addLiveSample(value);
            added = true;
        }
        // Set units unless already defined
        if (getUnits() == null)
            updateUnits(value);
        if (automaticRefresh && added &&
            model.isPresent() &&
            samples.isHistoryRefreshNeeded(model.get().getTimerange()))
            model.get().fireItemRefreshRequested(PVItem.this);
    }

    /** {@inheritDoc} */
    @Override
    public PVSamples getSamples()
    {
        return samples;
    }


    private void updateUnits(final VType value)
    {
        final Display display = Display.displayOf(value);
        setUnits(display.getUnit());
    }

    /** Scan, i.e. add 'current' value to live samples */
    private void doScan()
    {
        final VType value = current_value;
        logger.log(Level.FINE, "PV {0} scans {1}", new Object[] { getName(), value });
        if (value == null)
            logDisconnected();
        else
            // Transform value to have 'now' as time stamp
            samples.addLiveSample(VTypeHelper.transformTimestampToNow(value));
    }

    /** Add one(!) 'disconnected' sample */
    private void logDisconnected()
    {
        if (! samples.lockForWriting())
            return;
        try
        {
            final int size = samples.size();
            if (size > 0)
            {
                final String last =
                        VTypeHelper.getMessage(samples.get(size - 1).getVType());
                // Does last sample already have 'disconnected' status?
                if (Messages.Model_Disconnected.equals(last))
                    return;
            }
            samples.addLiveSample(new PlotSample(Messages.LiveData, Messages.Model_Disconnected));
        }
        finally
        {
            samples.unlockForWriting();
        }
    }

    /** Add data retrieved from an archive to the 'historic' section
     *  @param server_name Archive server that provided these samples
     *  @param new_samples Historic data
     */
    public void mergeArchivedSamples(final String server_name,
            final List<VType> new_samples)
    {
        final boolean need_refresh;
        if (! samples.lockForWriting())
            return;
        try
        {
            samples.mergeArchivedData(server_name, new_samples);
            need_refresh = automaticRefresh && model.isPresent() &&
                    samples.isHistoryRefreshNeeded(model.get().getTimerange());
        }
        finally
        {
            samples.unlockForWriting();
        }
        if (need_refresh)
            model.get().fireItemRefreshRequested(this);
    }

    /** Write XML formatted PV configuration
     *  @param writer PrintWriter
     *  @throws Exception on error
     */
    @Override
    public void write(final XMLStreamWriter writer) throws Exception
    {
        writer.writeStartElement(XMLPersistence.TAG_PV);
        {
            writeCommonConfig(writer);
            writer.writeStartElement(XMLPersistence.TAG_SCAN_PERIOD);
            writer.writeCharacters(Double.toString(getScanPeriod()));
            writer.writeEndElement();
            writer.writeStartElement(XMLPersistence.TAG_LIVE_SAMPLE_BUFFER_SIZE);
            writer.writeCharacters(Integer.toString(getLiveCapacity()));
            writer.writeEndElement();
            writer.writeStartElement(XMLPersistence.TAG_REQUEST);
            writer.writeCharacters(getRequestType().name());
            writer.writeEndElement();
            int key = 1;
            for (ArchiveDataSource archive : archives)
            {
                writer.writeStartElement(XMLPersistence.TAG_ARCHIVE);
                {
                    writer.writeStartElement(XMLPersistence.TAG_NAME);
                    writer.writeCharacters(archive.getName());
                    writer.writeEndElement();
                    writer.writeStartElement(XMLPersistence.TAG_URL);
                    writer.writeCharacters(archive.getUrl());
                    writer.writeEndElement();
                    writer.writeStartElement(XMLPersistence.TAG_KEY);
                    writer.writeCharacters(Integer.toString(key++));
                    writer.writeEndElement();
                }
                writer.writeEndElement();
            }
        }
        writer.writeEndElement();
    }

    /** Create PVItem from XML document
     *  @param model Model to which this item will belong (but doesn't, yet)
     *  @param node XML node with item configuration
     *  @return PVItem
     *  @throws Exception on error
     */
    public static PVItem fromDocument(final Model model, final Element node) throws Exception
    {
        final String name = XMLUtil.getChildString(node, XMLPersistence.TAG_NAME).orElse("PV");
        final double period = XMLUtil.getChildDouble(node, XMLPersistence.TAG_SCAN_PERIOD).orElse(0.0);

        final PVItem item = new PVItem(name, period);
        final int buffer_size = XMLUtil.getChildInteger(node, XMLPersistence.TAG_LIVE_SAMPLE_BUFFER_SIZE).orElse(Preferences.buffer_size);
        item.setLiveCapacity(buffer_size);

        final String req_txt = XMLUtil.getChildString(node, XMLPersistence.TAG_REQUEST).orElse(RequestType.OPTIMIZED.name());
        try
        {
            final RequestType request = RequestType.valueOf(req_txt);
            item.setRequestType(request);
        }
        catch (Throwable ex)
        {
            // Ignore
        }

        item.configureFromDocument(model, node);

        // Load archives from saved configuration
        boolean have_imported_data = false;
        for (Element archive : XMLUtil.getChildElements(node, XMLPersistence.TAG_ARCHIVE))
        {
            final String url = XMLUtil.getChildString(archive, XMLPersistence.TAG_URL).orElse(null);
            final String arch = XMLUtil.getChildString(archive, XMLPersistence.TAG_NAME).orElse(null);
            if (url.startsWith(ImportArchiveReaderFactory.PREFIX))
                have_imported_data = true;
            item.addArchiveDataSource(new ArchiveDataSource(url, arch));
        }

        // When requested, use default archive sources for 'real' archives (RDB, ...)
        // Do not clobber an imported archive data source, a specific file which was
        // probably not meant to be replaced by a default.
        if (Preferences.use_default_archives  &&  !have_imported_data)
            item.useDefaultArchiveDataSources();

        return item;
    }

    @Override
    public void dispose()
    {
        archives.clear();
        current_value = null;
        samples.clear();
    }
}
