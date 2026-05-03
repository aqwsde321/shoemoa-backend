# OpenAPI Slack Report

Shoemoa 백엔드는 GitHub Actions에서 배포된 OpenAPI 문서를 이전 기준선과 비교하고, 변경이 있으면 Slack으로 공지합니다.

이 workflow는 배포 차단용 breaking change gate가 아니라 팀 공지용입니다. 프론트엔드, 백엔드, QA가 API 계약 변경을 빠르게 확인하는 것이 목적입니다.

GitHub Actions runner의 `localhost`는 배포 서버가 아닙니다. 이 workflow는 반드시 배포가 끝난 서버의 외부 접근 가능한 `/v3/api-docs` URL을 호출합니다.

## 실행 방식

Workflow:

- `.github/workflows/openapi-slack-report.yml`

자동 실행:

- `main` push CI 성공 후 `dev` 대상으로 실행
- Render 배포 반영을 기다리기 위해 기본 180초 대기 후 `/v3/api-docs`를 조회

수동 실행 대상:

- `dev`
- `stg`
- `prod`

흐름:

```text
배포 완료
-> CI 성공 후 OpenAPI Slack Report workflow 자동 실행 또는 수동 실행
-> 배포된 /v3/api-docs 다운로드
-> 이전 openapi/review/catalog/endpoints.json 복원
-> npx --yes openapi-projector catalog 실행
-> 변경이 있으면 openapi/changes.md 를 Slack 전송
-> 갱신된 endpoints.json 을 openapi-baseline 브랜치에 저장
```

## GitHub Secrets

`dev` 대상은 기본값으로 Render 백엔드 Swagger 문서를 사용합니다.

```text
https://shoemoa-backend.onrender.com/v3/api-docs
```

환경별 URL을 바꾸려면 Repository/Environment variables 또는 secrets에 아래 값을 설정합니다. secret이 variable보다 우선합니다.

```text
DEV_OAS_URL=https://dev-api.example.com/v3/api-docs
STG_OAS_URL=https://stg-api.example.com/v3/api-docs
PROD_OAS_URL=https://api.example.com/v3/api-docs
```

Slack 공지를 보내려면 Repository/Environment secrets에 아래 값을 설정합니다.

```text
SLACK_WEBHOOK_URL=https://hooks.slack.com/services/...
```

`*_OAS_URL`에는 `localhost`나 `127.0.0.1`을 넣지 않습니다. workflow는 이런 값을 배포 서버 URL이 아닌 것으로 보고 실패시킵니다.

Swagger 접근에 인증이 필요하면 공통 인증 헤더를 추가합니다.

```text
OAS_AUTH_HEADER=Authorization: Bearer ...
```

환경별 인증 값이 다르면 아래 secret을 사용합니다. 환경별 값이 있으면 공통 `OAS_AUTH_HEADER`보다 우선합니다.

```text
DEV_OAS_AUTH_HEADER=Authorization: Bearer ...
STG_OAS_AUTH_HEADER=Authorization: Bearer ...
PROD_OAS_AUTH_HEADER=Authorization: Bearer ...
```

## Baseline

기준선은 `openapi-baseline` 브랜치에 저장합니다.

```text
dev/endpoints.json
stg/endpoints.json
prod/endpoints.json
```

첫 실행에서는 이전 기준선이 없으므로 기준선을 저장하고 Slack에 기준선 생성 메시지를 보냅니다. 두 번째 실행부터 변경이 있을 때 Slack 변경 공지가 전송됩니다.

## 산출물

`openapi-projector catalog`는 아래 파일을 생성합니다.

```text
openapi/changes.md
openapi/changes.json
openapi/review/catalog/endpoints.json
```

`openapi/changes.md` 전체 내용은 GitHub Actions artifact로 업로드됩니다. Slack 메시지는 길이 제한 때문에 일부만 전송될 수 있습니다.
