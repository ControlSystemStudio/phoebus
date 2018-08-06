/*******************************************************************************
 * Copyright (c) 2010-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.archive.engine.model;

/** Describes how a channel acts on a group.
 *  @author Kay Kasemir
 */
public enum Enablement
{
    /** Channel is simply part of the group, but doesn't act on the group */
    Passive,
    /** Channel enables archiving of the group when non-zero */
    Enabling,
    /** Channel disables archiving of the group when non-zero */
    Disabling
}
