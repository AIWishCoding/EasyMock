/* EasyMock JavaScript */

// ========== 主页面功能 ==========

function filterEndpoints() {
    var keyword = document.getElementById('endpointSearch').value.toLowerCase();
    var items = document.querySelectorAll('.endpoint-item');
    items.forEach(function(item) {
        var url = item.getAttribute('data-url') || '';
        item.style.display = url.toLowerCase().indexOf(keyword) !== -1 ? '' : 'none';
    });
}

function filterRulesByEndpoint(elem) {
    var url = elem.getAttribute('data-url');

    document.querySelectorAll('.endpoint-item').forEach(function(el) {
        el.classList.remove('active');
    });
    elem.classList.add('active');

    var rows = document.querySelectorAll('.rule-row');
    var visibleCount = 0;
    rows.forEach(function(row) {
        var rowUrl = row.getAttribute('data-url');
        if (rowUrl === url) {
            row.style.display = '';
            visibleCount++;
        } else {
            row.style.display = 'none';
        }
    });

    var filterBadge = document.getElementById('filterBadge');
    var clearBtn = document.getElementById('clearFilterBtn');
    var ruleCount = document.getElementById('ruleCount');
    if (filterBadge) {
        filterBadge.textContent = url;
        filterBadge.classList.remove('d-none');
    }
    if (clearBtn) {
        clearBtn.classList.remove('d-none');
    }
    if (ruleCount) {
        ruleCount.textContent = visibleCount;
    }
}

function clearFilter() {
    document.querySelectorAll('.endpoint-item').forEach(function(el) {
        el.classList.remove('active');
    });
    document.querySelectorAll('.rule-row').forEach(function(row) {
        row.style.display = '';
    });

    var filterBadge = document.getElementById('filterBadge');
    var clearBtn = document.getElementById('clearFilterBtn');
    var ruleCount = document.getElementById('ruleCount');
    if (filterBadge) filterBadge.classList.add('d-none');
    if (clearBtn) clearBtn.classList.add('d-none');
    if (ruleCount) {
        ruleCount.textContent = document.querySelectorAll('.rule-row').length;
    }
}

function apiFetch(url, options) {
    return fetch(url, options).then(function(resp) {
        return resp.text().then(function(text) {
            var data = null;
            try {
                data = text ? JSON.parse(text) : null;
            } catch (e) {
                throw new Error('HTTP ' + resp.status);
            }
            if (!resp.ok) {
                throw new Error((data && data.message) || ('HTTP ' + resp.status));
            }
            return data;
        });
    });
}

function toggleRule(checkbox) {
    var id = checkbox.getAttribute('data-id');
    apiFetch(API_PATH + '/rules/' + id + '/toggle', { method: 'PUT' })
        .then(function(data) {
            checkbox.checked = data.enabled;
        })
        .catch(function(err) {
            alert('切换规则状态失败: ' + err.message);
            checkbox.checked = !checkbox.checked;
        });
}

function deleteRule(btn) {
    if (!confirm('确定要删除该规则吗？')) return;
    var id = btn.getAttribute('data-id');
    apiFetch(API_PATH + '/rules/' + id, { method: 'DELETE' })
        .then(function(data) {
            if (data.success) {
                location.reload();
            } else {
                alert('删除规则失败: ' + (data.message || '未知错误'));
            }
        })
        .catch(function(err) {
            alert('删除规则失败: ' + err.message);
        });
}

function importRules() {
    document.getElementById('importFile').click();
}

function handleImport(input) {
    if (!input.files || !input.files[0]) return;
    var formData = new FormData();
    formData.append('file', input.files[0]);

    apiFetch(API_PATH + '/import', { method: 'POST', body: formData })
        .then(function(data) {
            if (data.success) {
                alert('导入成功，共导入 ' + data.count + ' 条规则');
                location.reload();
            } else {
                alert('导入失败: ' + (data.message || '未知错误'));
            }
        })
        .catch(function(err) {
            alert('导入失败: ' + err.message);
        });

    input.value = '';
}

// ========== 规则表单功能 ==========

var codeMirrorEditor = null;
var conditionGroupCounter = 0;

// 新建规则时的示例返回值
var EXAMPLE_RESPONSE_BODY = JSON.stringify({
    "code": 200,
    "message": "操作成功",
    "data": {
        "id": "mock-001",
        "status": "SUCCESS"
    }
}, null, 2);

function initRuleForm() {
    // 初始化规则级 CodeMirror 编辑器（URL 模式使用）
    var textarea = document.getElementById('responseBodyEditor');
    if (textarea && typeof CodeMirror !== 'undefined') {
        codeMirrorEditor = CodeMirror.fromTextArea(textarea, {
            mode: { name: 'javascript', json: true },
            lineNumbers: true,
            matchBrackets: true,
            autoCloseBrackets: true,
            tabSize: 2,
            indentWithTabs: false
        });

        var val = codeMirrorEditor.getValue().trim();
        if (val) {
            try {
                var formatted = JSON.stringify(JSON.parse(val), null, 2);
                codeMirrorEditor.setValue(formatted);
            } catch (e) {
                // 保持原样
            }
        } else if (!IS_EDIT) {
            codeMirrorEditor.setValue(EXAMPLE_RESPONSE_BODY);
        }
    }

    // 加载已有参数条件
    if (typeof EXISTING_MATCH_TYPE !== 'undefined' && EXISTING_MATCH_TYPE === 'PARAMETER') {
        document.getElementById('matchTypeParam').checked = true;

        // 先加载已有条件组数据（在 toggleParamConditions 之前，避免误添加默认条件组）
        if (typeof EXISTING_CONDITIONS !== 'undefined' && EXISTING_CONDITIONS) {
            try {
                var data;
                if (typeof EXISTING_CONDITIONS === 'string') {
                    data = JSON.parse(EXISTING_CONDITIONS);
                } else {
                    data = EXISTING_CONDITIONS;
                }
                if (Array.isArray(data) && data.length > 0) {
                    // 检测格式：新格式有 conditions 键，旧格式有 path 键
                    if (data[0].conditions) {
                        // 新格式：ConditionGroup 数组
                        data.forEach(function(group) {
                            addConditionGroup(
                                group.name || '',
                                group.conditions || [],
                                group.responseBody || ''
                            );
                        });
                    } else if (data[0].path) {
                        // 旧格式：扁平 ParamCondition 数组 -> 包装为单条件组
                        var legacyResponseBody = '';
                        if (typeof EXISTING_RESPONSE_BODY !== 'undefined' && EXISTING_RESPONSE_BODY) {
                            legacyResponseBody = typeof EXISTING_RESPONSE_BODY === 'string'
                                ? EXISTING_RESPONSE_BODY : JSON.stringify(EXISTING_RESPONSE_BODY);
                        }
                        addConditionGroup('条件组 1', data, legacyResponseBody);
                    }
                }
            } catch (e) {
                // 忽略解析错误
            }
        }

        // 再切换显示（如果容器为空则自动添加默认条件组）
        toggleParamConditions();
    }

    // 表单提交处理
    var form = document.getElementById('ruleForm');
    if (form) {
        form.addEventListener('submit', function(e) {
            e.preventDefault();
            saveRule();
        });
    }
}

function toggleParamConditions() {
    var isParam = document.getElementById('matchTypeParam').checked;
    var paramSection = document.getElementById('paramConditionsSection');
    var urlResponseSection = document.getElementById('urlResponseSection');

    paramSection.style.display = isParam ? '' : 'none';
    urlResponseSection.style.display = isParam ? 'none' : '';

    // 参数模式下，如果没有条件组则自动添加一个
    if (isParam) {
        var container = document.getElementById('conditionGroupsContainer');
        if (container.children.length === 0) {
            addConditionGroup();
        }
    }
}

function addConditionGroup(name, conditions, responseBody) {
    var container = document.getElementById('conditionGroupsContainer');
    conditionGroupCounter++;
    var groupIndex = conditionGroupCounter;

    var panel = document.createElement('div');
    panel.className = 'condition-group-panel card mb-3';
    panel.setAttribute('data-group-index', groupIndex);

    // Card header
    var header = document.createElement('div');
    header.className = 'card-header d-flex justify-content-between align-items-center';
    header.innerHTML =
        '<input type="text" class="form-control form-control-sm group-name" ' +
        'placeholder="条件组名称（如：场景1-VIP订单）" ' +
        'value="' + escapeHtml(name || ('条件组 ' + groupIndex)) + '" style="max-width:300px">' +
        '<button type="button" class="btn btn-outline-danger btn-sm" onclick="removeConditionGroup(this)" title="删除条件组">删除</button>';

    // Card body
    var body = document.createElement('div');
    body.className = 'card-body';

    // Conditions section
    var condLabel = document.createElement('div');
    condLabel.className = 'form-label fw-bold mb-2';
    condLabel.textContent = '参数条件（组内所有条件需同时满足）';

    var condContainer = document.createElement('div');
    condContainer.className = 'group-conditions-container mb-2';

    var addCondBtn = document.createElement('button');
    addCondBtn.type = 'button';
    addCondBtn.className = 'btn btn-outline-secondary btn-sm mb-3';
    addCondBtn.textContent = '+ 添加条件';
    addCondBtn.onclick = function() {
        addConditionToGroup(condContainer);
    };

    // Response body
    var respDiv = document.createElement('div');
    respDiv.className = 'mb-1';
    respDiv.innerHTML =
        '<label class="form-label fw-bold">Mock 返回值 (JSON) <span class="text-danger">*</span></label>' +
        '<textarea class="form-control group-response-body" rows="6" ' +
        'placeholder="请输入该条件组的 Mock 返回值（JSON 格式）"></textarea>';

    body.appendChild(condLabel);
    body.appendChild(condContainer);
    body.appendChild(addCondBtn);
    body.appendChild(respDiv);

    panel.appendChild(header);
    panel.appendChild(body);
    container.appendChild(panel);

    // Fill conditions
    if (conditions && Array.isArray(conditions) && conditions.length > 0) {
        conditions.forEach(function(c) {
            addConditionToGroup(condContainer, c.path, c.operator, c.value);
        });
    } else {
        // 默认添加一条示例条件
        addConditionToGroup(condContainer, '$.orderId', 'EQUALS', '12345');
    }

    // Fill response body
    var respTextarea = panel.querySelector('.group-response-body');
    if (responseBody) {
        try {
            var formatted = JSON.stringify(JSON.parse(responseBody), null, 2);
            respTextarea.value = formatted;
        } catch (e) {
            respTextarea.value = responseBody;
        }
    } else {
        respTextarea.value = EXAMPLE_RESPONSE_BODY;
    }
}

function addConditionToGroup(condContainer, path, operator, value) {
    var row = document.createElement('div');
    row.className = 'condition-row';

    var operators = ['EQUALS', 'NOT_EQUALS', 'CONTAINS', 'GT', 'GTE', 'LT', 'LTE', 'REGEX', 'EXISTS', 'NOT_EXISTS'];

    var optionsHtml = operators.map(function(op) {
        var selected = (operator && operator === op) ? ' selected' : '';
        return '<option value="' + op + '"' + selected + '>' + op + '</option>';
    }).join('');

    row.innerHTML =
        '<input type="text" class="form-control condition-path" placeholder="例如: $.orderId" value="' + escapeHtml(path || '') + '" style="flex:2">' +
        '<select class="form-select condition-operator" style="flex:1">' + optionsHtml + '</select>' +
        '<input type="text" class="form-control condition-value" placeholder="期望值" value="' + escapeHtml(value || '') + '" style="flex:2">' +
        '<button type="button" class="btn btn-outline-danger btn-sm btn-remove" onclick="this.parentElement.remove()">X</button>';

    condContainer.appendChild(row);
}

function removeConditionGroup(btn) {
    var container = document.getElementById('conditionGroupsContainer');
    if (container.children.length <= 1) {
        alert('至少需要保留一个条件组');
        return;
    }
    var panel = btn.closest('.condition-group-panel');
    if (panel) {
        panel.remove();
    }
}

function collectConditionGroups() {
    var groups = [];
    var container = document.getElementById('conditionGroupsContainer');
    var panels = container.querySelectorAll('.condition-group-panel');
    panels.forEach(function(panel) {
        var name = panel.querySelector('.group-name').value.trim();
        var responseBody = panel.querySelector('.group-response-body').value.trim();

        var conditions = [];
        var rows = panel.querySelectorAll('.condition-row');
        rows.forEach(function(row) {
            var path = row.querySelector('.condition-path').value.trim();
            var op = row.querySelector('.condition-operator').value;
            var val = row.querySelector('.condition-value').value.trim();
            if (path) {
                conditions.push({ path: path, operator: op, value: val });
            }
        });

        groups.push({
            name: name,
            conditions: conditions,
            responseBody: responseBody
        });
    });
    return groups;
}

function escapeHtml(str) {
    if (!str) return '';
    return str.replace(/&/g, '&amp;').replace(/"/g, '&quot;')
              .replace(/'/g, '&#39;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
}

function saveRule() {
    var name = document.getElementById('ruleName').value.trim();
    var url = document.getElementById('ruleUrl').value.trim();
    var matchType = document.querySelector('input[name="matchType"]:checked').value;
    var priority = parseInt(document.getElementById('rulePriority').value);
    if (isNaN(priority)) priority = 0;
    var description = document.getElementById('ruleDescription').value.trim();

    if (!name || !url) {
        alert('请填写规则名称和请求路径');
        return;
    }
    if (!url.startsWith('/')) {
        alert('请求路径必须以 / 开头');
        return;
    }

    var responseBody;
    var paramConditions = null;

    if (matchType === 'PARAMETER') {
        // 参数模式：从条件组收集数据
        var groups = collectConditionGroups();
        if (groups.length === 0) {
            alert('参数级别匹配至少需要配置一个条件组');
            return;
        }

        // 校验每个条件组
        for (var i = 0; i < groups.length; i++) {
            var g = groups[i];
            if (g.conditions.length === 0) {
                alert('条件组 "' + (g.name || (i + 1)) + '" 至少需要配置一个条件');
                return;
            }
            if (!g.responseBody) {
                alert('条件组 "' + (g.name || (i + 1)) + '" 的 Mock 返回值不能为空');
                return;
            }
            try {
                JSON.parse(g.responseBody);
            } catch (e) {
                alert('条件组 "' + (g.name || (i + 1)) + '" 的 Mock 返回值不是合法的 JSON 格式: ' + e.message);
                return;
            }
        }

        paramConditions = JSON.stringify(groups);
        // 规则级 responseBody 填占位值以满足数据库非空约束
        responseBody = '{}';
    } else {
        // URL 模式：从规则级编辑器取值
        if (codeMirrorEditor) {
            responseBody = codeMirrorEditor.getValue().trim();
        } else {
            responseBody = document.getElementById('responseBodyEditor').value.trim();
        }

        if (!responseBody) {
            alert('请填写 Mock 返回值');
            return;
        }

        try {
            JSON.parse(responseBody);
        } catch (e) {
            alert('Mock 返回值不是合法的 JSON 格式: ' + e.message);
            return;
        }
    }

    var payload = {
        name: name,
        url: url,
        matchType: matchType,
        paramConditions: paramConditions,
        responseBody: responseBody,
        responseStatus: parseInt(document.getElementById('ruleResponseStatus').value),
        enabled: document.getElementById('ruleEnabled').checked,
        priority: priority,
        description: description || null
    };

    var method, apiUrl;
    if (IS_EDIT && RULE_ID) {
        method = 'PUT';
        apiUrl = API_PATH + '/rules/' + RULE_ID;
    } else {
        method = 'POST';
        apiUrl = API_PATH + '/rules';
    }

    apiFetch(apiUrl, {
        method: method,
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
    })
    .then(function(data) {
        window.location.href = PAGE_PATH;
    })
    .catch(function(err) {
        alert('保存规则失败: ' + err.message);
    });
}
