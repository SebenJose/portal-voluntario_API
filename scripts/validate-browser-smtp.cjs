// Uses a real browser, HTTP API and local SMTP sink. No production accounts or messages.
const assert = require('node:assert/strict');
const fs = require('node:fs/promises');
const { execFileSync } = require('node:child_process');
const { chromium } = require(process.env.PLAYWRIGHT_MODULE);
const origin = process.env.E2E_ORIGIN;
const api = process.env.E2E_API;
const artifacts = process.env.E2E_ARTIFACTS;
const studentEmail = 'estudante@e2e.test';
const password = 'Senha-E2E-inicial-123';
const newPassword = 'Senha-E2E-nova-456';
const checks = [];
const errors = [];
function passed(name) { checks.push(name); console.log('OK:', name); }
async function apiCall(path, method = 'GET', body, token) {
  const response = await fetch(api + path, { method, headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) }, body: body ? JSON.stringify(body) : undefined });
  assert.ok(response.ok, `${method} ${path}: ${response.status}`);
  return response.status === 204 ? null : response.json();
}
async function mailFor(subject) {
  for (let i = 0; i < 45; i++) {
    const result = await (await fetch(process.env.E2E_MAIL_URL + '/api/v1/messages')).json();
    const message = result.messages.find(item => item.Subject.includes(subject));
    if (message) return (await fetch(process.env.E2E_MAIL_URL + '/api/v1/message/' + message.ID)).json();
    await new Promise(resolve => setTimeout(resolve, 1000));
  }
  throw new Error('SMTP não recebeu: ' + subject);
}
async function login(page, email, secret, destination = '/painel') {
  await page.goto(`${origin}/entrar?next=${encodeURIComponent(destination)}`);
  await page.getByLabel('E-mail', { exact: true }).fill(email);
  await page.getByLabel('Senha', { exact: true }).fill(secret);
  await page.getByRole('button', { name: 'Entrar no portal', exact: true }).click();
  await page.waitForURL(origin + destination);
}
(async () => {
  const browser = await chromium.launch({ executablePath: process.env.CHROME_PATH || '/usr/bin/google-chrome', headless: true });
  const context = await browser.newContext({ viewport: { width: 1440, height: 1000 }, locale: 'pt-BR' });
  context.on('page', page => {
    page.on('console', message => { if (message.type() === 'error') console.error('Console:', message.text()); });
    page.on('pageerror', error => { errors.push(error.message); console.error('Browser:', error.message); });
    page.on('request', request => { if (request.method() !== 'GET') console.log('Request:', request.method(), new URL(request.url()).pathname); });
    page.on('response', response => { if (response.status() >= 400) console.error('HTTP:', response.status(), new URL(response.url()).pathname); });
  });
  const page = await context.newPage();
  page.setDefaultTimeout(20000);
  try {
    await page.goto(origin + '/painel');
    await page.waitForURL(/\/entrar\?next=/);
    passed('Rota privada redireciona visitante para login');
    await page.goto(origin + '/criar-conta');
    await page.getByRole('button', { name: 'Mostrar senha', exact: true }).first().click();
    await page.getByRole('button', { name: 'Ocultar senha', exact: true }).first().waitFor();
    await page.getByLabel('Nome completo', { exact: true }).fill('Estudante E2E');
    await page.getByLabel('E-mail', { exact: true }).fill(studentEmail);
    await page.getByLabel('Senha', { exact: true }).fill(password);
    await page.getByLabel('Confirmar senha', { exact: true }).fill(password);
    await page.getByLabel('Confirmar senha', { exact: true }).press('Tab');
    await page.getByRole('button', { name: 'Criar minha conta' }).click();
    await page.getByRole('status').filter({ hasText: /conta|cadastro/i }).waitFor();
    passed('Cadastro de estudante pela interface');
    await login(page, studentEmail, password);
    for (const path of ['/oportunidades', '/minhas-atividades', '/certificados', '/conta']) {
      const response = await page.goto(origin + path);
      assert.equal(response.status(), 200);
      assert.equal(new URL(page.url()).pathname, path);
      await page.locator('#page-title').waitFor();
    }
    await page.goto(origin + '/administracao');
    await page.waitForURL(origin + '/painel');
    passed('Navegação do estudante e bloqueio de administração');
    await page.screenshot({ path: artifacts + '/painel-desktop.png', fullPage: true });

    const recovery = await context.newPage();
    await recovery.goto(origin + '/recuperar-senha');
    await recovery.getByLabel('E-mail').fill(studentEmail);
    await recovery.getByRole('button', { name: 'Enviar instruções' }).click();
    await recovery.getByRole('status').filter({ hasText: 'Se houver uma conta' }).waitFor();
    const message = await mailFor('Recuperar senha');
    assert.equal(message.To[0].Address, studentEmail);
    const resetUrl = message.Text.match(/http:\/\/localhost:13000\/redefinir-senha#token=[\w-]+/)?.[0];
    assert.ok(resetUrl, 'E-mail contém link completo de recuperação');
    await recovery.goto(resetUrl);
    await recovery.getByLabel('Nova senha', { exact: true }).fill(newPassword);
    assert.equal(new URL(recovery.url()).hash, '');
    await recovery.getByLabel('Confirmar nova senha', { exact: true }).fill(newPassword);
    await recovery.getByRole('button', { name: 'Salvar nova senha' }).press('Enter');
    await recovery.getByRole('status').filter({ hasText: 'Senha alterada' }).waitFor();
    await page.goto(origin + '/painel');
    await page.waitForURL(/\/entrar\?next=/);
    await login(page, studentEmail, newPassword);
    passed('SMTP real local, redefinição no navegador e revogação da sessão antiga');
    const reused = await context.newPage();
    await reused.goto(resetUrl);
    await reused.getByLabel('Nova senha', { exact: true }).fill(password);
    await reused.getByLabel('Confirmar nova senha', { exact: true }).fill(password);
    await reused.getByRole('button', { name: 'Salvar nova senha' }).press('Enter');
    await reused.getByRole('alert').filter({ hasText: /inválido|expirou/ }).waitFor();
    passed('Link de recuperação não pode ser reutilizado');

    const adminContext = await browser.newContext({ viewport: { width: 1440, height: 1000 } });
    const adminPage = await adminContext.newPage();
    adminPage.on('pageerror', error => errors.push(error.message));
    await login(adminPage, process.env.BOOTSTRAP_ADMIN_EMAIL, process.env.BOOTSTRAP_ADMIN_PASSWORD, '/administracao');
    for (const category of ['ENSINO', 'PESQUISA', 'EXTENSAO']) {
      const field = adminPage.locator('#regulation-limit-' + category);
      await field.fill('100');
      const form = adminPage.locator('form').filter({ has: field });
      await form.getByRole('button', { name: 'Salvar limite' }).click();
      await form.getByRole('status').waitFor();
    }
    const downloadPromise = adminPage.waitForEvent('download');
    await adminPage.getByRole('button', { name: 'Exportar banco de horas (CSV)' }).click();
    const download = await downloadPromise;
    await download.saveAs(artifacts + '/banco-horas.csv');
    assert.match(await fs.readFile(artifacts + '/banco-horas.csv', 'utf8'), /Estudante E2E/);
    passed('Administração configura eixos e exporta relatório CSV pelo navegador');
    await adminPage.screenshot({ path: artifacts + '/administracao.png', fullPage: true });

    const admin = await apiCall('/auth/login', 'POST', { email: process.env.BOOTSTRAP_ADMIN_EMAIL, senha: process.env.BOOTSTRAP_ADMIN_PASSWORD });
    const student = await apiCall('/auth/login', 'POST', { email: studentEmail, senha: newPassword });
    const groups = await apiCall('/grupos-regulamento');
    const activity = await apiCall('/oportunidades', 'POST', { titulo: 'Atividade E2E', descricao: 'Validação integrada de inscrição e cancelamento', cargaHoraria: 2, vagas: 3, dataInicio: new Date(Date.now() + 86400000).toISOString(), dataFim: new Date(Date.now() + 93600000).toISOString(), grupoHorasId: groups[0].id, modalidade: 'PRESENCIAL', localAtividade: 'Sala de testes' }, admin.accessToken);
    // Test the browser registration UI; fixture publication uses the public API.
    await page.goto(origin + '/oportunidades');
    await page.getByText('Atividade E2E', { exact: true }).waitFor();
    await page.screenshot({ path: artifacts + '/oportunidades.png', fullPage: true });
    await page.getByRole('button', { name: 'Quero participar', exact: true }).click();
    await page.getByRole('button', { name: 'Cancelar inscrição', exact: true }).waitFor();
    passed('Inscrição realizada pela interface e confirmada no catálogo');
    await apiCall(`/oportunidades/${activity.id}/cancelamento`, 'POST', { motivo: 'Cancelamento de validação SMTP' }, admin.accessToken);
    const cancellation = await mailFor('Atividade cancelada');
    assert.equal(cancellation.To[0].Address, studentEmail);
    assert.match(cancellation.Text, /Cancelamento de validação SMTP/);
    passed('Aviso de cancelamento entregue por SMTP ao inscrito');
    const completed = await apiCall('/oportunidades', 'POST', { titulo: 'Certificado E2E', descricao: 'Atividade para validar emissão e retificação', cargaHoraria: 2, vagas: 3, dataInicio: new Date(Date.now() + 86400000).toISOString(), dataFim: new Date(Date.now() + 93600000).toISOString(), grupoHorasId: groups[0].id, modalidade: 'PRESENCIAL', localAtividade: 'Sala de testes' }, admin.accessToken);
    await apiCall(`/oportunidades/${completed.id}/inscricoes`, 'POST', undefined, student.accessToken);
    assert.ok(Number.isSafeInteger(completed.id));
    // Only the disposable fixture clock is advanced. All certificate operations use the UI.
    execFileSync('docker', ['exec', '-i', process.env.E2E_DB_CONTAINER, 'psql', '-U', 'portal_e2e', '-d', 'portal_e2e', '-v', 'ON_ERROR_STOP=1'], {
      input: `UPDATE oportunidades SET data_inicio = now() - interval '2 days', data_fim = now() - interval '1 day' WHERE id = ${completed.id};`,
      stdio: ['pipe', 'ignore', 'pipe'],
    });
    await adminPage.goto(origin + '/organizacao');
    await adminPage.getByLabel('Atividade para gerenciar').selectOption(String(completed.id));
    await adminPage.getByRole('button', { name: 'Presente', exact: true }).click();
    await adminPage.getByRole('status').filter({ hasText: 'Presença atualizada' }).waitFor();
    await adminPage.getByRole('button', { name: 'Emitir PDFs e enviar avisos' }).click();
    await adminPage.getByRole('link', { name: 'Verificação pública', exact: true }).waitFor();
    const originalLink = await adminPage.getByRole('link', { name: 'Verificação pública', exact: true }).getAttribute('href');
    const certificateMail = await mailFor('Certificado:');
    assert.equal(certificateMail.To[0].Address, studentEmail);
    assert.ok(certificateMail.Text.includes(originalLink));
    await page.goto(origin + '/certificados');
    const pdfPromise = page.waitForEvent('download');
    await page.getByRole('button', { name: 'Baixar certificado em PDF' }).click();
    await (await pdfPromise).saveAs(artifacts + '/certificado.pdf');
    assert.equal((await fs.readFile(artifacts + '/certificado.pdf')).subarray(0, 5).toString(), '%PDF-');
    passed('Presença, emissão, entrega SMTP e download do PDF pelo navegador');
    await adminPage.getByRole('button', { name: 'Retificar certificado de Estudante E2E' }).click();
    await adminPage.getByLabel('Carga horária', { exact: true }).fill('3');
    await adminPage.getByLabel('Motivo da retificação').fill('Correção verificada no navegador');
    await adminPage.getByRole('button', { name: 'Confirmar retificação e emitir novo código' }).press('Enter');
    await adminPage.getByRole('status').filter({ hasText: 'Certificado retificado' }).waitFor();
    await page.reload();
    await page.getByRole('button', { name: 'Ver histórico de emissões' }).click();
    await page.getByText('Admin E2E: Correção verificada no navegador', { exact: true }).waitFor();
    await page.screenshot({ path: artifacts + '/historico-certificado.png', fullPage: true });
    const publicPage = await browser.newPage();
    await publicPage.goto(origin + originalLink);
    assert.match(await publicPage.locator('body').innerText(), /retificado/i);
    assert.ok(!(await publicPage.locator('body').innerText()).includes('Correção verificada no navegador'));
    passed('Retificação, histórico privado e identificação pública do código substituído');
    await publicPage.close();
    await page.goto(origin + '/minhas-atividades');
    await page.setViewportSize({ width: 390, height: 844 });
    await page.screenshot({ path: artifacts + '/atividades-mobile.png', fullPage: true });
    assert.equal(await page.evaluate(() => document.documentElement.scrollWidth > window.innerWidth), false);
    passed('Página de atividades em viewport móvel sem rolagem horizontal');
    assert.deepEqual(errors, [], 'Sem exceções JavaScript no navegador');
    await fs.writeFile(artifacts + '/resultado.json', JSON.stringify({ checks, browser: await browser.version(), javascriptErrors: errors }, null, 2));
  } catch (error) {
    let failureIndex = 0;
    for (const currentContext of browser.contexts()) {
      for (const currentPage of currentContext.pages()) {
        await currentPage.screenshot({ path: `${artifacts}/falha-${failureIndex++}.png`, fullPage: true }).catch(() => {});
      }
    }
    await fs.writeFile(artifacts + '/falha.txt', String(error) + '\n' + JSON.stringify(errors));
    await fs.writeFile(artifacts + '/falha.html', await page.content());
    throw error;
  } finally { await browser.close(); }
})().catch(error => { console.error(error.message); process.exitCode = 1; });
