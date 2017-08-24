/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.applications.pvtree;

import org.phoebus.applications.pvtree.model.TreeModel;
import org.phoebus.applications.pvtree.model.TreeModelItem;
import org.phoebus.applications.pvtree.model.TreeModelListener;

/** Demo of the model
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class TreeModelDemo
{
    private static void dump(final TreeModel model)
    {
        System.out.println("-- PV Tree --");
        dump(model.getRoot(), 0);
        System.out.println("-------------");
    }

    private static void dump(final TreeModelItem item, final int level)
    {
        for (int i=0; i<level; ++i)
            System.out.print("  ");
        System.out.println(item);
        for (TreeModelItem link : item.getLinks())
            dump(link, level + 1);
    }

    public static void main(String[] args) throws Exception
    {
        TestHelper.setupLogging();

        final TreeModel model = new TreeModel();
        model.addListener(new TreeModelListener()
        {
            @Override
            public void itemLinkAdded(final TreeModelItem item, final TreeModelItem link)
            {
                link.start();
            }

            @Override
            public void itemChanged(final TreeModelItem item)
            {
                System.out.println("Update from " + item);
            }
        });
        model.setRootPV("sim://sine");
        model.getRoot().start();

        Thread.sleep(4000);

        dump(model);

        model.dispose();

        TestHelper.checkShutdown();
    }
}
