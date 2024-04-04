/*
 * Copyright (C) 2020 European Spallation Source ERIC.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 */

package org.phoebus.aspects.ui;

import javafx.event.EventHandler;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogEvent;
import javafx.stage.Modality;
import javafx.stage.Window;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;

import java.util.List;

/**
 * Aspect used to intercept launch/close of modal dialogs in order to set opacity all other windows.
 */
@SuppressWarnings("unused")
@Aspect
public class ModalDialogLaunchAspect {

    /**
     * If {@link JoinPoint} target is a modal {@link Dialog}, this will set 0.5 opacity
     * other windows when {@link Dialog#showAndWait()} is called.
     *
     * @param joinPoint {@link JoinPoint} who's target should be a {@link Dialog}
     */
    @Before("call(* javafx.scene.control.Dialog.showAndWait())")
    public void beforeShowAndWait(JoinPoint joinPoint) {
        // Consider only modal dialogs
        if (joinPoint.getTarget() instanceof Dialog<?> && !((Dialog) joinPoint.getTarget()).getModality().equals(Modality.NONE)) {
            setOpacity(0.5);
            addOnCloseHandler((Dialog) joinPoint.getTarget());
        }
    }

    /**
     * If {@link JoinPoint} target is a modal {@link Dialog}, this will set 0.5 opacity
     * other windows when {@link Dialog#show()} is called.
     *
     * @param joinPoint {@link JoinPoint} who's target should be a {@link Dialog}
     */
    @Before("call(* javafx.scene.control.Dialog.show())")
    public void beforeShow(JoinPoint joinPoint) {
        beforeShowAndWait(joinPoint);
    }

    /**
     * Called if exception is thrown in {@link Dialog}.
     */
    @AfterThrowing("call(* javafx.scene.control.Dialog.showAndWait())")
    public void afterThrowingShowAndWait(JoinPoint joinPoint){
        // Consider only modal dialogs
        if (joinPoint.getTarget() instanceof Dialog<?> && !((Dialog) joinPoint.getTarget()).getModality().equals(Modality.NONE)) {
            setOpacity(1.0);
        }
    }

    /**
     * Called if exception is thrown in {@link Dialog}.
     */
    @AfterThrowing("call(* javafx.scene.control.Dialog.show())")
    public void afterThrowingShow(JoinPoint joinPoint){
        afterThrowingShowAndWait(joinPoint);
    }

    private void setOpacity(double opacity) {
       List<Window> windows = Window.getWindows();
        for (Window window : windows) {
            // ContextMenu is a Window, so do not disable as it may not be present in
            // the Window.getWindows() list when Dialog.showAndWait() returns.
            if (window instanceof ContextMenu) {
                continue;
            }
            // Add null checks just in case.
            if(window.getScene() != null && window.getScene().getRoot() != null){
                window.getScene().getRoot().setOpacity(opacity);
            }
        }
    }

    private void addOnCloseHandler(Dialog dialog){
        final EventHandler<DialogEvent> eventHandler = dialog.getOnCloseRequest();
        dialog.setOnCloseRequest(e -> {
            // Dialog may already have an onClose handler.
            // If so, call it, then restore opacity.c
            if(eventHandler != null){
                eventHandler.handle((DialogEvent)e);
            }
            setOpacity(1.0);
        });
    }
}