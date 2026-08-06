package org.phoebus.service.saveandrestore.web.config;

import org.phoebus.saveandrestore.util.SnapshotUtil;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class ComparisonControllerRealSnapshotUtilTestConfig {

    @Bean("realSnapshotUtil")
    @Primary
    public SnapshotUtil snapshotUtil() {
        return new SnapshotUtil();
    }
}
