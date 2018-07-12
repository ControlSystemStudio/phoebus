/*******************************************************************************
 * Copyright (c) 2015 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.properties;

import static org.csstudio.display.builder.model.ModelPlugin.logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

import javax.xml.stream.XMLStreamWriter;

import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.WidgetPropertyDescriptor;
import org.csstudio.display.builder.model.macros.MacroXMLUtil;
import org.csstudio.display.builder.model.persist.ModelReader;
import org.csstudio.display.builder.model.persist.ModelWriter;
import org.csstudio.display.builder.model.persist.XMLTags;
import org.csstudio.display.builder.model.properties.OpenDisplayActionInfo.Target;
import org.phoebus.framework.macros.Macros;
import org.phoebus.framework.persistence.XMLUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/** Widget property that describes actions.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ActionsWidgetProperty extends WidgetProperty<ActionInfos>
{
    private static final String OPEN_DISPLAY = "open_display";
    private static final String WRITE_PV = "write_pv";
    private static final String EXECUTE_SCRIPT = "execute";
    private static final String EXECUTE_COMMAND = "command";
    private static final String OPEN_FILE = "open_file";
    private static final String OPEN_WEBPAGE = "open_webpage";

    /** Constructor
     *  @param descriptor Property descriptor
     *  @param widget Widget that holds the property and handles listeners
     *  @param default_value Default and initial value
     */
    public ActionsWidgetProperty(
            final WidgetPropertyDescriptor<ActionInfos> descriptor,
            final Widget widget,
            final ActionInfos default_value)
    {
        super(descriptor, widget, default_value);
    }

    /** @param value Must be ActionInfos or ActionInfo array(!), not List */
    @Override
    public void setValueFromObject(final Object value) throws Exception
    {
        if (value instanceof ActionInfos)
            setValue((ActionInfos) value);
        else if (value instanceof ActionInfo[])
            setValue(new ActionInfos(Arrays.asList((ActionInfo[]) value)));
        else if ((value instanceof Collection) &&
                ((Collection<?>)value).isEmpty())
           setValue(new ActionInfos(Collections.emptyList()));
        else
            throw new Exception("Need ActionInfos or ActionInfo[], got " + value);
    }

    /** @param mode One of the modes from org.csstudio.opibuilder.runmode.RunModeService.DisplayMode
     *  @return
     */
    private Target modeToTargetConvert(int mode)
    {
        switch (mode)
        {
        // 0 - REPLACE
        case 0: return OpenDisplayActionInfo.Target.REPLACE;
        // 7 - NEW_WINDOW
        // 8 - NEW_SHELL
        case 7:
        case 8: return OpenDisplayActionInfo.Target.WINDOW;
        // 1 - NEW_TAB
        // 2 - NEW_TAB_LEFT
        // 3 - NEW_TAB_RIGHT
        // 4 - NEW_TAB_TOP
        // 5 - NEW_TAB_BOTTOM
        // 6 - NEW_TAB_DETACHED
        default: return OpenDisplayActionInfo.Target.TAB;
        }
    }

    @Override
    public void writeToXML(final ModelWriter model_writer, final XMLStreamWriter writer) throws Exception
    {
        if (value.isExecutedAsOne())
            writer.writeAttribute(XMLTags.EXECUTE_AS_ONE, Boolean.TRUE.toString());
        for (final ActionInfo info : value.getActions())
        {
            // <action type="..">
            writer.writeStartElement(XMLTags.ACTION);
            if (info instanceof OpenDisplayActionInfo)
            {
                final OpenDisplayActionInfo action = (OpenDisplayActionInfo) info;
                writer.writeAttribute(XMLTags.TYPE, OPEN_DISPLAY);
                writer.writeStartElement(XMLTags.FILE);
                writer.writeCharacters(action.getFile());
                writer.writeEndElement();
                if (! action.getMacros().getNames().isEmpty())
                {
                    writer.writeStartElement(XMLTags.MACROS);
                    MacroXMLUtil.writeMacros(writer, action.getMacros());
                    writer.writeEndElement();
                }
                writer.writeStartElement(XMLTags.TARGET);
                writer.writeCharacters(action.getTarget().name().toLowerCase());
                writer.writeEndElement();
                if (action.getPane().length() > 0)
                {
                    writer.writeStartElement(XMLTags.NAME);
                    writer.writeCharacters(action.getPane());
                    writer.writeEndElement();
                }
            }
            else if (info instanceof WritePVActionInfo)
            {
                final WritePVActionInfo action = (WritePVActionInfo) info;
                writer.writeAttribute(XMLTags.TYPE, WRITE_PV);
                writer.writeStartElement(XMLTags.PV_NAME);
                writer.writeCharacters(action.getPV());
                writer.writeEndElement();
                writer.writeStartElement(XMLTags.VALUE);
                writer.writeCharacters(action.getValue());
                writer.writeEndElement();
            }
            else if (info instanceof ExecuteScriptActionInfo)
            {
                final ExecuteScriptActionInfo action = (ExecuteScriptActionInfo) info;
                final ScriptInfo script = action.getInfo();
                writer.writeAttribute(XMLTags.TYPE, EXECUTE_SCRIPT);
                writer.writeStartElement(XMLTags.SCRIPT);
                writer.writeAttribute(XMLTags.FILE, script.getPath());
                final String text = script.getText();
                if (text != null)
                {
                    writer.writeStartElement(XMLTags.TEXT);
                    writer.writeCData(text);
                    writer.writeEndElement();
                }
                writer.writeEndElement();
            }
            else if (info instanceof OpenFileActionInfo)
            {
                final OpenFileActionInfo action = (OpenFileActionInfo) info;
                writer.writeAttribute(XMLTags.TYPE, OPEN_FILE);
                writer.writeStartElement(XMLTags.FILE);
                writer.writeCharacters(action.getFile());
                writer.writeEndElement();
            }
            else if (info instanceof OpenWebpageActionInfo)
            {
                final OpenWebpageActionInfo action = (OpenWebpageActionInfo) info;
                writer.writeAttribute(XMLTags.TYPE, OPEN_WEBPAGE);
                writer.writeStartElement(XMLTags.URL);
                writer.writeCharacters(action.getURL());
                writer.writeEndElement();
            }
            else if (info instanceof ExecuteCommandActionInfo)
            {
                final ExecuteCommandActionInfo action = (ExecuteCommandActionInfo) info;
                writer.writeAttribute(XMLTags.TYPE, EXECUTE_COMMAND);
                writer.writeStartElement(XMLTags.COMMAND);
                writer.writeCharacters(action.getCommand());
                writer.writeEndElement();
            }
            else
                throw new Exception("Cannot write action of type " + info.getClass().getName());
            if (! info.getDescription().isEmpty())
            {
                writer.writeStartElement(XMLTags.DESCRIPTION);
                writer.writeCharacters(info.getDescription());
                writer.writeEndElement();
            }
            // </action>
            writer.writeEndElement();
        }
    }

    @Override
    public void readFromXML(final ModelReader model_reader, final Element property_xml) throws Exception
    {
        final boolean execute_as_one =
            Boolean.parseBoolean(property_xml.getAttribute(XMLTags.EXECUTE_AS_ONE))  ||
            Boolean.parseBoolean(property_xml.getAttribute("hook_all")); // Legacy files

        final List<ActionInfo> actions = new ArrayList<>();
        for (final Element action_xml : XMLUtil.getChildElements(property_xml, XMLTags.ACTION))
        {
            String type = action_xml.getAttribute(XMLTags.TYPE);

            if ("OPEN_OPI_IN_VIEW".equals(type))
            {   // No longer supporting open-in-view with <Position>
                // to select left, right, ...  part stack.
                // Change into 'open display' for new tab
                type = OPEN_DISPLAY;
                final Document doc = action_xml.getOwnerDocument();
                final Element target = doc.createElement(XMLTags.TARGET);
                target.appendChild(doc.createTextNode(OpenDisplayActionInfo.Target.TAB.name()));
                action_xml.appendChild(target);
            }

            final String description = XMLUtil.getChildString(action_xml, XMLTags.DESCRIPTION).orElse("");
            if (OPEN_DISPLAY.equalsIgnoreCase(type)) // legacy used uppercase type name
            {   // Use <file>, falling back to legacy <path>
                final String file = XMLUtil.getChildString(action_xml, XMLTags.FILE)
                                           .orElse(XMLUtil.getChildString(action_xml, XMLTags.PATH)
                                           .orElse(""));

                OpenDisplayActionInfo.Target target = OpenDisplayActionInfo.Target.REPLACE;
                // Legacy used <replace> with value 0/1/2 for TAB/REPLACE/WINDOW
                final Optional<String> replace = XMLUtil.getChildString(action_xml, "replace");
                // later it switched to <mode> with many more options
                final Optional<String> mode = XMLUtil.getChildString(action_xml, "mode");
                if (replace.isPresent())
                {
                    if ("0".equals(replace.get()))
                        target = OpenDisplayActionInfo.Target.TAB;
                    else if ("2".equals(replace.get()))
                        target = OpenDisplayActionInfo.Target.WINDOW;
                }
                else if (mode.isPresent())
                    target = modeToTargetConvert(Integer.valueOf(mode.get()));
                else
                    target = OpenDisplayActionInfo.Target.valueOf(
                            XMLUtil.getChildString(action_xml, XMLTags.TARGET)
                            .orElse(OpenDisplayActionInfo.Target.REPLACE.name())
                            .toUpperCase() );

                final Macros macros;
                final Element macro_xml = XMLUtil.getChildElement(action_xml, XMLTags.MACROS);
                if (macro_xml != null)
                    macros = MacroXMLUtil.readMacros(macro_xml);
                else
                    macros = new Macros();

                final String pane = XMLUtil.getChildString(action_xml, XMLTags.NAME).orElse("");

                actions.add(new OpenDisplayActionInfo(description, file, macros, target, pane));
            }
            else if (WRITE_PV.equalsIgnoreCase(type)) // legacy used uppercase type name
            {
                // Compare legacy XML:
                // <action type="WRITE_PV">
                //     <pv_name>$(M).TWR</pv_name>
                //     <value>1</value>
                //     <timeout>10</timeout>
                //     <confirm_message/>
                //     <description>-</description>
                // </action>

                // PV Name should be set.
                final String pv_name = XMLUtil.getChildString(action_xml, XMLTags.PV_NAME).orElse("");
                if (pv_name.isEmpty())
                    logger.log(Level.WARNING, "Ignoring <action type='" + WRITE_PV + "'> with empty <pv_name> on " + getWidget());

                // PV may be empty to write "".
                // In contrast to legacy opibuilder the value is _not_ trimmed,
                // so it's possible to write "   " (which opibuilder wrote as "")
                final String value = XMLUtil.getChildString(action_xml, XMLTags.VALUE).orElse("");
                actions.add(new WritePVActionInfo(description, pv_name, value));
            }
            else if (EXECUTE_SCRIPT.equals(type))
            {
                // <script file="EmbeddedPy">
                //   <text>  the embedded text  </text>
                // </script>
                final Element el = XMLUtil.getChildElement(action_xml, XMLTags.SCRIPT);
                if (el == null)
                    throw new Exception("Missing <script..>");
                else
                {
                    final String path = el.getAttribute(XMLTags.FILE);
                    final String text = XMLUtil.getChildString(el, XMLTags.TEXT).orElse(null);
                    final ScriptInfo info = new ScriptInfo(path, text, false, Collections.emptyList());
                    actions.add(new ExecuteScriptActionInfo(description, info));
                }
            }
            else if ("EXECUTE_PYTHONSCRIPT".equalsIgnoreCase(type) ||
                     "EXECUTE_JAVASCRIPT".equalsIgnoreCase(type))
            {
                // Legacy XML:
                // <action type="EXECUTE_PYTHONSCRIPT"> .. or "EXECUTE_JAVASCRIPT"
                //     <path>script.py</path>
                //     <scriptText><![CDATA[ /* The script */ ]]></scriptText>
                //     <embedded>false</embedded>
                //     <description>A script</description>
                // </action>
                final boolean embed = Boolean.parseBoolean(XMLUtil.getChildString(action_xml, "embedded").orElse("false"));
                final String path = XMLUtil.getChildString(action_xml, XMLTags.PATH).orElse("");
                final String text = XMLUtil.getChildString(action_xml, "scriptText").orElse("");
                final ScriptInfo info;
                if (embed)
                {
                    final String dialect = type.contains("PYTHON")
                            ? ScriptInfo.EMBEDDED_PYTHON : ScriptInfo.EMBEDDED_JAVASCRIPT;
                    info = new ScriptInfo(dialect, text, false, Collections.emptyList());
                }
                else
                    info = new ScriptInfo(path, null, false, Collections.emptyList());
                actions.add(new ExecuteScriptActionInfo(description, info));
            }
            else if (OPEN_FILE.equalsIgnoreCase(type)) // legacy used uppercase type name
            {   // Use <file>, falling back to legacy <path>
                final String file = XMLUtil.getChildString(action_xml, XMLTags.FILE)
                                           .orElse(XMLUtil.getChildString(action_xml, XMLTags.PATH)
                                           .orElse(""));
                actions.add(new OpenFileActionInfo(description, file));
            }
            else if (OPEN_WEBPAGE.equalsIgnoreCase(type)) // legacy used uppercase type name
            {   // Use <url>, falling back to legacy <hyperlink>
                final String url = XMLUtil.getChildString(action_xml, XMLTags.URL)
                                           .orElse(XMLUtil.getChildString(action_xml, "hyperlink")
                                           .orElse(""));
                actions.add(new OpenWebpageActionInfo(description, url));
            }
            else if (EXECUTE_COMMAND.equalsIgnoreCase(type) ||
                    "EXECUTE_CMD".equalsIgnoreCase(type))
            {
                // Legacy:
                // <action type="EXECUTE_CMD">
                //   <command>echo Hello</command>
                //   <command_directory>$(user.home)</command_directory>
                //   <wait_time>10</wait_time>
                //   <description>Hello</description>
                // </action>
                //
                // New:
                // <action type="command">
                //   <command>echo Hello</command>
                //   <description>Hello</description>
                // </action>
                String command = XMLUtil.getChildString(action_xml, XMLTags.COMMAND).orElse("");
                String directory = XMLUtil.getChildString(action_xml, "command_directory")
                                                .orElse(null);
                // Legacy allowed "opi.dir" as magic macro.
                // Commands are now by default resolved relative to the display file.
                if ("$(opi.dir)".equals(directory))
                    directory = null;
                // Legacy allowed user.home as a 'current working directory'.
                // Commands are now executed with their location as cwd.
                if ("$(user.home)".equals(directory))
                    directory = null;
                // If a legacy directory was provided, locate command there
                if (directory != null  &&  !directory.isEmpty())
                    command = directory + "/" + command;
                actions.add(new ExecuteCommandActionInfo(description, command));
            }
            else
                logger.log(Level.WARNING, "Ignoring action of unknown type '" + type + "'");
        }
        setValue(new ActionInfos(actions, execute_as_one));
    }

    @Override
    public String toString()
    {
        return ActionInfos.toString(value.getActions());
    }
}
