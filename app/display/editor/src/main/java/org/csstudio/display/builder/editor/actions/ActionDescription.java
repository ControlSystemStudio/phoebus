/*******************************************************************************
 * Copyright (c) 2015-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.editor.actions;

import static org.csstudio.display.builder.editor.Plugin.logger;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

import org.csstudio.display.builder.editor.DisplayEditor;
import org.csstudio.display.builder.editor.Messages;
import org.csstudio.display.builder.editor.app.DisplayEditorInstance;
import org.csstudio.display.builder.editor.undo.SetWidgetPropertyAction;
import org.csstudio.display.builder.editor.undo.UpdateWidgetOrderAction;
import org.csstudio.display.builder.model.ChildrenProperty;
import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.Widget;
import org.phoebus.framework.preferences.PhoebusPreferenceService;
import org.phoebus.framework.spi.AppResourceDescriptor;
import org.phoebus.framework.util.ResourceParser;
import org.phoebus.ui.application.ApplicationLauncherService;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.dialog.NumericInputDialog;
import org.phoebus.ui.javafx.PlatformInfo;
import org.phoebus.ui.undo.CompoundUndoableAction;
import org.phoebus.ui.undo.CompoundReversedUndoableAction;
import org.phoebus.ui.undo.UndoableActionManager;

import javafx.geometry.Point2D;
import javafx.stage.Stage;

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
        new ActionDescription("icons/copy_edit.png", Messages.Copy + " [" + PlatformInfo.SHORTCUT + "-C]")
    {
        @Override
        public void run(final DisplayEditor editor, final boolean selected)
        {
            editor.copyToClipboard();
        }
    };

    /** Duplicate selected widgets */
    public static final ActionDescription DUPLICATE =
        new ActionDescription("icons/duplicate.png", Messages.Duplicate + " [" + PlatformInfo.SHORTCUT + "-D]")
    {
        @Override
        public void run(final DisplayEditor editor, final boolean selected)
        {
            editor.duplicateWidgets();
        }
    };

    /** Delete selected widgets */
    public static final ActionDescription DELETE =
        new ActionDescription("icons/delete.png", Messages.Delete + " [Delete]")
    {
        @Override
        public void run(final DisplayEditor editor, final boolean selected)
        {
            editor.removeWidgets();
        }
    };

    /** Delete selected widgets after copying to clipboard */
    public static final ActionDescription CUT =
        new ActionDescription("icons/cut_edit.png", Messages.Cut + " [" + PlatformInfo.SHORTCUT + "-X]")
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
            DisplayEditor.saveGrid(selected);
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
            DisplayEditor.saveSnap(selected);
        }
    };

    /** Enable/disable showing widget coordinates in tracker */
    public static final ActionDescription ENABLE_COORDS =
        new ActionDescription("icons/coords.png", Messages.ShowCoordinates)
    {
        @Override
        public void run(final DisplayEditor editor, final boolean selected)
        {
            if (editor.getModel() != null)
                editor.getSelectedWidgetUITracker().showLocationAndSize(selected);
            else
                editor.getSelectedWidgetUITracker().setShowLocationAndSize(selected);
            DisplayEditor.saveCoords(selected);
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
        new ActionDescription("icons/widget_up.png", Messages.MoveUp)
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
            List<Widget> widgets = editor.getWidgetSelectionHandler().getSelection();
            if (widgets.isEmpty())
                return;
            if (widgets.size() > 1)
                widgets = orderWidgetsByIndex(widgets);
            final CompoundUndoableAction compound = new CompoundUndoableAction(Messages.MoveUp);
            for (Widget widget : widgets)
            {
                final List<Widget> children = ChildrenProperty.getParentsChildren(widget).getValue();
                int orig = children.indexOf(widget);
                if (orig > 0)
                    compound.add(new UpdateWidgetOrderAction(widget, orig, orig-1));
                else
                    // This would result in moving this widget to the bottom, let's not move at all
                    return;
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
            List<Widget> widgets = editor.getWidgetSelectionHandler().getSelection();
            if (widgets.isEmpty())
                return;
            if (widgets.size() > 1)
            {
                // When multiple widgets are selected, they are moved to back
                // in reverse original order:
                // Move 'b, c' to back in [ a, b, c ]
                // -- move 'c' to back -> [ c, a, b ]
                // -- move 'b' to back -> [ b, c, a ]
                // Doing this in the original order would reverse the order of selection.
                widgets = orderWidgetsByIndex(widgets);
                Collections.reverse(widgets);
            }

            // Without CompoundReversedUndoableAction, widgets would actually end up in back,
            // but un-doing the operation would then misplace them.
            final CompoundReversedUndoableAction compound = new CompoundReversedUndoableAction(Messages.MoveToBack);
            for (Widget widget : widgets)
                compound.add(new UpdateWidgetOrderAction(widget, 0));
            editor.getUndoableActionManager().execute(compound);
        }
    };

    /** Move widget one step to the front */
    public static final ActionDescription MOVE_DOWN =
        new ActionDescription("icons/widget_down.png", Messages.MoveDown)
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
            if (widgets.size() > 1)
            {
                widgets = orderWidgetsByIndex(widgets);
                Collections.reverse(widgets);
            }

            final CompoundUndoableAction compound = new CompoundUndoableAction(Messages.MoveDown);
            for (Widget widget : widgets)
            {
                final List<Widget> children = ChildrenProperty.getParentsChildren(widget).getValue();
                int orig = children.indexOf(widget);
                if (orig < children.size()-1)
                    compound.add(new UpdateWidgetOrderAction(widget, orig, orig+1));
                else
                    // This would result in moving this widget to the top, let's not move at all
                    return;
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
            // When multiple widgets are selected, they are moved to front
            // in their original order:
            // Move 'a, b' up in [ a, b, c ]
            // -- move 'a' up -> [ b, c, a ]
            // -- move 'b' up -> [ c, a, b ]
            // Doing this in reverse original order would reverse the order of selection
            List<Widget> widgets = editor.getWidgetSelectionHandler().getSelection();
            if (widgets.isEmpty())
                return;
            if (widgets.size() > 1)
                widgets = orderWidgetsByIndex(widgets);
            // Same reasoning as in TO_BACK;
            // Without CompoundReversedUndoableAction, widgets would actually end up in front,
            // but un-doing the operation would then misplace them.
            final CompoundReversedUndoableAction compound = new CompoundReversedUndoableAction(Messages.MoveToFront);
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

    /** Align widgets on the grid constraint */
    public static final ActionDescription ALIGN_GRID =
        new ActionDescription("icons/grid.png", Messages.AlignGrid)
    {
        @Override
        public void run(final DisplayEditor editor, final boolean selected)
        {
            final List<Widget> widgets = editor.getWidgetSelectionHandler().getSelection();
            final UndoableActionManager undo = editor.getUndoableActionManager();
            for (Widget w : widgets)
            {
                int x = w.propX().getValue();
                int y = w.propY().getValue();
                Point2D constr = editor.getSelectedWidgetUITracker().gridConstrain(x, y);
                x = (int)constr.getX();
                y = (int)constr.getY();
                // Keeping it simple, two actions (possible enhancement use one action for both coordinates)
                undo.execute(new SetWidgetPropertyAction<>(w.propX(), x));
                undo.execute(new SetWidgetPropertyAction<>(w.propY(), y));
            }
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

            final int min = widgets.stream()
                                   .mapToInt(w -> w.propX().getValue())
                                   .min()
                                   .orElseThrow(NoSuchElementException::new);
            final int max = widgets.stream()
                                   .mapToInt(w -> w.propX().getValue() + w.propWidth().getValue())
                                   .max()
                                   .orElseThrow(NoSuchElementException::new);
            final int totalWidth = widgets.stream()
                                          .mapToInt(w -> w.propWidth().getValue())
                                          .sum();

            final int offset = ( max - min - totalWidth ) / ( N - 1 );
            final List<Widget> sortedWidgets = widgets.stream()
                                                      .sorted(( w1, w2 ) ->
            {
                    final int w1x = w1.propX().getValue().intValue();
                    final int w1w = w1.propWidth().getValue().intValue();
                    final int w2x = w2.propX().getValue().intValue();
                    final int w2w = w2.propWidth().getValue().intValue();

                    if ( w1x <= w2x && w1x + w1w <= w2x + w2w )
                    {   //  +------+        |   +------+
                        //  |  w1  |            |  w1  |
                        //  +------+        |   +------+
                        //     +--------+                  +--------+
                        //     |   w2   |   |              |   w2   |
                        //     +--------+                  +--------+
                        return -1;
                    }
                    else if ( w1x >= w2x && w1x + w1w >= w2x + w2w )
                    {   //  +------+        |   +------+
                        //  |  w2  |            |  w2  |
                        //  +------+        |   +------+
                        //     +--------+                  +--------+
                        //     |   w1   |   |              |   w1   |
                        //     +--------+                  +--------+
                        return 1;
                    }
                    else
                    {   //  +--------------------+  |  +--------------------+
                        //  |         w1         |     |         w1         |
                        //  +--------------------+  |  +--------------------+
                        //          +--------+         +--------------------+
                        //          |   w2   |      |  |         w2         |
                        //          +--------+         +--------------------+
                        return ( w1x + w1w / 2 ) - ( w2x + w2w / 2 );
                    }
                })
                .collect(Collectors.toList());

            if (offset >= 0)
            {
                //  Equal gap distribution...
                //  ------------------------------------------------------------
                Widget widget = sortedWidgets.get(0);
                int location = widget.propX().getValue();
                int width = widget.propWidth().getValue();

                for ( int i = 1; i < N - 1; i++ )
                {
                    widget = sortedWidgets.get(i);
                    location += width + offset;

                    undo.execute(new SetWidgetPropertyAction<>(widget.propX(), location));

                    width = widget.propWidth().getValue();
                }
            }
            else if ( offset < 0 )
            {
                //  Centers distribution...
                //  ------------------------------------------------------------
                //  First (leftmost) and last (rightmost) elements of the list
                //  are kept at their original position, the other widgets'
                //  centers are equally distributed between the centers of first
                //  and last widgets.
                Widget widget = sortedWidgets.get(N - 1);
                int location = widget.propX().getValue();
                int width = widget.propWidth().getValue();

                final int rightCenter = location + width / 2;

                widget = sortedWidgets.get(0);
                location = widget.propX().getValue();
                width = widget.propWidth().getValue();

                final int leftCenter = location + width / 2;
                final int coffset = ( rightCenter - leftCenter ) /  ( N - 1 );

                for ( int i = 1; i < N - 1; i++ )
                {
                    widget = sortedWidgets.get(i);
                    width = widget.propWidth().getValue();
                    undo.execute(new SetWidgetPropertyAction<>(widget.propX(), ( leftCenter + i * coffset ) - width / 2));
                }
            }
        }
    };

    /** Distribute widgets vertically */
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

            final int min = widgets.stream()
                                   .mapToInt(w -> w.propY().getValue())
                                   .min()
                                   .orElseThrow(NoSuchElementException::new);
            final int max = widgets.stream()
                                   .mapToInt(w -> w.propY().getValue() + w.propHeight().getValue())
                                   .max()
                                   .orElseThrow(NoSuchElementException::new);
            final int totalHeight = widgets.stream()
                                           .mapToInt(w -> w.propHeight().getValue())
                                           .sum();

            final int offset = ( max - min - totalHeight ) / ( N - 1 );
            final List<Widget> sortedWidgets = widgets.stream()
                                                      .sorted(( w1, w2 ) ->
            {
                    final int w1y = w1.propY().getValue().intValue();
                    final int w1h = w1.propHeight().getValue().intValue();
                    final int w2y = w2.propY().getValue().intValue();
                    final int w2h = w2.propHeight().getValue().intValue();

                    if ( w1y <= w2y && w1y + w1h <= w2y + w2h )
                    {   //  +----+          |  +----+
                        //  |    |             |    |
                        //  | w1 |  +----+  |  | w1 |
                        //  |    |  |    |     |    |
                        //  +----+  |    |  |  +----+
                        //          | w2 |
                        //          |    |  |          +----+
                        //          |    |             |    |
                        //          +----+  |          |    |
                        //                             | w2 |
                        //                  |          |    |
                        //                             |    |
                        //                  |          +----+
                        return -1;
                    }
                    else if ( w1y >= w2y && w1y + w1h >= w2y + w2h )
                    {   //  +----+          |  +----+
                        //  |    |             |    |
                        //  | w2 |  +----+  |  | w2 |
                        //  |    |  |    |     |    |
                        //  +----+  |    |  |  +----+
                        //          | w1 |
                        //          |    |  |          +----+
                        //          |    |             |    |
                        //          +----+  |          |    |
                        //                             | w1 |
                        //                  |          |    |
                        //                             |    |
                        //                  |          +----+
                        return 1;
                    }
                    else
                    {   //  +----+          |  +----+  +----+
                        //  |    |          |  |    |  |    |
                        //  |    |          |  |    |  |    |
                        //  |    |  +----+  |  |    |  |    |
                        //  | w1 |  |    |  |  | w1 |  | w2 |
                        //  |    |  | w2 |  |  |    |  |    |
                        //  |    |  |    |  |  |    |  |    |
                        //  |    |  +----+  |  |    |  |    |
                        //  +----+          |  +----+  +----+
                        return ( w1y + w1h / 2 ) - ( w2y + w2h / 2 );
                    }
            }).collect(Collectors.toList());

            if ( offset >= 0 )
            {
                //  Equal gap distribution...
                //  ------------------------------------------------------------
                Widget widget = sortedWidgets.get(0);
                int location = widget.propY().getValue();
                int height = widget.propHeight().getValue();

                for ( int i = 1; i < N - 1; i++ )
                {
                    widget = sortedWidgets.get(i);
                    location += height + offset;
                    undo.execute(new SetWidgetPropertyAction<>(widget.propY(), location));
                    height = widget.propHeight().getValue();
                }
            }
            else if ( offset < 0 )
            {
                //  Centers distribution...
                //  ------------------------------------------------------------
                //  First (topmost) and last (bottom-most) elements of the list
                //  are kept at their original position, the other widgets'
                //  centers are equally distributed between the centers of first
                //  and last widgets.
                Widget widget = sortedWidgets.get(N - 1);
                int location = widget.propY().getValue();
                int height = widget.propHeight().getValue();

                final int bottomCenter = location + height / 2;

                widget = sortedWidgets.get(0);
                location = widget.propY().getValue();
                height = widget.propHeight().getValue();

                final int topCenter = location + height / 2;
                final int coffset = ( bottomCenter - topCenter ) /  ( N - 1 );
                for ( int i = 1; i < N - 1; i++ )
                {
                    widget = sortedWidgets.get(i);
                    height = widget.propHeight().getValue();
                    undo.execute(new SetWidgetPropertyAction<>(widget.propY(), ( topCenter + i * coffset ) - height / 2));
                }
            }
        }
    };

    private static final String DISTRIBUTION_VGAP = "distribution_vgap";
    private static final String DISTRIBUTION_HGAP = "distribution_hgap";
    
    /** Distribute widgets horizontally with preset gap */
    public static final ActionDescription DIST_HORIZ_GAP =
        new ActionDescription("icons/distribute_hcg.png", Messages.DistributeHorizontallyGap)
    {
        @Override
        public void run(final DisplayEditor editor, final boolean selected)
        {
            final List<Widget> widgets = editor.getWidgetSelectionHandler().getSelection();
            final UndoableActionManager undo = editor.getUndoableActionManager();
            final int N = widgets.size();
            final Preferences prefs = PhoebusPreferenceService.userNodeForClass(DisplayEditorInstance.class);

            if (N < 3)
                return;

            int offset = prefs.getInt(DISTRIBUTION_HGAP, editor.getModel().propGridStepX().getValue());
            
            NumericInputDialog input = new NumericInputDialog(Messages.DistributeGapTitle, Messages.DistributeGapMessage, offset, val -> null);
            DialogHelper.positionDialog(input, editor.getContextMenuNode(), 0, 0);
            final double res = input.prompt();
            if (Double.isNaN(res))
                return;
            offset = (int)res;
            prefs.putInt(DISTRIBUTION_HGAP, offset);

            final List<Widget> sortedWidgets = widgets.stream()
                                                      .sorted(( w1, w2 ) ->
            {
                    final int w1x = w1.propX().getValue().intValue();
                    final int w1w = w1.propWidth().getValue().intValue();
                    final int w2x = w2.propX().getValue().intValue();
                    final int w2w = w2.propWidth().getValue().intValue();
                    // Description see DIST_HORIZ above
                    if ( w1x <= w2x && w1x + w1w <= w2x + w2w )
                        return -1;
                    else if ( w1x >= w2x && w1x + w1w >= w2x + w2w )
                        return 1;
                    else
                        return ( w1x + w1w / 2 ) - ( w2x + w2w / 2 );
                })
                .collect(Collectors.toList());

            //  Equal gap distribution...
            //  ------------------------------------------------------------
            Widget widget = sortedWidgets.get(0);
            int location = widget.propX().getValue();
            int width = widget.propWidth().getValue();

            for ( int i = 1; i < N; i++ )
            {
                widget = sortedWidgets.get(i);
                location += width + offset;

                undo.execute(new SetWidgetPropertyAction<>(widget.propX(), location));

                width = widget.propWidth().getValue();
            }
        }
    };

    /** Distribute widgets vertically with preset gap*/
    public static final ActionDescription DIST_VERT_GAP =
        new ActionDescription("icons/distribute_vcg.png", Messages.DistributeVerticallyGap)
    {
        @Override
        public void run(final DisplayEditor editor, final boolean selected)
        {
            final List<Widget> widgets = editor.getWidgetSelectionHandler().getSelection();
            final UndoableActionManager undo = editor.getUndoableActionManager();
            final int N = widgets.size();
            final Preferences prefs = PhoebusPreferenceService.userNodeForClass(DisplayEditorInstance.class);

            if (N < 3)
                return;

            int offset = prefs.getInt(DISTRIBUTION_VGAP, editor.getModel().propGridStepY().getValue());

            NumericInputDialog input = new NumericInputDialog(Messages.DistributeGapTitle, Messages.DistributeGapMessage, offset, val -> null);
            DialogHelper.positionDialog(input, editor.getContextMenuNode(), 0, 0);
            final double res = input.prompt();
            if (Double.isNaN(res))
                return;
            offset = (int)res;
            prefs.putInt(DISTRIBUTION_VGAP, offset);

            final List<Widget> sortedWidgets = widgets.stream()
                                                      .sorted(( w1, w2 ) ->
            {
                    final int w1y = w1.propY().getValue().intValue();
                    final int w1h = w1.propHeight().getValue().intValue();
                    final int w2y = w2.propY().getValue().intValue();
                    final int w2h = w2.propHeight().getValue().intValue();
                    // Description see DIST_VERT above
                    if ( w1y <= w2y && w1y + w1h <= w2y + w2h )
                        return -1;
                    else if ( w1y >= w2y && w1y + w1h >= w2y + w2h )
                        return 1;
                    else
                        return ( w1y + w1h / 2 ) - ( w2y + w2h / 2 );
            }).collect(Collectors.toList());

            //  Equal gap distribution...
            //  ------------------------------------------------------------
            Widget widget = sortedWidgets.get(0);
            int location = widget.propY().getValue();
            int height = widget.propHeight().getValue();

            for ( int i = 1; i < N; i++ )
            {
                widget = sortedWidgets.get(i);
                location += height + offset;
                undo.execute(new SetWidgetPropertyAction<>(widget.propY(), location));
                height = widget.propHeight().getValue();
            }
        }
    };

    /** Open in external Editor */
    public static final ActionDescription OPEN_EXTERNAL =
        new ActionDescription("icons/file.png", Messages.OpenInExternalEditor)
    {
        @Override
        public void run(final DisplayEditor editor, final boolean selected)
        {
            String resource = editor.getModel().getUserData(DisplayModel.USER_DATA_INPUT_FILE);
            try {
                AppResourceDescriptor application = ApplicationLauncherService.findApplication(new URI("display.xml"), true, (Stage)editor.getContextMenuNode().getScene().getWindow());
                if (application == null)
                    return;
                logger.log(Level.INFO, "Opening " + resource + " with " + application.getName());
                application.create(ResourceParser.getURI(new File(resource)));
            }
            catch (Exception e)
            {
                logger.log(Level.WARNING, "Opening " + resource + " failed.", e);
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
