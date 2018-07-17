/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model;

import static org.csstudio.display.builder.model.ModelPlugin.logger;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.io.InputStream;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

import org.csstudio.display.builder.model.persist.DelayedStream;
import org.csstudio.display.builder.model.persist.NamedWidgetFonts;
import org.csstudio.display.builder.model.persist.WidgetClassesService;
import org.csstudio.display.builder.model.persist.WidgetFontService;
import org.csstudio.display.builder.model.properties.NamedWidgetFont;
import org.csstudio.display.builder.model.properties.WidgetFont;
import org.csstudio.display.builder.model.widgets.LabelWidget;
import org.junit.Test;

/** JUnit test of {@link WidgetClassSupport}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ClassSupportUnitTest
{
    @Test
    public void testUsingWidgetClass()
    {
        // Default behavior
        final Widget widget = new LabelWidget();
        assertThat(widget.getProperty("text").isUsingWidgetClass(), equalTo(false));
        assertThat(widget.getProperty("font").isUsingWidgetClass(), equalTo(false));

        // Can be changed
        widget.getProperty("text").useWidgetClass(true);
        assertThat(widget.getProperty("text").isUsingWidgetClass(), equalTo(true));
    }

    protected WidgetClassSupport getExampleClasses() throws Exception
    {
        final WidgetClassSupport classes = new WidgetClassSupport();
        final InputStream stream = getClass().getResourceAsStream("/examples/classes.bcf");
        classes.loadClasses(stream);
        return classes;
    }

    @Test
    public void testClassfile() throws Exception
    {
        final WidgetClassSupport widget_classes = getExampleClasses();
        System.out.println(widget_classes);

        // Every widget has a DEFAULT class
        assertThat(widget_classes.getWidgetClasses("textupdate"), hasItem("DEFAULT"));

        // Label has more classes defined in classes.bcf
        assertThat(widget_classes.getWidgetClasses("label"), hasItem("DEFAULT"));
        assertThat(widget_classes.getWidgetClasses("label"), hasItem("TITLE"));
        assertThat(widget_classes.getWidgetClasses("label"), hasItem("COMMENT"));
    }

    @Test
    public void testPropertyUpdates() throws Exception
    {
        final WidgetClassSupport widget_classes = getExampleClasses();

        final LabelWidget widget = new LabelWidget();
        assertThat(widget.getWidgetClass(), equalTo(WidgetClassSupport.DEFAULT));
        assertThat(widget.propFont().isUsingWidgetClass(), equalTo(false));

        // Original, default font of Label
        WidgetFont value = widget.propFont().getValue();
        assertThat(value, instanceOf(NamedWidgetFont.class));
        final NamedWidgetFont orig_font = (NamedWidgetFont) value;
        System.out.println("Original font: " + orig_font);

        // TITLE class -> widget now using a different font
        widget.propClass().setValue("TITLE");
        widget_classes.apply(widget);
        value = widget.propFont().getValue();
        System.out.println("TITLE class font: " + value);
        assertThat(value, instanceOf(NamedWidgetFont.class));
        final NamedWidgetFont title_font = (NamedWidgetFont) value;
        assertThat(title_font.getName(), not(equalTo(orig_font.getName())));

        // COMMENT class -> widget now using a different font
        widget.propClass().setValue("COMMENT");
        widget_classes.apply(widget);
        value = widget.propFont().getValue();
        System.out.println("COMMENT class font: " + value);
        assertThat(value, instanceOf(NamedWidgetFont.class));
        final NamedWidgetFont comment_font = (NamedWidgetFont) value;
        assertThat(comment_font.getName(), not(equalTo(orig_font.getName())));
        assertThat(comment_font.getName(), not(equalTo(title_font.getName())));
        assertThat(widget.propFont().isUsingWidgetClass(), equalTo(true));

        // DEFAULT class -> stays with the last fone, but no longer 'is using class'
        widget.propClass().setValue("DEFAULT");
        widget_classes.apply(widget);
        value = widget.propFont().getValue();
        System.out.println("DEFAULT class font: " + value);
        assertThat(value, instanceOf(NamedWidgetFont.class));
        final NamedWidgetFont default_font = (NamedWidgetFont) value;
        assertThat(default_font, equalTo(comment_font));
        assertThat(widget.propFont().isUsingWidgetClass(), equalTo(false));
    }

    @Test
    public void testErrors() throws Exception
    {
        final AtomicReference<String> last_log_message = new AtomicReference<>();
        final Handler log_handler = new Handler()
        {
            @Override
            public void publish(final LogRecord record)
            {
                last_log_message.set(record.getMessage());
            }

            @Override
            public void flush()
            {
                // Ignore
            }

            @Override
            public void close() throws SecurityException
            {
                // Ignore
            }
        };
        logger.addHandler(log_handler);

        final WidgetClassSupport widget_classes = getExampleClasses();

        // Using an unknown widget class results in a warning
        Widget widget = new LabelWidget();
        widget.propClass().setValue("NonexistingClass");
        widget_classes.apply(widget);
        assertThat(last_log_message.get(), containsString("NonexistingClass"));

        // Unknown widget type also generates warning
        widget = new Widget("Bogus");
        widget_classes.apply(widget);
        assertThat(last_log_message.get().toLowerCase(), containsString("unknown widget type"));

        // Re-defining the same class
        assertThat(widget_classes.getWidgetClasses("label"), hasItem("TITLE"));
        final LabelWidget another = new LabelWidget();
        another.setPropertyValue("name", "TITLE");
        another.propFont().setValue(WidgetFontService.get(NamedWidgetFonts.DEFAULT_BOLD));
        another.propFont().useWidgetClass(true);
        widget_classes.registerClass(another);
        assertThat(last_log_message.get(), containsString("TITLE"));
        assertThat(last_log_message.get(), containsString("more than once"));
    }

    // Time-based test, may occasionally fail because background thread doesn't get to run as expected
    @Test
    public void testService() throws Exception
    {
        final DelayedStream delayed_stream = new DelayedStream("/examples/classes.bcf");
        WidgetClassesService.loadWidgetClasses(new String[] { "test" }, name -> delayed_stream.call());

        long start = System.currentTimeMillis();
        WidgetClassSupport widget_classes = WidgetClassesService.getWidgetClasses();
        long end = System.currentTimeMillis();
        // Could not open the classes.bcf, but still returned a non-null default
        assertThat(widget_classes, not(nullValue()));
        System.out.println(String.format("Load time with similated delay: %.1f seconds", (end - start)/1000.0));

        Collection<String> classes = widget_classes.getWidgetClasses("label");
        System.out.println("Default classes for 'label': " + classes);
        assertThat(classes.size(), equalTo(1));

        // Allow loading of actual file
        delayed_stream.proceed();
        TimeUnit.SECONDS.sleep(2);
        widget_classes = WidgetClassesService.getWidgetClasses();
        classes = widget_classes.getWidgetClasses("label");
        System.out.println("Updated classes for 'label': " + classes);
        assertThat(classes, hasItem("COMMENT"));
    }
}
