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

package org.phoebus.service.saveandrestore.migration;

import org.phoebus.applications.saveandrestore.model.ConfigPv;
import org.phoebus.applications.saveandrestore.model.Configuration;
import org.phoebus.applications.saveandrestore.model.ConfigurationData;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.NodeType;
import org.phoebus.applications.saveandrestore.model.Snapshot;
import org.phoebus.applications.saveandrestore.model.SnapshotData;
import org.phoebus.applications.saveandrestore.model.SnapshotItem;
import org.phoebus.applications.saveandrestore.model.Tag;
import org.phoebus.service.saveandrestore.persistence.dao.impl.elasticsearch.ElasticsearchDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * Utility class executing a migration of save-n-restore data from (legacy) RDB-based service to
 * the service backed by Elasticsearch.
 * <p>
 * This is essentially non-interactive. Migration starts on top-level and traverses the entire tree structure
 * of objects. Source and target service should not be used during migration.
 * <p>
 * A dry run can be used to make sure connections work and that data can be read from the legacy service.
 * <p>
 * Simple debug printouts are sent to console to display progress.
 * <p>
 * This class assumes that the migration is executed by the target service in a special mode. The command line
 * is used to start the service and run the migration. See {@link org.phoebus.service.saveandrestore.application.Application}.
 */
public class MigrateRdbToElastic {

    private String legacyServiceUrl;
    private boolean dryRun;

    private ElasticsearchDAO elasticsearchDAO;

    private int folderCount;
    private int configurationCount;
    private int snapshotCount;
    private int snapshotItemCount;

    private final Logger logger = Logger.getLogger(MigrateRdbToElastic.class.getName());

    public MigrateRdbToElastic(ElasticsearchDAO elasticsearchDAO, String legacyServiceUrl, boolean dryRun) {
        this.legacyServiceUrl = legacyServiceUrl;
        this.dryRun = dryRun;
        this.elasticsearchDAO = elasticsearchDAO;

    }

    public void runMigration() {
        RestTemplate restTemplate = new RestTemplate();
        Node legacyRootNode = restTemplate.getForObject(legacyServiceUrl + "/root", Node.class);
        Node newRootNode = elasticsearchDAO.getRootNode();
        processNextNode(restTemplate, legacyServiceUrl, legacyRootNode, newRootNode, dryRun);

        logger.info("Folders: " + folderCount);
        logger.info("Configurations: " + configurationCount);
        logger.info("Snapshots: " + snapshotCount);
        logger.info("Snapshot items: " + snapshotItemCount);
    }

    private void processNextNode(RestTemplate restTemplate,
                                 String legacyServiceUrl,
                                 Node legacyParentNode,
                                 Node newParentNode, boolean dryRun) {
        ResponseEntity<Node[]> responseEntity =
                restTemplate.getForEntity(legacyServiceUrl + "/node/" + legacyParentNode.getUniqueId() + "/children", Node[].class);
        Node[] childNodes = responseEntity.getBody();
        for (Node childNode : childNodes) {
            if (childNode.getNodeType().equals(NodeType.FOLDER)) {
                Node newFolderNode = createNewFolderNode(childNode, newParentNode, dryRun);
                processNextNode(restTemplate, legacyServiceUrl, childNode, newFolderNode, dryRun);
            } else if (childNode.getNodeType().equals(NodeType.CONFIGURATION)) {
                processConfigurationNode(restTemplate, legacyServiceUrl, childNode, newParentNode, dryRun);
            }
        }
    }

    private Node createNewFolderNode(Node legacyFolderNode, Node newParentNode, boolean dryRun) {
        logger.info("Create folder node \"" + legacyFolderNode.getName() + "\" in parent folder node \"" + newParentNode.getName() + "\"");
        if (!dryRun) {
            folderCount++;
            return elasticsearchDAO.createNode(newParentNode.getUniqueId(), legacyFolderNode);
        } else {
            return null;
        }
    }

    private void processConfigurationNode(RestTemplate restTemplate,
                                          String legacyServiceUrl,
                                          Node legacyConfigurationNode,
                                          Node parentNode,
                                          boolean dryRun) {
        logger.info("Process configuration node \"" + legacyConfigurationNode.getName() + "\" in folder node \"" + parentNode.getName() + "\"");
        Node newConfigurationNode = createConfiguration(restTemplate, legacyServiceUrl, legacyConfigurationNode, parentNode, dryRun);
        ResponseEntity<Node[]> responseEntity = restTemplate.getForEntity(legacyServiceUrl + "/node/" + legacyConfigurationNode.getUniqueId() + "/children", Node[].class);
        Node[] childNodes = responseEntity.getBody();
        for (Node childNode : childNodes) {
            createSnapshot(restTemplate, legacyServiceUrl, childNode, newConfigurationNode, dryRun);
        }
    }

    private Node createConfiguration(RestTemplate restTemplate,
                                     String legacyServiceUrl,
                                     Node legacyConfigurationNode,
                                     Node newParentNode,
                                     boolean dryRun) {
        logger.info("Create configuration  \"" + legacyConfigurationNode.getName() + "\" in folder node \"" + newParentNode.getName() + "\"");
        if (!dryRun) {
            ResponseEntity<ConfigPv[]> responseEntity =
                    restTemplate.getForEntity(legacyServiceUrl + "/config/" + legacyConfigurationNode.getUniqueId() + "/items", ConfigPv[].class);
            if (legacyConfigurationNode.getProperties() != null) {
                if (legacyConfigurationNode.getProperties().get("description") != null) {
                    legacyConfigurationNode.setDescription(legacyConfigurationNode.getProperties().get("description"));
                }
            }
            legacyConfigurationNode.setProperties(null); // We do not want Elastic to save properties

            ConfigPv[] configPvs = responseEntity.getBody();
            Configuration configuration = new Configuration();
            configuration.setConfigurationNode(legacyConfigurationNode);
            ConfigurationData configurationData = new ConfigurationData();
            configurationData.setPvList(Arrays.asList(configPvs));
            configurationData.setUniqueId(legacyConfigurationNode.getUniqueId());
            configuration.setConfigurationData(configurationData);
            configurationCount++;
            return elasticsearchDAO.createConfiguration(newParentNode.getUniqueId(), configuration).getConfigurationNode();
        } else {
            return null;
        }
    }

    private void createSnapshot(RestTemplate restTemplate,
                                       String legacyServiceUrl,
                                       Node legacySnapshotNode,
                                       Node newConfigurationNode,
                                       boolean dryRun) {
        logger.info("Create snapshot node \"" + legacySnapshotNode.getName() + "\" in configuration node \"" + newConfigurationNode.getName() + "\"");
        if (!dryRun) {
            if (legacySnapshotNode.getProperties() != null) {
                if (legacySnapshotNode.getProperties().get("comment") != null) {
                    legacySnapshotNode.setDescription(legacySnapshotNode.getProperties().get("comment"));
                }
                List<Tag> tags;
                if (legacySnapshotNode.getProperties().get("golden") != null  &&
                        legacySnapshotNode.getProperties().get("golden").equals("true")) {
                    if (legacySnapshotNode.getTags() == null) {
                        tags = new ArrayList<>();
                        legacySnapshotNode.setTags(tags);
                    }
                    legacySnapshotNode.getTags().add(Tag.goldenTag(legacySnapshotNode.getUserName()));
                }
            }
            legacySnapshotNode.setProperties(null);
            ResponseEntity<SnapshotItem[]> responseEntity =
                    restTemplate.getForEntity(legacyServiceUrl + "/snapshot/" + legacySnapshotNode.getUniqueId() + "/items", SnapshotItem[].class);
            SnapshotItem[] snapshotItems = responseEntity.getBody();
            snapshotItemCount += snapshotItems.length;
            Snapshot snapshot = new Snapshot();
            snapshot.setSnapshotNode(legacySnapshotNode);
            SnapshotData snapshotData = new SnapshotData();
            snapshotData.setSnasphotItems(Arrays.asList(snapshotItems));
            snapshotData.setUniqueId(legacySnapshotNode.getUniqueId());
            snapshot.setSnapshotData(snapshotData);
            snapshotCount++;
            elasticsearchDAO.saveSnapshot(newConfigurationNode.getUniqueId(), snapshot);
        }
    }
}
