package com.backend.app.boostrapper.config.database;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.zaxxer.hikari.HikariDataSource;

@Configuration
@EntityScan("com.backend.app.shared.models.entities")
@EnableTransactionManagement
public class DatabaseConnection {

    @Primary
    @Bean(name = "dataSource")
    public DataSource dataSource() {
        return DataSourceBuilder
            .create()
            .url("jdbc:mysql://localhost:3306/bookshop?useSSL=false&allowPublicKeyRetrieval=true&createDatabaseIfNotExist=true")
            .username("root")
            .password("G5!kT@2y9B#zU8%w")
            .driverClassName("com.mysql.cj.jdbc.Driver")
            .type(HikariDataSource.class)
            .build();
    }

    @Bean
    public DataSourceTransactionManager transactionManager() {
        return new DataSourceTransactionManager(dataSource());
    }
}
