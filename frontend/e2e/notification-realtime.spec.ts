import { expect, test } from '@playwright/test'

// 15-test-strategy.md §5: 실시간 기능(SSE)은 별도 시나리오로 "Task 배정 시 담당자 화면에 알림이
// 표시된다"를 검증한다. 두 사용자를 별도 BrowserContext(=별도 쿠키 저장소)로 완전히 분리해서 띄운다
// — 같은 브라우저 프로필의 두 탭을 함께 쓰면 세션 쿠키가 서로의 로그인 상태를 덮어써서(직접 겪은 문제)
// 신뢰할 수 없는 테스트가 된다.
test('assigning a task notifies the assignee in real time over SSE', async ({ browser }) => {
  const ownerContext = await browser.newContext()
  const mateContext = await browser.newContext()
  const ownerPage = await ownerContext.newPage()
  const matePage = await mateContext.newPage()

  const ownerEmail = `owner-${Date.now()}@teamflow.dev`
  const mateEmail = `mate-${Date.now()}@teamflow.dev`

  // Owner 가입 + 로그인
  await ownerPage.goto('/signup')
  await ownerPage.getByPlaceholder('이름').fill('Owner')
  await ownerPage.getByPlaceholder('이메일').fill(ownerEmail)
  await ownerPage.getByPlaceholder('비밀번호 (영문+숫자 8자 이상)').fill('password123')
  await ownerPage.getByRole('button', { name: '가입하기' }).click()
  await expect(ownerPage).toHaveURL(/\/login/)
  await ownerPage.getByPlaceholder('이메일').fill(ownerEmail)
  await ownerPage.getByPlaceholder('비밀번호').fill('password123')
  await ownerPage.getByRole('button', { name: '로그인' }).click()
  await expect(ownerPage).toHaveURL(/\/dashboard/)

  // Owner가 프로젝트 생성 후 링크 초대 발급
  await ownerPage.goto('/projects/new')
  await ownerPage.getByPlaceholder('프로젝트명').fill('SSE Notification Project')
  await ownerPage.getByRole('button', { name: '생성' }).click()
  await expect(ownerPage).toHaveURL(/\/projects\/(\d+)$/)
  const projectId = new URL(ownerPage.url()).pathname.split('/').pop()

  await ownerPage.goto(`/projects/${projectId}/members`)
  await ownerPage.getByRole('button', { name: '초대' }).click()
  const inviteLinkText = await ownerPage.locator('text=/https?:\\/\\/.*\\/invitations\\/.+/').textContent();
  const inviteUrl = inviteLinkText!.match(/https?:\/\/\S+/)![0]

  // Mate 가입 + 로그인 + 초대 수락 (별도 context, 별도 쿠키)
  await matePage.goto('/signup')
  await matePage.getByPlaceholder('이름').fill('Mate')
  await matePage.getByPlaceholder('이메일').fill(mateEmail)
  await matePage.getByPlaceholder('비밀번호 (영문+숫자 8자 이상)').fill('password123')
  await matePage.getByRole('button', { name: '가입하기' }).click()
  await expect(matePage).toHaveURL(/\/login/)
  await matePage.getByPlaceholder('이메일').fill(mateEmail)
  await matePage.getByPlaceholder('비밀번호').fill('password123')
  await matePage.getByRole('button', { name: '로그인' }).click()
  await expect(matePage).toHaveURL(/\/dashboard/)
  await matePage.goto(inviteUrl)
  await expect(matePage).toHaveURL(/\/projects$/)

  // Mate는 대시보드에 그대로 머문다 — SSE 연결이 이미 맺어진 상태에서 알림을 받아야 하므로 새로고침하지 않는다.
  await matePage.goto(`/projects/${projectId}/board`)
  await matePage.getByRole('button', { name: '알림' }).click()

  // Owner가 Task를 만들고 Mate를 담당자로 지정 -> TASK_ASSIGNED 알림 발행.
  await ownerPage.goto(`/projects/${projectId}/board`)
  await ownerPage.getByRole('button', { name: '+ Task' }).click()
  await ownerPage.getByPlaceholder('Task 제목').fill('Notify Mate')
  await ownerPage.getByRole('button', { name: '생성' }).click()
  await ownerPage.getByText('Notify Mate').click()
  await ownerPage.getByLabel('담당자').selectOption({ label: 'Mate' })

  // Mate의 이미 열려 있는 화면에 새로고침 없이 실시간 알림이 뜨는지 확인.
  await expect(matePage.getByText('Notify Mate', { exact: false })).toBeVisible({ timeout: 10_000 })

  await ownerContext.close()
  await mateContext.close()
})
