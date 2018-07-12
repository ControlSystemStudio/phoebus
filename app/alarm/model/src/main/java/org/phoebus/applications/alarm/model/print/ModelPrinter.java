/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.model.print;

import java.io.PrintStream;

import org.phoebus.applications.alarm.model.AlarmTreeItem;
import org.phoebus.applications.alarm.model.AlarmTreeLeaf;

/** Simple text-based dump of a model hierachy
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ModelPrinter
{
    public static void print(final AlarmTreeItem<?> item)
    {
        print(item, System.out);
    }

    public static void print(final AlarmTreeItem<?> item, final PrintStream out)
    {
        print(item, out, 0);
    }

    private static void print(final AlarmTreeItem<?> item, final PrintStream out, final int level)
    {
        for (int i=0; i<level; ++i)
            out.print("  ");
        if (item instanceof AlarmTreeLeaf)
        {
            out.print("PV " + item.getName() + ": " + item.getState().severity);
            final AlarmTreeLeaf leaf = (AlarmTreeLeaf) item;
            if (! leaf.isEnabled())
                out.print(" - disabled");
        }
        else
            out.print(item.getName() + ": " + item.getState().severity);
        out.println();
        for (AlarmTreeItem<?> child : item.getChildren())
            print(child, out, level+1);
    }
}
