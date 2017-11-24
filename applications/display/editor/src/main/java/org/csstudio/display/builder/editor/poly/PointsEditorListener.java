/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.editor.poly;

import org.csstudio.display.builder.model.properties.Points;

/** Listener to {@link PointsEditor}
 *  @author Kay Kasemir
 */
public interface PointsEditorListener
{
    /** @param points Points that have been modified */
    public void pointsChanged(Points points);

    /** Called when editor is finished and should be disposed */
    public void done();
}
