package com.example.monitoring.worker.db;

import com.example.monitoring.common.domain.CheckEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;

@Component
public class CheckConnProvider {

    private static final int CONNECT_TIMEOUT_SEC = 10;
    private static final int QUERY_TIMEOUT_SEC = 30;

    public JdbcTemplate getJdbcTemplate(CheckEntity check) {
        return new JdbcTemplate(createDataSource(check));
    }

    private DataSource createDataSource(CheckEntity check) {
        String url = resolveDbUrl(check);
        if (!StringUtils.hasText(url) || !StringUtils.hasText(check.getDbUsername())) {
            throw new IllegalStateException("Check DB info missing. checkId=" + check.getId() + ", url=" + (url != null ? "set" : "null") + ", user=" + (check.getDbUsername() != null ? "set" : "null"));
        }

        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName(driverClassFor(url));
        ds.setUrl(appendConnectionProps(url));
        ds.setUsername(check.getDbUsername());
        ds.setPassword(check.getDbPassword() != null ? check.getDbPassword() : "");

        return ds;
    }

    private String driverClassFor(String url) {
        if (url == null) return null;
        if (url.startsWith("jdbc:oracle:")) return "oracle.jdbc.OracleDriver";
        if (url.startsWith("jdbc:postgresql:")) return "org.postgresql.Driver";
        if (url.startsWith("jdbc:mysql:")) return "com.mysql.cj.jdbc.Driver";
        if (url.startsWith("jdbc:sqlserver:")) return "com.microsoft.sqlserver.jdbc.SQLServerDriver";
        return null;
    }

    private String appendConnectionProps(String url) {
        if (url == null) return null;
        String sep = url.contains("?") ? "&" : "?";
        int ct = CONNECT_TIMEOUT_SEC * 1000;
        int st = QUERY_TIMEOUT_SEC * 1000;
        if (url.startsWith("jdbc:postgresql:")) {
            return url + sep + "connectTimeout=" + ct + "&socketTimeout=" + st;
        }
        if (url.startsWith("jdbc:mysql:")) {
            return url + sep + "connectTimeout=" + ct + "&socketTimeout=" + st;
        }
        return url;
    }

    private String resolveDbUrl(CheckEntity check) {
        if (StringUtils.hasText(check.getDbUrl())) {
            return check.getDbUrl();
        }
        String host = check.getHost();
        String dbType = check.getDbType();
        Integer dbPort = check.getDbPort();
        String dbName = check.getDbName();
        if (!StringUtils.hasText(host) || !StringUtils.hasText(dbType) || dbPort == null || !StringUtils.hasText(dbName)) {
            return null;
        }
        String subprotocol = switch (dbType.toLowerCase()) {
            case "postgresql" -> "postgresql";
            case "mysql" -> "mysql";
            case "oracle" -> "oracle:thin";
            case "mssql" -> "sqlserver";
            default -> dbType.toLowerCase();
        };
        if ("oracle:thin".equals(subprotocol)) {
            return "jdbc:oracle:thin:@" + host + ":" + dbPort + ":" + dbName;
        }
        return "jdbc:" + subprotocol + "://" + host + ":" + dbPort + "/" + dbName;
    }
}
