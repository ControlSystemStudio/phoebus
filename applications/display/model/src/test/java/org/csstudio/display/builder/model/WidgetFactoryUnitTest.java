/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.BeforeClass;
import org.junit.Test;

/** JUnit test of the {@link WidgetFactory}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class WidgetFactoryUnitTest
{
    /** Initialize factory with test widget classes. */
    public static void initializeFactory()
    {
        final WidgetFactory factory = WidgetFactory.getInstance();

        if (factory.getWidgetDescriptions().stream().anyMatch(d -> d.getType().equals("custom")))
            return;

        factory.addWidgetType(new WidgetDescriptor("base",
                WidgetCategory.MISC, "Base", "dummy_icon.png",
                "Widget base class, only useful for tests")
        {
            @Override
            public Widget createWidget()
            {
                return new Widget(getType());
            }
        });
        factory.addWidgetType(new WidgetDescriptor("custom",
                WidgetCategory.GRAPHIC, "Custom", "dummy_icon.png",
                "Custom Widget, has a few additional properties",
                Arrays.asList("older_custom1", "old_custom2"))
        {
            @Override
            public Widget createWidget()
            {
                return new CustomWidget();
            }
        });
    }

    /** @param descriptor WidgetDescriptor
     *  @return Real widget or one of the test cases created in here
     */
    private static boolean isTestWidget(final WidgetDescriptor descriptor)
    {
        return List.of("base", "custom", "demo", "plot").contains(descriptor.getType());
    }

    /** Initialize factory for tests */
    @BeforeClass
    public static void setup()
    {
        initializeFactory();
    }

    /** List widget descriptions */
    @Test
    public void testWidgetDescriptions()
    {
        final Set<WidgetDescriptor> descriptions = WidgetFactory.getInstance().getWidgetDescriptions();
        for (final WidgetDescriptor description : descriptions)
            System.out.println(description.getCategory() + ": " + description);
        assertThat(descriptions.size() > 2, equalTo(true));

        // Widgets should be ordered by category
        final List<WidgetCategory> categories =
            descriptions.stream().map(WidgetDescriptor::getCategory).collect(Collectors.toList());
        int last_ordinal = -1;
        for (final WidgetCategory category : categories)
        {
            if (category.ordinal() < last_ordinal)
                fail("Widgets are not ordered by category");
            last_ordinal = category.ordinal();
        }
    }

    private Widget createWidget(final String type) throws Exception
    {
        final List<WidgetDescriptor> descr = WidgetFactory.getInstance().getAllWidgetDescriptors(type);
        if (descr.size() != 1)
            throw new Exception("Got " + descr.size() + " widgets for " + type);
        return descr.get(0).createWidget();
    }

    /** Create widgets
     *  @throws Exception on error
     */
    @Test
    public void testWidgetCreation() throws Exception
    {
        Widget widget = createWidget("base");
        System.out.println(widget);
        assertThat(widget.getType(), equalTo("base"));

        widget = createWidget("custom");
        System.out.println(widget);
        assertThat(widget.getType(), equalTo("custom"));
    }

    /** Fail on unknown widget
     *  @throws Exception on error
     */
    @Test
    public void testUnknownWidgetType() throws Exception
    {
        try
        {
            createWidget("bogus");
            fail("Created unknown widget?!");
        }
        catch (final Exception ex)
        {
            assertThat(ex.getMessage().toLowerCase(), containsString("unknown"));
        }
    }

    /** Create widgets via alternate type
     *  @throws Exception on error
     */
    @Test
    public void testAlternateWidgetTypes() throws Exception
    {
        Widget widget = createWidget("older_custom1");
        assertThat(widget, not(nullValue()));
        System.out.println(widget);
        assertThat(widget.getType(), equalTo("custom"));

        widget = createWidget("old_custom2");
        assertThat(widget, not(nullValue()));
        System.out.println(widget);
        assertThat(widget.getType(), equalTo("custom"));
    }

    /** Check that each widget has an icon */
    @Test
    public void testIcons() throws Exception
    {
        for (WidgetDescriptor descriptor : WidgetFactory.getInstance().getWidgetDescriptions())
        {
            if (isTestWidget(descriptor))
                continue;
            System.out.println(descriptor);
            System.out.println("  Icon URL : " + descriptor.getIconURL());
            if (descriptor.getIconURL() == null)
                continue;
            assertThat(descriptor.getIconURL(), not(nullValue()));

            try
            (
                final InputStream stream = descriptor.getIconURL().openStream();
            )
            {
                final int size = stream.available();
                System.out.println("  Icon size: " + size + " bytes");
                assertTrue(size > 0);
            }
        }
    }

    /** List all widgets, sorted by number of properties */
    @Test
    public void widgetStats() throws Exception
    {
        System.out.format("%-20s %s\n", "Widget Type", "Number of Properties");
        WidgetFactory.getInstance()
                     .getWidgetDescriptions()
                     .stream()
                     .filter(desc -> ! isTestWidget(desc))
                     .map(desc -> desc.createWidget())
                     .sorted((a, b) -> a.getProperties().size() - b.getProperties().size())
                     .forEach(widget ->
                     {
                         System.out.format("%-20s %d\n", widget.getType(), widget.getProperties().size());
                     });
    }
}
