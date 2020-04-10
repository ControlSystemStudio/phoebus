/*
 * Copyright (C) 2019 European Spallation Source ERIC.
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

package org.phoebus.logbook.ui;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import org.phoebus.logbook.LogService;
import org.phoebus.logbook.Logbook;

import java.text.MessageFormat;
import java.util.Collection;

/**
 * Simple utility class for construct an alert dialog informing
 * the user about logbook service issues.
 */
public class LogbookAvailabilityChecker{

    public static boolean isLogbookAvailable(){
        String logBookProviderId = LogbookUiPreferences.logbook_factory;
        Collection<Logbook> logbooks =
                null;
        try {
            logbooks = LogService.getInstance().getLogFactories(logBookProviderId).getLogClient().listLogbooks();
        } catch (Exception e) {
            showAlert(logBookProviderId);
            return false;
        }
        if(logbooks == null || logbooks.isEmpty()){
            showAlert(logBookProviderId);
            return false;
        }
        return true;
    }

    private static void showAlert(String logBookProviderId){
        Platform.runLater(() -> {
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle(Messages.LogbookServiceUnavailableTitle);
            alert.setHeaderText(
                    MessageFormat.format(Messages.LogbookServiceHasNoLogbooks, logBookProviderId));
            alert.showAndWait();
        });
    }
}
