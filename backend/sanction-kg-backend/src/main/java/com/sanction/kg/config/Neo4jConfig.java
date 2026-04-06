package com.sanction.kg.config;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Config;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PreDestroy;
import java.util.concurrent.TimeUnit;

@Configuration
public class Neo4jConfig {

    private static final Logger log = LoggerFactory.getLogger(Neo4jConfig.class);

    @Value("${neo4j.uri}")
    private String uri;

    @Value("${neo4j.username}")
    private String username;

    @Value("${neo4j.password}")
    private String password;

    @Value("${neo4j.max-connection-pool-size:50}")
    private int maxConnectionPoolSize;

    @Value("${neo4j.connection-acquisition-timeout:30000}")
    private long connectionAcquisitionTimeout;

    private Driver driver;

    @Bean
    public Driver neo4jDriver() {
        log.info("Initializing Neo4j driver for URI: {}", uri);
        Config config = Config.builder()
                .withMaxConnectionPoolSize(maxConnectionPoolSize)
                .withConnectionAcquisitionTimeout(connectionAcquisitionTimeout, TimeUnit.MILLISECONDS)
                .withMaxTransactionRetryTime(5, TimeUnit.SECONDS)
                .build();
        driver = GraphDatabase.driver(uri, AuthTokens.basic(username, password), config);
        driver.verifyConnectivity();
        log.info("Neo4j driver initialized successfully");
        return driver;
    }

    @PreDestroy
    public void close() {
        if (driver != null) {
            log.info("Closing Neo4j driver");
            driver.close();
        }
    }
}
