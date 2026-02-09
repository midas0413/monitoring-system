package com.example.monitoring.web.service;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * Check 등록 시 SHELL/SQL을 대상에 테스트. 연결 정보를 파라미터로 받음.
 */
@Service
public class CheckTestService {

    private static final int SSH_CONNECT_TIMEOUT_MS = 10_000;
    private static final int SSH_CMD_TIMEOUT_MS = 30_000;

    public TestResult testShell(String host, Integer port, String sshUsername, String sshPassword, String sshPrivateKeyPath, String script) {
        if (!StringUtils.hasText(script)) {
            return TestResult.fail("스크립트를 입력하세요.");
        }
        if (!StringUtils.hasText(host)) {
            return TestResult.fail("Host를 입력하세요.");
        }
        int p = (port != null) ? port : 22;
        if (!StringUtils.hasText(sshUsername)) {
            return TestResult.fail("SSH Username을 입력하세요.");
        }
        if (!StringUtils.hasText(sshPassword) && !StringUtils.hasText(sshPrivateKeyPath)) {
            return TestResult.fail("SSH 비밀번호 또는 Private Key 경로가 필요합니다.");
        }

        try (SSHClient ssh = new SSHClient()) {
            ssh.addHostKeyVerifier(new PromiscuousVerifier());
            ssh.setConnectTimeout(SSH_CONNECT_TIMEOUT_MS);
            ssh.setTimeout(SSH_CMD_TIMEOUT_MS);
            ssh.connect(host, p);

            if (StringUtils.hasText(sshPrivateKeyPath)) {
                ssh.authPublickey(sshUsername, sshPrivateKeyPath);
            } else {
                ssh.authPassword(sshUsername, sshPassword != null ? sshPassword : "");
            }

            try (var session = ssh.startSession()) {
                var cmd = session.exec(script.trim());
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                ByteArrayOutputStream err = new ByteArrayOutputStream();
                cmd.getInputStream().transferTo(out);
                cmd.getErrorStream().transferTo(err);
                cmd.join(SSH_CMD_TIMEOUT_MS, TimeUnit.MILLISECONDS);

                int exitCode = cmd.getExitStatus() != null ? cmd.getExitStatus() : -1;
                String stdout = out.toString(StandardCharsets.UTF_8).trim();
                String stderr = err.toString(StandardCharsets.UTF_8).trim();

                boolean success = (exitCode == 0);
                String output = StringUtils.hasText(stdout) ? stdout : stderr;
                if (!success && StringUtils.hasText(stderr)) {
                    output = stderr;
                }
                return success ? TestResult.ok(output) : TestResult.fail(output);
            }
        } catch (IOException e) {
            return TestResult.fail(e.getMessage());
        }
    }

    public TestResult testSql(String host, String dbType, Integer dbPort, String dbName, String dbUsername, String dbPassword, String sql) {
        if (!StringUtils.hasText(sql)) {
            return TestResult.fail("SQL을 입력하세요.");
        }
        if (!StringUtils.hasText(host) || !StringUtils.hasText(dbType) || dbPort == null || !StringUtils.hasText(dbName)) {
            return TestResult.fail("Host, DB종류, Port, DB명을 입력하세요.");
        }
        if (!StringUtils.hasText(dbUsername)) {
            return TestResult.fail("DB Username을 입력하세요.");
        }

        String url = buildDbUrl(host, dbType, dbPort, dbName);
        if (url == null) {
            return TestResult.fail("DB URL 구성 실패");
        }

        try {
            DriverManagerDataSource ds = new DriverManagerDataSource();
            ds.setUrl(url);
            ds.setUsername(dbUsername);
            ds.setPassword(dbPassword != null ? dbPassword : "");

            JdbcTemplate jdbc = new JdbcTemplate(ds);
            String sqlTrimmed = sql.trim();
            String output;
            try {
                Object val = jdbc.queryForObject(sqlTrimmed, Object.class);
                output = (val == null) ? "null" : val.toString();
            } catch (org.springframework.dao.IncorrectResultSizeDataAccessException e) {
                var rows = jdbc.queryForList(sqlTrimmed);
                output = rows.isEmpty() ? "(empty)" : rows.toString();
            }
            return TestResult.ok(output);
        } catch (Exception e) {
            return TestResult.fail(e.getMessage());
        }
    }

    private String buildDbUrl(String host, String dbType, Integer dbPort, String dbName) {
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

    public record TestResult(boolean success, String output) {
        public static TestResult ok(String output) {
            return new TestResult(true, output);
        }
        public static TestResult fail(String output) {
            return new TestResult(false, output);
        }
    }
}
