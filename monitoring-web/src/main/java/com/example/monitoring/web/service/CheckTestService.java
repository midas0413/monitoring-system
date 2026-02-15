package com.example.monitoring.web.service;

import com.example.monitoring.web.config.WorkerApiProperties;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * Check 등록 시 SHELL/SQL을 대상에 테스트. 연결 정보를 파라미터로 받음.
 * Worker API가 활성화되어 있으면 Worker를 통해 실행, 그렇지 않으면 Web에서 직접 실행.
 */
@Service
public class CheckTestService {

    private static final Logger log = LoggerFactory.getLogger(CheckTestService.class);

    private static final int SSH_CONNECT_TIMEOUT_MS = 10_000;
    private static final int SSH_CMD_TIMEOUT_MS = 30_000;

    @Autowired(required = false)
    private WorkerApiProperties workerApiProperties;

    private final RestTemplate restTemplate;
    
    public CheckTestService() {
        this.restTemplate = new RestTemplate();
    }
    
    private void configureRestTemplate() {
        // Worker API 설정에 따라 타임아웃 동적 설정
        int timeoutMs = (workerApiProperties != null && workerApiProperties.getTimeoutMs() > 0) 
            ? workerApiProperties.getTimeoutMs() 
            : 30000;
        
        org.springframework.http.client.SimpleClientHttpRequestFactory factory = 
            new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000); // 연결 타임아웃은 5초 고정
        factory.setReadTimeout(timeoutMs); // 읽기 타임아웃은 설정값 사용
        this.restTemplate.setRequestFactory(factory);
    }

    /**
     * Worker API를 통해 테스트 실행
     * @return 성공 시 TestResult, 실패 시 TestResult.fail() (오류 메시지 포함)
     */
    private TestResult callWorkerApi(String endpoint, MultiValueMap<String, String> params) {
        if (workerApiProperties == null || !workerApiProperties.isEnabled()) {
            log.debug("Worker API가 비활성화되어 있습니다. 직접 실행합니다.");
            return null; // Worker API 비활성화 시 null 반환하여 직접 실행
        }

        try {
            // RestTemplate 타임아웃 설정 업데이트
            configureRestTemplate();
            
            String url = workerApiProperties.getBaseUrl() + "/api/worker" + endpoint;
            log.info("Worker API 호출 시작: endpoint={}, url={}, params={}", endpoint, url, params);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            
            // API 키가 설정되어 있으면 헤더에 추가
            if (workerApiProperties.getApiKey() != null && !workerApiProperties.getApiKey().isEmpty()) {
                headers.set("X-Worker-API-Key", workerApiProperties.getApiKey());
                log.debug("Worker API 키를 헤더에 추가했습니다.");
            }
            
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
            
            ResponseEntity<TestResult> response = restTemplate.postForEntity(url, request, TestResult.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("Worker API 호출 성공: endpoint={}, success={}", endpoint, response.getBody().success());
                return response.getBody();
            }
            // HTTP 응답은 성공했지만 body가 없는 경우
            log.warn("Worker API 응답이 비어있습니다: endpoint={}, status={}", endpoint, response.getStatusCode());
            return TestResult.fail("Worker API 응답이 비어있습니다.");
        } catch (org.springframework.web.client.ResourceAccessException e) {
            // 연결 실패 (Connection refused 등)
            String errorMsg = "Worker API 연결 실패: " + e.getMessage();
            if (e.getCause() != null) {
                errorMsg += " (" + e.getCause().getMessage() + ")";
            }
            log.error("Worker API 연결 실패: endpoint={}, error={}", endpoint, errorMsg, e);
            return TestResult.fail(errorMsg + "\n\nWorker 서버가 실행 중인지 확인하세요. (URL: " + workerApiProperties.getBaseUrl() + ")\nRead timed out인 경우 worker.api.timeout-ms(또는 WORKER_API_TIMEOUT_MS) 값을 늘려 보세요.");
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            // HTTP 4xx 오류
            log.error("Worker API HTTP 4xx 오류: endpoint={}, status={}, error={}", endpoint, e.getStatusCode(), e.getMessage());
            return TestResult.fail("Worker API 오류 (" + e.getStatusCode() + "): " + e.getMessage());
        } catch (org.springframework.web.client.HttpServerErrorException e) {
            // HTTP 5xx 오류
            log.error("Worker API HTTP 5xx 오류: endpoint={}, status={}, error={}", endpoint, e.getStatusCode(), e.getMessage());
            return TestResult.fail("Worker 서버 오류 (" + e.getStatusCode() + "): " + e.getMessage());
        } catch (Exception e) {
            // 기타 오류
            log.error("Worker API 호출 중 예상치 못한 오류: endpoint={}, error={}", endpoint, e.getMessage(), e);
            return TestResult.fail("Worker API 호출 실패: " + e.getMessage());
        }
    }

    /**
     * VPN 연결 테스트 (다중 방법 시도)
     * 1. Ping 테스트 (ICMP)
     * 2. TCP 연결 테스트 (여러 포트 시도)
     * 3. HTTP 요청 테스트 (선택적)
     */
    public TestResult testVpnConnection(String host) {
        log.info("VPN 연결 테스트 시작: host={}, workerApiEnabled={}", host, 
                workerApiProperties != null && workerApiProperties.isEnabled());
        
        // Worker API 호출 시도
        if (workerApiProperties != null && workerApiProperties.isEnabled()) {
            log.info("Worker API를 통해 VPN 연결 테스트 실행: host={}", host);
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("host", host);
            TestResult result = callWorkerApi("/test/vpn-connection", params);
            // callWorkerApi는 실패 시 TestResult.fail()을 반환하므로 바로 반환
            log.info("VPN 연결 테스트 완료 (Worker API): host={}, success={}", host, result.success());
            return result;
        }
        
        // Worker API 비활성화 시 직접 실행
        log.warn("Worker API가 비활성화되어 있습니다. Web에서 직접 VPN 연결 테스트를 실행합니다: host={}", host);
        if (!StringUtils.hasText(host)) {
            return TestResult.fail("VPN Host를 입력하세요.");
        }

        try {
            // 호스트에서 포트 추출 (host:port 형식 지원)
            String[] parts = host.split(":");
            String hostname = parts[0];
            int specifiedPort = parts.length > 1 ? Integer.parseInt(parts[1]) : -1;

            StringBuilder resultMsg = new StringBuilder();
            boolean success = false;

            // 1. Ping 테스트 시도 (ICMP)
            try {
                java.net.InetAddress addr = java.net.InetAddress.getByName(hostname);
                boolean reachable = addr.isReachable(3000); // 3초 타임아웃
                if (reachable) {
                    resultMsg.append("✓ Ping 성공 (").append(hostname).append(")\n");
                    success = true;
                } else {
                    resultMsg.append("✗ Ping 실패 (").append(hostname).append(")\n");
                }
            } catch (Exception e) {
                resultMsg.append("⚠ Ping 테스트 불가 (권한 또는 네트워크 문제: ").append(e.getMessage()).append(")\n");
            }

            // 2. TCP 연결 테스트
            int[] portsToTest;
            if (specifiedPort > 0) {
                portsToTest = new int[]{specifiedPort};
            } else {
                // 기본 포트들 시도: 80, 443, 22, 8080
                portsToTest = new int[]{80, 443, 22, 8080};
            }

            boolean tcpSuccess = false;
            for (int port : portsToTest) {
                try (java.net.Socket socket = new java.net.Socket()) {
                    socket.connect(new java.net.InetSocketAddress(hostname, port), 3000);
                    resultMsg.append("✓ TCP 연결 성공 (").append(hostname).append(":").append(port).append(")\n");
                    tcpSuccess = true;
                    success = true;
                    break; // 하나라도 성공하면 중단
                } catch (Exception e) {
                    // 개별 포트 실패는 무시하고 다음 포트 시도
                }
            }

            if (!tcpSuccess) {
                resultMsg.append("✗ TCP 연결 실패 (모든 포트 시도 실패: ");
                for (int i = 0; i < portsToTest.length; i++) {
                    if (i > 0) resultMsg.append(", ");
                    resultMsg.append(portsToTest[i]);
                }
                resultMsg.append(")\n");
            }

            // 3. HTTP 요청 테스트 (포트 80 또는 443이 성공한 경우)
            if (tcpSuccess && (specifiedPort == 80 || specifiedPort == 443 || specifiedPort == -1)) {
                try {
                    String protocol = (specifiedPort == 443 || (specifiedPort == -1 && tcpSuccess)) ? "https" : "http";
                    int httpPort = specifiedPort > 0 ? specifiedPort : (protocol.equals("https") ? 443 : 80);
                    java.net.URL url = new java.net.URL(protocol + "://" + hostname + ":" + httpPort);
                    java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(3000);
                    conn.setReadTimeout(3000);
                    conn.setRequestMethod("HEAD");
                    int responseCode = conn.getResponseCode();
                    if (responseCode >= 200 && responseCode < 400) {
                        resultMsg.append("✓ HTTP 응답 성공 (").append(responseCode).append(")\n");
                    } else {
                        resultMsg.append("⚠ HTTP 응답: ").append(responseCode).append("\n");
                    }
                    conn.disconnect();
                } catch (Exception e) {
                    // HTTP 테스트 실패는 무시 (TCP 연결만으로도 충분)
                    resultMsg.append("⚠ HTTP 테스트 불가: ").append(e.getMessage()).append("\n");
                }
            }

            if (success) {
                return TestResult.ok(resultMsg.toString().trim());
            } else {
                return TestResult.fail(resultMsg.toString().trim());
            }

        } catch (NumberFormatException e) {
            return TestResult.fail("잘못된 포트 형식입니다: " + host);
        } catch (Exception e) {
            // UnknownHostException 등은 내부 try-catch에서 이미 처리됨
            return TestResult.fail("VPN 연결 테스트 실패: " + e.getMessage());
        }
    }

    /**
     * SSH 연결 테스트 (스크립트 실행 없이 연결만 확인)
     */
    public TestResult testSshConnection(String host, Integer port, String sshUsername, String sshPassword, String sshPrivateKeyPath) {
        log.info("SSH 연결 테스트 시작: host={}, workerApiEnabled={}", host,
                workerApiProperties != null && workerApiProperties.isEnabled());
        
        // Worker API 호출 시도
        if (workerApiProperties != null && workerApiProperties.isEnabled()) {
            log.info("Worker API를 통해 SSH 연결 테스트 실행: host={}", host);
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("host", host);
            if (port != null) params.add("port", String.valueOf(port));
            params.add("sshUsername", sshUsername);
            if (sshPassword != null) params.add("sshPassword", sshPassword);
            if (sshPrivateKeyPath != null) params.add("sshPrivateKeyPath", sshPrivateKeyPath);
            TestResult result = callWorkerApi("/test/ssh-connection", params);
            log.info("SSH 연결 테스트 완료 (Worker API): host={}, success={}", host, result.success());
            // callWorkerApi는 실패 시 TestResult.fail()을 반환하므로 바로 반환
            return result;
        }
        
        // Worker API 비활성화 시 직접 실행
        log.warn("Worker API가 비활성화되어 있습니다. Web에서 직접 SSH 연결 테스트를 실행합니다: host={}", host);
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

            // 연결 성공 확인을 위해 간단한 명령 실행 (echo)
            try (var session = ssh.startSession()) {
                var cmd = session.exec("echo 'SSH connection test successful'");
                cmd.join(5_000, TimeUnit.MILLISECONDS);
                int exitCode = cmd.getExitStatus() != null ? cmd.getExitStatus() : -1;
                if (exitCode == 0) {
                    return TestResult.ok("SSH 연결이 성공적으로 확인되었습니다.");
                } else {
                    return TestResult.fail("SSH 연결은 되었지만 명령 실행에 실패했습니다. (exit code: " + exitCode + ")");
                }
            }
        } catch (IOException e) {
            return TestResult.fail("SSH 연결 실패: " + e.getMessage());
        }
    }

    public TestResult testShell(String host, Integer port, String sshUsername, String sshPassword, String sshPrivateKeyPath, String script) {
        log.info("Shell script 테스트 시작: host={}, workerApiEnabled={}", host, 
                workerApiProperties != null && workerApiProperties.isEnabled());
        
        // Worker API 호출 시도
        if (workerApiProperties != null && workerApiProperties.isEnabled()) {
            log.info("Worker API를 통해 Shell script 테스트 실행: host={}", host);
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("host", host);
            if (port != null) params.add("port", String.valueOf(port));
            params.add("sshUsername", sshUsername);
            if (sshPassword != null) params.add("sshPassword", sshPassword);
            if (sshPrivateKeyPath != null) params.add("sshPrivateKeyPath", sshPrivateKeyPath);
            params.add("script", script);
            TestResult result = callWorkerApi("/test/shell", params);
            log.info("Shell script 테스트 완료 (Worker API): host={}, success={}", host, result.success());
            // callWorkerApi는 실패 시 TestResult.fail()을 반환하므로 바로 반환
            return result;
        }
        
        // Worker API 비활성화 시 직접 실행
        log.warn("Worker API가 비활성화되어 있습니다. Web에서 직접 Shell script 테스트를 실행합니다: host={}", host);
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

    /**
     * 로그 파일 모니터링 테스트
     */
    public TestResult testLogs(String host, Integer port, String sshUsername, String sshPassword, 
                                String sshPrivateKeyPath, String logFilePath, String includeKeywords, String excludeKeywords) {
        log.info("로그 파일 테스트 시작: host={}, logFilePath={}, workerApiEnabled={}", host, logFilePath,
                workerApiProperties != null && workerApiProperties.isEnabled());
        
        // Worker API 호출 시도
        if (workerApiProperties != null && workerApiProperties.isEnabled()) {
            log.info("Worker API를 통해 로그 파일 테스트 실행: host={}, logFilePath={}", host, logFilePath);
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("host", host);
            if (port != null) params.add("port", String.valueOf(port));
            params.add("sshUsername", sshUsername);
            if (sshPassword != null) params.add("sshPassword", sshPassword);
            if (sshPrivateKeyPath != null) params.add("sshPrivateKeyPath", sshPrivateKeyPath);
            params.add("logFilePath", logFilePath);
            if (includeKeywords != null) params.add("includeKeywords", includeKeywords);
            if (excludeKeywords != null) params.add("excludeKeywords", excludeKeywords);
            TestResult result = callWorkerApi("/test/logs", params);
            log.info("로그 파일 테스트 완료 (Worker API): host={}, success={}", host, result.success());
            // callWorkerApi는 실패 시 TestResult.fail()을 반환하므로 바로 반환
            return result;
        }
        
        // Worker API 비활성화 시 직접 실행
        log.warn("Worker API가 비활성화되어 있습니다. Web에서 직접 로그 파일 테스트를 실행합니다: host={}", host);
        if (!StringUtils.hasText(logFilePath)) {
            return TestResult.fail("로그 파일 경로를 입력하세요.");
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

            // 여러 로그 파일 경로 처리
            String[] logPaths = logFilePath.split(",");
            StringBuilder combinedOutput = new StringBuilder();
            boolean hasError = false;
            String lastError = null;

            for (String path : logPaths) {
                String trimmedPath = path.trim();
                if (trimmedPath.isEmpty()) {
                    continue;
                }

                try {
                    // 로그 파일 검색 명령어 구성
                    String command = buildLogSearchCommand(trimmedPath, includeKeywords, excludeKeywords);
                    
                    try (var session = ssh.startSession()) {
                        var cmd = session.exec(command);
                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        ByteArrayOutputStream err = new ByteArrayOutputStream();
                        cmd.getInputStream().transferTo(out);
                        cmd.getErrorStream().transferTo(err);
                        cmd.join(SSH_CMD_TIMEOUT_MS, TimeUnit.MILLISECONDS);

                        int exitCode = cmd.getExitStatus() != null ? cmd.getExitStatus() : -1;
                        String stdout = out.toString(StandardCharsets.UTF_8).trim();
                        String stderr = err.toString(StandardCharsets.UTF_8).trim();

                        // stderr에 실제 에러 메시지가 있는지 확인
                        boolean hasRealError = StringUtils.hasText(stderr) && 
                                (stderr.contains("No such file") || 
                                 stderr.contains("Permission denied") ||
                                 stderr.contains("cannot open") ||
                                 stderr.contains("cannot read") ||
                                 stderr.contains("No such file or directory") ||
                                 stderr.contains("Access denied"));

                        if (hasRealError) {
                            // 실제 파일 읽기 오류
                            hasError = true;
                            lastError = "Failed to read " + trimmedPath + ": " + stderr;
                        } else if (exitCode == 0 || StringUtils.hasText(stdout)) {
                            // 성공 또는 grep이 매칭을 찾은 경우
                            if (StringUtils.hasText(stdout)) {
                                if (combinedOutput.length() > 0) {
                                    combinedOutput.append("\n---\n");
                                }
                                combinedOutput.append("[").append(trimmedPath).append("]\n");
                                combinedOutput.append(stdout);
                            } else if (exitCode == 0) {
                                // exitCode가 0이고 stdout이 비어있으면 매칭되는 라인 없음
                                if (combinedOutput.length() > 0) {
                                    combinedOutput.append("\n---\n");
                                }
                                combinedOutput.append("[").append(trimmedPath).append("]\n");
                                combinedOutput.append("(No matching lines found)");
                            }
                        } else if (exitCode == 1 && !StringUtils.hasText(stderr)) {
                            // grep이 매칭을 못 찾아서 exit code 1이지만 stderr가 없으면 정상 (매칭 없음)
                            if (combinedOutput.length() > 0) {
                                combinedOutput.append("\n---\n");
                            }
                            combinedOutput.append("[").append(trimmedPath).append("]\n");
                            combinedOutput.append("(No matching lines found)");
                        } else {
                            // 기타 오류
                            hasError = true;
                            lastError = "Failed to read " + trimmedPath + ": " + 
                                       (StringUtils.hasText(stderr) ? stderr : "exitCode=" + exitCode);
                        }
                    }
                } catch (Exception e) {
                    hasError = true;
                    lastError = "Error processing " + trimmedPath + ": " + e.getMessage();
                }
            }

            String finalOutput = combinedOutput.toString();
            if (finalOutput.isEmpty()) {
                if (hasError) {
                    return TestResult.fail(lastError != null ? lastError : "로그 파일을 읽을 수 없습니다.");
                } else {
                    return TestResult.ok("매칭되는 로그 항목이 없습니다.");
                }
            }

            if (hasError && lastError != null) {
                finalOutput += "\n[WARNING] " + lastError;
            }

            return TestResult.ok(finalOutput);
        } catch (IOException e) {
            return TestResult.fail("SSH 연결 실패: " + e.getMessage());
        }
    }

    /**
     * 로그 파일 검색 명령어 구성 (MonitoringRuleExecutorService와 동일한 로직)
     */
    private String buildLogSearchCommand(String logPath, String includeKeywords, String excludeKeywords) {
        StringBuilder command = new StringBuilder();
        
        // includeKeywords가 있으면 grep으로 필터링, 없으면 tail로 최근 로그만 읽기
        if (StringUtils.hasText(includeKeywords)) {
            // 키워드를 OR로 연결 (예: ERROR|FAIL|CRITICAL)
            String[] keywords = includeKeywords.split(",");
            StringBuilder pattern = new StringBuilder();
            for (int i = 0; i < keywords.length; i++) {
                if (i > 0) pattern.append("|");
                // 정규식 특수 문자 이스케이프
                String keyword = keywords[i].trim().replaceAll("[.\\\\+*?\\[^\\]$(){}=!<>|:\\-]", "\\\\$0");
                pattern.append(keyword);
            }
            
            // grep으로 키워드 검색 (최근 1000줄만)
            command.append("tail -n 1000 ").append(logPath)
                   .append(" | grep -E '").append(pattern).append("'");
        } else {
            // 키워드가 없으면 최근 100줄만 읽기
            command.append("tail -n 100 ").append(logPath);
        }
        
        // excludeKeywords가 있으면 추가 필터링
        if (StringUtils.hasText(excludeKeywords)) {
            String[] excludeKeys = excludeKeywords.split(",");
            for (String excludeKey : excludeKeys) {
                String trimmed = excludeKey.trim();
                if (!trimmed.isEmpty()) {
                    // 정규식 특수 문자 이스케이프
                    String escaped = trimmed.replaceAll("[.\\\\+*?\\[^\\]$(){}=!<>|:\\-]", "\\\\$0");
                    command.append(" | grep -v -E '").append(escaped).append("'");
                }
            }
        }
        
        return command.toString();
    }

    public TestResult testSql(String host, String dbType, Integer dbPort, String dbName, String dbUsername, String dbPassword, String sql) {
        log.info("SQL 테스트 시작: host={}, dbType={}, dbName={}, workerApiEnabled={}", host, dbType, dbName,
                workerApiProperties != null && workerApiProperties.isEnabled());
        
        // Worker API 호출 시도
        if (workerApiProperties != null && workerApiProperties.isEnabled()) {
            log.info("Worker API를 통해 SQL 테스트 실행: host={}, dbType={}, dbName={}", host, dbType, dbName);
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("host", host);
            params.add("dbType", dbType);
            params.add("dbPort", String.valueOf(dbPort));
            params.add("dbName", dbName);
            params.add("dbUsername", dbUsername);
            if (dbPassword != null) params.add("dbPassword", dbPassword);
            params.add("sql", sql);
            TestResult result = callWorkerApi("/test/sql", params);
            log.info("SQL 테스트 완료 (Worker API): host={}, success={}", host, result.success());
            // callWorkerApi는 실패 시 TestResult.fail()을 반환하므로 바로 반환
            return result;
        }
        
        // Worker API 비활성화 시 직접 실행
        log.warn("Worker API가 비활성화되어 있습니다. Web에서 직접 SQL 테스트를 실행합니다: host={}", host);
        if (!StringUtils.hasText(sql)) {
            return TestResult.fail("SQL을 입력하세요.");
        }
        if (!StringUtils.hasText(host) || !StringUtils.hasText(dbType) || dbPort == null || !StringUtils.hasText(dbName)) {
            return TestResult.fail("Host, DB종류, Port, DB명을 입력하세요.");
        }
        if (!StringUtils.hasText(dbUsername)) {
            return TestResult.fail("DB Username을 입력하세요.");
        }

        String driverClassName = getDriverClassName(dbType);
        if (driverClassName == null) {
            return TestResult.fail("지원하지 않는 DB 타입입니다: " + dbType);
        }

        // Oracle의 경우 여러 연결 형식을 모두 시도
        String[] urlsToTry = null;
        if ("oracle".equalsIgnoreCase(dbType)) {
            // DB Name에서 / 제거 (Service Name 형식에서 사용)
            String cleanDbName = dbName.startsWith("/") ? dbName.substring(1) : dbName;
            
            // 여러 형식 시도: SID, Service Name, TNS (Service Name)
            urlsToTry = new String[]{
                "jdbc:oracle:thin:@" + host + ":" + dbPort + ":" + cleanDbName,  // SID 형식
                "jdbc:oracle:thin:@//" + host + ":" + dbPort + "/" + cleanDbName,  // Service Name 형식 (슬래시 포함)
                "jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS=(PROTOCOL=TCP)(HOST=" + host + ")(PORT=" + dbPort + "))(CONNECT_DATA=(SERVICE_NAME=" + cleanDbName + ")))"  // TNS 형식 (Service Name)
            };
        } else {
            String url = buildDbUrl(host, dbType, dbPort, dbName);
            if (url == null) {
                return TestResult.fail("DB URL 구성 실패: Host, DB Type, Port, DB Name을 확인하세요.");
            }
            urlsToTry = new String[]{url};
        }

        Exception lastException = null;
        for (String url : urlsToTry) {
            try {
                DriverManagerDataSource ds = new DriverManagerDataSource();
                ds.setDriverClassName(driverClassName);
                ds.setUrl(url);
                ds.setUsername(dbUsername);
                ds.setPassword(dbPassword != null ? dbPassword : "");

                JdbcTemplate jdbc = new JdbcTemplate(ds);
                jdbc.setQueryTimeout(10); // 10초 쿼리 타임아웃 설정
                
                String sqlTrimmed = sql.trim();
                String output;
                try {
                    // 1행 1열 결과 시도
                    Object val = jdbc.queryForObject(sqlTrimmed, Object.class);
                    output = (val == null) ? "null" : val.toString();
                } catch (org.springframework.dao.IncorrectResultSizeDataAccessException e) {
                    // 여러 행 반환 시
                    var rows = jdbc.queryForList(sqlTrimmed);
                    if (rows.isEmpty()) {
                        output = "(empty result)";
                    } else if (rows.size() == 1) {
                        output = rows.get(0).toString();
                    } else {
                        output = "(" + rows.size() + " rows) " + rows.get(0).toString();
                    }
                }
                String urlInfo = urlsToTry.length > 1 ? "\n사용된 URL: " + url : "";
                return TestResult.ok("SQL 실행 성공\n결과: " + output + urlInfo);
            } catch (org.springframework.jdbc.CannotGetJdbcConnectionException e) {
                lastException = e;
                // Oracle의 경우 다음 URL 형식 시도
                if (urlsToTry.length > 1 && url != urlsToTry[urlsToTry.length - 1]) {
                    continue; // 다음 URL 시도
                }
                // 마지막 시도였거나 단일 URL인 경우
                String errorMsg = "DB 연결 실패: ";
                if (e.getCause() != null) {
                    errorMsg += e.getCause().getMessage();
                } else {
                    errorMsg += e.getMessage();
                }
                if (urlsToTry.length > 1 && "oracle".equalsIgnoreCase(dbType)) {
                    errorMsg += "\n\n시도한 연결 형식:\n";
                    for (int i = 0; i < urlsToTry.length; i++) {
                        String formatName = switch(i) {
                            case 0 -> "SID 형식";
                            case 1 -> "Service Name 형식";
                            case 2 -> "TNS 형식 (Service Name)";
                            default -> "형식 " + (i + 1);
                        };
                        errorMsg += "  " + (i + 1) + ". " + formatName + ": " + urlsToTry[i] + "\n";
                    }
                    errorMsg += "\n해결 방법:\n";
                    errorMsg += "1. Oracle 서버에서 실제 SID 또는 Service Name 확인:\n";
                    errorMsg += "   - SQL*Plus에서: SELECT instance_name FROM v$instance; (SID 확인)\n";
                    errorMsg += "   - 또는: SELECT name FROM v$services; (Service Name 확인)\n";
                    errorMsg += "2. DB Name 필드에 확인된 정확한 SID 또는 Service Name 입력\n";
                    errorMsg += "3. Service Name 사용 시: DB Name에 /service_name 형식으로 입력 (예: /IRMDB)\n";
                    errorMsg += "4. 또는 Oracle DBA에게 정확한 연결 정보 확인";
                }
                return TestResult.fail(errorMsg + "\n연결 정보를 확인하세요. (Host: " + host + ", Port: " + dbPort + ", DB: " + dbName + ")");
            } catch (org.springframework.dao.DataAccessException e) {
                // 연결은 성공했지만 SQL 실행 오류
                String errorMsg = "SQL 실행 오류: ";
                if (e.getCause() != null) {
                    errorMsg += e.getCause().getMessage();
                } else {
                    errorMsg += e.getMessage();
                }
                return TestResult.fail(errorMsg);
            } catch (Exception e) {
                lastException = e;
                if (urlsToTry.length > 1 && url != urlsToTry[urlsToTry.length - 1]) {
                    continue; // 다음 URL 시도
                }
                return TestResult.fail("예상치 못한 오류: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            }
        }

        // 모든 시도 실패
        if (lastException != null) {
            String finalErrorMsg = "모든 연결 형식 시도 실패";
            if (lastException instanceof org.springframework.jdbc.CannotGetJdbcConnectionException) {
                org.springframework.jdbc.CannotGetJdbcConnectionException connEx = 
                    (org.springframework.jdbc.CannotGetJdbcConnectionException) lastException;
                if (connEx.getCause() != null) {
                    finalErrorMsg += ": " + connEx.getCause().getMessage();
                } else {
                    finalErrorMsg += ": " + connEx.getMessage();
                }
            } else {
                finalErrorMsg += ": " + lastException.getMessage();
            }
            
            if ("oracle".equalsIgnoreCase(dbType)) {
                finalErrorMsg += "\n\n시도한 연결 형식:\n";
                for (int i = 0; i < urlsToTry.length; i++) {
                    String formatName = switch(i) {
                        case 0 -> "SID 형식";
                        case 1 -> "Service Name 형식";
                        case 2 -> "TNS 형식 (Service Name)";
                        default -> "형식 " + (i + 1);
                    };
                    finalErrorMsg += "  " + (i + 1) + ". " + formatName + ": " + urlsToTry[i] + "\n";
                }
                finalErrorMsg += "\n해결 방법:\n";
                finalErrorMsg += "1. Oracle 서버에서 실제 SID 또는 Service Name 확인:\n";
                finalErrorMsg += "   - SQL*Plus에서: SELECT instance_name FROM v$instance; (SID 확인)\n";
                finalErrorMsg += "   - 또는: SELECT name FROM v$services; (Service Name 확인)\n";
                finalErrorMsg += "2. DB Name 필드에 확인된 정확한 SID 또는 Service Name 입력\n";
                finalErrorMsg += "3. Service Name 사용 시: DB Name에 /service_name 형식으로 입력 (예: /IRMDB)\n";
                finalErrorMsg += "4. 또는 Oracle DBA에게 정확한 연결 정보 확인";
            }
            return TestResult.fail(finalErrorMsg);
        }
        return TestResult.fail("DB 연결 실패: 알 수 없는 오류");
    }

    private String buildDbUrl(String host, String dbType, Integer dbPort, String dbName) {
        if (!StringUtils.hasText(host) || !StringUtils.hasText(dbType) || dbPort == null || !StringUtils.hasText(dbName)) {
            return null;
        }
        String dbTypeLower = dbType.toLowerCase();
        String subprotocol = switch (dbTypeLower) {
            case "postgresql" -> "postgresql";
            case "mysql" -> "mysql";
            case "oracle" -> "oracle:thin";
            case "mssql", "sqlserver" -> "sqlserver";
            default -> dbTypeLower;
        };
        if ("oracle:thin".equals(subprotocol)) {
            // Oracle: DB Name이 /로 시작하면 Service Name 형식, 아니면 SID 형식
            String cleanDbName = dbName.startsWith("/") ? dbName.substring(1) : dbName;
            if (dbName.startsWith("/")) {
                // Service Name 형식: jdbc:oracle:thin:@//host:port/service_name
                return "jdbc:oracle:thin:@//" + host + ":" + dbPort + "/" + cleanDbName;
            } else {
                // SID 형식: jdbc:oracle:thin:@host:port:SID
                return "jdbc:oracle:thin:@" + host + ":" + dbPort + ":" + cleanDbName;
            }
        }
        if ("sqlserver".equals(subprotocol)) {
            // MSSQL: jdbc:sqlserver://host:port;databaseName=dbname
            return "jdbc:sqlserver://" + host + ":" + dbPort + ";databaseName=" + dbName;
        }
        return "jdbc:" + subprotocol + "://" + host + ":" + dbPort + "/" + dbName;
    }

    private String getDriverClassName(String dbType) {
        if (dbType == null) return null;
        return switch (dbType.toLowerCase()) {
            case "postgresql" -> "org.postgresql.Driver";
            case "mysql" -> "com.mysql.cj.jdbc.Driver";
            case "oracle" -> "oracle.jdbc.OracleDriver";
            case "mssql", "sqlserver" -> "com.microsoft.sqlserver.jdbc.SQLServerDriver";
            default -> null;
        };
    }

    /**
     * 디스크 공간 모니터링 테스트
     */
    public TestResult testDiskSpace(String host, Integer port, String sshUsername, String sshPassword,
                                    String sshPrivateKeyPath, String diskPath) {
        log.info("디스크 공간 테스트 시작: host={}, diskPath={}, workerApiEnabled={}", host, diskPath,
                workerApiProperties != null && workerApiProperties.isEnabled());
        
        // Worker API 호출 시도
        if (workerApiProperties != null && workerApiProperties.isEnabled()) {
            log.info("Worker API를 통해 디스크 공간 테스트 실행: host={}, diskPath={}", host, diskPath);
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("host", host);
            if (port != null) params.add("port", String.valueOf(port));
            params.add("sshUsername", sshUsername);
            if (sshPassword != null) params.add("sshPassword", sshPassword);
            if (sshPrivateKeyPath != null) params.add("sshPrivateKeyPath", sshPrivateKeyPath);
            if (diskPath != null) params.add("diskPath", diskPath);
            TestResult result = callWorkerApi("/test/disk-space", params);
            log.info("디스크 공간 테스트 완료 (Worker API): host={}, success={}", host, result.success());
            // callWorkerApi는 실패 시 TestResult.fail()을 반환하므로 바로 반환
            return result;
        }
        
        // Worker API 비활성화 시 직접 실행
        log.warn("Worker API가 비활성화되어 있습니다. Web에서 직접 디스크 공간 테스트를 실행합니다: host={}", host);
        // diskPath는 선택 입력 (비어있으면 전체 마운트 포인트 확인)
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

            // df -h 명령어로 디스크 사용률 확인
            // diskPath가 지정되어 있으면 해당 경로만, 없으면 전체 확인
            String command;
            if (StringUtils.hasText(diskPath)) {
                // 특정 경로의 디스크 사용률 확인
                command = "df -h " + diskPath.trim();
            } else {
                // 전체 디스크 사용률 확인
                command = "df -h";
            }

            try (var session = ssh.startSession()) {
                var cmd = session.exec(command);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                ByteArrayOutputStream err = new ByteArrayOutputStream();
                cmd.getInputStream().transferTo(out);
                cmd.getErrorStream().transferTo(err);
                cmd.join(SSH_CMD_TIMEOUT_MS, TimeUnit.MILLISECONDS);

                int exitCode = cmd.getExitStatus() != null ? cmd.getExitStatus() : -1;
                String stdout = out.toString(StandardCharsets.UTF_8).trim();
                String stderr = err.toString(StandardCharsets.UTF_8).trim();

                if (exitCode == 0) {
                    if (StringUtils.hasText(stdout)) {
                        // df 출력을 파싱하여 사용률 정보 추출
                        String[] lines = stdout.split("\n");
                        StringBuilder result = new StringBuilder();
                        result.append("디스크 사용률 정보:\n\n");
                        
                        // 헤더 라인 출력
                        if (lines.length > 0) {
                            result.append(lines[0]).append("\n");
                            int separatorLength = Math.min(80, lines[0].length());
                            for (int j = 0; j < separatorLength; j++) {
                                result.append("-");
                            }
                            result.append("\n");
                        }
                        
                        // 데이터 라인 출력 (헤더 제외)
                        for (int i = 1; i < lines.length; i++) {
                            if (StringUtils.hasText(lines[i])) {
                                result.append(lines[i]).append("\n");
                            }
                        }
                        
                        // 사용률 추출 및 요약 정보 추가
                        result.append("\n--- 요약 ---\n");
                        for (int i = 1; i < lines.length; i++) {
                            if (StringUtils.hasText(lines[i])) {
                                String[] parts = lines[i].split("\\s+");
                                if (parts.length >= 5) {
                                    // Filesystem Size Used Avail Use% Mounted on
                                    String filesystem = parts[0];
                                    String size = parts[1];
                                    String used = parts[2];
                                    String avail = parts[3];
                                    String usePercent = parts[4];
                                    String mountedOn = parts.length > 5 ? parts[5] : "";
                                    
                                    result.append(String.format("파일시스템: %s, 사용률: %s, 사용: %s/%s, 마운트: %s\n",
                                            filesystem, usePercent, used, size, mountedOn));
                                }
                            }
                        }
                        
                        return TestResult.ok(result.toString());
                    } else {
                        return TestResult.ok("디스크 정보를 가져올 수 없습니다. (출력 없음)");
                    }
                } else {
                    String errorMsg = StringUtils.hasText(stderr) ? stderr : "exitCode=" + exitCode;
                    return TestResult.fail("디스크 정보 조회 실패: " + errorMsg);
                }
            }
        } catch (IOException e) {
            return TestResult.fail("SSH 연결 실패: " + e.getMessage());
        }
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
