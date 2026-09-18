/**
 * Copyright (C) 2026 European Spallation Source ERIC.
 */

package org.phoebus.service.saveandrestore.web.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;
import jakarta.servlet.*;
import org.mockito.Mockito;
import org.phoebus.saveandrestore.util.SnapshotUtil;
import org.phoebus.service.saveandrestore.persistence.dao.NodeDAO;
import org.phoebus.service.saveandrestore.persistence.dao.impl.elasticsearch.ConfigurationDataRepository;
import org.phoebus.service.saveandrestore.persistence.dao.impl.elasticsearch.ElasticsearchTreeRepository;
import org.phoebus.service.saveandrestore.persistence.dao.impl.elasticsearch.FilterRepository;
import org.phoebus.service.saveandrestore.persistence.dao.impl.elasticsearch.SnapshotDataRepository;
import org.phoebus.service.saveandrestore.search.SearchUtil;
import org.phoebus.service.saveandrestore.websocket.WebSocketService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.mock.web.MockServletContext;

import java.util.Base64;

import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.socket.WebSocketSession;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
@ComponentScan(basePackages = {"org.phoebus.service.saveandrestore.web.controllers",
        "org.phoebus.service.saveandrestore.web.config"})
@SuppressWarnings("unused")
@EnableWebMvc
@EnableWebSecurity
@Profile("!IT")
public class ControllersTestConfig {

    @Value("${role.user:sar-user}")
    private String roleUser;

    @Value("${role.admin:sar-admin}")
    private String roleAdmin;

    @Value("${demo.user:user}")
    private String demoUser;

    @Value("${demo.user.password:userPass}")
    private String demoUserPassword;

    @Value("${demo.admin:admin}")
    private String demoAdmin;

    @Value("${demo.admin.password:adminPass}")
    private String demoAdminPassword;

    @Value("${demo.readOnly:johndoe}")
    private String demoReadOnly;

    @Value("${demo.readOnly.password:1234}")
    private String demoReadOnlyPassword;

    @Bean
    public String demoUser() {
        return demoUser;
    }

    @Bean
    public String demoAdmin() {
        return demoAdmin;
    }

    @Bean()
    public String roleAdmin(){
        return roleAdmin.toUpperCase();
    }

    @Bean
    public String roleUser(){
        return roleUser.toUpperCase();
    }

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

    @Bean("restClient")
    public Rest5Client restClient() {
        return Mockito.mock(Rest5Client.class);
    }

    @Bean("elasticObjectMapper")
    public ObjectMapper elasticObjectMapper() {
        return JsonMapper.builder().build();
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
        return "Basic " + Base64.getEncoder().encodeToString((demoUser + ":" + demoUserPassword).getBytes());
    }

    @Bean("adminAuthorization")
    public String adminAuthorization() {
        return "Basic " + Base64.getEncoder().encodeToString((demoAdmin + ":" + demoAdminPassword).getBytes());
    }

    @Bean("readOnlyAuthorization")
    public String readOnlyAuthorization() {
        return "Basic " + Base64.getEncoder().encodeToString((demoReadOnly + ":" + demoReadOnlyPassword).getBytes());
    }

    @Bean
    public ExecutorService executorService() {
        return Executors.newCachedThreadPool();
    }

    @Bean
    public SnapshotUtil snapshotUtil() {
       return new SnapshotUtil();
    }

    @Bean
    public WebSocketSession webSocketSession() {
        return Mockito.mock(WebSocketSession.class);
    }

    @Bean
    public WebSocketService webSocketService() {
        return Mockito.mock(WebSocketService.class);
    }

    @Bean
    public SimpMessagingTemplate simpMessagingTemplate() {
        return Mockito.mock(SimpMessagingTemplate.class);
    }

    @Bean
    public SimpUserRegistry simpUserRegistry() {
        return Mockito.mock(SimpUserRegistry.class);
    }

    @Bean
    public long connectionTimeout() {
        return 5000;
    }

    @Bean
    public ServletContext servletContext() {
        MockServletContext mockServletContext = new MockServletContext();
        mockServletContext.setContextPath("/");
        return mockServletContext;
    }

    @Bean
    public String context() {
        return servletContext().getContextPath().length() > 1 ?
                servletContext().getContextPath() : "";
    }
}
