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

package org.phoebus.service.saveandrestore.persistence.dao.impl.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.DeleteIndexRequest;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.phoebus.applications.saveandrestore.model.ConfigPv;
import org.phoebus.applications.saveandrestore.model.ConfigurationData;
import org.phoebus.service.saveandrestore.persistence.config.ElasticConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration test to be executed against a running Elasticsearch 8.x instance.
 * It must be run with application property spring.profiles.active=IT.
 */
@TestInstance(Lifecycle.PER_CLASS)
@SpringBootTest
@ContextConfiguration(classes = ElasticConfig.class)
@TestPropertySource(locations = "classpath:test_application.properties")
@Profile("IT")
@SuppressWarnings("unused")
public class ConfigurationDataRepositoryTestIT {

    @Autowired
    private ElasticsearchClient client;

    @Value("${elasticsearch.tree_node.index:test_saveandrestore_configuration}")
    private String ES_CONFIGURATION_INDEX;

    @Autowired
    private ConfigurationDataRepository configurationDataRepository;

    @Test
    public void testCreateAndFindConfigurationData() {
        String uniqueId = UUID.randomUUID().toString();
        ConfigurationData configurationData = new ConfigurationData();
        configurationData.setUniqueId(uniqueId);
        configurationData.setPvList(List.of(ConfigPv.builder().pvName("pvName").readbackPvName("readbackPvName").build()));

        configurationData = configurationDataRepository.save(configurationData);

        assertEquals(uniqueId, configurationData.getUniqueId());
        assertEquals(1, configurationData.getPvList().size());

        assertTrue(configurationDataRepository.findById(uniqueId).isPresent());
        assertTrue(configurationDataRepository.existsById(uniqueId));
        assertTrue(configurationDataRepository.findById("invalid").isEmpty());
    }

    @Test
    public void testUpdate() {

        String uniqueId = UUID.randomUUID().toString();
        ConfigurationData configurationData = new ConfigurationData();
        configurationData.setUniqueId(uniqueId);
        ConfigPv configPv1 = ConfigPv.builder().pvName("pvName").readbackPvName("readbackPvName").build();
        configurationData.setPvList(List.of(configPv1));

        configurationData = configurationDataRepository.save(configurationData);

        ConfigPv configPv2 = ConfigPv.builder().pvName("pvName2").readbackPvName("readbackPvName").build();

        configurationData.setPvList(Arrays.asList(configPv1, configPv2));

        configurationData = configurationDataRepository.save(configurationData);

        assertEquals(2, configurationData.getPvList().size());


    }

    @Test
    public void testDeleteById() {
        String uniqueId = UUID.randomUUID().toString();
        ConfigurationData configurationData = new ConfigurationData();
        configurationData.setUniqueId(uniqueId);
        ConfigPv configPv1 = ConfigPv.builder().pvName("pvName").readbackPvName("readbackPvName").build();
        configurationData.setPvList(List.of(configPv1));

        configurationData = configurationDataRepository.save(configurationData);

        configurationDataRepository.deleteById(configurationData.getUniqueId());

        assertFalse(configurationDataRepository.existsById(configurationData.getUniqueId()));
    }

    @Test
    public void testDeleteAll(){
        String uniqueId = UUID.randomUUID().toString();
        ConfigurationData configurationData = new ConfigurationData();
        configurationData.setUniqueId(uniqueId);
        ConfigPv configPv1 = ConfigPv.builder().pvName("pvName").readbackPvName("readbackPvName").build();
        configurationData.setPvList(List.of(configPv1));

        configurationDataRepository.save(configurationData);

        String uniqueId2 = UUID.randomUUID().toString();
        ConfigurationData configurationData2 = new ConfigurationData();
        configurationData2.setUniqueId(uniqueId2);
        configurationData2.setPvList(List.of(configPv1));

        configurationDataRepository.save(configurationData2);
        configurationDataRepository.deleteAll();

        assertEquals(0, configurationDataRepository.count());
    }


    @AfterAll
    public void dropIndex() {
        try {
            BooleanResponse exists = client.indices().exists(ExistsRequest.of(e -> e.index(ES_CONFIGURATION_INDEX)));
            if (exists.value()) {
                client.indices().delete(
                        DeleteIndexRequest.of(
                                c -> c.index(ES_CONFIGURATION_INDEX)));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
