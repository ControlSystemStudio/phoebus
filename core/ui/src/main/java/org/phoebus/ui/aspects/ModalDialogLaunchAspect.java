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

package org.phoebus.ui.aspects;

import javafx.scene.control.Dialog;
import javafx.stage.Stage;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.phoebus.ui.docking.DockPane;
import org.phoebus.ui.docking.DockStage;

import java.util.List;

@Aspect
public class ModalDialogLaunchAspect {

    @Before("call(* javafx.scene.control.Dialog.showAndWait())")
    public void beforeShowDialog(JoinPoint joinPoint){
        List<Stage> dockStages = DockStage.getDockStages();
        dockStages.forEach(s -> {
            DockStage.getDockPanes(s).forEach(p -> {
                p.disableProperty().set(true);
            });
        });
    }

    @After("call(* javafx.scene.control.Dialog.showAndWait())")
    public void afterDismiss(){
        List<Stage> dockStages = DockStage.getDockStages();

        dockStages.forEach(s -> {
            DockStage.getDockPanes(s).forEach(p -> {
                p.disableProperty().set(false);
            });
        });
    }
}
