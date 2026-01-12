📌 알바솔로몬 (AlbaSolomon)

알바솔로몬은 아르바이트 근무 스케줄 자동화 및 급여 계산 지원 시스템입니다.
사용자(사장님/알바생) 입력을 기반으로 근무 일정을 자동 배정하고,
주휴수당 및 야간수당을 포함한 급여 집계를 제공합니다.

🛠 기술 스택
- Backend
Java 17, Spring Boot 3
Spring Security + JWT
Spring Data JPA (Hibernate)

- Database / Infra
MySQL (AWS RDS)
AWS EC2
Docker, GitHub Actions (CI/CD)

- 협업 및 문서화
GitHub (형상 관리)
Swagger (OpenAPI) 기반 API 문서
ERD Cloud / dbdiagram 기반 데이터베이스 설계

🔑 주요 기능

👥 사용자 관리: 회원가입, 로그인 (JWT 기반 인증/인가)

📅 스케줄 자동 배정: 알바생 가능 일정 → 근무표(WorkShift) 자동 생성

💰 급여 계산: 주휴수당/야간수당 포함 집계, 급여 요약 제공

🔔 확장 기능(예정):

스케줄 알림 발송

배치 처리(Spring Batch)로 급여 자동 정산

관리자 대시보드

🛠️ 실행 방법
# 프로젝트 빌드
./mvnw clean install

# 서버 실행
./mvnw spring-boot:run


서버 실행 후 API는 기본적으로 http://localhost:8080
 에서 확인할 수 있습니다.
