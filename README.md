# certimate-manager-backend

Spring Boot 3 + Java 21 + Maven + MySQL 기반 백엔드입니다.

## 기술 스택
- Java 21
- Spring Boot 3.3 (Web, Data JPA, Validation)
- MySQL 8
- Maven
- Lombok

## 폴더 구조
```
src/main/java/com/certimate/manager
 ├─ config/            공통 설정 (CORS 등)
 ├─ controller/        REST 컨트롤러
 ├─ service/           서비스 인터페이스
 ├─ service/impl/      서비스 구현체
 ├─ repository/        JPA Repository
 ├─ domain/entity/      JPA 엔티티
 ├─ dto/request/        요청 DTO
 ├─ dto/response/       응답 DTO
 ├─ exception/          커스텀 예외, 전역 예외 처리
 └─ common/             공통 응답 포맷(ApiResponse) 등
```
`Example*` 파일들은 컨트롤러→서비스→레포지토리→엔티티로 이어지는 패턴 예시입니다.
실제 도메인(예: Certificate, Domain, Member 등) 개발을 시작하면 이 패턴을 그대로 복사해서
새 도메인 패키지/파일을 만들고, Example 관련 파일은 삭제해 주세요.

## 로컬 개발 환경 설정

### 1. 사전 요구사항
- JDK 21
- Maven 3.9+ (또는 IntelliJ 등 IDE에 내장된 Maven 사용 가능)
- Docker (로컬 MySQL 구동용, 선택)

### 2. 로컬 MySQL 실행
```bash
docker compose up -d
```
`local` 프로필 기본값은 `localhost:3306`, DB `certimate_manager`, 계정 `root/root` 입니다.
(docker-compose.yml의 값과 일치)

### 3. 애플리케이션 실행
```bash
mvn spring-boot:run
```
기본 프로필은 `local` 입니다. 다른 프로필로 실행하려면:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### 4. 동작 확인
```bash
curl http://localhost:8080/api/examples
```

## 프로필
| 프로필 | 용도 | DB 접속 정보 |
|---|---|---|
| local | 개인 로컬 개발 | application.yml 기본값 사용 |
| dev | 개발 서버 | 환경변수 `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` 필요 |
| prod | 운영 서버 | 환경변수 `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` 필요 |

dev/prod 배포 시에는 `.env.example`을 참고해 `.env`(커밋 금지)를 만들고 배포 환경(예: Docker, EC2)에
환경변수로 주입하세요.

## 브랜치 전략 (제안)
- `main`: 배포 가능한 상태만 유지
- `develop`: 다음 릴리즈를 위한 통합 브랜치
- `feature/{이슈번호}-{설명}`: 기능 개발 브랜치, PR로 develop에 머지

## Docker 빌드
```bash
docker build -t certimate-manager-backend .
docker run -p 8080:8080 --env-file .env certimate-manager-backend
```
