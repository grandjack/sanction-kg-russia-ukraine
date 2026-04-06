package com.sanction.kg.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * MariaDB/JPA configuration.
 * DataSource is auto-configured by Spring Boot from application.yml.
 * JPA repositories are scanned from the mariadb package.
 */
@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = "com.sanction.kg.repository.mariadb")
public class MariaDbConfig {
    // Spring Boot auto-configuration handles DataSource, EntityManagerFactory,
    // and PlatformTransactionManager based on application.yml properties.
}
