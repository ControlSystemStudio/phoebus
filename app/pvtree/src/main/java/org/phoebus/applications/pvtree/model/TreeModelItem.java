/*******************************************************************************
 * Copyright (c) 2017-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.applications.pvtree.model;

import static org.phoebus.applications.pvtree.PVTreeApplication.logger;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

import org.epics.vtype.AlarmSeverity;
import org.epics.vtype.VType;
import org.phoebus.applications.pvtree.Settings;
import org.phoebus.pv.PV;
import org.phoebus.pv.PVPool;

import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

/** One 'item' in the PV Tree
 *
 *  <p>Typically a PV with value and maybe links.
 *
 *  <p>Also used for constants, like "INP=10"
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class TreeModelItem
{
    /** Log level used to trace the progress of resolving links */
    static final Level TRACE = Level.FINE;

    /** The model to which this whole tree belongs. */
    private final TreeModel model;

    /** Parent item or <code>null</code> */
    private final TreeModelItem parent;

    /** The info provided by the parent or creator ("PV", "INPA", ...) */
    private final String info;

    /** The name of this PV tree item as shown in the tree. */
    private final String pv_name;

    /** The name of the record.
     *  <p>
     *  For example, the 'name' could be 'fred.SEVR', then 'fred'
     *  would be the record name.
     */
    private final String record_name;

    /** Marker used for tree items that are not from PV but constant */
    private static final String CONSTANT_TYPE = "[const]";

    private volatile String type = "unknown";

    /** Value received while updates might have been were frozen */
    private volatile String current_value = null;

    /** Alarm received while updates might have been were frozen */
    private volatile AlarmSeverity current_severity = AlarmSeverity.UNDEFINED;

    /** Most recent value to show (may be frozen). */
    private volatile String value = null;

    /** Most recent severity (may be frozen). */
    private volatile AlarmSeverity severity = AlarmSeverity.UNDEFINED;

    /** Links */
    private final List<TreeModelItem> links = new CopyOnWriteArrayList<>();

    /** PV for value of this item */
    private final AtomicReference<PV> value_pv = new AtomicReference<>();
    private volatile Disposable value_flow;

    /** PV for type of this item (released after read once) */
    private AtomicReference<PV> type_pv = new AtomicReference<>();
    private volatile Disposable type_flow;

    /** List of links to read (empty when done) */
    private final ConcurrentLinkedQueue<String> links_to_read = new ConcurrentLinkedQueue<>();

    /** Current link to read (released when all links were read) */
    private AtomicReference<PV> link_pv = new AtomicReference<>();
    private volatile Disposable link_flow;
    private final Consumer<? super VType> link_consumer;

    /** PV tree item
     *  @param model The model to which this whole tree belongs.
     *  @param parent The parent of this item, or <code>null</code> for the root.
     *  @param info The info provided by the parent or creator ("PV", "INPA", ...)
     *  @param pv_name The PV name of this tree node.
     */
    public TreeModelItem(final TreeModel model, final TreeModelItem parent, final String info, final String pv_name)
    {
        this.model = model;
        this.parent = parent;
        this.pv_name = pv_name;
        this.info = info;

        // In case this is "record.field", get the record name.
        final int sep = pv_name.lastIndexOf('.');
        if (sep > 0)
            record_name = pv_name.substring(0, sep);
        else
            record_name = pv_name;

        logger.log(Level.FINE,
                "New Tree item {0}, record name {1}",
                new Object[] { pv_name, record_name});

        link_consumer = value->
        {
            final String field = links_to_read.poll();
            if (field == null)
            {
                logger.log(Level.WARNING, "Unexpected link update " + getPVName() + " = " + value);
                return;
            }
            String text = AlarmFormatter.formatValue(value);
            logger.log(TRACE, "Link " + record_name + "." + field + " -> " + text);
            disposeLinkPV();

            // The value could be
            // a) a record name followed by "... NPP NMS". Remove that.
            // b) a hardware input/output "@... " or "#...". Keep that.
            if (text.length() > 1 &&
                text.charAt(0) != '@' &&
                text.charAt(0) != '#')
            {
                int i = text.indexOf(' ');
                if (i > 0)
                    text = text.substring(0, i);
            }

            if (! text.isEmpty())
            {
                try
                {
                    final TreeModelItem new_item = new TreeModelItem(model, TreeModelItem.this, field, text);
                    links.add(new_item);
                    model.itemLinkAdded(TreeModelItem.this, new_item);
                }
                catch (Exception ex)
                {
                    logger.log(Level.WARNING,
                            "Cannot add tree node for link " + field + " = " + text, ex);
                }
            }
            // This decrement the links read _so_far_ to zero,
            // since the new TreeModelItem just created has not
            // started to request its links.
            // Tree will thus expand a few times,
            // whenever a bunch of links have resolved,
            // but at least not for every single change
            model.decrementLinks();
            resolveNextLink();
        };
    }

    /** @return Parent or <code>null</code> for root */
    public TreeModelItem getParent()
    {
        return parent;
    }

    /** Start primary PV, fetch type, then links */
    public void start()
    {
        try
        {
            // Is this a link to follow, or just a constant to display?
            // Hardware links "@vme..." or constant numbers "-12.3"
            // cause us to stop here:
            if (! PVNameFilter.isPvName(pv_name))
            {   // No PV
                type = CONSTANT_TYPE;
                // Clear alarm
                current_severity = null;
                severity = null;
                value = null;
                return;
            }
            else
            {
                // Throttle data. UI updates are then throttled once more
                final long ms = Math.round(Settings.max_update_period * 1000);
                final PV pv = PVPool.getPV(pv_name);
                value_flow = pv
                    .onValueEvent()
                    .throttleLatest(ms, TimeUnit.MILLISECONDS)
                    .subscribe(value ->
                {
                    current_value = AlarmFormatter.format(value);
                    current_severity = org.phoebus.core.vtypes.VTypeHelper.getSeverity(value);
                    updateValue();
                });
                value_pv.set(pv);
            }

            fetchType();
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Cannot start " + this, ex);
        }
    }

    /** @return Name of this PV. */
    public String getPVName()
    {
        return pv_name;
    }

    /** @return Alarm severity. <code>null</code> for constant items that are not a PV */
    public AlarmSeverity getSeverity()
    {
        return severity;
    }

    /** @return Links of this PV, i.e. child nodes in tree */
    public List<TreeModelItem> getLinks()
    {
        return links;
    }

    private void fetchType()
    {
        try
        {
            final PV pv = PVPool.getPV(record_name + ".RTYP");
            type_flow = pv.onValueEvent().firstOrError().subscribe(value ->
            {
                type = AlarmFormatter.formatValue(value);
                logger.log(TRACE, "Type " + type);
                disposeTypePV();
                // Notify model to redraw this PV
                model.itemUpdated(TreeModelItem.this);
                fetchLinks();
            });
            type_pv.set(pv);
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Cannot fetch RTYP for " + this, ex);
        }
    }

    private void fetchLinks()
    {
        // Avoid loops
        final TreeModelItem dup = model.findDuplicate(this);
        if (dup != null)
        {
            logger.log(TRACE, "Known item " + record_name + "." + info + ", not traversing inputs (again)");
            return;
        }
        final List<String> type_links = Settings.field_info.get(type);
        if (type_links == null)
        {
            logger.log(TRACE, "Type " + type + " has no known links");
            return;
        }
        // Could fetch all links in parallel,
        // but to keep the model clean we drop empty links.
        // Reading them one by one, then adding only
        // those links that have a value,
        // reduces the number of UI updates.
        // It results in a steadily 'growing' tree as opposed
        // to a tree that initially shows all yet-to-be-resolved links,
        // then removes the empty ones while
        // expanding the non-empty subtrees
        links_to_read.addAll(type_links);
        model.incrementLinks(links_to_read.size());
        resolveNextLink();
    }

    private void resolveNextLink()
    {
        final String field = links_to_read.peek();
        if (field == null)
            return;

        String link_name = record_name + "." + field;
        if (Settings.read_long_fields)
            link_name += "$";
        try
        {
            logger.log(TRACE, "Resolve " + link_name);
            final PV pv = PVPool.getPV(link_name);
            link_flow = pv.onValueEvent().firstOrError().subscribe(link_consumer);
            link_pv.set(pv);
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Cannot create link PV" + link_name, ex);
        }
    }

    /** Update value (and severity) from 'current_value/severity'
     *  .. unless the model is 'frozen'
     */
    void updateValue()
    {
        if (model.isLatched()  &&  value != null)
            return;
        value = current_value;
        severity = current_severity;
        model.itemUpdated(this);
    }

    private void disposeLinkPV()
    {
        final PV pv = link_pv.getAndSet(null);
        if (pv == null)
            return;
        link_flow.dispose();
        PVPool.releasePV(pv);
    }

    private void disposeTypePV()
    {
        final PV pv = type_pv.getAndSet(null);
        if (pv == null)
            return;
        type_flow.dispose();
        PVPool.releasePV(pv);
    }

    private void disposeValuePV()
    {
        final PV pv = value_pv.getAndSet(null);
        if (pv == null)
            return;
        value_flow.dispose();
        PVPool.releasePV(pv);
    }

    void dispose()
    {
        disposeValuePV();
        disposeLinkPV();
        disposeTypePV();
        for (TreeModelItem link : links)
            link.dispose();
        links.clear();
    }

    /** @return Returns a String. No really, it does! */
    @Override
    public String toString()
    {
        final StringBuffer b = new StringBuffer();
        b.append(info).append(" '").append(pv_name).append("'");
        if (type == CONSTANT_TYPE)
            b.append(' ').append(CONSTANT_TYPE);
        else
        {
            b.append(" (").append(type).append(")");
            if (value != null)
                b.append("  =  ").append(value);
            else
                b.append(" [DISCONNECTED]");
        }
        return b.toString();
    }
}
