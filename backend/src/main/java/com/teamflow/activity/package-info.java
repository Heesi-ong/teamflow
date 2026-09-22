/**
 * ActivityLog 기록/조회(감사 로그). 의존: project, member, user, common.
 * (member: GET /activities 조회는 다른 프로젝트-scoped 조회 API와 동일하게 프로젝트 멤버인지
 * 검증해야 한다 — 08-api-specification.md §10. user: 행위자 이름을 응답에 포함하기 위함.)
 */
package com.teamflow.activity;
