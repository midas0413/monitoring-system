package com.example.monitoring.worker.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * Worker API Controller
 * Web에서 Worker를 통해 체크/테스트를 실행하기 위한 REST API
 */
@RestController
@RequestMapping("/api/worker")
public class WorkerApiController {

    private static final Logger log = LoggerFactory.getLogger(WorkerApiController.class);

    @Autowired
    private WorkerCheckTestService workerCheckTestService;

    @PostMapping("/test/shell")
    public WorkerCheckTestService.TestResult testShell(
            @RequestParam String host,
            @RequestParam(required = false) Integer port,
            @RequestParam String sshUsername,
            @RequestParam(required = false) String sshPassword,
            @RequestParam(required = false) String sshPrivateKeyPath,
            @RequestParam String script) {
        log.info("Shell script 테스트 요청 수신: host={}", host);
        WorkerCheckTestService.TestResult result = workerCheckTestService.testShell(host, port, sshUsername, sshPassword, sshPrivateKeyPath, script);
        log.info("Shell script 테스트 완료: host={}, success={}", host, result.success());
        return result;
    }

    @PostMapping("/test/sql")
    public WorkerCheckTestService.TestResult testSql(
            @RequestParam String host,
            @RequestParam String dbType,
            @RequestParam Integer dbPort,
            @RequestParam String dbName,
            @RequestParam String dbUsername,
            @RequestParam(required = false) String dbPassword,
            @RequestParam String sql) {
        log.info("SQL 테스트 요청 수신: host={}, dbType={}, dbName={}", host, dbType, dbName);
        WorkerCheckTestService.TestResult result = workerCheckTestService.testSql(host, dbType, dbPort, dbName, dbUsername, dbPassword, sql);
        log.info("SQL 테스트 완료: host={}, success={}", host, result.success());
        return result;
    }

    @PostMapping("/test/ssh-connection")
    public WorkerCheckTestService.TestResult testSshConnection(
            @RequestParam String host,
            @RequestParam(required = false) Integer port,
            @RequestParam String sshUsername,
            @RequestParam(required = false) String sshPassword,
            @RequestParam(required = false) String sshPrivateKeyPath) {
        log.info("SSH 연결 테스트 요청 수신: host={}", host);
        WorkerCheckTestService.TestResult result = workerCheckTestService.testSshConnection(host, port, sshUsername, sshPassword, sshPrivateKeyPath);
        log.info("SSH 연결 테스트 완료: host={}, success={}", host, result.success());
        return result;
    }

    @PostMapping("/test/logs")
    public WorkerCheckTestService.TestResult testLogs(
            @RequestParam String host,
            @RequestParam(required = false) Integer port,
            @RequestParam String sshUsername,
            @RequestParam(required = false) String sshPassword,
            @RequestParam(required = false) String sshPrivateKeyPath,
            @RequestParam String logFilePath,
            @RequestParam(required = false) String includeKeywords,
            @RequestParam(required = false) String excludeKeywords) {
        log.info("로그 파일 테스트 요청 수신: host={}, logFilePath={}", host, logFilePath);
        WorkerCheckTestService.TestResult result = workerCheckTestService.testLogs(host, port, sshUsername, sshPassword, sshPrivateKeyPath,
                logFilePath, includeKeywords, excludeKeywords);
        log.info("로그 파일 테스트 완료: host={}, success={}", host, result.success());
        return result;
    }

    @PostMapping("/test/vpn-connection")
    public WorkerCheckTestService.TestResult testVpnConnection(
            @RequestParam String host) {
        log.info("VPN 연결 테스트 요청 수신: host={}", host);
        WorkerCheckTestService.TestResult result = workerCheckTestService.testVpnConnection(host);
        log.info("VPN 연결 테스트 완료: host={}, success={}", host, result.success());
        return result;
    }

    @PostMapping("/test/disk-space")
    public WorkerCheckTestService.TestResult testDiskSpace(
            @RequestParam String host,
            @RequestParam(required = false) Integer port,
            @RequestParam String sshUsername,
            @RequestParam(required = false) String sshPassword,
            @RequestParam(required = false) String sshPrivateKeyPath,
            @RequestParam(required = false) String diskPath) {
        log.info("디스크 공간 테스트 요청 수신: host={}, diskPath={}", host, diskPath);
        WorkerCheckTestService.TestResult result = workerCheckTestService.testDiskSpace(host, port, sshUsername, sshPassword,
                sshPrivateKeyPath, diskPath);
        log.info("디스크 공간 테스트 완료: host={}, success={}", host, result.success());
        return result;
    }

    @GetMapping("/server-status")
    public ServerStatusResponse getServerStatus(@RequestParam Long serverId) {
        return workerCheckTestService.getServerStatus(serverId);
    }

    public record ServerStatusResponse(String status, String message) {
        public static ServerStatusResponse ok(String status) {
            return new ServerStatusResponse(status, null);
        }
        public static ServerStatusResponse error(String message) {
            return new ServerStatusResponse("UNKNOWN", message);
        }
    }
}
