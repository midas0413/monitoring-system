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
        if (!props.isAlimtalkAvailable()) {
            return AligoResult.fail("Aligo 알림톡 설정 부족 (apiKey, userId, sender, senderKey, templateCode)");
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("apikey", props.getApiKey());
            params.add("userid", props.getUserId());
            params.add("senderkey", props.getSenderKey());
            params.add("tpl_code", props.getTemplateCode());
            params.add("sender", props.getSender());
            params.add("receiver_1", normalizePhone(receiver));
            params.add("subject_1", truncate(title, 50));
            params.add("message_1", truncate(message, 1000));
            if (props.isTestMode()) {
                params.add("testMode", "Y");
            }

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(ALIMTALK_URL, request, String.class);

            return parseAlimtalkResponse(response.getBody());

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
            }

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(SMS_URL, request, String.class);
            return parseSmsResponse(response.getBody());
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
            }

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(SMS_URL, request, String.class);

            return parseSmsResponse(response.getBody());

        } catch (Exception e) {
            log.error("Aligo LMS 발송 실패. receiver={}", receiver, e);
            return AligoResult.fail(e.getMessage());
        }
    }

    private AligoResult parseAlimtalkResponse(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            int code = root.path("code").asInt(1);
            String message = root.path("message").asText("");

            if (code == 0) {
                return AligoResult.ok(message);
            }
            return AligoResult.fail("code=" + code + ", " + message);
        } catch (Exception e) {
            return AligoResult.fail("응답 파싱 실패: " + body);
        }
    }

    private AligoResult parseSmsResponse(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            int resultCode = root.path("result_code").asInt(-1);
            String message = root.path("message").asText("");

            if (resultCode > 0) {
                return AligoResult.ok(message);
            }
            return AligoResult.fail("result_code=" + resultCode + ", " + message);
        } catch (Exception e) {
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
