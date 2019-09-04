/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.ui;

import org.phoebus.framework.nls.NLS;

/** Localized messages
 *  @author Kay Kasemir
 */
public class Messages
{
    public static String scan_active_prompt;
    public static String scan_abort;
    public static String scan_abort_all;
    public static String scan_jump_to_current_command;
    public static String scan_move_down;
    public static String scan_move_up;
    public static String scan_name_id_fmt;
    public static String scan_next;
    public static String scan_pause;
    public static String scan_pause_all;
    public static String scan_remove;
    public static String scan_resume;
    public static String scan_resume_all;
    public static String scan_simulate;
    public static String scan_submit;
    public static String scan_submit_unqueued;

    static
    {
        NLS.initializeMessages(Messages.class);
    }
}
