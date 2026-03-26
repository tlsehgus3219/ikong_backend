# iKong♥ REST API 명세서 v1.0

> 비접촉 mmWave 레이더 기반 독신 고령자 안전 모니터링 시스템  
> **Base URL:** `https://api.ikong.com/api/v1`  
> **Format:** `application/json`  
> **Auth:** `Bearer JWT`

---

## 목차

1. [인증 방식](#인증-방식)
2. [공통 에러 코드](#공통-에러-코드)
3. [인증 (Auth)](#1-인증-auth)
4. [사용자 (User)](#2-사용자-user)
5. [보호자 (Guardian)](#3-보호자-guardian)
6. [장치 (Device)](#4-장치-device)
7. [생체 데이터 (VitalData)](#5-생체-데이터-vitaldata)
8. [낙상 / 응급](#6-낙상--응급)
9. [건강 통계 (HealthStats)](#7-건강-통계-healthstats)
10. [알림 (Notification)](#8-알림-notification)

---

## 인증 방식

🔒 표시 엔드포인트는 모든 요청 헤더에 아래를 포함해야 합니다.

```
Authorization: Bearer <access_token>
```

- **Access Token** 유효기간: 1시간
- **Refresh Token** 유효기간: 30일
- 만료 시 `POST /auth/refresh`로 재발급

---

## 공통 에러 코드

| HTTP 상태 | error code | 설명 |
|---|---|---|
| 400 | `VALIDATION_ERROR` | 요청 파라미터 형식 오류 |
| 401 | `UNAUTHORIZED` | 토큰 없음 또는 만료 |
| 403 | `FORBIDDEN` | 권한 없음 (타인 리소스 접근) |
| 404 | `NOT_FOUND` | 리소스 없음 |
| 409 | `CONFLICT` | 중복 또는 상태 충돌 |
| 500 | `INTERNAL_ERROR` | 서버 내부 오류 |

---

## 1. 인증 (Auth)

> 회원가입, 로그인, JWT 토큰 관리, 비밀번호 변경

### POST `/auth/signup` — 회원가입

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `email` | string | ✅ | 로그인용 이메일 (unique) |
| `password` | string | ✅ | 8자 이상 비밀번호 |
| `name` | string | ✅ | 사용자 실명 |
| `phone` | string | ✅ | 전화번호 (010-xxxx-xxxx) |
| `birth_date` | date | ➖ | 생년월일 (YYYY-MM-DD) |

**Response**

`201 Created`
```json
{
  "user_id": 1,
  "email": "user@example.com",
  "name": "홍길동",
  "created_at": "2025-03-25T09:00:00Z"
}
```

`400 Bad Request`
```json
{
  "error": "EMAIL_ALREADY_EXISTS",
  "message": "이미 사용 중인 이메일입니다."
}
```

---

### POST `/auth/login` — 로그인 (JWT 발급)

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `email` | string | ✅ | 이메일 |
| `password` | string | ✅ | 비밀번호 |

**Response**

`200 OK`
```json
{
  "access_token": "eyJhbGci...",
  "refresh_token": "eyJhbGci...",
  "token_type": "Bearer",
  "expires_in": 3600
}
```

`401 Unauthorized`
```json
{
  "error": "INVALID_CREDENTIALS",
  "message": "이메일 또는 비밀번호가 올바르지 않습니다."
}
```

---

### POST `/auth/refresh` — Access Token 재발급

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `refresh_token` | string | ✅ | 기존 Refresh Token |

**Response**

`200 OK`
```json
{
  "access_token": "eyJhbGci...",
  "expires_in": 3600
}
```

`401 Unauthorized`
```json
{
  "error": "INVALID_REFRESH_TOKEN",
  "message": "유효하지 않은 Refresh Token입니다."
}
```

---

### POST `/auth/logout` 🔒 — 로그아웃

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `refresh_token` | string | ✅ | 무효화할 Refresh Token |

**Response**

`200 OK`
```json
{
  "message": "로그아웃되었습니다."
}
```

---

### PUT `/auth/password` 🔒 — 비밀번호 변경

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `current_password` | string | ✅ | 현재 비밀번호 |
| `new_password` | string | ✅ | 새 비밀번호 (8자 이상) |

**Response**

`200 OK`
```json
{
  "message": "비밀번호가 변경되었습니다."
}
```

`400 Bad Request`
```json
{
  "error": "WRONG_PASSWORD",
  "message": "현재 비밀번호가 올바르지 않습니다."
}
```

---

## 2. 사용자 (User)

> 내 프로필 조회 및 수정, 계정 삭제

### GET `/users/me` 🔒 — 내 프로필 조회

**Response**

`200 OK`
```json
{
  "user_id": 1,
  "email": "user@example.com",
  "name": "홍길동",
  "phone": "010-1234-5678",
  "birth_date": "1950-05-10",
  "created_at": "2025-03-25T09:00:00Z"
}
```

---

### PUT `/users/me` 🔒 — 내 프로필 수정

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `name` | string | ➖ | 이름 변경 |
| `phone` | string | ➖ | 전화번호 변경 |
| `birth_date` | date | ➖ | 생년월일 변경 |

**Response**

`200 OK`
```json
{
  "user_id": 1,
  "name": "홍길동",
  "phone": "010-9999-0000",
  "updated_at": "2025-03-25T10:00:00Z"
}
```

---

### DELETE `/users/me` 🔒 — 계정 삭제

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `password` | string | ✅ | 본인 확인용 비밀번호 |

**Response**

`200 OK`
```json
{
  "message": "계정이 삭제되었습니다."
}
```

`400 Bad Request`
```json
{
  "error": "WRONG_PASSWORD"
}
```

---

## 3. 보호자 (Guardian)

> 보호자 등록, 조회, 수정, 삭제 및 권한 관리

### GET `/guardians` 🔒 — 보호자 목록 조회

**Response**

`200 OK`
```json
{
  "guardians": [
    {
      "guardian_id": 1,
      "name": "홍철수",
      "phone": "010-1111-2222",
      "relation": "아들",
      "is_primary": true,
      "is_active": true
    }
  ]
}
```

---

### POST `/guardians` 🔒 — 보호자 등록

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `name` | string | ✅ | 보호자 이름 |
| `phone` | string | ✅ | 보호자 전화번호 |
| `email` | string | ➖ | 보호자 이메일 |
| `relation` | string | ✅ | 관계 (아들/딸/배우자 등) |
| `is_primary` | boolean | ➖ | 대표 보호자 여부 (기본 false) |

**Response**

`201 Created`
```json
{
  "guardian_id": 2,
  "name": "홍영희",
  "relation": "딸",
  "is_primary": false,
  "created_at": "2025-03-25T11:00:00Z"
}
```

`400 Bad Request`
```json
{
  "error": "MAX_GUARDIAN_EXCEEDED",
  "message": "보호자는 최대 5명까지 등록 가능합니다."
}
```

---

### PUT `/guardians/:guardian_id` 🔒 — 보호자 정보 수정

**Path Parameters**

| 이름 | 타입 | 설명 |
|---|---|---|
| `guardian_id` | number | 수정할 보호자 ID |

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `name` | string | ➖ | 이름 |
| `phone` | string | ➖ | 전화번호 |
| `is_active` | boolean | ➖ | 알림 수신 여부 |
| `is_primary` | boolean | ➖ | 대표 보호자 지정 |

**Response**

`200 OK`
```json
{
  "guardian_id": 2,
  "name": "홍영희",
  "is_active": false
}
```

`404 Not Found`
```json
{
  "error": "GUARDIAN_NOT_FOUND"
}
```

---

### DELETE `/guardians/:guardian_id` 🔒 — 보호자 삭제

**Path Parameters**

| 이름 | 타입 | 설명 |
|---|---|---|
| `guardian_id` | number | 삭제할 보호자 ID |

**Response**

`200 OK`
```json
{
  "message": "보호자가 삭제되었습니다."
}
```

---

## 4. 장치 (Device)

> mmWave 레이더 센서 등록, 관리, Heartbeat

> 💡 **센서 등록 플로우:** Raspberry Pi 부팅 시 `serial_number`로 `POST /devices`를 호출해 등록. 이후 생체 데이터 전송 시 발급된 `device_id`를 함께 포함.

### GET `/devices` 🔒 — 내 장치 목록 조회

**Response**

`200 OK`
```json
{
  "devices": [
    {
      "device_id": 1,
      "device_type": "MR60BHA2",
      "serial_number": "MR60-000A1B2C",
      "location_label": "침실",
      "is_active": true,
      "created_at": "2025-03-20T09:00:00Z"
    }
  ]
}
```

---

### POST `/devices` 🔒 — 센서 등록

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `device_type` | string | ✅ | 센서 모델명 (MR60BHA2 / MR60FDA2) |
| `serial_number` | string | ✅ | 센서 고유 시리얼 번호 (unique) |
| `location_label` | string | ✅ | 설치 위치 (침실 / 거실 / 화장실 등) |

**Response**

`201 Created`
```json
{
  "device_id": 3,
  "device_type": "MR60BHA2",
  "serial_number": "MR60-000G5H6I",
  "location_label": "화장실",
  "is_active": true,
  "created_at": "2025-03-25T14:00:00Z"
}
```

`409 Conflict`
```json
{
  "error": "SERIAL_ALREADY_REGISTERED",
  "message": "이미 등록된 시리얼 번호입니다."
}
```

---

### GET `/devices/:device_id` 🔒 — 특정 장치 상세 조회

**Path Parameters**

| 이름 | 타입 | 설명 |
|---|---|---|
| `device_id` | number | 조회할 장치 ID |

**Response**

`200 OK`
```json
{
  "device_id": 1,
  "device_type": "MR60BHA2",
  "serial_number": "MR60-000A1B2C",
  "location_label": "침실",
  "is_active": true,
  "last_recorded_at": "2025-03-25T14:29:00Z",
  "created_at": "2025-03-20T09:00:00Z"
}
```

`404 Not Found`
```json
{
  "error": "DEVICE_NOT_FOUND"
}
```

---

### PUT `/devices/:device_id` 🔒 — 장치 정보 수정

**Path Parameters**

| 이름 | 타입 | 설명 |
|---|---|---|
| `device_id` | number | 수정할 장치 ID |

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `location_label` | string | ➖ | 설치 위치 변경 |
| `device_type` | string | ➖ | 모델명 변경 (센서 교체 시) |

**Response**

`200 OK`
```json
{
  "device_id": 1,
  "location_label": "안방",
  "updated_at": "2025-03-25T15:00:00Z"
}
```

---

### PATCH `/devices/:device_id/status` 🔒 — 장치 활성/비활성 토글

> 💡 센서를 일시적으로 중단할 때 사용. 병원 입원, 장기 외출 등 오탐 알림 방지 목적.

**Path Parameters**

| 이름 | 타입 | 설명 |
|---|---|---|
| `device_id` | number | 상태 변경할 장치 ID |

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `is_active` | boolean | ✅ | `true`: 활성화 / `false`: 비활성화 |

**Response**

`200 OK`
```json
{
  "device_id": 1,
  "is_active": false,
  "message": "장치가 비활성화되었습니다."
}
```

---

### DELETE `/devices/:device_id` 🔒 — 장치 삭제

**Path Parameters**

| 이름 | 타입 | 설명 |
|---|---|---|
| `device_id` | number | 삭제할 장치 ID |

**Response**

`200 OK`
```json
{
  "message": "장치가 삭제되었습니다."
}
```

`409 Conflict`
```json
{
  "error": "DEVICE_HAS_VITAL_DATA",
  "message": "연결된 생체 데이터가 있습니다. 비활성화를 먼저 권장합니다."
}
```

---

### GET `/devices/:device_id/status` 🔒 — 장치 연결 상태 확인

**Path Parameters**

| 이름 | 타입 | 설명 |
|---|---|---|
| `device_id` | number | 확인할 장치 ID |

**Response**

`200 OK` (연결됨)
```json
{
  "device_id": 1,
  "is_active": true,
  "is_connected": true,
  "last_heartbeat_at": "2025-03-25T14:29:55Z",
  "signal_status": "GOOD"
}
```

`200 OK` (연결 끊김)
```json
{
  "device_id": 1,
  "is_active": true,
  "is_connected": false,
  "last_heartbeat_at": "2025-03-25T11:00:00Z",
  "signal_status": "LOST"
}
```

---

### POST `/devices/:device_id/heartbeat` 🔒 — 장치 생존 신호 전송 (센서→서버)

**Path Parameters**

| 이름 | 타입 | 설명 |
|---|---|---|
| `device_id` | number | 신호를 보내는 장치 ID |

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `timestamp` | timestamp | ✅ | 센서 현재 시각 (ISO 8601) |
| `firmware_version` | string | ➖ | 펌웨어 버전 (예: v1.2.3) |

**Response**

`200 OK`
```json
{
  "received_at": "2025-03-25T14:30:00Z",
  "message": "OK"
}
```

---

## 5. 생체 데이터 (VitalData)

> 실시간 심박수/호흡수 수신 및 이력 조회

### GET `/vital/realtime` 🔒 — 실시간 생체 데이터 조회

**Response**

`200 OK`
```json
{
  "user_id": 1,
  "device_id": 1,
  "heart_rate": 72.5,
  "breathing_rate": 16.2,
  "movement": true,
  "recorded_at": "2025-03-25T14:30:00Z"
}
```

---

### POST `/vital` 🔒 — 생체 데이터 저장 (센서→서버)

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `device_id` | number | ✅ | 전송 장치 ID |
| `heart_rate` | float | ✅ | 심박수 (BPM) |
| `breathing_rate` | float | ✅ | 호흡수 (RPM) |
| `movement` | boolean | ✅ | 움직임 감지 여부 |
| `recorded_at` | timestamp | ✅ | 측정 시각 (ISO 8601) |

**Response**

`201 Created`
```json
{
  "vital_id": 1024,
  "recorded_at": "2025-03-25T14:30:00Z"
}
```

---

### GET `/vital/history` 🔒 — 생체 데이터 이력 조회

**Query Parameters**

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `from` | date | ✅ | 조회 시작일 (YYYY-MM-DD) |
| `to` | date | ✅ | 조회 종료일 (YYYY-MM-DD) |
| `type` | string | ➖ | `heart_rate` \| `breathing_rate` \| `movement` |
| `page` | number | ➖ | 페이지 번호 (기본 1) |
| `size` | number | ➖ | 페이지 크기 (기본 100) |

**Response**

`200 OK`
```json
{
  "total": 1440,
  "page": 1,
  "data": [
    {
      "recorded_at": "2025-03-25T00:00:00Z",
      "heart_rate": 65.0,
      "breathing_rate": 14.8,
      "movement": false
    }
  ]
}
```

---

## 6. 낙상 / 응급

> 낙상 감지 이벤트 관리 및 응급 상황 처리

### GET `/fall-events` 🔒 — 낙상 이벤트 목록 조회

**Query Parameters**

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `status` | string | ➖ | `PENDING` \| `CONFIRMED` \| `FALSE_ALARM` |
| `from` | date | ➖ | 조회 시작일 |

**Response**

`200 OK`
```json
{
  "fall_events": [
    {
      "fall_event_id": 5,
      "device_id": 1,
      "detected_at": "2025-03-24T03:12:00Z",
      "is_confirmed": false,
      "status": "PENDING"
    }
  ]
}
```

---

### PATCH `/fall-events/:fall_event_id` 🔒 — 낙상 이벤트 상태 업데이트

**Path Parameters**

| 이름 | 타입 | 설명 |
|---|---|---|
| `fall_event_id` | number | 낙상 이벤트 ID |

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `status` | string | ✅ | `CONFIRMED` \| `FALSE_ALARM` |

**Response**

`200 OK`
```json
{
  "fall_event_id": 5,
  "status": "CONFIRMED",
  "is_confirmed": true
}
```

---

### POST `/emergency` 🔒 — 응급 이벤트 생성 (수동 119 포함)

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `type` | string | ✅ | `FALL` \| `NO_MOVEMENT` \| `MANUAL_119` |
| `fall_event_id` | number | ➖ | 연관 낙상 이벤트 ID |

**Response**

`201 Created`
```json
{
  "emergency_id": 12,
  "type": "MANUAL_119",
  "status": "REQUESTED",
  "created_at": "2025-03-25T14:35:00Z"
}
```

---

### GET `/emergency/:emergency_id` 🔒 — 응급 이벤트 상세 조회

**Path Parameters**

| 이름 | 타입 | 설명 |
|---|---|---|
| `emergency_id` | number | 응급 이벤트 ID |

**Response**

`200 OK`
```json
{
  "emergency_id": 12,
  "type": "MANUAL_119",
  "status": "SENT",
  "notifications": [
    { "guardian_id": 1, "status": "SUCCESS" }
  ],
  "created_at": "2025-03-25T14:35:00Z"
}
```

`404 Not Found`
```json
{
  "error": "EMERGENCY_NOT_FOUND"
}
```

---

## 7. 건강 통계 (HealthStats)

> 일별 / 주간 / 월간 건강 집계 데이터 조회

### GET `/health-stats/daily` 🔒 — 일별 건강 통계 조회

**Query Parameters**

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `date` | date | ✅ | 조회 날짜 (YYYY-MM-DD) |

**Response**

`200 OK`
```json
{
  "date": "2025-03-25",
  "avg_heart_rate": 70.2,
  "min_heart_rate": 55.0,
  "max_heart_rate": 98.5,
  "avg_breathing_rate": 15.8,
  "movement_count": 324,
  "sleep_hours": 7.5
}
```

---

### GET `/health-stats/weekly` 🔒 — 주간 건강 통계 조회

**Query Parameters**

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `start_date` | date | ✅ | 주 시작일 (월요일 기준) |

**Response**

`200 OK`
```json
{
  "start_date": "2025-03-17",
  "end_date": "2025-03-23",
  "daily": [
    {
      "date": "2025-03-17",
      "avg_heart_rate": 69.0,
      "avg_breathing_rate": 15.2,
      "sleep_hours": 7.0
    }
  ],
  "weekly_avg_heart_rate": 71.3,
  "weekly_avg_breathing_rate": 15.5
}
```

---

### GET `/health-stats/monthly` 🔒 — 월간 건강 통계 조회

**Query Parameters**

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `year` | number | ✅ | 연도 (예: 2025) |
| `month` | number | ✅ | 월 (1~12) |

**Response**

`200 OK`
```json
{
  "year": 2025,
  "month": 3,
  "monthly_avg_heart_rate": 70.5,
  "monthly_avg_breathing_rate": 15.6,
  "total_sleep_hours": 210.5,
  "fall_count": 1,
  "emergency_count": 1
}
```

---

## 8. 알림 (Notification)

> 보호자에게 발송된 알림 이력 조회

### GET `/notifications` 🔒 — 알림 목록 조회

**Query Parameters**

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `status` | string | ➖ | `SUCCESS` \| `FAIL` |
| `page` | number | ➖ | 페이지 번호 (기본 1) |
| `size` | number | ➖ | 페이지 크기 (기본 20) |

**Response**

`200 OK`
```json
{
  "total": 20,
  "notifications": [
    {
      "notification_id": 101,
      "message": "낙상이 감지되었습니다. 확인이 필요합니다.",
      "sent_at": "2025-03-25T14:35:01Z",
      "status": "SUCCESS",
      "guardian": {
        "name": "홍철수",
        "relation": "아들"
      }
    }
  ]
}
```

---

## 엔드포인트 요약

| 도메인 | GET | POST | PUT | PATCH | DELETE | 합계 |
|---|---|---|---|---|---|---|
| 인증 | 0 | 4 | 1 | 0 | 0 | **5** |
| 사용자 | 1 | 0 | 1 | 0 | 1 | **3** |
| 보호자 | 1 | 1 | 1 | 0 | 1 | **4** |
| 장치 | 3 | 2 | 1 | 1 | 1 | **8** |
| 생체 데이터 | 2 | 1 | 0 | 0 | 0 | **3** |
| 낙상 / 응급 | 2 | 2 | 0 | 1 | 0 | **5** |
| 건강 통계 | 3 | 0 | 0 | 0 | 0 | **3** |
| 알림 | 1 | 0 | 0 | 0 | 0 | **1** |
| **합계** | **13** | **10** | **4** | **2** | **3** | **32** |

---

*iKong♥ API Spec v1.0 · 아이콩 팀 · 2025*
