/**
 * 프로젝트 통계 집계(Redis 캐싱), Task/Document/Comment 통합 검색. 의존: project, member, task,
 * document, comment, activity, common. 순수 조회/집계(reporting) 목적이라 다른 Module의 내부
 * Repository/Entity가 아니라 각 Module의 Service가 노출하는 읽기 전용 메서드만 사용한다
 * (05-backend-architecture.md §1의 "Service를 통해서만 협력" 원칙 유지).
 */
package com.teamflow.dashboard;
