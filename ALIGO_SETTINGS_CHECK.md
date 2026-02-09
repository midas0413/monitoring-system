# Aligo 설정 점검 결과

## 현재 설정 상태

### application.yml 설정
```yaml
aligo:
  api-key: ${ALIGO_API_KEY:ou73by0iwvi33psmwrgvsjgvxtsc8t7f}
  user-id: ${ALIGO_USER_ID:midas09}
  sender: ${ALIGO_SENDER:02-736-3500}
  sender-key: ${ALIGO_SENDER_KEY:fab6a2937b472d39f03bb3ca2873bd331ce3a7af}
  template-code: ${ALIGO_TEMPLATE_CODE:UC_2669}
  test-mode: ${ALIGO_TEST_MODE:N}
```

## 설정 항목별 점검

### ✅ 필수 설정 (SMS/LMS 발송용)
1. **api-key**: ✅ 설정됨 (기본값 또는 환경변수)
   - 알리고 관리자 > API 인증키
   - 환경변수: `ALIGO_API_KEY`

2. **user-id**: ✅ 설정됨 (기본값 또는 환경변수)
   - 알리고 로그인 아이디
   - 환경변수: `ALIGO_USER_ID`

3. **sender**: ✅ 설정됨 (기본값 또는 환경변수)
   - 발신번호 (사이트에 등록된 번호만 사용 가능)
   - 환경변수: `ALIGO_SENDER`
   - 현재값: `02-736-3500`

### ✅ 알림톡 전용 설정 (KAKAO 알림톡용)
4. **sender-key**: ✅ 설정됨 (기본값 또는 환경변수)
   - 카카오 채널 연동 후 발급된 발신프로필 키
   - 환경변수: `ALIGO_SENDER_KEY`
   - 현재값: `fab6a2937b472d39f03bb3ca2873bd331ce3a7af`

5. **template-code**: ✅ 설정됨 (기본값 또는 환경변수)
   - 알리고에서 승인된 알림톡 템플릿 코드
   - 환경변수: `ALIGO_TEMPLATE_CODE`
   - 현재값: `UC_2669`
   - 템플릿 변수: var1(서버명), var2(알림내용)

### ✅ 기타 설정
6. **test-mode**: ✅ 설정됨
   - `Y`: 테스트 모드 (실제 전송 안함)
   - `N`: 실제 전송 (현재 설정)
   - 환경변수: `ALIGO_TEST_MODE`

## 기능별 설정 요구사항

### SMS 발송
- ✅ **필수**: `api-key`, `user-id`, `sender`
- ✅ **현재 상태**: 모든 필수 설정 완료

### LMS 발송
- ✅ **필수**: `api-key`, `user-id`, `sender`
- ✅ **현재 상태**: 모든 필수 설정 완료

### KAKAO 알림톡 발송
- ✅ **필수**: `api-key`, `user-id`, `sender`, `sender-key`, `template-code`
- ✅ **현재 상태**: 모든 필수 설정 완료
- **동작 방식**:
  1. 알림톡 시도 (템플릿 변수: var1=서버명, var2=알림내용)
  2. 실패 시 LMS로 자동 fallback

## 코드에서의 설정 검증

### AligoProperties.java
- `isSmsAvailable()`: api-key, user-id, sender 확인
- `isAlimtalkAvailable()`: api-key, user-id, sender, sender-key, template-code 확인

### AligoClient.java
- 알림톡 API: `https://kakaoapi.aligo.in/akv10/alimtalk/send/`
- SMS/LMS API: `https://apis.aligo.in/send/`
- 템플릿 변수 지원: var1, var2

### KakaoDeliverer.java
- 알림톡 우선 시도 → 실패 시 LMS fallback
- 서버명 자동 추출 (body에서 target=, serverName=, [서버명] 패턴)

## 발견된 문제점

### ⚠️ application.yml 중복 설정
- `spring.mail` 설정이 2곳에 중복되어 있음 (4-11줄, 58-66줄)
- 하위 설정(58-66줄)이 상위 설정을 덮어씀
- 수정 필요

## 권장 사항

1. **환경변수 사용 권장**
   - 민감한 정보(api-key, sender-key 등)는 환경변수로 관리
   - 기본값은 개발/테스트용으로만 사용

2. **test-mode 확인**
   - 운영 환경에서는 `test-mode: N` 확인
   - 테스트 시에는 `test-mode: Y`로 설정

3. **템플릿 코드 확인**
   - `UC_2669` 템플릿이 알리고에 승인되어 있는지 확인
   - 템플릿 변수(var1, var2)가 올바르게 설정되어 있는지 확인

4. **발신번호 확인**
   - `02-736-3500`이 알리고 사이트에 등록되어 있는지 확인
