# 문화생활 기록 API

로컬 실행: `./gradlew bootRun` (PostgreSQL 실행 필요)

기본 주소: `http://localhost:8080`

POST·PUT·DELETE 요청에는 세션 쿠키와 CSRF 토큰이 필요하다.
발급·사용 방법은 [인증 API 문서](auth.md)를 참고한다.

## API

| 메서드 | 경로 | 응답 |
| --- | --- | --- |
| POST | `/api/entries` | 201, 기록 및 Location 헤더 |
| GET | `/api/entries/{id}` | 200, 기록 |
| GET | `/api/entries?page=0&size=20` | 200, 페이지 |
| PUT | `/api/entries/{id}` | 200, 수정된 기록 |
| DELETE | `/api/entries/{id}` | 204, 본문 없음 |

## 작성·수정 요청

`Content-Type: application/json`

```json
{
  "title": "영화 관람",
  "category": "MOVIE",
  "experiencedOn": "2026-01-01",
  "review": "여운이 남았다",
  "rating": 4.5,
  "expenseAmount": 15000
}
```

- 제목: 공백 불가, 최대 200자
- 카테고리: `MOVIE`, `EXHIBITION`, `PERFORMANCE`, `BOOK`, `OTHER`
- 경험한 날짜: 필수, 미래 날짜 불가
- 감상: 선택, 최대 10,000자
- 평점: 선택, 0~5점, 0.5점 단위
- 지출: 필수, 원화 정수, 0~999,999,999,999원

PUT은 기록 전체를 수정한다. 필수 항목은 모두 전달해야 하며,
감상·평점을 생략하거나 null로 전달하면 해당 값을 지운다.
생성 시각은 유지하고 실제 변경 시 수정 시각을 갱신한다.

## 목록

페이지 번호는 0부터 시작하며 기본 크기는 20, 허용 크기는 1~100이다.
경험한 날짜 내림차순, 같은 날짜에서는 ID 내림차순으로 정렬한다.

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0
}
```

범위를 벗어난 페이지는 빈 content를 반환한다.

### 검색과 카테고리

```text
GET /api/entries?keyword=영화
GET /api/entries?category=MOVIE
GET /api/entries?keyword=영화&category=MOVIE&page=0&size=20
```

- keyword: 제목 부분 일치 검색. 영문 대소문자를 구분하지 않는다.
- 검색어 앞뒤 공백은 제거하며, 빈 문자열이나 공백만 있으면 검색 조건을 적용하지 않는다.
- `%`, `_`, `!`는 와일드카드가 아닌 입력한 문자 그대로 검색한다.
- category: 카테고리 enum 이름으로 조회한다. 예: `MOVIE`, `BOOK`.
- 두 조건을 함께 보내면 모두 만족하는 기록만 조회한다.
- 조건을 생략하면 전체 목록을 조회하며, 기존 정렬과 페이지네이션을 유지한다.
- 일치하는 결과가 없으면 빈 content를 반환한다.
- 정의되지 않은 카테고리는 400 (`INVALID_REQUEST`)을 반환한다.

## 오류

- 400: 입력 검증 실패 (`INVALID_INPUT`), 잘못된 JSON·타입 (`INVALID_REQUEST`)
- 404: 존재하지 않는 기록 조회·수정·삭제 (`ENTRY_NOT_FOUND`)

```json
{
  "code": "INVALID_INPUT",
  "message": "입력값을 확인해 주세요.",
  "errors": [
    { "field": "title", "message": "제목은 필수입니다." }
  ]
}
```

필드별 오류가 없는 경우 errors는 빈 배열이다.
회원 인증 API는 추가됐지만 기록 소유권은 아직 연결하지 않았다.
현재 기록 API는 로그인 여부와 관계없이 사용자 구분 없이 동작한다.
