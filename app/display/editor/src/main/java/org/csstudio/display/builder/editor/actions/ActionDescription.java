/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.editor.actions;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import org.csstudio.display.builder.editor.DisplayEditor;
import org.csstudio.display.builder.editor.Messages;
import org.csstudio.display.builder.editor.undo.SetWidgetPropertyAction;
import org.csstudio.display.builder.editor.undo.UpdateWidgetOrderAction;
import org.csstudio.display.builder.model.ChildrenProperty;
import org.csstudio.display.builder.model.Widget;
import org.phoebus.ui.undo.CompoundUndoableAction;
import org.phoebus.ui.undo.UndoableActionManager;

/** Description of an action
 *
 *  Wraps the functionality, i.e. icon, tool tip, and what to execute,
 *  for use in a Java FX Button or Eclipse Action.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public abstract class ActionDescription
{
    /** Copy selected widgets */
    public static final ActionDescription COPY =
        new ActionDescription("icons/copy_edit.png", Messages.Copy)
    {
        @Override
        public void run(final DisplayEditor editor, final boolean selected)
        {
            editor.copyToClipboard();
        }
    };

    /** Delete selected widgets */
    public static final ActionDescription DELETE =
        new ActionDescription("icons/delete.png", Messages.Delete)
    {
        @Override
        public void run(final DisplayEditor editor, final boolean selected)
        {
            editor.cutToClipboard();
        }
    };

    /** Enable/disable grid */
    public static final ActionDescription ENABLE_GRID =
        new ActionDescription("icons/grid.png", Messages.Grid)
    {
        @Override
        public void run(final DisplayEditor editor, final boolean selected)
        {
            editor.getSelectedWidgetUITracker().enableGrid(selected);
        }
    };

    /** Enable/disable snapping to other widgets */
    public static final ActionDescription ENABLE_SNAP =
        new ActionDescription("icons/snap.png", Messages.Snap)
    {
        @Override
        public void run(final DisplayEditor editor, final boolean selected)
        {
            editor.getSelectedWidgetUITracker().enableSnap(selected);
        }
    };

    /** Enable/disable showing widget coordinates in tracker */
    public static final ActionDescription ENABLE_COORDS =
        new ActionDescription("icons/coords.png", Messages.ShowCoordinates)
    {
        @Override
        public void run(final DisplayEditor editor, final boolean selected)
        {
            editor.getSelectedWidgetUITracker().showLocationAndSize(selected);
        }
    };

    /** Order widgets by their index in the parent's list of children
     *  <p>Original list will not be modified
     *  @param widgets Widgets in any order, because user may have selected them in random order
     *  @return Widgets sorted by their location in parent
     */
    private static List<Widget> orderWidgetsByIndex(final List<Widget> widgets)
    {
        final List<Widget> sorted = new ArrayList<>(widgets);
        sorted.sort((a, b) -> ChildrenProperty.getParentsChildren(a).getValue().indexOf(a) -
                              ChildrenProperty.getParentsChildren(b).getValue().indexOf(b));
        return sorted;
    }

    /** Move widget one step to the back */
    public static final ActionDescription MOVE_UP =
        new ActionDescription("icons/up.png", Messages.MoveUp)
    {
        @Override
        public void run(final DisplayEditor editor, final boolean selected)
        {
            // When multiple widgets are selected, they are moved 'up'
            // in their original order:
            // Move 'b, c' up in [ a, b, c ]
            // -- move 'b' up -> [ b, a, c ]
            // -- move 'c' up -> [ b, c, a ]
            // Doing this in reverse original order would leave the list unchanged.
            final List<Widget> widgets = orderWidgetsByIndex(editor.getWidgetSelectionHandler().getSelection());
            if (widgets.isEmpty())
                return;
            final CompoundUndoableAction compound = new CompoundUndoableAction(Messages.MoveUp);
            for (Widget widget : widgets)
            {
                final List<Widget> children = ChildrenProperty.getParentsChildren(widget).getValue();
                int orig = children.indexOf(widget);
                if (orig > 0)
                    compound.add(new UpdateWidgetOrderAction(widget, orig, orig-1));
                else
                    compound.add(new UpdateWidgetOrderAction(widget, orig, -1));
            }
            editor.getUndoableActionManager().execute(compound);
        }
    };

    /** Move widget to back */
    public static final ActionDescription TO_BACK =
        new ActionDescription("icons/toback.png", Messages.MoveToBack)
    {
        @Override
        public void run(final DisplayEditor editor, final boolean selected)
        {
            final List<Widget> widgets = editor.getWidgetSelectionHandler().getSelection();
            if (widgets.isEmpty())
                return;
            final CompoundUndoableAction compound = new CompoundUndoableAction(Messages.MoveToBack);
            for (Widget widget : widgets)
                compound.add(new UpdateWidgetOrderAction(widget, 0));
            editor.getUndoableActionManager().execute(compound);
        }
    };

    /** Move widget one step to the front */
    public static final ActionDescription MOVE_DOWN =
        new ActionDescription("icons/down.png", Messages.MoveDown)
    {
        @Override
        public void run(final DisplayEditor editor, final boolean selected)
        {
            // When multiple widgets are selected, they are moved 'down'
            // in reverse original order:
            // Move 'a, b' down in [ a, b, c ]
            // -- move 'b' down -> [ a, c, b ]
            // -- move 'a' down -> [ c, a, b ]
            // Doing this in the original order would leave the list unchanged.
            List<Widget> widgets = editor.getWidgetSelectionHandler().getSelection();
            if (widgets.isEmpty())
                return;
            widgets = orderWidgetsByIndex(widgets);
            Collections.reverse(widgets);

            final CompoundUndoableAction compound = new CompoundUndoableAction(Messages.MoveDown);
            for (Widget widget : widgets)
            {
                final List<Widget> children = ChildrenProperty.getParentsChildren(widget).getValue();
                int orig = children.indexOf(widget);
                if (orig < children.size()-1)
                    compound.add(new UpdateWidgetOrderAction(widget, orig, orig+1));
                else
                    compound.add(new UpdateWidgetOrderAction(widget, orig, 0));
            }
            editor.getUndoableActionManager().execute(compound);
        }
    };

    /** Move widget to front */
    public static final ActionDescription TO_FRONT =
        new ActionDescription("icons/tofront.png", Messages.MoveToFront)
    {
        @Override
        public void run(final DisplayEditor editor, final boolean selected)
        {
            List<Widget> widgets = editor.getWidgetSelectionHandler().getSelection();
            if (widgets.isEmpty())
                return;
            // Same reasoning as in MOVE_DOWN
            // Without reversing, widgets would actually end up in front,
            // but un-doing the operation would them misplace them.
            widgets = orderWidgetsByIndex(widgets);
            Collections.reverse(widgets);
            final CompoundUndoableAction compound = new CompoundUndoableAction(Messages.MoveToFront);
            for (Widget widget : widgets)
                compound.add(new UpdateWidgetOrderAction(widget, -1));
            editor.getUndoableActionManager().execute(compound);
        }
    };

    // Alignment icons from GEF (https://github.com/eclipse/gef/tree/master/org.eclipse.gef/src/org/eclipse/gef/internal/icons)
    /** Align widgets on left edge */
    public static final ActionDescription ALIGN_LEFT =
        new ActionDescription("icons/alignleft.gif", Messages.AlignLeft)
    {
        @Override
        public void run(final DisplayEditor editor, final boolean selected)
        {
            final List<Widget> widgets = editor.getWidgetSelectionHandler().getSelection();
            final UndoableActionManager undo = editor.getUndoableActionManager();
            if (widgets.size() < 2)
                return;
            final int min = widgets.stream()
                                   .mapToInt(w -> w.propX().getValue())
                                   .min()
                                   .orElseThrow(NoSuchElementException::new);
            for (Widget w : widgets)
                undo.execute(new SetWidgetPropertyAction<>(w.propX(), min));
        }
    };

    /** Align widgets on (vertical) center line */
    public static final ActionDescription ALIGN_CENTER =
        new ActionDescription("icons/aligncenter.gif", Messages.AlignCenter)
    {
        @Override
        public void run(final DisplayEditor editor, final boolean selected)
        {
            final List<Widget> widgets = editor.getWidgetSelectionHandler().getSelection();
            final UndoableActionManager undo = editor.getUndoableActionManager();
            if (widgets.size() < 2)
                return;
            final int min = widgets.stream()
                                   .mapToInt(w -> w.propX().getValue())
                                   .min()
                                   .orElseThrow(NoSuchElementException::new);
            final int max = widgets.stream()
                                   .mapToInt(w -> w.propX().getValue() + w.propWidth().getValue())
                                   .max()
                                   .orElseThrow(NoSuchElementException::new);
            final int center = (min + max) / 2;
            for (Widget w : widgets)
                undo.execute(new SetWidgetPropertyAction<>(w.propX(),
                                                           center - w.propWidth().getValue()/2));
        }
    };

    /** Align widgets on right edge */
    public static final ActionDescription ALIGN_RIGHT =
        new ActionDescription("icons/alignright.gif", Messages.AlignRight)
    {
        @Override
        public void run(final DisplayEditor editor, final boolean selected)
        {
            final List<Widget> widgets = editor.getWidgetSelectionHandler().getSelection();
            final UndoableActionManager undo = editor.getUndoableActionManager();
            if (widgets.size() < 2)
                return;
            final int max = widgets.stream()
                                   .mapToInt(w -> w.propX().getValue() + w.propWidth().getValue())
                                   .max()
                                   .orElseThrow(NoSuchElementException::new);
            for (Widget w : widgets)
                undo.execute(new SetWidgetPropertyAction<>(w.propX(), max - w.propWidth().getValue()));
        }
    };

    /** Align widgets on top edge */
    public static final ActionDescription ALIGN_TOP =
        new ActionDescription("icons/aligntop.gif", Messages.AlignTop)
    {
        @Override
        public void run(final DisplayEditor editor, final boolean selected)
        {
            final List<Widget> widgets = editor.getWidgetSelectionHandler().getSelection();
            final UndoableActionManager undo = editor.getUndoableActionManager();
            if (widgets.size() < 2)
                return;
            final int min = widgets.stream()
                                   .mapToInt(w -> w.propY().getValue())
                                   .min()
                                   .orElseThrow(NoSuchElementException::new);
            for (Widget w : widgets)
                undo.execute(new SetWidgetPropertyAction<>(w.propY(), min));
        }
    };

    /** Align widgets on (horizontal) middle line */
    public static final ActionDescription ALIGN_MIDDLE =
        new ActionDescription("icons/alignmid.gif", Messages.AlignMiddle)
    {
        @Override
        public void run(final DisplayEditor editor, final boolean selected)
        {
            final List<Widget> widgets = editor.getWidgetSelectionHandler().getSelection();
            final UndoableActionManager undo = editor.getUndoableActionManager();
            if (widgets.size() < 2)
                return;
            final int min = widgets.stream()
                                   .mapToInt(w -> w.propY().getValue())
                                   .min()
                                   .orElseThrow(NoSuchElementException::new);
            final int max = widgets.stream()
                                   .mapToInt(w -> w.propY().getValue() + w.propHeight().getValue())
                                   .max()
                                   .orElseThrow(NoSuchElementException::new);
            final int middle = ( min + max ) / 2;
            for (Widget w : widgets)
                undo.execute(new SetWidgetPropertyAction<>(w.propY(),
                                                           middle - w.propHeight().getValue()/2));
        }
    };

    /** Align widgets on bottom edge */
    public static final ActionDescription ALIGN_BOTTOM =
        new ActionDescription("icons/alignbottom.gif", Messages.AlignBottom)
    {
        @Override
        public void run(final DisplayEditor editor, final boolean selected)
        {
            final List<Widget> widgets = editor.getWidgetSelectionHandler().getSelection();
            final UndoableActionManager undo = editor.getUndoableActionManager();
            if (widgets.size() < 2)
                return;
            final int max = widgets.stream()
                                   .mapToInt(w -> w.propY().getValue() + w.propHeight().getValue())
                                   .max()
                                   .orElseThrow(NoSuchElementException::new);
            for (Widget w : widgets)
                undo.execute(new SetWidgetPropertyAction<>(w.propY(),
                                                           max - w.propHeight().getValue()));
        }
    };

    /** Set widgets to same width */
    public static final ActionDescription MATCH_WIDTH =
        new ActionDescription("icons/matchwidth.gif", Messages.MatchWidth)
    {
        @Override
        public void run(final DisplayEditor editor, final boolean selected)
        {
            final List<Widget> widgets = editor.getWidgetSelectionHandler().getSelection();
            final UndoableActionManager undo = editor.getUndoableActionManager();
            if (widgets.size() < 2)
                return;
            final int dest = widgets.get(0).propWidth().getValue();
            for (int i=1; i<widgets.size(); ++i)
                undo.execute(new SetWidgetPropertyAction<>(widgets.get(i).propWidth(), dest));
        }
    };

    /** Set widgets to same height */
    public static final ActionDescription MATCH_HEIGHT =
        new ActionDescription("icons/matchheight.gif", Messages.MatchHeight)
    {
        @Override
        public void run(final DisplayEditor editor, final boolean selected)
        {
            final List<Widget> widgets = editor.getWidgetSelectionHandler().getSelection();
            final UndoableActionManager undo = editor.getUndoableActionManager();
            if (widgets.size() < 2)
                return;
            final int dest = widgets.get(0).propHeight().getValue();
            for (int i=1; i<widgets.size(); ++i)
                undo.execute(new SetWidgetPropertyAction<>(widgets.get(i).propHeight(), dest));
        }
    };

    /** Distribute widgets horizontally */
    public static final ActionDescription DIST_HORIZ =
        new ActionDescription("icons/distribute_hc.png", Messages.DistributeHorizontally)
    {
        @Override
        public void run(final DisplayEditor editor, final boolean selected)
        {
            final List<Widget> widgets = editor.getWidgetSelectionHandler().getSelection();
            final UndoableActionManager undo = editor.getUndoableActionManager();
            final int N = widgets.size();
            if (N < 3)
                return;

            // Get left/right
            int left = widgets.get(0).propX().getValue() + widgets.get(0).propWidth().getValue()/2;
            int right = left;
            for (int i=1; i<N; ++i)
            {
                int center = widgets.get(i).propX().getValue() + widgets.get(i).propWidth().getValue()/2;
                left = Math.min(left, center);
                right = Math.max(right, center);
            }

            // Set widget's X coord to distribute centers horizontally
            for (int i=0; i<N; ++i)
            {
                final int dest = left + i*(right - left)/(N-1);
                undo.execute(new SetWidgetPropertyAction<>(widgets.get(i).propX(),
                                                                  dest - widgets.get(i).propWidth().getValue()/2));
            }
        }
    };

    /** Distribute widgets horizontally */
    public static final ActionDescription DIST_VERT =
        new ActionDescription("icons/distribute_vc.png", Messages.DistributeVertically)
    {
        @Override
        public void run(final DisplayEditor editor, final boolean selected)
        {
            final List<Widget> widgets = editor.getWidgetSelectionHandler().getSelection();
            final UndoableActionManager undo = editor.getUndoableActionManager();
            final int N = widgets.size();
            if (N < 3)
                return;

            // Get top/bottom
            int top = widgets.get(0).propY().getValue() + widgets.get(0).propHeight().getValue()/2;
            int bottom = top;
            for (int i=1; i<N; ++i)
            {
                int middle = widgets.get(i).propY().getValue() + widgets.get(i).propHeight().getValue()/2;
                top = Math.min(top, middle);
                bottom = Math.max(bottom, middle);
            }

            // Set widget's Y coord to distribute centers horizontally
            for (int i=0; i<N; ++i)
            {
                final int dest = top + i*(bottom - top)/(N-1);
                undo.execute(new SetWidgetPropertyAction<>(widgets.get(i).propY(),
                                                                  dest - widgets.get(i).propHeight().getValue()/2));
            }
        }
    };

    private final String icon;
    private final String tool_tip;

    /** @param icon Icon path
     *  @param tool_tip Tool tip
     */
    public ActionDescription(final String icon, final String tool_tip)
    {
        this.icon = icon;
        this.tool_tip = tool_tip;
    }

    public String getIcon()
    {
        return icon;
    }

    public URL getIconResourcePath()
    {
        return DisplayEditor.class.getResource("/" + icon);
    }

    /** @return Tool tip */
    public String getToolTip()
    {
        return tool_tip;
    }

    /** Execute the action
     *
     *  <p>For plain "do it" actions, 'selected' will always be <code>true</code>.
     *  For actions that are bound to a toggle button because some feature
     *  is enabled or disabled, 'selected' reflects if the button was toggled 'on' or 'off'.
     *
     *  @param editor {@link DisplayEditor}
     *  @param selected Selected?
     */
    abstract public void run(DisplayEditor editor, boolean selected);

    /** Execute the action
     *
     *  @param editor {@link DisplayEditor}
     */
    public void run(final DisplayEditor editor)
    {
        run(editor, true);
    }
}
