# 문화생활 기록 API

로컬 실행: `./gradlew bootRun` (PostgreSQL 실행 필요)

기본 주소: `http://localhost:8080`

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
현재 인증 기능은 없으며 로컬 단일 사용자용으로 구현되어 있다.
