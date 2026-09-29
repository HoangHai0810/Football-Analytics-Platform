package com.football.analytics.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;

@Repository
public class ClickHouseRepository {
    private static final Logger log = LoggerFactory.getLogger(ClickHouseRepository.class);

    @Value("${clickhouse.host:localhost}")
    private String host;

    @Value("${clickhouse.port:8123}")
    private int port;

    @Value("${clickhouse.database:football_analytics}")
    private String database;

    @Value("${clickhouse.user:default}")
    private String user;

    @Value("${clickhouse.password:clickhouse_dev}")
    private String password;

    @Value("${clickhouse.socket-timeout-ms:5000}")
    private int socketTimeoutMs;

    public boolean testConnection() {
        String url = String.format("jdbc:ch://%s:%d/%s", host, port, database);
        Properties props = new Properties();
        props.setProperty("user", user);
        props.setProperty("password", password);
        props.setProperty("socket_timeout", String.valueOf(socketTimeoutMs));

        try (Connection conn = DriverManager.getConnection(url, props);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT 1")) {
            if (rs.next()) {
                log.info("ClickHouse connection verified successfully on port {}", port);
                return true;
            }
        } catch (Exception e) {
            log.debug("ClickHouse is currently offline or unreachable: {}. Falling back to high-fidelity seed store.", e.getMessage());
        }
        return false;
    }

    public String getClickhouseUrl() {
        return String.format("http://%s:%d", host, port);
    }
}
