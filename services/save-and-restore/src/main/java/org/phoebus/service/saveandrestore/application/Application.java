/** 
 * Copyright (C) 2018 European Spallation Source ERIC.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.phoebus.service.saveandrestore.application;

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
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

@SpringBootApplication(scanBasePackages = "org.phoebus.service.saveandrestore")
@EnableScheduling
@EnableAutoConfiguration
public class Application {

    private static ConfigurableApplicationContext context;

    private static void help()
    {
        System.out.println();
        System.out.println("Command-line arguments:");
        System.out.println();
        System.out.println("-help                                    - This text");
        System.out.println("-migrate http://legacy.service.url       - Migrate data from legacy service at http://legacy.service.url");
        System.out.println();
    }

    public static void main(String[] args) {

        // load the default properties
        final Properties properties = PropertiesHelper.getProperties();

        final List<String> arguments = new ArrayList<>(List.of(args));
        final Iterator<String> iterator = arguments.iterator();
        boolean runMigration = false;
        try {
            while (iterator.hasNext()) {
                final String command = iterator.next();
                if (command.equals("-h") || command.equals("-help")) {
                    help();
                    System.exit(0);
                    return;
                }
                else if(command.equals("-migrate")){
                    if (!iterator.hasNext())
                        throw new Exception("Missing -migrate legacy.service.url");
                    runMigration = true;
                    iterator.remove();
                    properties.put("legacy.service.url",iterator.next());
                    iterator.remove();
                }
                else if(command.equals("-dryRun")){
                    iterator.remove();
                    properties.put("dryRun", true);
                }

            }
        }
        catch (Exception ex) {
            System.out.println("\n>>>> Print StackTrace ....");
            ex.printStackTrace();
            System.out.println("\n>>>> Please check available arguments of save & restore as follows:");
            help();
            System.exit(-1);
            return;
        }

    	context = SpringApplication.run(Application.class, args);

        if(runMigration){
            try {
                String dryRun =  properties.getProperty("dryRun", "false");
                runMigration(properties.getProperty("legacy.service.url"), Boolean.parseBoolean(dryRun));
            } catch (Exception e) {
                e.printStackTrace();
            }
            finally {
                close();
            }
        }
    }

    private static void close() {
        System.out.println("\n Shutdown");
        if (context != null) {
            context.close();
        }
        System.exit(0);
    }

    private static void runMigration(String legacyServiceUrl, boolean dryRun) {
        ElasticsearchDAO elasticsearchDAO = context.getBean(ElasticsearchDAO.class);
        RestTemplate restTemplate = new RestTemplate();
        Node legacyRootNode = restTemplate.getForObject(legacyServiceUrl + "/root", Node.class);
        Node newRootNode = elasticsearchDAO.getRootNode();
        processNextNode(restTemplate, legacyServiceUrl, legacyRootNode, newRootNode, dryRun);
    }

    private static void processNextNode(RestTemplate restTemplate,
                                        String legacyServiceUrl,
                                        Node legacyParentNode,
                                        Node newParentNode, boolean dryRun){
        ResponseEntity<Node[]> responseEntity =
                restTemplate.getForEntity(legacyServiceUrl + "/node/" + legacyParentNode.getUniqueId() + "/children", Node[].class);
        Node[] childNodes = responseEntity.getBody();
        for(Node childNode : childNodes){
            if(childNode.getNodeType().equals(NodeType.FOLDER)){
                Node newFolderNode = createNewFolderNode(childNode, newParentNode, dryRun);
                processNextNode(restTemplate, legacyServiceUrl, childNode, newFolderNode, dryRun);
            }
            else if(childNode.getNodeType().equals(NodeType.CONFIGURATION)){
                processConfigurationNode(restTemplate, legacyServiceUrl, childNode, newParentNode, dryRun);
            }
        }
    }

    private static Node createNewFolderNode(Node legacyFolderNode, Node newParentNode, boolean dryRun){
        System.out.println("Create folder node " + legacyFolderNode.getName() + " in parent folder node " + newParentNode.getName());
        if(!dryRun){
            ElasticsearchDAO elasticsearchDAO = context.getBean(ElasticsearchDAO.class);
            return elasticsearchDAO.createNode(newParentNode.getUniqueId(), legacyFolderNode);
        }
        else{
            return null;
        }
    }

    private static void processConfigurationNode(RestTemplate restTemplate,
                                                 String legacyServiceUrl,
                                                 Node legacyConfigurationNode,
                                                 Node parentNode, 
                                                 boolean dryRun){
        System.out.println("Process configuration node " + legacyConfigurationNode.getName() + " in folder node " + parentNode.getName());
        Node newConfigurationNode = createConfiguration(restTemplate, legacyServiceUrl, legacyConfigurationNode, parentNode, dryRun);
        ResponseEntity<Node[]> responseEntity = restTemplate.getForEntity(legacyServiceUrl + "/node/" + legacyConfigurationNode.getUniqueId() + "/children", Node[].class);
        Node[] childNodes = responseEntity.getBody();
        for(Node childNode : childNodes){
            createSnapshot(restTemplate, legacyServiceUrl, childNode, newConfigurationNode, dryRun);
        }
    }

    private static Node createConfiguration(RestTemplate restTemplate,
                                            String legacyServiceUrl,
                                            Node legacyConfigurationNode,
                                            Node newParentNode,
                                            boolean dryRun){
        System.out.println("Create configuration  " + legacyConfigurationNode.getName() + " in folder node " + newParentNode.getName());
        if(!dryRun){
            ResponseEntity<ConfigPv[]> responseEntity =
                    restTemplate.getForEntity(legacyServiceUrl + "/config/" + legacyConfigurationNode.getUniqueId() + "/items", ConfigPv[].class);
            if(legacyConfigurationNode.getProperties() != null){
                if(legacyConfigurationNode.getProperties().get("description") != null){
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
            ElasticsearchDAO elasticsearchDAO = context.getBean(ElasticsearchDAO.class);
            return elasticsearchDAO.createConfiguration(newParentNode.getUniqueId(), configuration).getConfigurationNode();
        }
        else{
            return null;
        }
    }

    private static void createSnapshot(RestTemplate restTemplate,
                                       String legacyServiceUrl,
                                       Node legacySnapshotNode,
                                       Node newConfigurationNode,
                                       boolean dryRun){
        System.out.println("Create snapshot node " + legacySnapshotNode.getName() + " in configuration node " + newConfigurationNode.getName());
        if(!dryRun){
            if(legacySnapshotNode.getProperties() != null){
                if(legacySnapshotNode.getProperties().get("comment") != null){
                    legacySnapshotNode.setDescription(legacySnapshotNode.getProperties().get("comment"));
                }
                List<Tag> tags;
                if(legacySnapshotNode.getProperties().get("golden") != null){
                    if(legacySnapshotNode.getTags() == null){
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
            Snapshot snapshot = new Snapshot();
            snapshot.setSnapshotNode(legacySnapshotNode);
            SnapshotData snapshotData = new SnapshotData();
            snapshotData.setSnasphotItems(Arrays.asList(snapshotItems));
            snapshotData.setUniqueId(legacySnapshotNode.getUniqueId());
            snapshot.setSnapshotData(snapshotData);

            ElasticsearchDAO elasticsearchDAO = context.getBean(ElasticsearchDAO.class);
            elasticsearchDAO.saveSnapshot(newConfigurationNode.getUniqueId(), snapshot);
        }
    }
}
