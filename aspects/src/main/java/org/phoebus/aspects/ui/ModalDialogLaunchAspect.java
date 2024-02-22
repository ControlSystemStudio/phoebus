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

import javafx.collections.ObservableList;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Dialog;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;

@Aspect
public class ModalDialogLaunchAspect {


    @Before("call(* javafx.scene.control.Dialog.showAndWait())")
    public  void beforeShowDialog(JoinPoint joinPoint) {
        System.out.println("A");
        Dialog dialog = (Dialog) joinPoint.getTarget();
        Parent scene = dialog.getOwner().getScene().getRoot();
        System.out.println("Before " + scene);
        scene.disableProperty().setValue(true);
        ObservableList<Window> windows = Stage.getWindows();
        dialog.setOnHiding(e -> {
            windows.forEach(w -> {
                w.getScene().getRoot().getChildrenUnmodifiable().forEach(c -> System.out.println(c));
                System.out.println("After loop " + w.getScene());
                w.getScene().getRoot().disableProperty().setValue(false);

            });
        });
        if (!dialog.getModality().equals(Modality.NONE)) {
            windows.forEach(w -> {
                w.getScene().getRoot().getChildrenUnmodifiable().forEach(c -> System.out.println(c));
                System.out.println("Before loop " + w.getScene());
                w.getScene().getRoot().disableProperty().setValue(true);
            });
        }
    }

    /*
    @Before("call(* javafx.scene.control.Dialog.showAndWait())")
    public void beforeShowDialogBefore(JoinPoint joinPoint) {
        System.out.println("AA");
        Dialog dialog = (Dialog) joinPoint.getTarget();
        ObservableList<Window> windows = Window.getWindows();
        if (!dialog.getModality().equals(Modality.NONE)) {
            windows.forEach(w -> {
                System.out.println("Before " + w.getScene());
                w.getScene().getRoot().setOpacity(0.5);
            });
        }
    }

     */

    /*

    @After("call(* javafx.scene.control.Dialog.showAndWait())")
    public  void afterDismissDialog(JoinPoint joinPoint){
        System.out.println("B");
        Dialog dialog = (Dialog) joinPoint.getTarget();
        Parent scene = dialog.getOwner().getScene().getRoot();
        System.out.println("After " + scene);
        scene.disableProperty().setValue(false);
        ObservableList<Window> windows = Stage.getWindows();
        if (!dialog.getModality().equals(Modality.NONE)) {
            windows.forEach(w -> {
                w.getScene().getRoot().getChildrenUnmodifiable().forEach(c -> System.out.println(c));
                System.out.println("After loop " + w.getScene());
                w.getScene().getRoot().disableProperty().setValue(false);

            });
        }
    }

     */


    /*
    @After("call(Object javafx.scene.control.Alert.showAndWait())")
    public void afterDismissAlert(JoinPoint joinPoint){
        System.out.println("C");
        Dialog dialog = (Dialog) joinPoint.getTarget();
        if(!dialog.getModality().equals(Modality.NONE)){
            synchronized (syncObject){
                Window.getWindows().forEach(w -> {
                    System.out.println("After " + w.getScene());
                    Platform.runLater(() ->  w.getScene().getRoot().setOpacity(1.0));

                });
            }
        }
        else{
            System.out.println("After: Non-modal");
        }
    }

     */
}
