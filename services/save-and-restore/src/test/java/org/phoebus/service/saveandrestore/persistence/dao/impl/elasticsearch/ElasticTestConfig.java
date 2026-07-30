package org.phoebus.service.saveandrestore.persistence.dao.impl.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

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
