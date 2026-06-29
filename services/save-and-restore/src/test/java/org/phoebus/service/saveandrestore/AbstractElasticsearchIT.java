/*
 * Copyright (C) 2026 European Spallation Source ERIC.
 */

package org.phoebus.service.saveandrestore;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.phoebus.service.saveandrestore.persistence.config.ElasticConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.UUID;

/**
 * Base class for save-and-restore integration tests run against a live Elasticsearch 8.x instance
 * (Spring profile {@code IT}).
 *
 * <p>Every concrete IT class gets its own randomly named set of Elasticsearch indices and its own
 * freshly created root folder node, so classes are fully isolated and cannot pollute one another
 * (e.g. a bulk {@code deleteAll()} in one class can no longer wipe the root that another class
 * depends on). {@link DirtiesContext} evicts the Spring context after each class: the context
 * cache key is derived from the {@link DynamicPropertySource} method (not the random values it
 * produces), so without eviction all subclasses would share a single context and a single index
 * set. Evicting forces the next class to rebuild the context, re-run {@code randomIndices} for a
 * fresh suffix, and re-create its root. The throwaway indices are dropped once the class finishes.
 */
@TestInstance(Lifecycle.PER_CLASS)
@SpringBootTest
@ContextConfiguration(classes = ElasticConfig.class)
@TestPropertySource(locations = "classpath:test_application.properties")
@Profile("IT")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class AbstractElasticsearchIT {

    private static final List<String> INDEX_PROPERTY_KEYS = List.of(
            "elasticsearch.tree_node.index",
            "elasticsearch.configuration_node.index",
            "elasticsearch.snapshot_node.index",
            "elasticsearch.composite_snapshot_node.index",
            "elasticsearch.filter.index");

    /**
     * Invoked once per subclass context build, producing a fresh suffix each time so each IT class
     * resolves to a distinct set of index names (and therefore a distinct, isolated Spring
     * context). Index names must be lowercase, which a lowercased UUID satisfies.
     */
    @DynamicPropertySource
    static void randomIndices(DynamicPropertyRegistry registry) {
        String suffix = "_" + UUID.randomUUID().toString().toLowerCase();
        registry.add("elasticsearch.tree_node.index", () -> "it_tree" + suffix);
        registry.add("elasticsearch.configuration_node.index", () -> "it_config" + suffix);
        registry.add("elasticsearch.snapshot_node.index", () -> "it_snapshot" + suffix);
        registry.add("elasticsearch.composite_snapshot_node.index", () -> "it_composite" + suffix);
        registry.add("elasticsearch.filter.index", () -> "it_filter" + suffix);
    }

    @Autowired
    private ElasticsearchClient client;

    @Autowired
    private Environment environment;

    @AfterAll
    void dropIndices() throws Exception {
        for (String key : INDEX_PROPERTY_KEYS) {
            String index = environment.getProperty(key);
            if (index != null) {
                client.indices().delete(d -> d.index(index).ignoreUnavailable(true));
            }
        }
    }
}
