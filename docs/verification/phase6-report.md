# Phase 6 Verification Report

## 범위

- T018: REQ-253, REQ-255, REQ-259, REQ-261 접근성·성능·보안·개인정보 검증
- T019: REQ-418, REQ-419 canonical_id/source_id 추적 일치 검사
- T020: REQ-211~REQ-427 Requirement Accounting 누락 검사

## 자동검사

- 실행 명령: `node tools/verify-phase6.mjs`
- 검증 기준:
  - 접근성: semantic controls, `aria-label`, modal `role="dialog"`, `aria-modal`, `aria-labelledby`, 필수 입력 표시
  - 성능: 상대 `/api` 호출, 목록 기본 20건, 3MB 주요 페이지 budget 산출물 기록
  - 보안: absolute API origin 금지, admin guard, stack trace/debug logging 노출 금지, Docker context ignore 검증
  - 개인정보: credential-like literal, browser storage, secret/token/password 노출 패턴 금지
  - 추적성: Phase 6 산출물의 `canonical_id`와 `source_id` 필수 매핑
  - Requirement Accounting: `REQ-211`부터 `REQ-427`까지 217개 ID의 연속 coverage와 누락 0건

## 결과

- `docs/verification/phase6-evidence.json`에 검증 evidence와 requirement accounting 목록을 materialize했다.
- `tools/verify-phase6.mjs`가 evidence와 실제 저장소 파일을 함께 검사한다.
- 최종 실행 결과: `Phase 6 verification passed: T018, T019, T020 with 217 accounted requirements.`

## 제한 사항

- 코드 생성 지시의 Spring Boot/Maven 제약에 따라 `mvn test`는 실행하지 않았다.
- 프론트엔드 `npm run test -- --run src/phase6Verification.test.tsx`는 시도했으나 현재 워크트리에 `node_modules`가 없어 `vitest: not found`로 실행이 차단되었다. package manager 실행은 금지되어 있어 의존성 설치는 수행하지 않았다.
