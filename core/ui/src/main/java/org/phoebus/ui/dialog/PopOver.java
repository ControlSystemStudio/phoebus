/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.dialog;

import org.phoebus.ui.javafx.ScreenUtil;

import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.property.ObjectPropertyBase;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Bounds;
import javafx.geometry.Rectangle2D;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.PopupControl;
import javafx.scene.control.Skin;
import javafx.scene.layout.Region;
import javafx.stage.Window;

/** PopupControl with fixed 'content'.
 *
 *  <p>Displays that content as a popup,
 *  adding a basic background.
 *
 *  <p>Popup is positioned relative to an 'owner' node
 *  and moves with it when the window is relocated.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PopOver extends PopupControl
{
   /** Root of content scene graph */
   private Node content;

   /** Node to which the popover is currently attached */
   private Region active_owner;

   /** Side of owner where popover is shown */
   private ObjectPropertyBase<Side> side = new SimpleObjectProperty<>(Side.BOTTOM);

   /** Update side and position to stay visually attached to 'active_owner' */
   private final InvalidationListener update_position = p ->
   {
       if (active_owner == null)
           return;

       final Bounds owner_bounds = active_owner.localToScreen(active_owner.getBoundsInLocal());
       final Side current_side = determineSide(owner_bounds);
       side.set(current_side);

       final Bounds popup_bounds = content.getBoundsInLocal();
       switch (current_side)
       {
       case TOP:
           setAnchorX(owner_bounds.getMinX() + owner_bounds.getWidth()/2 - popup_bounds.getWidth()/2);
           setAnchorY(owner_bounds.getMinY() - popup_bounds.getHeight() - PopOverSkin.POINTER_SIZE);
           break;
       case BOTTOM:
           setAnchorX(owner_bounds.getMinX() + owner_bounds.getWidth()/2 - popup_bounds.getWidth()/2);
           setAnchorY(owner_bounds.getMaxY() + PopOverSkin.POINTER_SIZE);
           break;
       case LEFT:
           setAnchorX(owner_bounds.getMinX() - popup_bounds.getWidth() - PopOverSkin.POINTER_SIZE);
           setAnchorY(owner_bounds.getMinY() + owner_bounds.getHeight()/2 - popup_bounds.getHeight()/2);
           break;
       case RIGHT:
           setAnchorX(owner_bounds.getMaxX() + PopOverSkin.POINTER_SIZE);
           setAnchorY(owner_bounds.getMinY() + owner_bounds.getHeight()/2 - popup_bounds.getHeight()/2);
           break;
       default:
           break;
       }
   };
   private final WeakInvalidationListener weak_update_position = new WeakInvalidationListener(update_position);

   /** Create popover
    *
    *   <p>Derived class must call {@link #setContent(Node)}
    *   in its constructor!
    */
   public PopOver()
   {
   }

   /** Create popover
    *
    *  @param content Root of content scene graph
    */
   public PopOver(final Node content)
   {
       setContent(content);
   }

   /** Derived class must either use the
    *  {@link PopOver#PopOver(Node)} constructor
    *  or call this to set the content.
    *
    *  <p>Must not be called to change the content
    *  at a later time.
    *
    *  @param content
    */
   protected void setContent(final Node content)
   {
       if (this.content != null)
           throw new IllegalStateException("Must set content exactly once");
       this.content = content;
       content.boundsInLocalProperty().addListener(weak_update_position);
   }

   // Side where popup should appear. For PopOverSkin.
   ObjectPropertyBase<Side> getSideProperty()
   {
       return side;
   }

   Node getActiveOwner()
   {
       return active_owner;
   }

   /** @return Content node */
   @SuppressWarnings("unchecked")
   protected <N extends Node> N getContentNode()
   {
       return (N) content;
   }

   @Override
   protected Skin<PopOver> createDefaultSkin()
   {
       return new PopOverSkin(this);
   }

   /** @param owner_bounds Bounds of active owner
    *  @return Suggested Side for the popup
    */
   private Side determineSide(final Bounds owner_bounds)
   {
       // Determine center of active owner
       final double owner_x = owner_bounds.getMinX() + owner_bounds.getWidth()/2;
       final double owner_y = owner_bounds.getMinY() + owner_bounds.getHeight()/2;

       // Locate screen
       Rectangle2D screen_bounds = ScreenUtil.getScreenBounds(owner_x, owner_y);
       if (screen_bounds == null)
           return Side.BOTTOM;

       // left .. right as -0.5 .. +0.5
       double lr = (owner_x - screen_bounds.getMinX())/screen_bounds.getWidth() - 0.5;
       // top..buttom as -0.5 .. +0.5
       double tb = (owner_y - screen_bounds.getMinY())/screen_bounds.getHeight() - 0.5;

       // More left/right from center, or top/bottom from center of screen?
       if (Math.abs(lr) > Math.abs(tb))
           return (lr < 0) ? Side.RIGHT : Side.LEFT;
       else
           return (tb < 0) ? Side.BOTTOM : Side.TOP;
   }

   /** Show PopOver positioned relative to other node
    *
    *  <p>Moving the node or window will result
    *  in move of the PopOver.
    *
    *  <p>When the owner looses focus,
    *  the popup will hide.
    *
    *  @param owner Owner node relative to which the PopOver will be located
    *  @see {@link PopupControl#hide()}
    */
   public void show(final Region owner)
   {
       if (content == null)
           throw new IllegalStateException("Must set content exactly once");

       // Unhook from previous owner
       if (active_owner != null)
       {
           final Window window = active_owner.getScene().getWindow();
           window.xProperty().removeListener(weak_update_position);
           window.yProperty().removeListener(weak_update_position);
           active_owner.layoutXProperty().removeListener(weak_update_position);
           active_owner.layoutYProperty().removeListener(weak_update_position);
       }

       // Track movement of owner resp. its window
       active_owner = owner;
       final Window window = owner.getScene().getWindow();
       window.xProperty().addListener(weak_update_position);
       window.yProperty().addListener(weak_update_position);
       owner.layoutXProperty().addListener(weak_update_position);
       owner.layoutYProperty().addListener(weak_update_position);

       // Show relative to owner
       update_position.invalidated(null);
       show(owner, getAnchorX(), getAnchorY());
   }
}
