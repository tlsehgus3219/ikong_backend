# Contributing Guide

> 백엔드 팀 협업 규칙 문서입니다. 모든 팀원은 PR 전에 반드시 숙지하세요.

---

## 1. 브랜치 전략

```
main
└── dev
    ├── feature/기능명
    ├── fix/버그명
    └── hotfix/긴급수정명
```

| 브랜치 | 용도 | 병합 대상 |
|--------|------|-----------|
| `main` | 배포 가능한 코드만 유지 | - |
| `dev` | 개발 통합 브랜치 | `main` |
| `feature/*` | 새 기능 개발 | `dev` |
| `fix/*` | 버그 수정 | `dev` |
| `hotfix/*` | 운영 긴급 수정 | `main` + `dev` |

### 브랜치 네이밍 규칙

```
feature/user-login
feature/post-crud
fix/null-pointer-in-user-service
hotfix/db-connection-timeout
```

- 영어 소문자 + 하이픈(`-`) 사용
- 너무 포괄적인 이름 금지 (`feature/update` ❌)

---

## 2. 커밋 메시지 컨벤션

| Type | 설명 |
|------|------|
| `feat` | 새 기능 추가 |
| `fix` | 버그 수정 |
| `refactor` | 리팩토링 (기능 변경 없음) |
| `docs` | 문서 수정 |
| `test` | 테스트 코드 추가/수정 |
| `chore` | 빌드, 설정, 의존성 변경 |
| `style` | 코드 포맷, 세미콜론 등 (로직 변경 없음) |

### 예시

```
feat:회원가입 API 구현

- POST /api/v1/users 엔드포인트 추가
- 이메일 중복 체크 로직 포함
- BCrypt 비밀번호 암호화 적용
```

```
fix:UserService에서 NPE 발생하는 문제 수정
```

### 규칙
- subject는 50자 이내
- 과거형 금지 (`Added` ❌ → `Add` ✅ 또는 한글로 `추가` ✅)
- 마침표 금지

---

## 3. Pull Request 규칙

### PR 생성 전 체크리스트

```
[ ] 로컬에서 빌드 성공 확인 (./gradlew build)
[ ] 단위 테스트 통과 확인
[ ] 불필요한 주석, System.out.println 제거
[ ] 변경 범위와 무관한 파일 포함 여부 확인
```

### PR 작성 양식

```markdown
## 변경 사항
- 어떤 기능을 추가/수정했는지 간략히

## 작업 내용
- 구체적인 구현 내용

## 테스트
- 어떻게 테스트했는지

## 참고 사항 (선택)
- 리뷰어가 알아야 할 특이사항
```

### PR 규칙
- 변경 파일 **300줄 이하** 권장 (초과 시 분리 고려)
- `main` 브랜치 직접 push 금지
- 승인 **1명 이상** 받아야 merge 가능
- 리뷰 요청 후 **24시간 이내** 리뷰 응답

---

## 4. 코드 리뷰 에티켓

### 리뷰어

- 코드를 비판하되, 사람을 비판하지 않는다
- `nit:` 접두사 = 사소한 의견 (반영 안 해도 됨)
- 단순 지적보다 대안 제시 권장
- approve 없이 merge하지 않는다

### 작성자

- 리뷰 의견에 방어적으로 반응하지 않는다
- 반영하지 않을 경우 이유를 댓글로 남긴다
- resolve는 작성자가 직접 한다

---

## 5. Spring Boot 코드 컨벤션

### 패키지 구조

```
com.project.{domain}
├── controller
├── service
├── repository
├── entity
└── dto
```

### 네이밍

| 대상 | 규칙 | 예시 |
|------|------|------|
| 클래스 | PascalCase | `UserService` |
| 메서드/변수 | camelCase | `findByEmail` |
| 상수 | UPPER_SNAKE_CASE | `MAX_RETRY_COUNT` |
| 테이블/컬럼 | snake_case | `created_at` |
| URL | kebab-case | `/api/v1/user-profile` |

### API 응답 형식 통일

```json
{
  "success": true,
  "data": { },
  "message": "요청이 처리되었습니다"
}
```

### 기타
- `@Autowired` 필드 주입 금지 → 생성자 주입 사용
- `Optional` 남용 금지 (서비스 반환값에만 사용)
- 매직 넘버 금지 → 상수 또는 Enum으로 관리

---

## 6. 기타

- `.env`, `application-secret.yml` 등 시크릿 파일 커밋 금지
- `application.yml`에 민감 정보 하드코딩 금지 → 환경 변수 사용
- `develop` 브랜치는 매주 월요일 팀 전체 동기화

---

*규칙 변경이 필요할 경우 팀원 전체 동의 후 이 문서를 업데이트하세요.*
