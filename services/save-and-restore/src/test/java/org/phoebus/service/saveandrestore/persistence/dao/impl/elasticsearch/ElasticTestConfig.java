package org.phoebus.service.saveandrestore.persistence.dao.impl.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch._types.Result;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.epics.vtype.VType;
import org.mockito.Mockito;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.NodeType;
import org.phoebus.applications.saveandrestore.model.json.VTypeDeserializer;
import org.phoebus.applications.saveandrestore.model.json.VTypeSerializer;
import org.phoebus.service.saveandrestore.model.ESTreeNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.phoebus.applications.saveandrestore.model.Node.ROOT_FOLDER_UNIQUE_ID;

@Configuration
@ComponentScan(basePackages = {"org.phoebus.service.saveandrestore"})
@SuppressWarnings("unused")
@Profile("!IT")
public class ElasticTestConfig {


    @Bean({"client"})
    public ElasticsearchClient getClient() {
        return Mockito.mock(ElasticsearchClient.class);
    }

    @Bean
    public ElasticsearchTreeRepository elasticsearchTreeRepository(){
        return Mockito.mock(ElasticsearchTreeRepository.class);
    }

    @Bean
    public ConfigurationDataRepository configurationDataRepository(){
        return Mockito.mock(ConfigurationDataRepository.class);
    }

    @SuppressWarnings("unused")
    @Bean
    public SnapshotDataRepository snapshotDataRepository(){
        return Mockito.mock(SnapshotDataRepository.class);
    }
}
