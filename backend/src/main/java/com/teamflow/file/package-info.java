/**
 * S3 Presigned URL 발급, 파일 메타데이터 관리. 의존: project, member, task, common.
 * (task: project_files.task_id로 특정 Task에 첨부할 수 있어 등록 시 해당 Task가
 * 프로젝트 소속인지 검증해야 한다 — 07-database-design.md project_files.)
 */
package com.teamflow.file;
