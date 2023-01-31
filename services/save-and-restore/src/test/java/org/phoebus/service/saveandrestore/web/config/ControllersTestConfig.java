/**
 * Copyright (C) 2018 European Spallation Source ERIC.
 * <p>
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.phoebus.service.saveandrestore.web.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import org.mockito.Mockito;
import org.phoebus.service.saveandrestore.persistence.dao.NodeDAO;
import org.phoebus.service.saveandrestore.persistence.dao.impl.elasticsearch.ConfigurationDataRepository;
import org.phoebus.service.saveandrestore.persistence.dao.impl.elasticsearch.ElasticsearchTreeRepository;
import org.phoebus.service.saveandrestore.persistence.dao.impl.elasticsearch.FilterRepository;
import org.phoebus.service.saveandrestore.persistence.dao.impl.elasticsearch.SnapshotDataRepository;
import org.phoebus.service.saveandrestore.search.SearchUtil;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@SpringBootConfiguration
@ComponentScan(basePackages = "org.phoebus.service.saveandrestore.web.controllers")
@SuppressWarnings("unused")
@Profile("!IT")
public class ControllersTestConfig {

    @Bean
    public NodeDAO nodeDAO() {
        return Mockito.mock(NodeDAO.class);
    }

    @Bean
    public ElasticsearchTreeRepository elasticsearchTreeRepository() {
        return Mockito.mock(ElasticsearchTreeRepository.class);
    }

    @Bean
    public ConfigurationDataRepository configurationRepository() {
        return Mockito.mock(ConfigurationDataRepository.class);
    }

    @Bean
    public FilterRepository filterRepository() {
        return Mockito.mock(FilterRepository.class);
    }

    @Bean
    public SnapshotDataRepository snapshotRepository() {
        return Mockito.mock(SnapshotDataRepository.class);
    }

    @Bean
    public ElasticsearchClient client() {
        return Mockito.mock(ElasticsearchClient.class);
    }

    @SuppressWarnings("unused")
    @Bean
    public AcceptHeaderResolver acceptHeaderResolver() {
        return new AcceptHeaderResolver();
    }

    @SuppressWarnings("unused")
    @Bean
    public SearchUtil searchUtil() {
        return new SearchUtil();
    }
}
