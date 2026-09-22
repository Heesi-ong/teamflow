import { expect, test } from '@playwright/test'

// 15-test-strategy.md §5 핵심 시나리오: 회원가입 -> 로그인 -> 프로젝트 생성 -> 팀원 초대
// -> Task 생성 -> Task 담당자 지정 -> Task 상태 변경 -> Task 완료.
test('core scenario: signup through task completion', async ({ page }) => {
  const email = `e2e-${Date.now()}@teamflow.dev`

  // S1. 회원가입
  await page.goto('/signup')
  await page.getByPlaceholder('이름').fill('E2E User')
  await page.getByPlaceholder('이메일').fill(email)
  await page.getByPlaceholder('비밀번호 (영문+숫자 8자 이상)').fill('password123')
  await page.getByRole('button', { name: '가입하기' }).click()

  // S2. 로그인
  await expect(page).toHaveURL(/\/login/)
  await page.getByPlaceholder('이메일').fill(email)
  await page.getByPlaceholder('비밀번호').fill('password123')
  await page.getByRole('button', { name: '로그인' }).click()
  await expect(page).toHaveURL(/\/dashboard/)

  // S3. 프로젝트 생성
  await page.goto('/projects/new')
  await page.getByPlaceholder('프로젝트명').fill('E2E Core Scenario Project')
  await page.getByRole('button', { name: '생성' }).click()
  await expect(page).toHaveURL(/\/projects\/\d+$/)
  await expect(page.getByRole('heading', { name: 'E2E Core Scenario Project' })).toBeVisible()

  // S4. 팀원 초대 (링크 초대 생성)
  await page.getByRole('link', { name: '팀원 관리' }).click()
  await expect(page).toHaveURL(/\/members$/)
  await page.getByRole('button', { name: '초대' }).click()
  await expect(page.getByText('초대 링크:')).toBeVisible()

  // S5. Task 생성
  await page.goBack()
  await page.getByRole('link', { name: 'Kanban Board' }).click()
  await expect(page).toHaveURL(/\/board$/)
  await page.getByRole('button', { name: '+ Task' }).click()
  await page.getByPlaceholder('Task 제목').fill('E2E Core Task')
  await page.getByRole('button', { name: '생성' }).click()
  await expect(page.getByText('E2E Core Task')).toBeVisible()

  // Task 모달 열기
  await page.getByText('E2E Core Task').click()

  // S6. Task 담당자 지정 (본인을 담당자로)
  await page.getByLabel('담당자').selectOption({ label: 'E2E User' })
  await expect(page.getByLabel('담당자')).toHaveValue(/\d+/)

  // S7. Task 상태 변경
  await page.getByLabel('상태').selectOption('IN_PROGRESS')
  await expect(page.getByLabel('상태')).toHaveValue('IN_PROGRESS')

  // S8. Task 완료
  await page.getByLabel('상태').selectOption('DONE')
  await expect(page.getByLabel('상태')).toHaveValue('DONE')
})
