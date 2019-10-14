/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.produc;

import org.phoebus.product.Launcher;

/** 'main' class to run
 *
 *  <p>Defers to the common Launcher.
 *  A site specific product could add local initialization code.
 *
 *  @author Kay Kasemir
 */
public class Main
{
    public static void main(String[] args) throws Exception
    {
        Launcher.main(args);
    }
}
