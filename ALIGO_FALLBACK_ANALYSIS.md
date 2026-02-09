# Aligo 대체발송 분석

## 문제 상황
알림이 Aligo 템플릿에 설정된 대체발송문자로 수신됨

## 대체발송 발생 원인

### 1. Aligo API 자동 대체발송
Aligo 알림톡 API는 알림톡 발송 실패 시 자동으로 대체발송(SMS/LMS)을 처리할 수 있습니다.

**발생 조건:**
- 수신자가 카카오톡 미가입자
- 수신자가 해당 카카오 채널을 친구 추가하지 않음
- 템플릿 형식 불일치로 인한 발송 실패
- Aligo 관리자 페이지에서 템플릿에 대체발송문자 설정됨

**Aligo API 응답 구조:**
```json
{
  "code": 0,  // 알림톡 실패 후 대체발송 성공 시에도 0 반환
  "message": "성공 메시지",
  "failover": "Y",  // 대체발송 발생 여부
  "sms_result": "대체발송 결과",
  "sms_result_code": "대체발송 결과 코드"
}
```

### 2. 우리 코드의 Fallback 로직
현재 `KakaoDeliverer`는 알림톡 실패 시 LMS로 재시도합니다:

```java
var ar = aligoClient.sendAlimtalk(...);
if (ar.success()) {
    return DeliverResult.ok(ar.message());
}
log.warn("[KAKAO] 알림톡 실패, LMS로 재시도. err={}", ar.message());

// 2) LMS 문자 fallback
var lr = aligoClient.sendLms(...);
```

## 해결 방안

### 방안 1: Aligo API 응답에서 대체발송 정보 확인
- `parseAlimtalkResponse()`에서 `failover`, `sms_result` 필드 확인
- 대체발송 발생 시 로그에 명확히 표시
- **구현 완료**: 응답 파싱 로직 강화

### 방안 2: 대체발송 비활성화 (선택사항)
Aligo 관리자 페이지에서 템플릿의 대체발송 설정을 비활성화할 수 있습니다.
- 장점: 알림톡 실패 시 명확히 실패로 처리
- 단점: 수신자가 카카오톡 미가입자일 경우 알림 미수신

### 방안 3: 우리 코드의 Fallback 로직 제거 (선택사항)
Aligo API가 자동으로 대체발송을 처리하므로, 우리 코드의 LMS fallback을 제거할 수 있습니다.
- 장점: 중복 발송 방지
- 단점: Aligo 대체발송 설정이 없을 경우 알림 미수신

## 권장 사항

1. **로그 강화**: 대체발송 발생 시 명확히 로그에 표시 (구현 완료)
2. **Aligo 템플릿 설정 확인**: 템플릿에 대체발송문자가 설정되어 있는지 확인
3. **수신자 확인**: 수신자가 카카오톡을 사용하고 해당 채널을 친구 추가했는지 확인
4. **템플릿 형식 확인**: 템플릿 형식과 전달되는 파라미터가 정확히 일치하는지 확인

## 다음 단계

1. 애플리케이션 재시작 후 로그 확인
2. 알림 발송 시 `parseAlimtalkResponse()` 로그에서 대체발송 정보 확인
3. Aligo 관리자 페이지에서 템플릿 대체발송 설정 확인
