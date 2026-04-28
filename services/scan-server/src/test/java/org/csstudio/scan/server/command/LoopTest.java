/*******************************************************************************
 * Copyright (c) 2026 Oak Ridge National Laboratory.
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
package org.csstudio.scan.server.command;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.csstudio.scan.command.LoopCommand;
import org.junit.jupiter.api.Test;

/** JUnit test for loop */
public class LoopTest
{
    @Test
    void testNormalLoop() throws Exception
    {
        LoopCommand cmd = new LoopCommand("loc://x(0)", 1.0, 10.0, 0.5, List.of());
        LoopCommandImpl impl = new LoopCommandImpl(cmd);

        System.out.println(cmd);

        double step = impl.getLoopStep();
        double start = step < 0 ? impl.getLoopEnd() : impl.getLoopStart();
        int num_steps = impl.getNumSteps();

        double last = Double.NaN;
        for (int i = 0; i < num_steps; i++)
        {
            last = impl.computeStep(start, step, i);
            System.out.println(last);
        }
        assertEquals(10.0, last);
    }

    @Test
    void testUpwardsLoop() throws Exception
    {
        LoopCommand cmd = new LoopCommand("loc://x(0)", 1.0, 1.1, 5.0, List.of());
        LoopCommandImpl impl = new LoopCommandImpl(cmd);

        System.out.println(cmd);

        double step = impl.getLoopStep();
        double start = step < 0 ? impl.getLoopEnd() : impl.getLoopStart();
        int num_steps = impl.getNumSteps();

        double last = Double.NaN;
        for (int i = 0; i < num_steps; i++)
        {
            last = impl.computeStep(start, step, i);
            System.out.println(last);
        }
        assertEquals(1.1, last);
    }

    @Test
    void testDownwardsLoop() throws Exception
    {
        LoopCommand cmd = new LoopCommand("loc://x(0)", 1.1, 1.0, -5.0, List.of());
        LoopCommandImpl impl = new LoopCommandImpl(cmd);

        System.out.println(cmd);

        double step = impl.getLoopStep();
        double start = step < 0 ? impl.getLoopEnd() : impl.getLoopStart();
        int num_steps = impl.getNumSteps();

        double last = Double.NaN;
        for (int i = 0; i < num_steps; i++)
        {
            last = impl.computeStep(start, step, i);
            System.out.println(last);
        }
        assertEquals(1.0, last);
    }

    @Test
    void testTogglingLoop() throws Exception
    {
        LoopCommand cmd = new LoopCommand("loc://x(0)", 1.0, 1.1, -5.0, List.of());
        LoopCommandImpl impl = new LoopCommandImpl(cmd);

        System.out.println(cmd);

        double step = impl.getLoopStep();
        double start = step < 0 ? impl.getLoopEnd() : impl.getLoopStart();
        int num_steps = impl.getNumSteps();

        double last = Double.NaN;
        for (int i = 0; i < num_steps; i++)
        {
            last = impl.computeStep(start, step, i);
            System.out.println(last);
        }
        assertEquals(1.0, last);

        // Run loop again, expect toggled direction
        step = impl.getLoopStep();
        start = step < 0 ? impl.getLoopEnd() : impl.getLoopStart();
        num_steps = impl.getNumSteps();
        for (int i = 0; i < num_steps; i++)
        {
            last = impl.computeStep(start, step, i);
            System.out.println(last);
        }
        assertEquals(1.1, last);
    }
}
