package com.example.monitoring.worker.alert;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.Charset;

/**
 * Aligo API 클라이언트
 * - 알림톡: https://kakaoapi.aligo.in
 * - 문자: https://apis.aligo.in
 */
@Component
public class AligoClient {

    private static final Logger log = LoggerFactory.getLogger(AligoClient.class);

    private static final String ALIMTALK_URL = "https://kakaoapi.aligo.in/akv10/alimtalk/send/";
    private static final String SMS_URL = "https://apis.aligo.in/send/";

    private final AligoProperties props;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public AligoClient(AligoProperties props) {
        this.props = props;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 알림톡 발송 (템플릿 기반)
     * 템플릿에 #{변수} 형식이 있으면 message에 동일 변수명으로 치환된 값을 전달
     */
    public AligoResult sendAlimtalk(String receiver, String title, String message) {
        return sendAlimtalk(receiver, title, message, null, null);
    }

    /**
     * 알림톡 발송 (템플릿 변수 포함)
     * @param receiver 수신자 전화번호
     * @param title 제목
     * @param message 메시지 (템플릿 변수가 없을 때 사용)
     * @param var1 템플릿 변수1 (서버명 등)
     * @param var2 템플릿 변수2 (알림내용 등)
     */
    public AligoResult sendAlimtalk(String receiver, String title, String message, String var1, String var2) {
        return sendAlimtalk(receiver, title, message, var1, var2, null, null);
    }
    
    /**
     * 알림톡 발송 (템플릿 변수 및 템플릿 코드 포함)
     * @param receiver 수신자 전화번호
     * @param title 제목
     * @param message 메시지 (템플릿 변수가 없을 때 사용)
     * @param var1 템플릿 변수1 (서버명 등)
     * @param var2 템플릿 변수2 (알림내용 등)
     * @param templateCode 템플릿 코드 (null이면 AligoProperties의 기본 템플릿 코드 사용)
     * @param buttonInfo 버튼 정보 (JSON 형식, null이면 기본 버튼 사용)
     */
    public AligoResult sendAlimtalk(String receiver, String title, String message, String var1, String var2, String templateCode, String buttonInfo) {
        if (!props.isAlimtalkAvailable()) {
            return AligoResult.fail("Aligo 알림톡 설정 부족 (apiKey, userId, sender, senderKey, templateCode)");
        }

        // 템플릿 코드 결정: 파라미터로 받은 것이 있으면 사용, 없으면 기본값
        String tplCode = StringUtils.hasText(templateCode) ? templateCode : props.getTemplateCode();
        if (!StringUtils.hasText(tplCode)) {
            return AligoResult.fail("템플릿 코드가 설정되지 않았습니다. 알림 규칙에 카카오 템플릿 ID를 설정하거나 Aligo 기본 템플릿 코드를 설정하세요.");
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("apikey", props.getApiKey());
            params.add("userid", props.getUserId());
            params.add("senderkey", props.getSenderKey());
            params.add("tpl_code", tplCode);
            params.add("sender", props.getSender());
            params.add("receiver_1", normalizePhone(receiver));
            params.add("subject_1", truncate(title, 50));
            
            // message 파라미터가 이미 템플릿 메시지 형태로 치환된 경우 그대로 사용
            // 그렇지 않으면 기존 방식으로 조합
            String message1 = message;
            if (!StringUtils.hasText(message1)) {
                // message가 비어있으면 기존 방식으로 조합
                String systemInfo = StringUtils.hasText(var1) ? var1 : "시스템";
                String alertInfo = StringUtils.hasText(var2) ? var2 : "알림 내용 없음";
                
                if (StringUtils.hasText(alertInfo) && alertInfo.contains("\n")) {
                    message1 = String.format("%s 모니터링에 알림이 발생했습니다.\n알림내용 :\n%s", systemInfo, alertInfo);
                } else {
                    message1 = String.format("%s 모니터링에 알림이 발생했습니다.\n알림내용 : %s", systemInfo, alertInfo);
                }
            }
            params.add("message_1", truncate(message1, 1000));
            
            // 버튼 정보: 파라미터로 받은 것이 있으면 사용, 없으면 기본 버튼 사용
            String button1;
            if (StringUtils.hasText(buttonInfo)) {
                button1 = buttonInfo;
                log.info("템플릿에서 버튼 정보 사용: {}", button1);
            } else {
                // 기본 버튼 (채널 추가)
                button1 = "{\"button\":[{\"name\":\"채널추가\",\"linkType\":\"AC\",\"linkTypeName\":\"채널 추가\"}]}";
                log.info("기본 버튼 정보 사용: {}", button1);
            }
            params.add("button_1", button1);
            
            // curl 예제에는 var1, var2 파라미터가 없음
            // message_1에 직접 치환된 값을 전달하는 방식
            
            // 모든 파라미터 상세 로깅 (디버깅용)
            log.info("Aligo 알림톡 API 파라미터 전송:");
            log.info("  tpl_code={}", tplCode);
            log.info("  message_1={}", message1);
            log.info("  button_1={}", button1);
            log.info("  var1=전달 안 함 (curl 예제 방식)");
            log.info("  var2=전달 안 함 (curl 예제 방식)");
            log.info("  receiver={}", receiver);
            log.info("  sender={}", props.getSender());
            
            if (props.isTestMode()) {
                params.add("testMode", "Y");
                log.warn("Aligo 알림톡 테스트 모드: 실제 발송되지 않습니다. receiver={}", receiver);
            }

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(ALIMTALK_URL, request, String.class);

            AligoResult result = parseAlimtalkResponse(response.getBody());
            if (props.isTestMode() && result.success()) {
                log.warn("Aligo 알림톡 테스트 모드: API 응답은 성공이지만 실제 발송되지 않았습니다.");
            }
            return result;

        } catch (Exception e) {
            log.error("Aligo 알림톡 발송 실패. receiver={}", receiver, e);
            return AligoResult.fail(e.getMessage());
        }
    }

    /**
     * SMS/LMS 문자 발송 (90byte 이하면 SMS, 초과 시 LMS)
     */
    public AligoResult sendSms(String receiver, String title, String message) {
        if (!props.isSmsAvailable()) {
            return AligoResult.fail("Aligo 문자 설정 부족 (apiKey, userId, sender)");
        }

        try {
            String msg = StringUtils.hasText(title)
                    ? "[" + truncate(title, 44) + "] " + message
                    : message;
            msg = truncate(msg, 2000);

            // 90byte 이하면 SMS(단문), 초과 시 LMS(장문) - Aligo는 EUC-KR 기준
            int byteLen = msg.getBytes(Charset.forName("EUC-KR")).length;
            if (byteLen <= 90) {
                return sendSmsShort(receiver, msg);
            }
            return sendLms(receiver, title, message);
        } catch (Exception e) {
            log.error("Aligo SMS 발송 실패. receiver={}", receiver, e);
            return AligoResult.fail(e.getMessage());
        }
    }

    /** 단문 SMS (90byte 이하) */
    private AligoResult sendSmsShort(String receiver, String msg) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("key", props.getApiKey());
            params.add("user_id", props.getUserId());
            params.add("sender", props.getSender());
            params.add("receiver", normalizePhone(receiver));
            params.add("msg", truncateByBytes(msg, 90));
            params.add("msg_type", "SMS");
            if (props.isTestMode()) {
                params.add("testmode_yn", "Y");
                log.warn("Aligo SMS(단문) 테스트 모드: 실제 발송되지 않습니다. receiver={}", receiver);
            }

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(SMS_URL, request, String.class);
            
            AligoResult result = parseSmsResponse(response.getBody());
            if (props.isTestMode() && result.success()) {
                log.warn("Aligo SMS(단문) 테스트 모드: API 응답은 성공이지만 실제 발송되지 않았습니다.");
            }
            return result;
        } catch (Exception e) {
            log.error("Aligo SMS(단문) 발송 실패. receiver={}", receiver, e);
            return AligoResult.fail(e.getMessage());
        }
    }

    /**
     * LMS 문자 발송 (자유 형식, 템플릿 불필요)
     * 알림톡 설정이 없거나 실패 시 fallback으로 사용
     */
    public AligoResult sendLms(String receiver, String title, String message) {
        if (!props.isSmsAvailable()) {
            return AligoResult.fail("Aligo 문자 설정 부족 (apiKey, userId, sender)");
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            String msg = StringUtils.hasText(title)
                    ? "[" + truncate(title, 44) + "]\n" + message
                    : message;

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("key", props.getApiKey());
            params.add("user_id", props.getUserId());
            params.add("sender", props.getSender());
            params.add("receiver", normalizePhone(receiver));
            params.add("msg", truncate(msg, 2000));
            params.add("msg_type", "LMS");
            params.add("title", truncate(title != null ? title : "모니터링알림", 44));
            if (props.isTestMode()) {
                params.add("testmode_yn", "Y");
                log.warn("Aligo LMS 테스트 모드: 실제 발송되지 않습니다. receiver={}", receiver);
            }

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(SMS_URL, request, String.class);

            AligoResult result = parseSmsResponse(response.getBody());
            if (props.isTestMode() && result.success()) {
                log.warn("Aligo LMS 테스트 모드: API 응답은 성공이지만 실제 발송되지 않았습니다.");
            }
            return result;

        } catch (Exception e) {
            log.error("Aligo LMS 발송 실패. receiver={}", receiver, e);
            return AligoResult.fail(e.getMessage());
        }
    }

    private AligoResult parseAlimtalkResponse(String body) {
        try {
            log.info("Aligo 알림톡 API 응답 (전체): {}", body);
            JsonNode root = objectMapper.readTree(body);
            int code = root.path("code").asInt(1);
            String message = root.path("message").asText("");
            
            // 대체발송 관련 정보 확인
            String failover = root.path("failover").asText("");
            String smsResult = root.path("sms_result").asText("");
            String smsResultCode = root.path("sms_result_code").asText("");

            // info 필드에서 실제 발송 상태 확인
            JsonNode info = root.path("info");
            if (!info.isMissingNode()) {
                String type = info.path("type").asText("");
                long mid = info.path("mid").asLong(0);
                int scnt = info.path("scnt").asInt(0);  // 성공 카운트
                int fcnt = info.path("fcnt").asInt(0);  // 실패 카운트
                
                log.info("Aligo 알림톡 발송 상태: type={}, mid={}, scnt={}, fcnt={}", type, mid, scnt, fcnt);
                
                // fcnt가 0보다 크면 일부 또는 전체 실패
                if (fcnt > 0) {
                    log.warn("Aligo 알림톡 발송 실패 발생. scnt={}, fcnt={}, message={}", scnt, fcnt, message);
                    return AligoResult.fail("발송 실패: scnt=" + scnt + ", fcnt=" + fcnt + ", " + message);
                }
                
                // scnt가 0이면 모두 실패
                if (scnt == 0 && fcnt == 0) {
                    log.warn("Aligo 알림톡 발송 상태 불명확. scnt={}, fcnt={}, message={}", scnt, fcnt, message);
                }
            }

            if (code == 0) {
                // 알림톡 발송 성공 또는 대체발송 성공
                if (StringUtils.hasText(failover) && "Y".equals(failover)) {
                    log.warn("Aligo 알림톡 발송 실패 후 대체발송(SMS/LMS)으로 전송됨. code={}, message={}, sms_result={}, sms_result_code={}", 
                            code, message, smsResult, smsResultCode);
                    return AligoResult.ok("대체발송 성공: " + message);
                } else {
                    log.info("Aligo 알림톡 발송 성공. code={}, message={}", code, message);
                    return AligoResult.ok(message);
                }
            }
            log.warn("Aligo 알림톡 발송 실패. code={}, message={}, failover={}, response={}", code, message, failover, body);
            return AligoResult.fail("code=" + code + ", " + message);
        } catch (Exception e) {
            log.error("Aligo 알림톡 응답 파싱 실패. body={}", body, e);
            return AligoResult.fail("응답 파싱 실패: " + body);
        }
    }

    private AligoResult parseSmsResponse(String body) {
        try {
            log.debug("Aligo SMS/LMS 응답: {}", body);
            JsonNode root = objectMapper.readTree(body);
            int resultCode = root.path("result_code").asInt(-1);
            String message = root.path("message").asText("");

            // Aligo API: result_code > 0이면 성공 (메시지 ID), < 0이면 실패 (에러 코드)
            if (resultCode > 0) {
                log.info("Aligo SMS/LMS 발송 성공. result_code={}, message={}", resultCode, message);
                return AligoResult.ok(message);
            }
            log.warn("Aligo SMS/LMS 발송 실패. result_code={}, message={}, response={}", resultCode, message, body);
            return AligoResult.fail("result_code=" + resultCode + ", " + message);
        } catch (Exception e) {
            log.error("Aligo SMS/LMS 응답 파싱 실패. body={}", body, e);
            return AligoResult.fail("응답 파싱 실패: " + body);
        }
    }

    private String normalizePhone(String phone) {
        if (phone == null) return "";
        return phone.replaceAll("[^0-9]", "");
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        if (s.length() <= max) return s;
        return s.substring(0, max);
    }

    private String truncateByBytes(String s, int maxBytes) {
        if (s == null || s.isEmpty()) return "";
        try {
            Charset euckr = Charset.forName("EUC-KR");
            byte[] bytes = s.getBytes(euckr);
            if (bytes.length <= maxBytes) return s;
            for (int i = maxBytes; i > 0; i--) {
                String sub = new String(bytes, 0, i, euckr);
                if (sub.getBytes(euckr).length <= maxBytes) return sub;
            }
        } catch (Exception e) {
            return truncate(s, maxBytes);  // fallback: char 기준
        }
        return "";
    }

    public record AligoResult(boolean success, String message) {
        public static AligoResult ok(String msg) { return new AligoResult(true, msg); }
        public static AligoResult fail(String msg) { return new AligoResult(false, msg); }
    }
}
