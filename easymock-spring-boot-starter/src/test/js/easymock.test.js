const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const source = fs.readFileSync(path.join(__dirname, '../../main/resources/static/easymock/js/easymock.js'), 'utf8');

function load(fetchImpl, elements = {}) {
    const context = {
        fetch: fetchImpl,
        API_PATH: '/easymock/api',
        PAGE_PATH: '/easymock',
        IS_EDIT: true,
        RULE_ID: 1,
        codeMirrorEditor: null,
        window: { location: { href: '' } },
        location: { reload() {} },
        alert(message) { throw new Error(message); },
        document: {
            getElementById(id) { return elements[id] || null; },
            querySelector() { return { value: 'URL' }; }
        }
    };
    vm.createContext(context);
    vm.runInContext(source, context);
    return context;
}

function response(status, body) {
    return {
        ok: status >= 200 && status < 300,
        status,
        text: async () => typeof body === 'string' ? body : JSON.stringify(body)
    };
}

test('apiFetch returns JSON and exposes server validation messages', async () => {
    const context = load(async () => response(200, { enabled: false }));
    assert.equal((await context.apiFetch('/rules/1/toggle', {})).enabled, false);

    const failing = load(async () => response(400, { success: false, message: 'url: 路径无效' }));
    await assert.rejects(failing.apiFetch('/rules', {}), /路径无效/);
});

test('apiFetch reports non-JSON error responses without leaking parse errors', async () => {
    const context = load(async () => response(500, '<html>error</html>'));
    await assert.rejects(context.apiFetch('/rules', {}), /HTTP 500/);
});

test('saving an edited rule preserves status and enabled values', async () => {
    let payload;
    const elements = {
        ruleName: { value: 'disabled rule' },
        ruleUrl: { value: '/api/items/{id}' },
        rulePriority: { value: '5' },
        ruleDescription: { value: '' },
        ruleResponseStatus: { value: '201' },
        ruleEnabled: { checked: false },
        responseBodyEditor: { value: '{"ok":true}' }
    };
    const context = load(async (url, options) => {
        payload = JSON.parse(options.body);
        return response(200, { id: 1 });
    }, elements);
    context.saveRule();
    await new Promise(resolve => setImmediate(resolve));
    assert.equal(payload.responseStatus, 201);
    assert.equal(payload.enabled, false);
    assert.equal(context.window.location.href, '/easymock');
});
