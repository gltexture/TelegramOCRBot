package ru.gltexture.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

public class Database {
    private static final HikariDataSource ds;
    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:postgresql://localhost:5432/telegram_ocr_test");
        config.setUsername("postgres");
        config.setPassword("admin");
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setDriverClassName("org.postgresql.Driver");
        ds = new HikariDataSource(config);
    }

    public static DataSource get() {
        return Database.ds;
    }
}