/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.dialog;

import javafx.beans.InvalidationListener;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.Skin;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcTo;
import javafx.scene.shape.HLineTo;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.VLineTo;

/** Skin for {@link PopOver}
*
*  <p>Shows the popover's 'content' with a background
*  consisting of border and pointer to active owner.
*
*  @author Kay Kasemir
*/
public class PopOverSkin implements Skin<PopOver>
{
   private PopOver popover;

   /** Root node
    *  - 'background' for border and pointer
    *  - 'content' of popover
    */
   private final StackPane root = new StackPane();

   // popover.content uses (0, 0) - (w, h)
   // background extends around content by 'ROUND'
   private final Path background = new Path();

   /** Size of the pointer (includes BORDER_SIZE) */
   static final double POINTER_SIZE = 20.0;

   private static final double BORDER_SIZE = 10.0;

   private final InvalidationListener update_background = prop ->
   {
       final Bounds owner_bounds = popover.getActiveOwner().localToScreen(popover.getActiveOwner().getBoundsInLocal());
       final Bounds content_bounds = popover.getContentNode().localToScreen(popover.getContentNode().getBoundsInLocal());
       final double w = content_bounds.getWidth(),  h = content_bounds.getHeight();

       switch (popover.getSideProperty().get())
       {
       case TOP:
           {   // popup is on top of owner, reference point is on bottom
               final double ref_x = owner_bounds.getWidth()/2 + owner_bounds.getMinX()  - content_bounds.getMinX();
               background.getElements().setAll(
                       new MoveTo(0, -BORDER_SIZE),
                       new HLineTo(w),
                       new ArcTo(BORDER_SIZE, BORDER_SIZE, 0, w+BORDER_SIZE, 0, false, true),
                       new VLineTo(h),
                       new ArcTo(BORDER_SIZE, BORDER_SIZE, 0, w, h+BORDER_SIZE, false, true),
                       new HLineTo(ref_x + BORDER_SIZE),
                       new LineTo(ref_x, h+POINTER_SIZE),
                       new LineTo(ref_x-BORDER_SIZE, h+BORDER_SIZE),
                       new HLineTo(0),
                       new ArcTo(BORDER_SIZE, BORDER_SIZE, 0, -BORDER_SIZE, h, false, true),
                       new VLineTo(0),
                       new ArcTo(BORDER_SIZE, BORDER_SIZE, 0, 0, -BORDER_SIZE, false, true));
           }
           break;
       case BOTTOM:
           {   // popup is on bottom of owner, reference point is on top
               final double ref_x = owner_bounds.getWidth()/2 + owner_bounds.getMinX()  - content_bounds.getMinX();
               background.getElements().setAll(
                       new MoveTo(0, -BORDER_SIZE),
                       new HLineTo(ref_x - BORDER_SIZE),
                       new LineTo(ref_x, -POINTER_SIZE),
                       new LineTo(ref_x+BORDER_SIZE, -BORDER_SIZE),
                       new HLineTo(w),
                       new ArcTo(BORDER_SIZE, BORDER_SIZE, 0, w+BORDER_SIZE, 0, false, true),
                       new VLineTo(h),
                       new ArcTo(BORDER_SIZE, BORDER_SIZE, 0, w, h+BORDER_SIZE, false, true),
                       new HLineTo(0),
                       new ArcTo(BORDER_SIZE, BORDER_SIZE, 0, -BORDER_SIZE, h, false, true),
                       new VLineTo(0),
                       new ArcTo(BORDER_SIZE, BORDER_SIZE, 0, 0, -BORDER_SIZE, false, true));
           }
           break;
       case LEFT:
           {   // popup is left of owner, reference point is on right
               final double ref_y = owner_bounds.getHeight()/2 + owner_bounds.getMinY()  - content_bounds.getMinY();
               background.getElements().setAll(
                       new MoveTo(0, -BORDER_SIZE),
                       new HLineTo(w),
                       new ArcTo(BORDER_SIZE, BORDER_SIZE, 0, w+BORDER_SIZE, 0, false, true),
                       new VLineTo(ref_y-BORDER_SIZE),
                       new LineTo(w+POINTER_SIZE, ref_y),
                       new LineTo(w+BORDER_SIZE, ref_y+BORDER_SIZE),
                       new VLineTo(h),
                       new ArcTo(BORDER_SIZE, BORDER_SIZE, 0, w, h+BORDER_SIZE, false, true),
                       new HLineTo(0),
                       new ArcTo(BORDER_SIZE, BORDER_SIZE, 0, -BORDER_SIZE, h, false, true),
                       new VLineTo(0),
                       new ArcTo(BORDER_SIZE, BORDER_SIZE, 0, 0, -BORDER_SIZE, false, true));
           }
           break;
       default:
           {   // popup is right of owner, reference point is on left
               final double ref_y = owner_bounds.getHeight()/2 + owner_bounds.getMinY()  - content_bounds.getMinY();
               background.getElements().setAll(
                       new MoveTo(0, -BORDER_SIZE),
                       new HLineTo(w),
                       new ArcTo(BORDER_SIZE, BORDER_SIZE, 0, w+BORDER_SIZE, 0, false, true),
                       new VLineTo(h),
                       new ArcTo(BORDER_SIZE, BORDER_SIZE, 0, w, h+BORDER_SIZE, false, true),
                       new HLineTo(0),
                       new ArcTo(BORDER_SIZE, BORDER_SIZE, 0, -BORDER_SIZE, h, false, true),
                       new VLineTo(ref_y+BORDER_SIZE),
                       new LineTo(-POINTER_SIZE, ref_y),
                       new LineTo(-BORDER_SIZE, ref_y-BORDER_SIZE),
                       new VLineTo(0),
                       new ArcTo(BORDER_SIZE, BORDER_SIZE, 0, 0, -BORDER_SIZE, false, true));
           }
           break;
       }
   };

   PopOverSkin(final PopOver popover)
   {
       this.popover = popover;

       // Style the background:
       // Light, slightly transparent
       background.setStrokeWidth(0.5);
       background.setFill(Color.gray(1.0, 0.95));
       // Drop Shadow 'radius' will add to the overall size of 'root'
       background.setEffect(new DropShadow(5, 2, 2, Color.gray(0.0, 1.0)));

       // Background is laid out by 'update_background'.
       // It extends beyond the size of 'content',
       // and 'root' must not grow because of 'background',
       // because that would then grow 'content', which
       // grow 'background' and so in in infinite loop.
       background.setManaged(false);

       // Update the background (border, pointer)
       // whenever the 'side' or the size of 'content' change
       popover.getSideProperty().addListener(update_background);
       popover.getContentNode().boundsInLocalProperty().addListener(update_background);

       root.getChildren().setAll(background, popover.getContentNode());
       root.setPickOnBounds(false);
   }

   @Override
   public PopOver getSkinnable()
   {
       return popover;
   }

   @Override
   public Node getNode()
   {
       return root;
   }

   @Override
   public void dispose()
   {
       // NOP
   }
}
