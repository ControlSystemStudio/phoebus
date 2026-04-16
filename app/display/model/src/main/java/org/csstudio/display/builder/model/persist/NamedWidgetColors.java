/*******************************************************************************
 * Copyright (c) 2026 Oak Ridge National Laboratory.
 * Copyright (c) 2026 Brookhaven National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.persist;

/** Legacy compatibility wrapper for {@link org.phoebus.ui.color.NamedWidgetColors}.
 *
 * <p>Static constants ({@code TEXT}, {@code ALARM_MAJOR}, {@code BACKGROUND}, etc.)
 * and static methods are inherited from the parent and remain accessible via this shim.
 *
 * @deprecated Use {@link org.phoebus.ui.color.NamedWidgetColors}.
 */
@Deprecated(since = "5.0.3")
public class NamedWidgetColors extends org.phoebus.ui.color.NamedWidgetColors
{
    // No additional members required.
    // All public static constants (TEXT, ALARM_MAJOR, BACKGROUND, …) and
    // public methods are inherited from org.phoebus.ui.color.NamedWidgetColors.
}

