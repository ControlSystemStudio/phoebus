/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.model.json;

import java.io.ByteArrayOutputStream;
import java.util.List;

import org.phoebus.applications.alarm.client.ClientState;
import org.phoebus.applications.alarm.client.IdentificationHelper;
import org.phoebus.applications.alarm.model.AlarmState;
import org.phoebus.applications.alarm.model.AlarmTreeItem;
import org.phoebus.applications.alarm.model.AlarmTreeLeaf;
import org.phoebus.applications.alarm.model.BasicState;
import org.phoebus.applications.alarm.model.SeverityLevel;
import org.phoebus.applications.alarm.model.TitleDetail;
import org.phoebus.applications.alarm.model.TitleDetailDelay;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Write alarm model as JSON
 *  @author Kay Kasemir
 *  @author Evan Smith
 */
@SuppressWarnings("nls")
public class JsonModelWriter
{
    // Use Jackson ObjectMapper where it can be used
    // without adding annotations to data classes,
    // or without making immutable objects mutable.
    //
    // Otherwise use streaming JsonGenerator resp. JsonParser,
    // which is faster anyway and allows JSON code to be
    // limited to this package
    public static final ObjectMapper mapper = new ObjectMapper();

    /** @param state {@link BasicState} or {@link ClientState}
     *  @param maintenance_mode true if in maintenance mode
     *  @return Byte array for JSON text
     *  @throws Exception on error
     */
    public static byte[] toJsonBytes(final BasicState state, final boolean maintenance_mode, final boolean disable_notify) throws Exception
    {
        final ByteArrayOutputStream buf = new ByteArrayOutputStream();
        try
        (
            JsonGenerator jg = mapper.getFactory().createGenerator(buf);
        )
        {
            jg.writeStartObject();
            jg.writeStringField(JsonTags.SEVERITY, state.severity.name());
            if (state instanceof AlarmState)
            {
                final AlarmState as = (AlarmState) state;
                if (as.isLatched())
                    jg.writeBooleanField(JsonTags.LATCH, true);
            }
            if (state instanceof ClientState)
            {
                final ClientState as = (ClientState) state;
                jg.writeStringField(JsonTags.MESSAGE, as.message);
                jg.writeStringField(JsonTags.VALUE, as.value);
                {
                    jg.writeObjectFieldStart(JsonTags.TIME);
                    jg.writeNumberField(JsonTags.SECONDS, as.time.getEpochSecond());
                    jg.writeNumberField(JsonTags.NANO, as.time.getNano());
                    jg.writeEndObject();
                }
                jg.writeStringField(JsonTags.CURRENT_SEVERITY, as.current_severity.name());
                jg.writeStringField(JsonTags.CURRENT_MESSAGE, as.current_message);
            }
            if (maintenance_mode)
            {
                jg.writeStringField(JsonTags.MODE, JsonTags.MAINTENANCE);
            }
	    if (disable_notify)
            {
                jg.writeBooleanField(JsonTags.NOTIFY, false);
            }
            jg.writeEndObject();
        }
        return buf.toByteArray();
    }

    public static String toJsonString(final AlarmTreeItem<?> item) throws Exception
    {
        return toJson(item).toString();
    }

    public static byte[] toJsonBytes(final AlarmTreeItem<?> item) throws Exception
    {
        return toJson(item).toByteArray();
    }

    private static ByteArrayOutputStream toJson(final AlarmTreeItem<?> item) throws Exception
    {
        final ByteArrayOutputStream buf = new ByteArrayOutputStream();
        try
        (
            JsonGenerator jg = mapper.getFactory().createGenerator(buf);
        )
        {
            jg.writeStartObject();

            jg.writeStringField(JsonTags.USER, IdentificationHelper.getUser());
            jg.writeStringField(JsonTags.HOST, IdentificationHelper.getHost());

            if (item instanceof AlarmTreeLeaf)
                writeLeafDetail(jg, (AlarmTreeLeaf) item);

            writeTitleDetail(jg, JsonTags.GUIDANCE, item.getGuidance());
            writeTitleDetail(jg, JsonTags.DISPLAYS, item.getDisplays());
            writeTitleDetail(jg, JsonTags.COMMANDS, item.getCommands());
            writeTitleDetailDelay(jg, JsonTags.ACTIONS, item.getActions());

            jg.writeEndObject();
        }
        return buf;
    }

    private static void writeLeafDetail(final JsonGenerator jg, final AlarmTreeLeaf item) throws Exception
    {
        jg.writeStringField(JsonTags.DESCRIPTION, item.getDescription());
        if (! item.isEnabled())
            jg.writeBooleanField(JsonTags.ENABLED, false);
        if (! item.isLatching())
            jg.writeBooleanField(JsonTags.LATCHING, false);
        if (! item.isAnnunciating())
            jg.writeBooleanField(JsonTags.ANNUNCIATING, false);
        if (item.getDelay() > 0)
            jg.writeNumberField(JsonTags.DELAY, item.getDelay());
        if (item.getCount() > 0)
            jg.writeNumberField(JsonTags.COUNT, item.getCount());
        if (! item.getFilter().isEmpty())
            jg.writeStringField(JsonTags.FILTER, item.getFilter());
    }

    private static void writeTitleDetail(final JsonGenerator jg, final String name, final List<TitleDetail> infos) throws Exception
    {
        if (infos.isEmpty())
            return;

        jg.writeArrayFieldStart(name);
        for (TitleDetail info : infos)
        {
            jg.writeStartObject();
            jg.writeStringField(JsonTags.TITLE, info.title);
            jg.writeStringField(JsonTags.DETAILS, info.detail);
            jg.writeEndObject();
        }
        jg.writeEndArray();
    }

    private static void writeTitleDetailDelay(final JsonGenerator jg, final String name, final List<TitleDetailDelay> infos) throws Exception
    {
        if (infos.isEmpty())
            return;

        jg.writeArrayFieldStart(name);
        {
            for (TitleDetailDelay info : infos)
            {
                jg.writeStartObject();
                jg.writeStringField(JsonTags.TITLE, info.title);
                jg.writeStringField(JsonTags.DETAILS, info.detail);
                jg.writeNumberField(JsonTags.DELAY, info.delay);
                jg.writeEndObject();
            }
        }
        jg.writeEndArray();
    }

    /**
     * Create a JSON byte array of a command.
     * @param cmd - Command
     * @return byte[]
     * @throws Exception
     * @author Evan Smith
     */
    public static byte[] commandToBytes(final String cmd) throws Exception
    {
        final ByteArrayOutputStream buf = new ByteArrayOutputStream();
        try
        (
            JsonGenerator jg = mapper.getFactory().createGenerator(buf);
        )
        {
            jg.writeStartObject();
            jg.writeStringField(JsonTags.USER, IdentificationHelper.getUser());
            jg.writeStringField(JsonTags.HOST, IdentificationHelper.getHost());
            jg.writeStringField(JsonTags.COMMAND, cmd);
            jg.writeEndObject();
        }
        return buf.toByteArray();
    }

    /**
     * Create a JSON byte array of a *Talk topic message.
     * <p> This method handles the parsing of '*' and '!' in regards to the message format, and whether the message can be silenced.
     * @see <a href="http://cs-studio.sourceforge.net/docbook/ch14.html#fig_annunciator_view">CSS Annunciator View Docs</a>
     * @param severity
     * @param description
     * @return
     * @throws Exception
     */
    public static String talkToString(final SeverityLevel severity, final String description) throws Exception
    {
        String message = description; // Message to be annunciated.
        boolean noSev = false;      // Message should include alarm severity.
        boolean standout = false;   // Message should always be annunciated.

        int beginIndex = 0; // Beginning index of description substring.

        if (description.startsWith("*"))
        {
            noSev = true;
            beginIndex++;
        }
        if (description.substring(beginIndex).startsWith("!"))
        {
            standout = true;
            beginIndex++;
        }

        // The message should not include '*' or '!'.
        // If '*' or '!' is the entirety of the description, message will be an empty string.
        message = description.substring(beginIndex);

        // Add the severity if appropriate.
        if (! noSev)
        {
            message = severity.toString() + " Alarm: " + message;
        }

        final ByteArrayOutputStream buf = new ByteArrayOutputStream();
        try
        (
            JsonGenerator jg = mapper.getFactory().createGenerator(buf);
        )
        {
            jg.writeStartObject();
            jg.writeBooleanField(JsonTags.STANDOUT, standout);
            jg.writeStringField(JsonTags.SEVERITY, severity.toString());
            jg.writeStringField(JsonTags.TALK, message);
            jg.writeEndObject();
        }
        return buf.toString();
    }

   /** Create a deletion message for identifying who is creating a kafka tombstone
    *  @return byte[]
    *  @throws Exception
    */
    public static byte[] deleteMessageToBytes() throws Exception
    {
        final ByteArrayOutputStream buf = new ByteArrayOutputStream();
        try
        (
            JsonGenerator jg = mapper.getFactory().createGenerator(buf);
        )
        {
            final String user = IdentificationHelper.getUser();
            final String host = IdentificationHelper.getHost();
            // Why a delete occurred can be important. Perhaps in the future allow for this to be user set through some type of dialog.
            final String msg  = "Deleting";

            jg.writeStartObject();
            jg.writeStringField(JsonTags.USER, user);
            jg.writeStringField(JsonTags.HOST, host);
            jg.writeStringField(JsonTags.DELETE, msg);
            jg.writeEndObject();
        }
        return buf.toByteArray();
    }
}
