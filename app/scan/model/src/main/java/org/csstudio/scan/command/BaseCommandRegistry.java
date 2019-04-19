/*******************************************************************************
 * Copyright (c) 2018-2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * The scan engine idea is based on the "ScanEngine" developed
 * by the Software Services Group (SSG),  Advanced Photon Source,
 * Argonne National Laboratory,
 * Copyright (c) 2011 , UChicago Argonne, LLC.
 *
 * This implementation, however, contains no SSG "ScanEngine" source code
 * and is not endorsed by the SSG authors.
 ******************************************************************************/
package org.csstudio.scan.command;

import static java.util.Map.entry;

import java.net.URL;
import java.util.Map;
import java.util.function.Supplier;

import org.csstudio.scan.ScanSystem;
import org.csstudio.scan.spi.ScanCommandRegistry;

/** Register base commands via SPI
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class BaseCommandRegistry implements ScanCommandRegistry
{
    @Override
    public Map<String, URL> getImages()
    {
        return Map.ofEntries(
            entry("comment", ScanSystem.class.getResource("/icons/comment.gif")),
            entry("config_log", ScanSystem.class.getResource("/icons/configcommand.gif")),
            entry("delay", ScanSystem.class.getResource("/icons/delaycommand.gif")),
            entry("if", ScanSystem.class.getResource("/icons/ifcommand.gif")),
            entry("include", ScanSystem.class.getResource("/icons/includecommand.gif")),
            entry("log", ScanSystem.class.getResource("/icons/logcommand.gif")),
            entry("loop", ScanSystem.class.getResource("/icons/loopcommand.gif")),
            entry("parallel", ScanSystem.class.getResource("/icons/parallelcommand.gif")),
            entry("script", ScanSystem.class.getResource("/icons/scriptcommand.gif")),
            entry("sequence", ScanSystem.class.getResource("/icons/sequencecommand.gif")),
            entry("set", ScanSystem.class.getResource("/icons/setcommand.gif")),
            entry("wait", ScanSystem.class.getResource("/icons/waitcommand.gif"))
        );
    }

    @Override
    public Map<String, Supplier<ScanCommand>> getCommands()
    {
        return Map.ofEntries(
            entry("comment", CommentCommand::new),
            entry("config_log", ConfigLogCommand::new),
            entry("delay", DelayCommand::new),
            entry("if", IfCommand::new),
            entry("include", IncludeCommand::new),
            entry("log", LogCommand::new),
            entry("loop", LoopCommand::new),
            entry("parallel", ParallelCommand::new),
            entry("script", ScriptCommand::new),
            entry("sequence", SequenceCommand::new),
            entry("set", SetCommand::new),
            entry("wait", WaitCommand::new)
        );
    }
}
