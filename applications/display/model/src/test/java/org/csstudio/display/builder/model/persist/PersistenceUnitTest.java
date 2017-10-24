/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.persist;

import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propName;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.csstudio.display.builder.model.CustomWidget;
import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetFactoryUnitTest;
import org.csstudio.display.builder.model.WidgetPropertyDescriptor;
import org.csstudio.display.builder.model.properties.CommonWidgetProperties;
import org.csstudio.display.builder.model.properties.NamedWidgetFont;
import org.csstudio.display.builder.model.properties.StringWidgetProperty;
import org.csstudio.display.builder.model.properties.WidgetFontStyle;
import org.csstudio.display.builder.model.widgets.GroupWidget;
import org.csstudio.display.builder.model.widgets.LabelWidget;
import org.junit.BeforeClass;
import org.junit.Test;

/** JUnit test of widget persistence
 *
 *  <p>To read this properly, must use 'UTF-8'
 *  Eclipse Window -> Preferences -> General -> Workspace: Text file encoding
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PersistenceUnitTest
{
    private String getExampleFile() throws Exception
    {
        final BufferedReader reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("persist_example.xml")));
        final StringBuilder buf = new StringBuilder();
        String line;
        while ( (line = reader.readLine()) != null)
            buf.append(line).append("\n");
        return buf.toString();
    }

    /** Initialize factory for tests */
    @BeforeClass
    public static void setup()
    {
        WidgetFactoryUnitTest.initializeFactory();
    }

    protected String toXML(final DisplayModel model)
            throws Exception, IOException
    {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        try
        (
            final ModelWriter writer = new ModelWriter(out);
        )
        {
            writer.writeModel(model);
        }
        String xml = out.toString();
        return xml;
    }

    /** Writing widgets as XML
     *  @throws Exception on error
     */
    @Test
    public void testWidgetWriting() throws Exception
    {
        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try
        (
            final ModelWriter writer = new ModelWriter(stream);
        )
        {
            final Widget widget = new CustomWidget();
            widget.setPropertyValue(propName, "Demo");
            widget.getProperty(CustomWidget.propZeroTen).setValue(7);
            writer.writeWidget(widget);

            final GroupWidget group = new GroupWidget();
            group.setPropertyValue(propName, "My Group");

            final Widget child = new Widget("base");
            child.setPropertyValue(propName, "Jänner");
            group.runtimeChildren().addChild(child);

            writer.writeWidget(group);
        }

        final String xml = stream.toString();
        System.out.println(xml);

        final String desired = getExampleFile();
        assertThat(xml.replace("\r", ""), equalTo(desired));
    }

    /** Read widgets from XML
     *  @throws Exception on error
     */
    @Test
    public void testWidgetReading() throws Exception
    {
        final String xml = getExampleFile();
        final InputStream stream = new ByteArrayInputStream(xml.getBytes());
        final ModelReader reader = new ModelReader(stream);
        final DisplayModel model = reader.readModel();

        final List<Widget> widgets = model.getChildren();
        for (final Widget widget : widgets)
        {
            System.out.println(widget);
            System.out.println(
                widget.getProperties().stream()
                      .map(Object::toString).collect(Collectors.joining("\n")));
        }
        assertThat(widgets.size(), equalTo(2));
        final List<String> names =
            widgets.stream().map(Widget::getName).collect(Collectors.toList());
        assertThat(names, equalTo(Arrays.asList("Demo", "My Group")));

        assertThat(widgets.get(1), instanceOf(GroupWidget.class));
        assertThat(((GroupWidget)widgets.get(1)).runtimeChildren().getValue().get(0).getName(), equalTo("Jänner"));
   }

    /** Write and read multi-line string property
     *  @throws Exception on error
     */
    @Test
    public void testMultilineString() throws Exception
    {
        final WidgetPropertyDescriptor<String> prop = propName;
        final String written_value = "Line 1\n" +
                                     "Line 2\n" +
                                     "Line 3";

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        try
        (
            final ModelWriter writer = new ModelWriter(out);
        )
        {
            final Widget widget = new Widget("base");
            widget.setPropertyValue(prop, "StringTest");
            assertThat(widget.getProperty(prop),
                       instanceOf(StringWidgetProperty.class));
            widget.getProperty(prop).setValue(written_value);
            writer.writeWidget(widget);
        }
        checkReadback(out.toString(), prop, written_value);

        // Same text
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
              "<display version=\"1.0.0\">\n" +
              "  <widget type=\"base\" version=\"1.0.0\">\n" +
              "    <name>Line 1\n" +
              "Line 2\n" +
              "Line 3</name>\n" +
              "    <x>0</x>\n" +
              "    <y>0</y>\n" +
              "  </widget>\n" +
              "</display>";
        checkReadback(xml, prop, written_value);

        // .. with Unicode for the newlines
        xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<display version=\"1.0.0\">\n" +
                "  <widget type=\"base\" version=\"1.0.0\">\n" +
                "    <name>Line 1&#x000A;" +
                "Line 2&#x0A;" +
                "Line 3</name>\n" +
                "    <x>0</x>\n" +
                "    <y>0</y>\n" +
                "  </widget>\n" +
                "</display>";
        checkReadback(xml, prop, written_value);
    }

    private void checkReadback(final String xml, final WidgetPropertyDescriptor<String> prop,
            final String written_value) throws Exception
    {
        System.out.println(xml);

        final InputStream in = new ByteArrayInputStream(xml.getBytes());
        final ModelReader reader = new ModelReader(in);
        final List<Widget> widgets = reader.readModel().getChildren();
        assertThat(widgets.size(), equalTo(1));
        final String value = widgets.get(0).getProperty(prop).getValue();
        System.out.println("Readback: '" + value + "'");
        assertThat(value, equalTo(written_value));
    }

    /** Test persistence of display model's properties
     *  @throws Exception on error
     */
    @Test
    public void testDisplayModelPersistence() throws Exception
    {
        final DisplayModel model = new DisplayModel();
        model.getProperty("width").setValueFromObject(400);
        model.getProperty("height").setValueFromObject(800);

        final Widget widget = new Widget("base");
        widget.setPropertyValue(propName, "Test");
        widget.getProperty("x").setValueFromObject(42);
        model.runtimeChildren().addChild(widget);

        final String xml = toXML(model);
        System.out.println(xml);

        final ModelReader reader = new ModelReader(new ByteArrayInputStream(xml.getBytes()));
        final DisplayModel readback = reader.readModel();

        assertThat(readback.getPropertyValue(CommonWidgetProperties.propWidth), equalTo(400));
        assertThat(readback.getPropertyValue(CommonWidgetProperties.propHeight), equalTo(800));
        assertThat(readback.getChildren().size(), equalTo(1));
        assertThat(readback.getChildren().get(0).getPropertyValue(CommonWidgetProperties.propX), equalTo(42));
    }

    @Test
    public void testClassSupportPersistence() throws Exception
    {
        // By default, Label font does not use class
        final DisplayModel model = new DisplayModel();
        final LabelWidget label = new LabelWidget();
        assertThat(label.propFont().isUsingWidgetClass(), equalTo(false));
        label.propFont().setValue(new NamedWidgetFont("TEST", "Sans", WidgetFontStyle.REGULAR, 10.0));
        model.runtimeChildren().addChild(label);

        String xml = toXML(model);
        System.out.println(xml);
        assertThat(xml, containsString("display version=\"2"));
        assertThat(xml, containsString("font>"));
        DisplayModel readback = ModelReader.parseXML(xml);
        assertThat(readback.getChildren().get(0).getProperty("font").isUsingWidgetClass(), equalTo(false));

        // "use_class" is persisted in XML and read back
        label.propFont().useWidgetClass(true);
        xml = toXML(model);
        System.out.println(xml);
        assertThat(xml, containsString("font use_class=\"true\""));
        readback = ModelReader.parseXML(xml);
        assertThat(readback.getChildren().get(0).getProperty("font").isUsingWidgetClass(), equalTo(true));
    }
}
