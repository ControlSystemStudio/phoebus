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
import org.phoebus.saveandrestore.util.SnapshotUtil;
import org.phoebus.service.saveandrestore.persistence.dao.NodeDAO;
import org.phoebus.service.saveandrestore.persistence.dao.impl.elasticsearch.ConfigurationDataRepository;
import org.phoebus.service.saveandrestore.persistence.dao.impl.elasticsearch.ElasticsearchTreeRepository;
import org.phoebus.service.saveandrestore.persistence.dao.impl.elasticsearch.FilterRepository;
import org.phoebus.service.saveandrestore.persistence.dao.impl.elasticsearch.SnapshotDataRepository;
import org.phoebus.service.saveandrestore.search.SearchUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.util.Base64Utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootConfiguration
@ComponentScan(basePackages = "org.phoebus.service.saveandrestore.web.controllers")
@Import(WebSecurityConfig.class)
@SuppressWarnings("unused")
@Profile("!IT")
public class ControllersTestConfig {

    @Autowired
    private String demoUser;

    @Autowired
    private String demoUserPassword;

    @Autowired
    private String demoAdmin;

    @Autowired
    private String demoAdminPassword;

    @Autowired
    private String demoReadOnly;

    @Autowired
    private String demoReadOnlyPassword;

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

    @Bean("userAuthorization")
    public String userAuthorization() {
        return "Basic " + Base64Utils.encodeToString((demoUser + ":" + demoUserPassword).getBytes());
    }

    @Bean("adminAuthorization")
    public String adminAuthorization() {
        return "Basic " + Base64Utils.encodeToString((demoAdmin + ":" + demoAdminPassword).getBytes());
    }

    @Bean("readOnlyAuthorization")
    public String readOnlyAuthorization() {
        return "Basic " + Base64Utils.encodeToString((demoReadOnly + ":" + demoReadOnlyPassword).getBytes());
    }

    @Bean
    public ExecutorService executorService() {
        return Executors.newCachedThreadPool();
    }

    @Bean
    public SnapshotUtil snapshotUtil() {
        return new SnapshotUtil();
    }
}
