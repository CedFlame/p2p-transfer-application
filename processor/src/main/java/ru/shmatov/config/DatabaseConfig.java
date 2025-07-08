package ru.shmatov.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
public class DatabaseConfig {

    @Value("${database.username}")
    private String username;
    @Value("${database.password}")
    private String password;
    @Value("${database.url}")
    private String url;
    @Value("${database.driver}")
    private String driver;

    @Bean
    public DataSource dataSource() {
        HikariDataSource datasource = new HikariDataSource();
        datasource.setDriverClassName(driver);
        datasource.setJdbcUrl(url);
        datasource.setUsername(username);
        datasource.setPassword(password);
        return datasource;
    }

    @Bean
    public JdbcTemplate jdbcTemplate() {
        return new JdbcTemplate(dataSource());
    }

}
