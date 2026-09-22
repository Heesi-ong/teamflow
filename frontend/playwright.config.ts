import { defineConfig, devices } from '@playwright/test'

// 15-test-strategy.md §5: 핵심 시나리오(회원가입→...→Task 완료)와, 실시간 기능(SSE)은 별도 시나리오로 검증한다.
// §8: 실행 시간이 길어 CI에서는 main 브랜치 merge 후/nightly로만 돌린다 (상태: Optional) — 매 PR용
// Unit/Integration/API와는 별도 워크플로다. Backend + PostgreSQL/Redis(docker-compose.dev.yml)는
// 이 config가 띄우지 않으므로 실행 전 미리 떠 있어야 한다 — Frontend 개발 서버만 여기서 띄운다.
export default defineConfig({
  testDir: './e2e',
  fullyParallel: false,
  retries: 0,
  reporter: 'list',
  use: {
    baseURL: process.env.E2E_BASE_URL ?? 'http://localhost:5173',
    trace: 'retain-on-failure',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
  webServer: process.env.E2E_BASE_URL
    ? undefined
    : {
        command: 'npm run dev',
        url: 'http://localhost:5173',
        reuseExistingServer: true,
        timeout: 30_000,
      },
})
