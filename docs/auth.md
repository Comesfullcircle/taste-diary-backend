# 이메일 회원가입과 세션 로그인

## 구현 범위

이메일·비밀번호 가입, 로그인, 로그아웃, 내 정보 조회를 구현했다.
회원가입만으로 로그인되지는 않으며, 가입 후 별도로 로그인해야 한다.

기록은 로그인한 회원을 작성자로 저장하며 본인 기록만 조회·수정·삭제할 수 있다.
목록과 검색의 결과·페이지 집계에서도 다른 회원의 기록은 제외한다.

## API

| 메서드 | 경로 | 성공 응답 |
| --- | --- | --- |
| GET | `/api/auth/csrf` | 200, CSRF 헤더명과 토큰 |
| POST | `/api/auth/signup` | 201, 회원 정보 |
| POST | `/api/auth/login` | 200, 회원 정보 및 세션 유지 |
| GET | `/api/auth/me` | 200, 로그인한 회원 정보 |
| POST | `/api/auth/logout` | 204, 세션 무효화 및 쿠키 제거 |

가입·로그인 요청은 `Content-Type: application/json`으로 전달한다.

```json
{
  "email": "user@example.com",
  "password": "my-password-123"
}
```

회원 응답에는 id, email, createdAt만 포함한다. 비밀번호와 해시는 반환하지 않는다.

### 입력 규칙

- 이메일 앞뒤 공백을 제거하고 소문자로 통일한다.
- 이메일은 필수이며 최대 254자, 중복 가입은 허용하지 않는다.
- 가입 비밀번호는 8~72자이며 UTF-8 기준 최대 72바이트다.
- BCrypt의 입력 길이 제한 때문에 한글 등 다중 바이트 문자도 바이트 길이를 확인한다.
- 비밀번호의 공백이나 대소문자를 임의로 바꾸지 않는다.
- 이메일 소유 확인, 비밀번호 재설정은 아직 구현하지 않았다.

## 프론트엔드 요청 순서

1. `GET /api/auth/csrf`를 호출하고 세션 쿠키를 유지한다.
2. 응답의 headerName 이름으로 token 값을 헤더에 넣어 가입·로그인 요청을 보낸다.
3. 로그인 성공 후 CSRF 토큰이 초기화되므로 `/api/auth/csrf`를 다시 호출한다.
4. 이후 POST·PUT·DELETE 요청에는 새 CSRF 토큰을 넣는다. 기존 기록 API에도 필요하다.
5. `/api/auth/me`로 로그인 상태를 확인한다.
6. 로그아웃도 POST 요청과 유효한 CSRF 토큰이 필요하다.
7. 로그아웃 후 다시 가입·로그인하려면 새 CSRF 토큰과 세션을 받는다.

CSRF 응답 예시:

```json
{
  "headerName": "X-CSRF-TOKEN",
  "token": "서버가 발급한 값"
}
```

React가 다른 origin에서 실행된다면 요청에 `credentials: 'include'`를 사용해야 한다.
현재 별도 origin의 CORS 허용 설정은 없으며, 프론트 연결 시 개발 프록시 또는 허용 origin을 구성한다.
쿠키와 CSRF 토큰을 함께 유지해야 하며 세션 쿠키를 JavaScript로 읽을 필요는 없다.

## 세션

- 서버 세션과 HttpOnly 쿠키 사용, 브라우저에 별도 로그인 토큰을 저장하지 않음
- 로그인 성공 시 세션 ID 변경 및 CSRF 토큰 초기화
- 로그아웃 시 세션 무효화, 인증 정보와 JSESSIONID 쿠키 제거
- 비활성 세션 만료 시간 30분, 쿠키 SameSite=Lax
- 로컬 HTTP에서는 Secure=false, HTTPS 배포 시 `SESSION_COOKIE_SECURE=true` 설정 필요
- 세션은 서버 메모리에 보관되므로 서버 재시작 시 재로그인 필요

## 오류

| 상태 | code | 상황 |
| --- | --- | --- |
| 400 | `INVALID_INPUT` | 이메일·비밀번호 검증 실패 |
| 400 | `INVALID_REQUEST` | JSON 형식 오류 |
| 401 | `INVALID_CREDENTIALS` | 로그인 정보 불일치 |
| 401 | `UNAUTHORIZED` | 로그인 없이 내 정보 조회 |
| 403 | `INVALID_CSRF_TOKEN` | CSRF 토큰 누락 또는 오류 |
| 409 | `DUPLICATE_EMAIL` | 이메일 중복 |

가입 여부를 로그인 오류 메시지로 구분하지 않도록 없는 이메일과 잘못된 비밀번호는 같은 응답을 사용한다.

## 설계 이유와 후속 작업

React 웹을 먼저 구현하므로 Spring Security의 서버 세션 인증을 사용했다.
비밀번호는 BCrypt 해시만 저장하며, 이메일 중복은 사전 검사와 DB UNIQUE 제약으로 처리한다.
사용자 정의 로그인 Controller에서도 세션 ID 변경, CSRF 토큰 초기화와 SecurityContext 저장을 명시적으로 처리했다.

소셜 로그인은 회원과 별도의 소셜 계정 정보(provider, providerUserId)를 연결하는 방향으로 확장할 예정이다.
provider와 providerUserId 조합으로 계정을 구분하고, 이메일이 같다는 이유만으로 기존 회원과 자동 병합하지 않는다.
이번 변경에는 소셜 계정 테이블이나 Google·Apple·Kakao 연동을 포함하지 않았다.

기록 소유권 설계와 마이그레이션 내용은 [기록 소유권 개발 기록](entry-ownership.md)에 정리했다.

참고: [Spring Security 세션 관리](https://docs.spring.io/spring-security/reference/servlet/authentication/session-management.html),
[CSRF 보호](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html)
