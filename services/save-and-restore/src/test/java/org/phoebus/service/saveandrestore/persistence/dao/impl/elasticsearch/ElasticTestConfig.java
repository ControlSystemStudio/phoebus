package org.phoebus.service.saveandrestore.persistence.dao.impl.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import org.mockito.Mockito;
import org.phoebus.service.saveandrestore.search.SearchUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!IT")
@SuppressWarnings("unused")
public class ElasticTestConfig {

    @Bean
    public ElasticsearchDAO elasticsearchDAO() {
        return new ElasticsearchDAO();
    }


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

    @Bean
    public FilterRepository filterRepository() {
        return Mockito.mock(FilterRepository.class);
    }

    @Bean
    public CompositeSnapshotDataRepository compositeSnapshotDataRepository() {
        return Mockito.mock(CompositeSnapshotDataRepository.class);
    }

    @Bean
    public SearchUtil searchUtil() {
        return Mockito.mock(SearchUtil.class);
    }

    @SuppressWarnings("unused")
    @Bean
    public SnapshotDataRepository snapshotDataRepository(){
        return Mockito.mock(SnapshotDataRepository.class);
    }
}
