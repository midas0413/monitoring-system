# Notification Center SENT 상태 분석

## 문제 현상
- Notification Center에서 상태가 SENT인 건이 실제로 수신되지 않음

## 원인 분석

### 1. Aligo API 응답 처리 로직 문제

#### 현재 로직 (`AligoClient.parseSmsResponse()`)
```java
if (resultCode > 0) {
    return AligoResult.ok(message);
}
```

**문제점:**
- Aligo API는 HTTP 200 응답을 반환하지만, `result_code`가 양수여도 실제 발송 실패 가능
- Aligo API 문서에 따르면:
  - `result_code > 0`: 성공 (메시지 ID 반환)
  - `result_code < 0`: 실패 (에러 코드)
  - 하지만 일부 경우 실제 발송은 실패할 수 있음

#### 알림톡 응답 처리 (`AligoClient.parseAlimtalkResponse()`)
```java
if (code == 0) {
    return AligoResult.ok(message);
}
```

**문제점:**
- `code == 0`이면 성공으로 처리하지만, 실제 발송 실패 가능

### 2. 테스트 모드 확인 필요
- `aligo.test-mode: Y`인 경우 실제 발송이 안 됨
- 하지만 API 응답은 성공으로 반환될 수 있음

### 3. 실제 발송 실패 케이스
1. **수신자 번호 오류**: 잘못된 전화번호 형식
2. **발신번호 미등록**: Aligo 사이트에 등록되지 않은 발신번호
3. **템플릿 미승인**: 알림톡 템플릿이 승인되지 않음
4. **발신프로필 키 오류**: 카카오 채널 발신프로필 키가 잘못됨
5. **API 키/사용자 ID 오류**: 인증 실패

### 4. 로깅 부족
- 현재 로그에는 성공/실패만 기록되고, Aligo API의 상세 응답이 기록되지 않음
- 실제 발송 실패 원인을 파악하기 어려움

## 해결 방안

### 1. Aligo API 응답 상세 로깅 추가
- API 응답 전체를 로그에 기록
- `result_code`, `message`, `code` 등 상세 정보 기록

### 2. 응답 파싱 개선
- Aligo API 문서에 따른 정확한 성공/실패 판단
- 에러 메시지 상세 기록

### 3. 테스트 모드 확인
- 테스트 모드일 때 명확히 표시
- 실제 발송 여부 확인

### 4. 발송 결과 검증
- Aligo API의 실제 발송 결과 확인 (선택사항)
- 발송 상태 조회 API 활용
