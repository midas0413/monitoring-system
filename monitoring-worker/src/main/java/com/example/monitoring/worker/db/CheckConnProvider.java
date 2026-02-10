package com.example.monitoring.worker.db;

import com.example.monitoring.common.domain.CheckEntity;
import com.example.monitoring.common.domain.MonitoringRuleEntity;
import com.example.monitoring.common.repo.ServerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;

@Component
public class CheckConnProvider {

    private static final Logger log = LoggerFactory.getLogger(CheckConnProvider.class);
    private static final int CONNECT_TIMEOUT_SEC = 10;
    private static final int QUERY_TIMEOUT_SEC = 30;

    private final ServerRepository serverRepo;

    public CheckConnProvider(ServerRepository serverRepo) {
        this.serverRepo = serverRepo;
    }

    public JdbcTemplate getJdbcTemplate(CheckEntity check) {
        return new JdbcTemplate(createDataSource(check));
    }

    /**
     * MonitoringRuleEntity를 위한 JdbcTemplate 생성
     */
    public JdbcTemplate getJdbcTemplateForRule(MonitoringRuleEntity rule) {
        return new JdbcTemplate(createDataSourceForRule(rule));
    }

    private DataSource createDataSource(CheckEntity check) {
        String url = resolveDbUrl(check);
        if (!StringUtils.hasText(url) || !StringUtils.hasText(check.getDbUsername())) {
            String errorMsg = String.format("Check DB info missing. checkId=%d, url=%s, user=%s, host=%s, dbType=%s, dbPort=%s, dbName=%s",
                    check.getId(),
                    (url != null ? "set" : "null"),
                    (check.getDbUsername() != null ? check.getDbUsername() : "null"),
                    check.getHost(),
                    check.getDbType(),
                    check.getDbPort(),
                    check.getDbName());
            log.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }

        // 패스워드가 null이면 빈 문자열로 설정 (일부 DB는 패스워드 없이 접속 가능)
        // 하지만 패스워드가 필요한 경우를 위해 경고 로그 남김
        String password = check.getDbPassword() != null ? check.getDbPassword() : "";
        if (!StringUtils.hasText(password)) {
            // 패스워드가 없으면 빈 문자열로 설정하되, 실제 DB 접속 시 인증 실패 가능
            // 이 경우 계정 잠금을 방지하기 위해 로그 남김
            log.warn("Check DB password is empty. checkId={}, username={}. This may cause authentication failures and account lockouts.", 
                    check.getId(), check.getDbUsername());
        }

        String driverClassName = driverClassFor(url);
        if (driverClassName == null) {
            String errorMsg = String.format("Unsupported DB type or invalid URL. checkId=%d, url=%s, dbType=%s", 
                    check.getId(), url, check.getDbType());
            log.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }

        String finalUrl = appendConnectionProps(url);
        
        log.debug("Creating DataSource. checkId={}, url={}, username={}, driver={}", 
                check.getId(), finalUrl, check.getDbUsername(), driverClassName);

        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName(driverClassName);
        ds.setUrl(finalUrl);
        ds.setUsername(check.getDbUsername());
        ds.setPassword(password);

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

    private DataSource createDataSourceForRule(MonitoringRuleEntity rule) {
        String url = rule.getDbUrl();
        if (!StringUtils.hasText(url) || !StringUtils.hasText(rule.getDbUsername())) {
            String errorMsg = String.format("Rule DB info missing. ruleId=%d, url=%s, user=%s, dbType=%s, dbPort=%s, dbName=%s",
                    rule.getId(),
                    (url != null ? "set" : "null"),
                    (rule.getDbUsername() != null ? rule.getDbUsername() : "null"),
                    rule.getDbType(),
                    rule.getDbPort(),
                    rule.getDbName());
            log.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }

        String password = rule.getDbPassword() != null ? rule.getDbPassword() : "";
        if (!StringUtils.hasText(password)) {
            log.warn("Rule DB password is empty. ruleId={}, username={}. This may cause authentication failures and account lockouts.",
                    rule.getId(), rule.getDbUsername());
        }

        String driverClassName = driverClassFor(url);
        if (driverClassName == null) {
            String errorMsg = String.format("Unsupported DB type or invalid URL. ruleId=%d, url=%s, dbType=%s",
                    rule.getId(), url, rule.getDbType());
            log.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }

        String finalUrl = appendConnectionProps(url);

        log.debug("Creating DataSource for rule. ruleId={}, url={}, username={}, driver={}",
                rule.getId(), finalUrl, rule.getDbUsername(), driverClassName);

        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName(driverClassName);
        ds.setUrl(finalUrl);
        ds.setUsername(rule.getDbUsername());
        ds.setPassword(password);

        return ds;
    }
}
