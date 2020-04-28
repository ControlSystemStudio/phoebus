/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.model.json;

import static org.phoebus.applications.alarm.AlarmSystem.logger;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import org.phoebus.applications.alarm.client.AlarmClientLeaf;
import org.phoebus.applications.alarm.client.AlarmClientNode;
import org.phoebus.applications.alarm.client.ClientState;
import org.phoebus.applications.alarm.model.AlarmTreeItem;
import org.phoebus.applications.alarm.model.AlarmTreeLeaf;
import org.phoebus.applications.alarm.model.BasicState;
import org.phoebus.applications.alarm.model.SeverityLevel;
import org.phoebus.applications.alarm.model.TitleDetail;
import org.phoebus.applications.alarm.model.TitleDetailDelay;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;

/** Read alarm model from JSON
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class JsonModelReader
{
    // The following use 'Object' for the json node instead of actual
    // JsonNode to keep Jackson specifics within this package.
    // Later updates of the JsonModelReader/Writer will not affect code
    // that calls them.

    /** Parse JSON text
     *  @param json_text JSON text
     *  @return JSON object
     *  @throws Exception
     */
    public static Object parseJsonText(final String json_text) throws Exception
    {
        try
        (
            final JsonParser jp = JsonModelWriter.mapper.getFactory().createParser(json_text);
        )
        {
            return JsonModelWriter.mapper.readTree(jp);
        }
    }

    /** Is this the configuration or alarm state for a leaf?
     *  @param json JSON returned by {@link #parseJsonText(String)}
     *  @return <code>true</code> for {@link AlarmTreeLeaf}, <code>false</code> for {@link AlarmClientNode}
     */
    public static boolean isLeafConfigOrState(final Object json)
    {
        final JsonNode actual = (JsonNode) json;
        // Leaf config contains description
        // Leaf alarm state contains detail of AlarmState
        return actual.get(JsonTags.DESCRIPTION) != null  ||  actual.get(JsonTags.CURRENT_SEVERITY) != null;
    }

    /** Is this a configuration or state message?
     *  @param json JSON returned by {@link #parseJsonText(String)}
     *  @return <code>true</code> for {@link AlarmTreeLeaf}, <code>false</code> for {@link AlarmClientNode}
     */
    public static boolean isStateUpdate(final Object json)
    {
        final JsonNode actual = (JsonNode) json;
        // State updates contain SEVERITY
        return actual.get(JsonTags.SEVERITY) != null;
    }

    /** Update configuration of alarm tree item
     *  @param node {@link AlarmTreeItem}
     *  @param json JSON returned by {@link #parseAlarmItemConfig(String)}
     *  @return <code>true</code> if configuration changed, <code>false</code> if there was nothing to update
     */
    public static boolean updateAlarmItemConfig(final AlarmTreeItem<?> node, final Object json)
    {
        final JsonNode actual = (JsonNode) json;
        boolean changed = updateAlarmNodeConfig(node, actual);
        if (node instanceof AlarmTreeLeaf)
            changed |= updateAlarmLeafConfig((AlarmTreeLeaf) node, actual);
        return changed;
    }

    /** Update general {@link AlarmTreeItem} settings */
    private static boolean updateAlarmNodeConfig(final AlarmTreeItem<?> node, final JsonNode json)
    {
        boolean changed = false;

        JsonNode jn = json.get(JsonTags.GUIDANCE);
        if (jn == null)
            changed |= node.setGuidance(Collections.emptyList());
        else
            changed |= node.setGuidance(parseTitleDetail(jn));

        jn = json.get(JsonTags.DISPLAYS);
        if (jn == null)
            changed |= node.setDisplays(Collections.emptyList());
        else
            changed |= node.setDisplays(patchDisplays(parseTitleDetail(jn)));

        jn = json.get(JsonTags.COMMANDS);
        if (jn == null)
            changed |= node.setCommands(Collections.emptyList());
        else
            changed |= node.setCommands(parseTitleDetail(jn));

        jn = json.get(JsonTags.ACTIONS);
        if (jn == null)
            changed |= node.setActions(Collections.emptyList());
        else
            changed |= node.setActions(parseTitleDetailDelay(jn));

        return changed;
    }

    /** Patch legacy display links
     *
     *  <p>Remove "opi:" prefix from web links.
     *  Used to be necessary to force display runtime for http://...opi,
     *  but file extension now sufficient.
     *
     *  @param displays Original displays
     *  @return Displays where entries may be replaced
     */
    private static List<TitleDetail> patchDisplays(final List<TitleDetail> displays)
    {
        final int N = displays.size();
        for (int i=0; i<N; ++i)
        {
            final TitleDetail orig = displays.get(i);
            if (orig.detail.startsWith("opi:"))
            {
                final String update = orig.detail.substring(4);
                displays.set(i, new TitleDetail(orig.title, update));
                logger.log(Level.FINE, "Removing 'opi:' prefix from display link '" + orig.detail + "'");
            }
        }
        return displays;
    }

    private static List<TitleDetail> parseTitleDetail(final JsonNode array)
    {
        final List<TitleDetail> entries = new ArrayList<>(array.size());
        for (int i=0; i<array.size(); ++i)
        {
            final JsonNode info = array.get(i);

            JsonNode jn = info.get(JsonTags.TITLE);
            final String title = jn == null ? "" : jn.asText();

            jn = info.get(JsonTags.DETAILS);
            final String details = jn == null ? "" : jn.asText();

            entries.add(new TitleDetail(title, details));
        }
        return entries;
    }

    private static List<TitleDetailDelay> parseTitleDetailDelay(final JsonNode array)
    {
        final List<TitleDetailDelay> entries = new ArrayList<>(array.size());
        for (int i=0; i<array.size(); ++i)
        {
            final JsonNode info = array.get(i);

            JsonNode jn = info.get(JsonTags.TITLE);
            final String title = jn == null ? "" : jn.asText();

            jn = info.get(JsonTags.DETAILS);
            final String details = jn == null ? "" : jn.asText();

            jn = info.get(JsonTags.DELAY);
            final int delay = jn == null ? 0 : jn.asInt();

            entries.add(new TitleDetailDelay(title, details, delay));
        }
        return entries;
    }

    /** Update specifics of {@link AlarmTreeLeaf} */
    private static boolean updateAlarmLeafConfig(final AlarmTreeLeaf node, final JsonNode json)
    {
        // Is this a leaf configuration message?
        JsonNode jn = json.get(JsonTags.DESCRIPTION);
        if (jn == null)
            return false;

        boolean changed = node.setDescription(jn.asText());

        jn = json.get(JsonTags.ENABLED);
        changed |= node.setEnabled(jn == null ? true : jn.asBoolean());

        jn = json.get(JsonTags.LATCHING);
        changed |= node.setLatching(jn == null ? true : jn.asBoolean());

        jn = json.get(JsonTags.ANNUNCIATING);
        changed |= node.setAnnunciating(jn == null ? true : jn.asBoolean());

        jn = json.get(JsonTags.DELAY);
        changed |= node.setDelay(jn == null ? 0 : jn.asInt());

        jn = json.get(JsonTags.COUNT);
        changed |= node.setCount(jn == null ? 0 : jn.asInt());

        jn = json.get(JsonTags.FILTER);
        changed |= node.setFilter(jn == null ? "" : jn.asText());

        return changed;
    }

    /** Check for 'maintenance' mode indicator,
     *  included in alarm state updates
     *  @param json
     *  @return <code>true</code> if in maintenance mode
     */
    public static boolean isMaintenanceMode(final Object json)
    {
        final JsonNode actual = (JsonNode) json;
        JsonNode jn = actual.get(JsonTags.MODE);
        if (jn != null)
            return JsonTags.MAINTENANCE.equals(jn.asText());
        return false;
    }

    /** Check for 'notify' mode indicator,
     *  included in alarm state updates
     *  @param json
     *  @return <code>true</code> if in disable_notify mode
     */
    public static boolean isDisableNotify(final Object json)
    {
        final JsonNode actual = (JsonNode) json;
        JsonNode jn = actual.get(JsonTags.NOTIFY);
	return jn == null ? false : !jn.asBoolean();
    }

    public static boolean updateAlarmState(final AlarmTreeItem<?> node, final Object json)
    {
        final JsonNode actual = (JsonNode) json;
        if (node instanceof AlarmClientLeaf)
            return updateAlarmLeafState((AlarmClientLeaf) node, actual);
        if (node instanceof AlarmClientNode)
            return updateAlarmNodeState((AlarmClientNode) node, actual);
        return false;
    }

    /** @param _json JSon that might contain {@link ClientState}
     *  @return {@link ClientState} or <code>null</code>
     */
    public static ClientState parseClientState(final Object _json)
    {
        final JsonNode json = (JsonNode) _json;
        SeverityLevel severity = SeverityLevel.UNDEFINED;
        String message = "<?>";
        String value = "<?>";
        Instant time = null;
        SeverityLevel current_severity = SeverityLevel.UNDEFINED;
        String current_message = "<?>";

        JsonNode jn = json.get(JsonTags.SEVERITY);
        if (jn == null)
            return null;
        severity = SeverityLevel.valueOf(jn.asText());

        jn = json.get(JsonTags.LATCH);
        final boolean latch = jn != null  &&  Boolean.parseBoolean(jn.asText());

        jn = json.get(JsonTags.MESSAGE);
        if (jn == null)
            return null;
        message = jn.asText();

        jn = json.get(JsonTags.VALUE);
        if (jn == null)
            return null;
        value = jn.asText();

        jn = json.get(JsonTags.CURRENT_SEVERITY);
        if (jn == null)
            return null;
        current_severity = SeverityLevel.valueOf(jn.asText());

        jn = json.get(JsonTags.CURRENT_MESSAGE);
        if (jn == null)
            return null;
        current_message = jn.asText();

        jn = json.get(JsonTags.TIME);
        if (jn == null)
            return null;

        long secs = 0, nano = 0;
        JsonNode sub = jn.get(JsonTags.SECONDS);
        if (sub != null)
            secs = sub.asLong();
        sub = jn.get(JsonTags.NANO);
        if (sub != null)
            nano = sub.asLong();
        time = Instant.ofEpochSecond(secs, nano);

        return new ClientState(severity, message, value, time, current_severity, current_message, latch);
    }

    /** @param node Node to update from json
     *  @param json Json that might contain {@link ClientState}
     *  @return <code>true</code> if this changed the alarm state of the node
     */
    private static boolean updateAlarmLeafState(final AlarmClientLeaf node, final JsonNode json)
    {
        final ClientState state = parseClientState(json);
        return (state != null)  &&  node.setState(state);
    }

    private static boolean updateAlarmNodeState(final AlarmClientNode node, final JsonNode json)
    {
        SeverityLevel severity = SeverityLevel.UNDEFINED;

        JsonNode jn = json.get(JsonTags.SEVERITY);
        if (jn == null)
            return false;
        severity = SeverityLevel.valueOf(jn.asText());

        // Compare alarm state with node's current state
        if (node.getState().severity == severity)
            return false;
        final BasicState state = new BasicState(severity);
        node.setState(state);

        return true;
    }
}
