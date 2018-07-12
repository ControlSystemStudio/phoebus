/*******************************************************************************
 * Copyright (c) 2014-2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.javafx.rtplot.internal.undo;

import org.csstudio.javafx.rtplot.Messages;
import org.csstudio.javafx.rtplot.internal.AnnotationImpl;
import org.csstudio.javafx.rtplot.internal.Plot;
import org.phoebus.ui.undo.UndoableAction;

import javafx.geometry.Point2D;

/** Action to update Annotation
 *  @author Kay Kasemir
 */
public class UpdateAnnotationAction<XTYPE extends Comparable<XTYPE>> extends UndoableAction
{
    final private Plot<XTYPE> plot;
    final AnnotationImpl<XTYPE> annotation;
    final private XTYPE start_pos, end_pos;
    final private double start_val, end_val;
    final private Point2D start_offset, end_offset;

    public UpdateAnnotationAction(final Plot<XTYPE> plot, final AnnotationImpl<XTYPE> annotation,
            final XTYPE start_pos, final double start_val, final Point2D start_offset,
            final XTYPE end_pos, final double end_val, final Point2D end_offset)
    {
        super(Messages.UpdateAnnotation);
        this.plot = plot;
        this.annotation = annotation;
        this.start_pos = start_pos;
        this.start_val = start_val;
        this.start_offset = start_offset;
        this.end_pos = end_pos;
        this.end_val = end_val;
        this.end_offset = end_offset;
    }

    @Override
    public void run()
    {
        plot.updateAnnotation(annotation, end_pos, end_val, null, end_offset);
    }

    @Override
    public void undo()
    {
        plot.updateAnnotation(annotation, start_pos, start_val, null, start_offset);
    }
}
