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

package org.phoebus.applications.saveandrestore.logging;

import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.event.SaveAndRestoreEventReceiver;
import org.phoebus.logbook.Attachment;
import org.phoebus.logbook.LogEntry;
import org.phoebus.logbook.LogFactory;
import org.phoebus.logbook.LogService;
import org.phoebus.logbook.Logbook;
import org.phoebus.logbook.LogbookPreferences;
import org.phoebus.logbook.Property;
import org.phoebus.logbook.Tag;
import org.phoebus.security.store.SecureStore;
import org.phoebus.security.tokens.ScopedAuthenticationToken;
import org.phoebus.security.tokens.SimpleAuthenticationToken;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation of the {@link SaveAndRestoreEventReceiver} with the purpose of logging the events
 * (new snapshot or restore of a snapshot) to the electronic logbook. For this to work a log client must
 * be available, and at least one destination logbook must be configured in the preferences.
 */
@SuppressWarnings("unused")
public class SaveAndRestoreEventLogger implements SaveAndRestoreEventReceiver {

    private static final Logger logger = Logger.getLogger(SaveAndRestoreEventLogger.class.getName());

    /**
     * Called when a snapshot has been created and persisted by the save-and-restore service.
     *
     * @param node         The created and persisted {@link Node}
     * @param errorHandler An error handler callback.
     */
    @Override
    public void snapshotSaved(Node node, Consumer<String> errorHandler) {
        LogFactory logFactory = LogService.getInstance().getLogFactories().get(LogbookPreferences.logbook_factory);
        if (logFactory == null) {
            return;
        }
        try {
            SecureStore secureStore = new SecureStore();
            ScopedAuthenticationToken scopedAuthenticationToken = secureStore.getScopedAuthenticationToken("logbook");
            if (scopedAuthenticationToken == null) {
                errorHandler.accept(Messages.LogbookCredentialsMissing);
                return;
            }
            logFactory.getLogClient(new SimpleAuthenticationToken(scopedAuthenticationToken.getUsername(), scopedAuthenticationToken.getPassword()))
                    .set(new LogEntrySnapshotImpl(node));
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to log save-and-restore event", e);
            errorHandler.accept(e.getMessage());
        }
    }

    /**
     * Called when a snapshot has been restored.
     *
     * @param node         The restored snapshot {@link Node}
     * @param failedPVs    List of PV names that could not be restored. Empty if all PVs in the save set were
     *                     restored successfully.
     * @param errorHandler An error handler callback.
     */
    @Override
    public void snapshotRestored(Node node, List<String> failedPVs, Consumer<String> errorHandler) {
        LogFactory logFactory = LogService.getInstance().getLogFactories().get(LogbookPreferences.logbook_factory);
        if (logFactory == null) {
            return;
        }
        try {
            SecureStore secureStore = new SecureStore();
            ScopedAuthenticationToken scopedAuthenticationToken = secureStore.getScopedAuthenticationToken("logbook");
            if (scopedAuthenticationToken == null) {
                errorHandler.accept(Messages.LogbookCredentialsMissing);
                return;
            }
            logFactory.getLogClient(new SimpleAuthenticationToken(scopedAuthenticationToken.getUsername(), scopedAuthenticationToken.getPassword()))
                    .set(new LogEntryRestoreImpl(node, failedPVs));
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to log save-and-restore event", e);
            errorHandler.accept(e.getMessage());
        }
    }

    private String getSnapshotInfoTable(Node node) {
        StringBuilder stringBuilder = new StringBuilder();
        // This is needed!
        stringBuilder.append("| | |\n");
        // This is needed!
        stringBuilder.append("|-|-|\n");
        stringBuilder.append("| Snapshot name | ").append(node.getName()).append(" |\n");
        stringBuilder.append("| Created | ").append(node.getCreated()).append(" |\n");
        String isGolden = node.getProperties().get("golden");
        stringBuilder.append("| Golden | ").append(isGolden != null && "true".equals(isGolden) ? "yes" : "no").append(" |\n");
        stringBuilder.append("| User id | ").append(node.getUserName()).append(" |\n");

        return stringBuilder.toString();
    }

    private String getFailedPVsTable(List<String> pvs) {
        StringBuilder stringBuilder = new StringBuilder();
        // This is needed!
        stringBuilder.append("| |\n");
        // This is needed!
        stringBuilder.append("|-|\n");
        pvs.forEach(p -> stringBuilder.append("| ").append(p).append(" |\n"));

        return stringBuilder.toString();
    }

    private class LogEntrySnapshotImpl implements LogEntry {

        protected Node node;

        public LogEntrySnapshotImpl(Node node) {
            this.node = node;
        }

        @Override
        public Long getId() {
            return null;
        }

        @Override
        public String getOwner() {
            return node.getUserName();
        }

        @Override
        public String getTitle() {
            return "New snapshot created";
        }

        @Override
        public String getDescription() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(getSnapshotInfoTable(node));
            return stringBuilder.toString();
        }

        @Override
        public String getLevel() {
            return SaveAndRestoreLoggingPreferences.level;
        }

        @Override
        public Instant getCreatedDate() {
            return Instant.ofEpochMilli(node.getCreated().getTime());
        }

        @Override
        public Instant getModifiedDate() {
            return null;
        }

        @Override
        public int getVersion() {
            return 0;
        }

        @Override
        public Collection<Tag> getTags() {
            List<String> tags = SaveAndRestoreLoggingPreferences.tags;
            List<Tag> tagsList = new ArrayList<>();
            tags.forEach(t -> tagsList.add(new Tag() {
                @Override
                public String getName() {
                    return t;
                }

                @Override
                public String getState() {
                    return "Active";
                }
            }));
            return tagsList;
        }

        @Override
        public Tag getTag(String s) {
            return null;
        }

        @Override
        public Collection<Logbook> getLogbooks() {
            List<String> logbooks = SaveAndRestoreLoggingPreferences.logbooks;
            List<Logbook> logbookList = new ArrayList<>();
            logbooks.forEach(l -> logbookList.add(new Logbook() {
                @Override
                public String getName() {
                    return l;
                }

                @Override
                public String getOwner() {
                    return null;
                }
            }));
            return logbookList;
        }

        @Override
        public Collection<Attachment> getAttachments() {
            return Collections.emptyList();
        }

        @Override
        public Collection<Property> getProperties() {
            Property property = new Property() {
                @Override
                public String getName() {
                    return "resource";
                }

                @Override
                public Map<String, String> getAttributes() {
                    Map<String, String> map = new HashMap<>();
                    map.put("file", "file:/" + node.getUniqueId() + "?app=saveandrestore");
                    map.put("name", node.getName());
                    return map;
                }
            };
            return List.of(property);
        }

        @Override
        public Property getProperty(String s) {
            return null;
        }
    }

    private class LogEntryRestoreImpl extends LogEntrySnapshotImpl {

        private final List<String> failedPVs;

        public LogEntryRestoreImpl(Node node, List<String> failedPVs) {
            super(node);
            this.failedPVs = failedPVs;
        }

        @Override
        public String getTitle() {
            return "Snapshot restored";
        }

        @Override
        public String getDescription() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(super.getDescription());
            if (failedPVs != null && !failedPVs.isEmpty()) {
                stringBuilder.append("\n\n").append("**Failed to restore the following PVs:**\n\n");
                stringBuilder.append(getFailedPVsTable(failedPVs));
            }
            return stringBuilder.toString();
        }
    }
}
