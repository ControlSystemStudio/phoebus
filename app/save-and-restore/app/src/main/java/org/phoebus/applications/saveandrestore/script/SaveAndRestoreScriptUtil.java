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
 */

package org.phoebus.applications.saveandrestore.script;

import org.phoebus.applications.saveandrestore.Utilities;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.SnapshotItem;
import org.phoebus.applications.saveandrestore.service.SaveAndRestoreClient;
import org.phoebus.applications.saveandrestore.service.impl.SaveAndRestoreJerseyClient;
import org.phoebus.pv.PVPool;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class SaveAndRestoreScriptUtil {

    private static SaveAndRestoreClient saveAndRestoreClient;
    private static final Logger logger = Logger.getLogger(SaveAndRestoreScriptUtil.class.getName());

    static {
        saveAndRestoreClient = new SaveAndRestoreJerseyClient();
    }

    public static List<Node> getSnapshots(String saveSetId) {
        return saveAndRestoreClient.getChildNodes(saveSetId);
    }

    public static List<SnapshotItem> getSnapshotItems(String snapshotId) {
        return saveAndRestoreClient.getSnapshotItems(snapshotId);
    }

    public static void restore(String snapshotId) throws Exception {
        List<SnapshotItem> snapshotItems = saveAndRestoreClient.getSnapshotItems(snapshotId);
        List<SnapshotItem> restorableItems =
                snapshotItems.stream().filter(item -> !item.getConfigPv().isReadOnly()).collect(Collectors.toList());
        CountDownLatch countDownLatch = new CountDownLatch(restorableItems.size());
        List<String> restoreFailed = new ArrayList<>();
        for (SnapshotItem item : restorableItems) {
            final org.phoebus.pv.PV pv = PVPool.getPV(item.getConfigPv().getPvName());
            try {
                pv.write(Utilities.toRawValue(item.getValue()));
            } catch (Exception writeException) {
                restoreFailed.add(item.getConfigPv().getPvName());
            } finally {
                countDownLatch.countDown();
                PVPool.releasePV(pv);
            }
        }

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            logger.log(Level.INFO, "Encountered InterruptedException", e);
        }

        if (restoreFailed.isEmpty()) {
            logger.log(Level.FINE, "Restored snapshot {0}", snapshotId);
        } else {
            Collections.sort(restoreFailed);
            StringBuilder sb = new StringBuilder(restoreFailed.size() * 200);
            restoreFailed.forEach(e -> sb.append(e).append('\n'));
            logger.log(Level.WARNING,
                    "Not all PVs could be restored for {0}: The following errors occured:\n{1}",
                    new Object[]{snapshotId, sb.toString()});
        }
    }
}
