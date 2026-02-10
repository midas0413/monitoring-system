package com.example.monitoring.web.controller;

import com.example.monitoring.web.service.CheckTestService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/checks/test")
public class CheckTestController {

    private final CheckTestService checkTestService;

    public CheckTestController(CheckTestService checkTestService) {
        this.checkTestService = checkTestService;
    }

    @PostMapping("/shell")
    public CheckTestService.TestResult testShell(
            @RequestParam String host,
            @RequestParam(required = false) Integer port,
            @RequestParam String sshUsername,
            @RequestParam(required = false) String sshPassword,
            @RequestParam(required = false) String sshPrivateKeyPath,
            @RequestParam String script) {
        return checkTestService.testShell(host, port, sshUsername, sshPassword, sshPrivateKeyPath, script);
    }

    @PostMapping("/sql")
    public CheckTestService.TestResult testSql(
            @RequestParam String host,
            @RequestParam String dbType,
            @RequestParam Integer dbPort,
            @RequestParam String dbName,
            @RequestParam String dbUsername,
            @RequestParam(required = false) String dbPassword,
            @RequestParam String sql) {
        return checkTestService.testSql(host, dbType, dbPort, dbName, dbUsername, dbPassword, sql);
    }

    @PostMapping("/ssh-connection")
    public CheckTestService.TestResult testSshConnection(
            @RequestParam String host,
            @RequestParam(required = false) Integer port,
            @RequestParam String sshUsername,
            @RequestParam(required = false) String sshPassword,
            @RequestParam(required = false) String sshPrivateKeyPath) {
        return checkTestService.testSshConnection(host, port, sshUsername, sshPassword, sshPrivateKeyPath);
    }

    @PostMapping("/vpn-connection")
    public CheckTestService.TestResult testVpnConnection(
            @RequestParam String host) {
        return checkTestService.testVpnConnection(host);
    }
}
