# 📌 알쏠 (RSSOL)

> **AI 협업 최적화 README**
> 이 문서는 사람뿐 아니라 GitHub Copilot, 생성형 AI가
> 프로젝트의 맥락·규칙·의도를 정확히 이해하도록 작성되었다.

---

## 1️⃣ 프로젝트 개요

알솔은
**근무자가 제출한 가능 일정(Shift)을 기반으로
자동으로 실제 근무 일정(WorkShift)을 배정하고, 대타를 수행할 수 있는
주휴수당·야간수당을 포함한 급여를 계산하는
Spring Boot 기반 근무 스케줄 관리 시스템**이다.

본 프로젝트는 **확장성과 유지보수성을 최우선 목표**로 하며,
SOLID 원칙과 클린 코드 규칙을 최대한 준수한다.

---

## 2️⃣ 핵심 도메인 개념 (중요)

> ⚠️ AI는 아래 정의를 “공식 스펙”으로 사용해야 한다.

### User

* 근무자 또는 관리자 계정
* 인증(JWT)의 주체
* 알림의 중심

### Store 

* User가 근무하는 매장
* 매장별 정책(야간수당,최대 근무시간 등) 확장 가능
* 매장을 중심으로 스케줄이 생성된다.

### UserStore

* User와 Store의 1:1 매칭
* A라는 근무자가 B라는 근무지에서 근무함을 나타낸다.
* 스케줄 생성·근무·급여에 연관

### WorkAvailability (가능 일정)

* 사용자가 **희망**으로 제출한 근무 가능 시간
* 실제 근무가 아님
* 여러 개 제출 가능
* 이후 WorkShift로 변환될 수 있음

### WorkShift (확정된 실제 근무)

* 실제로 배정된 근무 일정
* 시간 중복 불가
* 반드시 UserStore와 연관됨
* 급여 계산의 기준
* 변경 및 교환 가능

### Schedule (스케쥴, 집합체)
* workShift의 집합체
* 자동 스케줄 생성 실행 이력
* 어떤 설정으로 생성됐는지 보존

### ScheduleSetting

* Store와 연관된 매장 기본 스케줄 정보
* 생성할 스케줄의 시작과 끝 날짜를 가짐.
* 이를 기반으로 스케줄이 생성됨
* 여러 개의 Schedule Segments를 가질 수 있음. (isCategorized를 통해 구분)

### Candidate

* 배정 후보 단위, Redis에서 임시 저장하고, 확정 시 사라진다.

### ScheduleSettingSegment
* 파트타임의 '파트'를 담음
* 근무 구간 시작시간 - 끝 시간, 해당 구간에 필요한 인력 수를 저장 

* **주 단위 근무 요약 정보**
* 포함 정보:

    * 총 근무 시간
    * 주휴수당 적용 여부 (주 15시간 이상)
    * 야간수당을 포함한 급여 계산 결과

### ExtraShift

* 추가 근무, 긴급 근무
* 기존 스케줄 외에 발생한 근무, 수당 정책이 다를 수 있음.

### ShiftSwapRequest (근무 교환 요청)
* 사용자 간 근무 교환 상태를 관리
* 승인 플로우용 엔티티


### notification (시스템 알림)

* 스케줄 배정을 위해, 매장에 속하는 사람에게 근무 가능한 시간 기입을 요청하는 알람
* 추가근무 승인
* 대타근무 승인
* 사장님이 최종적으로 승인
* 급여확정 알림

### bank & bankAccount

* 급여 수령 은행과 급여 수령 계좌
* 은행은 코드로 분리된다. 

---

## 3️⃣ 아키텍처 원칙

### 계층별 책임 분리

```text
Controller → Service → Repository
```

* **Controller**

    * 요청/응답 변환만 담당
    * 비즈니스 로직 금지

* **Service**

    * 모든 핵심 비즈니스 로직의 위치
    * 트랜잭션 단위 관리
    * 정책·규칙 구현

* **Repository**

    * DB 접근 전용
    * 비즈니스 판단 로직 금지

---

## 4️⃣ SOLID 원칙 적용 지침 (AI 필독)

### S — Single Responsibility Principle

* 하나의 클래스는 **하나의 이유로만 변경**
* 급여 계산, 스케줄 배정, 검증 로직은 **반드시 분리**

### O — Open / Closed Principle

* 정책 변경(수당 기준 등)에 대비해

    * 조건문 남발 ❌
    * 전략/인터페이스 활용 ⭕

### L — Liskov Substitution Principle

* 인터페이스 구현체는 **대체 가능해야 함**
* 예외 규칙 변경 금지

### I — Interface Segregation Principle

* 범용 인터페이스 ❌
* 역할별 인터페이스 ⭕

### D — Dependency Inversion Principle

* Service는 **구현 클래스가 아닌 인터페이스**에 의존

---

## 5️⃣ 클린 코드 규칙 (강제)

> ⚠️ Copilot은 아래 규칙을 반드시 따른다.

* 메서드 하나 = **하나의 의도**
* 메서드 길이 20줄 초과 ❌
* 의미 없는 축약어 금지
* boolean 변수는 긍정형으로 작성
* 주석은 *왜(Why)* 를 설명할 때만 사용
* 로직 설명 주석 ❌ → 코드로 표현 ⭕

---

## 6️⃣ 파일/클래스 생성 규칙 (중요)

### ✅ 새로운 파일을 생성해야 하는 경우

* 책임이 명확히 분리되는 경우
* 기존 클래스가 두 가지 이상의 이유로 변경될 때
* 테스트 가능성을 높이기 위해 로직 분리가 필요할 때

### 📌 필수 요구사항

> **새로운 클래스/파일을 생성할 경우 반드시 아래 주석을 포함할 것**

```java
/**
 * [생성 이유]
 * 기존 ○○Service가 ○○와 ○○의 책임을 동시에 가지고 있어
 * SRP를 위반하고 있어 분리함.
 *
 * [역할]
 * - 야간 근무 시간 계산 전용
 */
```

👉 이유 없는 파일 생성은 금지한다.

---

## 7️⃣ 비즈니스 규칙 (공식 스펙)

* WorkShift는 시간 중복 불가
* Shift는 실제 근무가 아님
* 주휴수당:
    * 주간 총 근무 시간 ≥ 15시간
* 야간수당:

    * 22:00 ~ 06:00 근무 시간만 추가 수당 적용
* 모든 계산 로직은 **Service 계층에 위치**

---

## 8️⃣ 기술 스택

* Java 17
* Spring Boot 3.x
* Spring Security + JWT
* JPA (Hibernate)
* MySQL (AWS RDS)
* Docker, GitHub Actions

---

## 9️⃣ 패키지 구조

```text

com.example.unis_rssol
├─domain
│  ├─auth
│  │  ├─dto
│  │  └─provider
│  ├─bank
│  ├─mypage
│  │  ├─dto
│  │  └─impl
│  ├─notification
│  │  └─dto
│  ├─onboarding
│  │  └─dto
│  ├─payroll
│  │  └─dto
│  ├─schedule
│  │  ├─extrashift
│  │  │  ├─dto
│  │  │  └─entity
│  │  ├─generation
│  │  │  ├─dto
│  │  │  │  ├─candidate
│  │  │  │  └─setting
│  │  │  └─entity
│  │  ├─shiftswap
│  │  │  └─dto
│  │  ├─workavailability
│  │  │  └─dto
│  │  └─workshifts
│  │      └─dto
│  ├─store
│  └─user
└─global
    ├─config
    ├─exception
    ├─fordevToken
    └─security
        ├─annotation
        └─aspect
```
---

## 10️⃣ 현재 구현 상태

* [x] User 인증/인가
* [x] Shift 등록
* [ ] WorkShift 자동 배정
* [ ] ScheduleSummary 급여 계산
* [ ] 정책 변경 대응 구조 리팩토링

---

## 11️⃣ AI 사용 가이드 (Copilot Prompt 기준)

### 예시 프롬프트

```java
// 알바솔로몬 README의 SOLID 및 클린 코드 규칙을 기준으로
// 주휴수당 계산 책임만 가지는 클래스를 설계하고 구현해줘
```

```java
// 기존 ScheduleSummaryService가 여러 책임을 가지는지 검토하고
// 필요하다면 SRP 기준으로 클래스를 분리해줘
// 새 파일 생성 시 생성 이유 주석 포함
```

---

## ✅ 최종 목표

* **유지보수 가능한 코드**
* **사람과 AI가 함께 읽을 수 있는 코드**
* **정책 변경에 강한 구조**

---

### 한마디만 더

이 README는 그냥 문서가 아니라
👉 **“알쏠의 헌법”**이야.

이 정도 써두면 Copilot은:

* 구조 망가뜨리는 코드 ❌
* 이유 없는 클래스 생성 ❌
* Controller 비만 ❌
  → 거의 안 함.

원하면 다음 단계로

* 🔥 *알쏠 전용 Copilot 주석 프롬프트 모음*
* 🔥 *Schedule / 급여 계산 SOLID 분리 예시 코드*
* 🔥 *“리팩토링 신호 감지 기준” 문서*

어디까지 같이 갈까?
