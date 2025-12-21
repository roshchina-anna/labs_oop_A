const MAX_POINTS = 500;
const AUTH_TOKEN_KEY = 'authToken';
const overlay = document.getElementById('overlay');
const arrayModal = document.getElementById('array-modal');
const operationsModal = document.getElementById('operations-modal');
const storageModal = document.getElementById('storage-modal');
const diffModal = document.getElementById('diff-modal');
const integralModal = document.getElementById('integral-modal');
const functionModal = document.getElementById('function-modal');
const errorModal = document.getElementById('error-modal');
const modals = [arrayModal, functionModal, operationsModal, storageModal, diffModal, integralModal, errorModal];

const arraySizeInput = document.getElementById('array-size');
const arrayFunctionNameInput = document.getElementById('array-function-name');
const buildTableButton = document.getElementById('build-table');
const arrayTableBody = document.getElementById('array-table-body');
const submitArrayButton = document.getElementById('submit-array');
const operationFunctionASelect = document.getElementById('operation-function-a');
const operationFunctionBSelect = document.getElementById('operation-function-b');
const operationTypeSelect = document.getElementById('operation-type');
const operationSizeInput = document.getElementById('operation-size');
const operationTableA = document.getElementById('operation-table-a');
const operationTableB = document.getElementById('operation-table-b');
const loadOperationTablesButton = document.getElementById('load-operation-tables');
const buildOperationTablesButton = document.getElementById('build-operation-tables');
const runOperationButton = document.getElementById('run-operation');

const saveFunctionSelect = document.getElementById('save-function-select');
const saveNameInput = document.getElementById('save-name');
const saveFunctionButton = document.getElementById('save-function');
const loadFunctionSelect = document.getElementById('load-function-select');
const loadFunctionButton = document.getElementById('load-function');
const clearSavedButton = document.getElementById('clear-saved');
const exportFunctionSelect = document.getElementById('export-function-select');
const exportNameInput = document.getElementById('export-name');
const exportFormatSelect = document.getElementById('export-format');
const exportDownloadButton = document.getElementById('export-download');
const importFormatSelect = document.getElementById('import-format');
const importFileInput = document.getElementById('import-file');
const importUploadButton = document.getElementById('import-upload');
const editFunctionSelect = document.getElementById('edit-function-select');
const editFunctionNameInput = document.getElementById('edit-function-name');
const editFunctionBody = document.getElementById('edit-function-body');
const loadEditFunctionButton = document.getElementById('load-edit-function');
const addEditPointButton = document.getElementById('add-edit-point');
const removeEditPointButton = document.getElementById('remove-edit-point');
const saveEditFunctionButton = document.getElementById('save-edit-function');

const diffSourceSelect = document.getElementById('diff-source-select');
const diffSizeInput = document.getElementById('diff-size');
const loadDiffTableButton = document.getElementById('load-diff-table');
const buildDiffTableButton = document.getElementById('build-diff-table');
const diffTableBody = document.getElementById('diff-table-body');
const diffResultBody = document.getElementById('diff-result-body');
const runDiffButton = document.getElementById('run-diff');

const integralFunctionSelect = document.getElementById('integral-function-select');
const integralFromInput = document.getElementById('integral-from');
const integralToInput = document.getElementById('integral-to');
const integralThreadsInput = document.getElementById('integral-threads');
const runIntegralButton = document.getElementById('run-integral');
const integralResult = document.getElementById('integral-result');

const functionSelect = document.getElementById('function-select');
const functionEntryNameInput = document.getElementById('function-entry-name');
const fromInput = document.getElementById('from-value');
const toInput = document.getElementById('to-value');
const functionCountInput = document.getElementById('function-count');
const submitFunctionButton = document.getElementById('submit-function');

const resultContainer = document.getElementById('result-container');
const errorMessage = document.getElementById('error-message');
const factorySelect = document.getElementById('factory-select');
const factoryLabel = document.getElementById('factory-label');
const factoryChip = document.getElementById('factory-chip');
const factoryBadges = document.querySelectorAll('.factory-name');
const authScreen = document.getElementById('auth-screen');
const appShell = document.getElementById('app-shell');
const userLabel = document.getElementById('user-label');
const openLoginButton = document.getElementById('open-login');
const openRegisterButton = document.getElementById('open-register');
const logoutButton = document.getElementById('logout');
const loginUsernameInput = document.getElementById('login-username');
const loginPasswordInput = document.getElementById('login-password');
const loginSubmit = document.getElementById('login-submit');
const registerUsernameInput = document.getElementById('register-username');
const registerPasswordInput = document.getElementById('register-password');
const registerSubmit = document.getElementById('register-submit');

let selectedFactory = localStorage.getItem('factoryType') || 'array';
let savedFunctions = [];
let entryCounter = 0;
const functionLibrary = [];
let currentUser = null;

factorySelect.addEventListener('change', (event) => {
    setFactory(event.target.value);
});
// Основные кнопки
['open-array-modal', 'open-array-from-nav'].forEach(id => {
    document.getElementById(id)?.addEventListener('click', () => openModal(arrayModal));
});
['open-function-modal', 'open-function-from-nav'].forEach(id => {
    document.getElementById(id)?.addEventListener('click', () => openModal(functionModal));
});
['open-operations-modal', 'open-operations-from-nav'].forEach(id => {
    document.getElementById(id)?.addEventListener('click', () => openModal(operationsModal));
});
['open-storage-modal'].forEach(id => {
    document.getElementById(id)?.addEventListener('click', () => openModal(storageModal));
});
['open-diff-modal', 'open-diff-from-nav'].forEach(id => {
    document.getElementById(id)?.addEventListener('click', () => openModal(diffModal));
});
['open-integral-from-nav'].forEach(id => {
    document.getElementById(id)?.addEventListener('click', () => openModal(integralModal));
});
factorySelect.addEventListener('change', (event) => setFactory(event.target.value));
document.querySelectorAll('[data-close]').forEach(button => button.addEventListener('click', closeAllModals));
openLoginButton.addEventListener('click', () => showAuthPanels('login'));
openRegisterButton.addEventListener('click', () => showAuthPanels('register'));
logoutButton.addEventListener('click', () => {
    clearAuth();
    savedFunctions = [];
    refreshSavedDropdown();
    refreshFunctionDropdowns();
    updateAuthUI();
});
loginSubmit.addEventListener('click', handleLogin);
registerSubmit.addEventListener('click', handleRegister);
buildTableButton.addEventListener('click', () => {
    try {
            const size = parseSize(arraySizeInput.value);
            renderTableBody(arrayTableBody, size);
        } catch (e) {
            showError(e.message);
    }
});

submitArrayButton.addEventListener('click', async () => {
    if (arrayTableBody.children.length === 0) {
        return showError('Постройте таблицу перед созданием функции.');
    }
    const {xValues, yValues} = collectTableValuesFromBody(arrayTableBody);
    if (!xValues.length || !yValues.length) {
        return showError('Заполните значения x и y.');
    }
    await sendRequest('/ui/tabulated/arrays', {
            xValues,
            yValues,
            name: arrayFunctionNameInput.value.trim()
        }, arrayModal);
});

submitFunctionButton.addEventListener('click', async () => {
    const body = {
        functionName: functionSelect.value,
        from: fromInput.value.trim(),
        to: toInput.value.trim(),
        count: functionCountInput.value.trim(),
        name: functionEntryNameInput.value.trim()
    };
    await sendRequest('/ui/tabulated/from-function', body, functionModal);
});
// Операции над функциями
buildOperationTablesButton.addEventListener('click', () => {
    try {
        const size = parseSize(operationSizeInput.value);
        renderTableBody(operationTableA, size);
        renderTableBody(operationTableB, size);
    } catch (e) {
        showError(e.message);
    }
});

loadOperationTablesButton.addEventListener('click', () => {
    const entryA = resolveEntry(operationFunctionASelect.value);
    const entryB = resolveEntry(operationFunctionBSelect.value);
    if (!entryA || !entryB) {
        return showError('Выберите две функции для загрузки.');
    }
    renderTableBody(operationTableA, entryA.points.length, entryA.points);
    renderTableBody(operationTableB, entryB.points.length, entryB.points);
});

runOperationButton.addEventListener('click', () => {
    if (operationTableA.children.length === 0 || operationTableB.children.length === 0) {
        return showError('Заполните обе таблицы перед вычислением.');
    }
    try {
        const pointsA = buildPointsFromBody(operationTableA, 'A');
        const pointsB = buildPointsFromBody(operationTableB, 'B');
        if (pointsA.length !== pointsB.length) {
            throw new Error('Количество точек в таблицах должно совпадать.');
        }
        const op = operationTypeSelect.value;
        const labelA = getOptionLabel(operationFunctionASelect) || 'Таблица A';
        const labelB = getOptionLabel(operationFunctionBSelect) || 'Таблица B';
        const symbol = operationTypeSelect.selectedOptions[0]?.textContent || 'операция';
        const resultPoints = pointsA.map((point, idx) => {
            const other = pointsB[idx];
            if (Math.abs(point.x - other.x) > 1e-9) {
                throw new Error('Значения x должны совпадать для поэлементной операции.');
            }
            let y;
            switch (op) {
                case 'add':
                    y = point.y + other.y;
                    break;
                case 'subtract':
                    y = point.y - other.y;
                    break;
                case 'multiply':
                    y = point.y * other.y;
                    break;
                case 'divide':
                    if (other.y === 0) {
                        throw new Error(`Деление на ноль в строке ${idx + 1}`);
                    }
                    y = point.y / other.y;
                    break;
                default:
                    y = point.y;
            }
            return {x: point.x, y};
        });
        addResultCard({source: `${labelA} · ${symbol}`, points: resultPoints});
        closeModal(operationsModal);
    } catch (e) {
        showError(e.message);
    }
});

// Сохранение и загрузка
saveFunctionButton.addEventListener('click', async () => {
    const entry = resolveEntry(saveFunctionSelect.value);
    const name = saveNameInput.value.trim();
    if (!entry) {
        return showError('Выберите функцию для сохранения.');
    }
    if (!name) {
        return showError('Введите название сохранения.');
    }
    if (!currentUser) {
            return showError('Войдите в систему, чтобы сохранять функции.');
        }
        try {
            const response = await authorizedFetch('/ui/storage/functions', {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({name, points: entry.points})
            });
            if (!response.ok) {
                const data = await response.json().catch(() => ({error: 'Не удалось сохранить функцию'}));
                return showError(data.error || 'Не удалось сохранить функцию');
            }
            const saved = await response.json();
            savedFunctions = savedFunctions.filter(f => f.id !== saved.id && f.name !== saved.name);
            savedFunctions.push(saved);
            refreshSavedDropdown();
            refreshFunctionDropdowns();
        } catch (e) {
            showError('Ошибка сохранения функции.');
        }
});

loadFunctionButton.addEventListener('click', () => {
    const entry = savedFunctions.find(f => String(f.id) === loadFunctionSelect.value);
    if (!entry) {
        return showError('Выберите сохранение для загрузки.');
    }
    addResultCard({source: `Загружено: ${entry.name}`, points: entry.points});
});
exportDownloadButton.addEventListener('click', async () => {
    const entry = resolveEntry(exportFunctionSelect.value);
    if (!entry) {
        return showError('Выберите функцию для выгрузки.');
    }
    const body = {
        points: entry.points,
        name: exportNameInput.value.trim() || 'function',
        format: exportFormatSelect.value,
        factoryType: selectedFactory
    };
    try {
        const response = await authorizedFetch('/ui/storage/export', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(body)
        });
        if (!response.ok) {
            const data = await response.json().catch(() => ({error: 'Не удалось скачать файл'}));
            return showError(data.error || 'Не удалось скачать файл');
        }
        const blob = await response.blob();
        const extension = exportFormatSelect.value || 'json';
        downloadFile(blob, `${body.name}.${extension}`);
    } catch (e) {
        showError('Ошибка при выгрузке файла.');
    }
});

importUploadButton.addEventListener('click', async () => {
    const file = importFileInput.files?.[0];
    if (!file) {
        return showError('Выберите файл для импорта.');
    }
    const format = importFormatSelect.value;
    try {
        const content = await file.text();
        const response = await authorizedFetch(`/ui/storage/import?format=${format}&factoryType=${selectedFactory}`, {
            method: 'POST',
            headers: {'Content-Type': 'text/plain'},
            body: content
        });
        if (!response.ok) {
            const data = await response.json().catch(() => ({error: 'Не удалось импортировать файл'}));
            return showError(data.error || 'Не удалось импортировать файл');
        }
        const data = await response.json();
        addResultCard(data);
        closeModal(storageModal);
    } catch (e) {
        showError('Ошибка при чтении файла.');
    }
});

clearSavedButton.addEventListener('click', async () => {
    if (!savedFunctions.length) {
        return;
    }
    if (!confirm('Удалить все сохранения?')) {
        return;
    }
    try {
        const response = await authorizedFetch('/ui/storage/functions', {method: 'DELETE'});
        if (!response.ok) {
            const data = await response.json().catch(() => ({error: 'Не удалось очистить сохранения'}));
            return showError(data.error || 'Не удалось очистить сохранения');
            }
        savedFunctions = [];
        refreshSavedDropdown();
        refreshFunctionDropdowns();
    } catch (e) {
        showError('Ошибка при очистке сохранений.');
    }
});

loadEditFunctionButton?.addEventListener('click', () => {
    const entry = savedFunctions.find(f => String(f.id) === editFunctionSelect.value);
    if (!entry) {
        return showError('Выберите сохранённую функцию для редактирования.');
    }
    editFunctionNameInput.value = entry.name;
    renderTableBody(editFunctionBody, entry.points.length, entry.points);
});

addEditPointButton?.addEventListener('click', () => {
    const nextIndex = editFunctionBody.children.length;
    appendPointRow(editFunctionBody, nextIndex);
});

removeEditPointButton?.addEventListener('click', () => {
    const rows = editFunctionBody.children;
    if (rows.length <= 2) {
        return showError('Нельзя удалять: нужно минимум две точки.');
    }
    editFunctionBody.removeChild(rows[rows.length - 1]);
});

saveEditFunctionButton?.addEventListener('click', async () => {
    const selectedId = editFunctionSelect.value;
    const entry = savedFunctions.find(f => String(f.id) === selectedId);
    if (!entry) {
        return showError('Выберите сохранённую функцию для сохранения изменений.');
    }
    const name = editFunctionNameInput.value.trim();
    if (!name) {
        return showError('Введите новое имя функции.');
    }
    if (editFunctionBody.children.length < 2) {
        return showError('Нужно минимум две точки.');
    }
    try {
        const points = buildPointsFromBody(editFunctionBody, 'редактирования');
        const response = await authorizedFetch('/ui/storage/functions', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({name, points})
        });
        if (!response.ok) {
            const data = await response.json().catch(() => ({error: 'Не удалось сохранить изменения'}));
            return showError(data.error || 'Не удалось сохранить изменения');
        }
        const saved = await response.json();
        savedFunctions = savedFunctions.filter(f => f.id !== saved.id && f.name !== saved.name);
        savedFunctions.push(saved);
        refreshSavedDropdown();
        refreshFunctionDropdowns();
        editFunctionSelect.value = String(saved.id);
        alert('Изменения сохранены.');
    } catch (e) {
        showError('Ошибка при сохранении изменений.');
    }
});

// Дифференцирование
buildDiffTableButton.addEventListener('click', () => {
    try {
        const size = parseSize(diffSizeInput.value);
        renderTableBody(diffTableBody, size);
        diffResultBody.innerHTML = '';
    } catch (e) {
        showError(e.message);
    }
});

loadDiffTableButton.addEventListener('click', () => {
    const entry = resolveEntry(diffSourceSelect.value);
    if (!entry) {
        return showError('Выберите функцию для загрузки.');
    }
    renderTableBody(diffTableBody, entry.points.length, entry.points);
    diffResultBody.innerHTML = '';
});

runDiffButton.addEventListener('click', () => {
    if (diffTableBody.children.length === 0) {
        return showError('Постройте или загрузите таблицу перед вычислением.');
    }
    try {
        const points = buildPointsFromBody(diffTableBody, 'f(x)');
        if (points.length < 2) {
            throw new Error('Нужно минимум две точки для вычисления производной.');
        }
        const derivatives = calculateDerivative(points);
        renderResultTable(diffResultBody, derivatives);
        const label = getOptionLabel(diffSourceSelect) || 'Производная функции';
        addResultCard({source: `Производная: ${label}`, points: derivatives});
    } catch (e) {
        showError(e.message);
    }
});
runIntegralButton.addEventListener('click', async () => {
    const entry = resolveEntry(integralFunctionSelect.value);
    if (!entry) {
        return showError('Выберите функцию для интегрирования.');
    }
    const body = {
        points: entry.points,
        from: integralFromInput.value.trim(),
        to: integralToInput.value.trim(),
        threads: integralThreadsInput.value.trim(),
        factoryType: selectedFactory
    };
    try {
        const response = await authorizedFetch('/ui/tabulated/integral', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(body)
        });
        if (!response.ok) {
            const data = await response.json().catch(() => ({error: 'Не удалось вычислить интеграл'}));
            return showError(data.error || 'Не удалось вычислить интеграл');
        }
        const data = await response.json();
        integralResult.textContent = `Результат: ${data.result.toFixed(6)}`;
        addResultCard({source: data.source, result: data.result});
        closeModal(integralModal);
    } catch (e) {
        showError('Ошибка вычисления интеграла.');
    }
});
async function sendRequest(url, body, modalToClose) {
    if (!currentUser) {
            showAuthPanels('login');
            return;
        }
    try {
        const payload = {...body, factoryType: selectedFactory};
        const response = await authorizedFetch(url, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(payload)
        });
        if (!response.ok) {
            const data = await response.json().catch(() => ({error: 'Ошибка обработки запроса'}));
            return showError(data.error || 'Неизвестная ошибка');
        }
        const data = await response.json();
        const customName = body?.name?.trim();
        if (customName) {
        data.source = customName;
        }
        addResultCard(data);
        closeModal(modalToClose);
    } catch (e) {
        showError('Не удалось выполнить запрос. Проверьте соединение.');
    }
}

function renderTableBody(tbody, size, points = []) {
    tbody.innerHTML = '';
    for (let i = 0; i < size; i++) {
        appendPointRow(tbody, i, points[i]);
    }
}
function appendPointRow(tbody, index, point) {
    const row = document.createElement('tr');
    const xCell = document.createElement('td');
    const yCell = document.createElement('td');

    const xInput = document.createElement('input');
    xInput.placeholder = `x${index + 1}`;
    const yInput = document.createElement('input');
    yInput.placeholder = `y${index + 1}`;
    if (point) {
        xInput.value = point.x;
        yInput.value = point.y;
    }
    xCell.appendChild(xInput);
    yCell.appendChild(yInput);
    row.appendChild(xCell);
    row.appendChild(yCell);
    tbody.appendChild(row);
}
async function handleLogin() {
    const username = loginUsernameInput.value.trim();
    const password = loginPasswordInput.value;
    if (!username || !password) {
        return showError('Введите логин и пароль.');
    }
    const token = btoa(`${username}:${password}`);
    try {
        const response = await authorizedFetch('/api/users/me', {
            headers: {Authorization: `Basic ${token}`}
        });
        if (!response.ok) {
            return showError('Неверные данные для входа.');
        }
        currentUser = await response.json();
        setAuthToken(token);
        updateAuthUI();
        await loadFunctions();
        await loadSavedFunctions();
    } catch (e) {
        showError('Не удалось выполнить вход.');
    }
}

async function handleRegister() {
    const username = registerUsernameInput.value.trim();
    const password = registerPasswordInput.value;
    if (!username || !password) {
        return showError('Заполните логин и пароль.');
    }
    try {
        const response = await fetch('/api/auth/register', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({username, password})
        });
        if (!response.ok) {
            const data = await response.json().catch(() => ({error: 'Не удалось зарегистрироваться.'}));
            return showError(data.error || 'Не удалось зарегистрироваться.');
        }
        const token = btoa(`${username}:${password}`);
        const meResponse = await authorizedFetch('/api/users/me', {
            headers: {Authorization: `Basic ${token}`}
        });
        if (meResponse.ok) {
            currentUser = await meResponse.json();
            setAuthToken(token);
            updateAuthUI();
            await loadFunctions();
            await loadSavedFunctions();
        }
    } catch (e) {
        showError('Ошибка при регистрации.');
    }
}

function showAuthPanels(mode) {
    if (mode === 'login') {
        loginUsernameInput?.focus();
    }
    if (mode === 'register') {
        registerUsernameInput?.focus();
    }
    authScreen.classList.remove('hidden');
    appShell.classList.add('locked');
}

function getAuthToken() {
    return localStorage.getItem(AUTH_TOKEN_KEY);
}

function setAuthToken(token) {
    localStorage.setItem(AUTH_TOKEN_KEY, token);
}

function clearAuth() {
    localStorage.removeItem(AUTH_TOKEN_KEY);
    currentUser = null;
}

async function hydrateAuth() {
    const token = getAuthToken();
    if (!token) {
        updateAuthUI();
        showAuthPanels('login');
        return;
    }
    try {
        const response = await authorizedFetch('/api/users/me', {
            headers: {Authorization: `Basic ${token}`}
        });
        if (!response.ok) {
            clearAuth();
        } else {
            currentUser = await response.json();
        }
    } catch (e) {
        clearAuth();
    }
    updateAuthUI();
}

function updateAuthUI() {
    const authenticated = !!currentUser;
    userLabel.textContent = authenticated ? currentUser.username : 'Гость';
    logoutButton.classList.toggle('hidden', !authenticated);
    openLoginButton.classList.toggle('hidden', authenticated);
    openRegisterButton.classList.toggle('hidden', authenticated);
    authScreen.classList.toggle('hidden', authenticated);
    appShell.classList.toggle('locked', !authenticated);
    if (!authenticated) {
        closeAllModals();
    }
}

async function authorizedFetch(url, options = {}) {
    const token = getAuthToken();
    const headers = {...options.headers};
    if (token) {
        headers['Authorization'] = `Basic ${token}`;
    }
    const response = await fetch(url, {...options, headers});
    if (response.status === 401) {
        clearAuth();
        updateAuthUI();
        showAuthPanels('login');
    }
    return response;
}
function collectTableValuesFromBody(tbody) {
    const xValues = [];
    const yValues = [];
    tbody.querySelectorAll('tr').forEach(row => {
        const inputs = row.querySelectorAll('input');
        xValues.push(inputs[0].value.trim());
        yValues.push(inputs[1].value.trim());
    });
    return {xValues, yValues};
}
function buildPointsFromBody(tbody, labelPrefix) {
    const {xValues, yValues} = collectTableValuesFromBody(tbody);
    if (xValues.length !== yValues.length) {
        throw new Error('Количество значений x и y должно совпадать.');
    }
    return xValues.map((xRaw, idx) => {
        const x = parseNumber(xRaw, `Значение ${labelPrefix} x${idx + 1} должно быть числом`);
        const y = parseNumber(yValues[idx], `Значение ${labelPrefix} y${idx + 1} должно быть числом`);
        return {x, y};
    });
}

function renderResultTable(tbody, points) {
    tbody.innerHTML = '';
    points.forEach(point => {
        const row = document.createElement('tr');
        const xCell = document.createElement('td');
        xCell.textContent = point.x.toFixed(4);
        const yCell = document.createElement('td');
        yCell.textContent = point.y.toFixed(4);
        row.appendChild(xCell);
        row.appendChild(yCell);
        tbody.appendChild(row);
    });
}

function calculateDerivative(points) {
    const derivatives = [];
    for (let i = 0; i < points.length; i++) {
        let slope;
        if (i === 0) {
            slope = (points[i + 1].y - points[i].y) / (points[i + 1].x - points[i].x);
        } else if (i === points.length - 1) {
            slope = (points[i].y - points[i - 1].y) / (points[i].x - points[i - 1].x);
        } else {
            slope = (points[i + 1].y - points[i - 1].y) / (points[i + 1].x - points[i - 1].x);
        }
        derivatives.push({x: points[i].x, y: slope});
    }
    return derivatives;
}

function openModal(modal) {
    modals.forEach(m => m.classList.add('hidden'));
    modal.classList.remove('hidden');
    overlay.classList.remove('hidden');
    syncFactoryBadges();
    refreshFunctionDropdowns();
    refreshSavedDropdown();
}

function closeModal(modal) {
    modal.classList.add('hidden');
    overlay.classList.add('hidden');
}

function closeAllModals() {
    modals.forEach(m => m.classList.add('hidden'));
    overlay.classList.add('hidden');
}

function showError(message) {
    errorMessage.textContent = message;
    openModal(errorModal);
}

function addResultCard(data) {
    const {source, points, result} = data;
        if ((!Array.isArray(points) || points.length === 0) && typeof result !== 'number') {
        return;
    }
    const card = document.createElement('div');
    card.className = 'card';

    const title = document.createElement('div');
    title.className = 'tag';
    title.textContent = source || 'Результат';
    card.appendChild(title);
    if (Array.isArray(points) && points.length) {
            const entry = registerFunction(source || 'Табулированная функция', points);
            const meta = document.createElement('div');
            meta.className = 'meta';
            const xs = points.map(p => p.x);
            const minX = Math.min(...xs);
            const maxX = Math.max(...xs);
            meta.innerHTML = `Точек: ${points.length}<br>Интервал: [${minX.toFixed(2)}; ${maxX.toFixed(2)}]`;

            const sampleList = document.createElement('div');
            sampleList.className = 'meta';
            const previewCount = Math.min(points.length, 5);
            const lines = points.slice(0, previewCount)
                .map((p, idx) => `#${idx + 1}: (${p.x.toFixed(2)}; ${p.y.toFixed(2)})`)
                .join('<br>');
            sampleList.innerHTML = `Первые значения:<br>${lines}`;

            card.appendChild(meta);
            card.appendChild(sampleList);
        }

        if (typeof result === 'number') {
            const valueRow = document.createElement('div');
            valueRow.className = 'meta';
            valueRow.textContent = `Значение: ${result.toFixed(6)}`;
            card.appendChild(valueRow);
        }
    resultContainer.prepend(card);
}
function registerFunction(source, points) {
    const entry = {
        id: `fn-${Date.now()}-${entryCounter++}`,
        source,
        points
    };
    functionLibrary.unshift(entry);
    refreshFunctionDropdowns();
    return entry;
}

async function loadFunctions() {
    try {
        const response = await authorizedFetch('/ui/tabulated/functions');
        const names = await response.json();
        functionSelect.innerHTML = '';
        names.forEach(name => {
            const option = document.createElement('option');
            option.value = name;
            option.textContent = name;
            functionSelect.appendChild(option);
        });
    } catch (e) {
        showError('Не удалось загрузить список функций.');
    }
}

function setFactory(type) {
    selectedFactory = type;
    localStorage.setItem('factoryType', selectedFactory);
    factorySelect.value = selectedFactory;
    syncFactoryBadges();
}

function syncFactoryBadges() {
    const name = selectedFactory === 'list'
        ? 'LinkedListTabulatedFunctionFactory'
        : 'ArrayTabulatedFunctionFactory';
    factoryBadges.forEach(span => span.textContent = name);
    factoryLabel.textContent = name;
    factoryChip.textContent = selectedFactory === 'list' ? 'список' : 'массив';
}

function refreshFunctionDropdowns() {
    const selects = [operationFunctionASelect, operationFunctionBSelect, diffSourceSelect, saveFunctionSelect, integralFunctionSelect, exportFunctionSelect];
    const options = getAvailableSources();
    selects.forEach(select => {
        if (!select) return;
        select.innerHTML = '';
        if (!options.length) {
            const opt = document.createElement('option');
            opt.textContent = 'Нет доступных функций';
            opt.disabled = true;
            opt.selected = true;
            select.appendChild(opt);
            select.disabled = true;
        } else {
            options.forEach(entry => {
                const opt = document.createElement('option');
                opt.value = entry.id;
                opt.textContent = entry.source;
                select.appendChild(opt);
            });
            select.disabled = false;
        }
    });
}

function refreshSavedDropdown() {
    loadFunctionSelect.innerHTML = '';
    editFunctionSelect.innerHTML = '';
    if (!savedFunctions.length) {
        const opt = document.createElement('option');
        opt.textContent = 'Нет сохранений';
        opt.disabled = true;
        opt.selected = true;
        loadFunctionSelect.appendChild(opt);
        loadFunctionSelect.disabled = true;
        const editOpt = opt.cloneNode(true);
        editFunctionSelect.appendChild(editOpt);
        editFunctionSelect.disabled = true;
    } else {
        savedFunctions.forEach(entry => {
            const opt = document.createElement('option');
            opt.value = entry.id;
            opt.textContent = entry.name;
            loadFunctionSelect.appendChild(opt);
            const editOpt = opt.cloneNode(true);
            editFunctionSelect.appendChild(editOpt);
        });
        loadFunctionSelect.disabled = false;
        editFunctionSelect.disabled = false;
    }
}

function getAvailableSources() {
    const savedEntries = savedFunctions.map(item => ({
        id: String(item.id),
        source: `Сохранение: ${item.name}`,
        points: item.points
    }));
    return [...functionLibrary, ...savedEntries];
}

function resolveEntry(id) {
    if (!id) return null;
    return getAvailableSources().find(entry => entry.id === id) || null;
}

function parseSize(raw) {
    if (!raw || !raw.trim()) {
        throw new Error('Введите количество точек.');
    }
    const size = Number(raw);
    if (!Number.isFinite(size)) {
        throw new Error('Размер таблицы должен быть числом.');
    }
    if (!Number.isInteger(size)) {
        throw new Error('Размер таблицы должен быть целым числом.');
    }
    if (size < 2) {
        throw new Error('Нужно минимум две точки.');
    }
    if (size > MAX_POINTS) {
        throw new Error(`Слишком большое значение. Максимум точек: ${MAX_POINTS}.`);
    }
    return size;
}

function parseNumber(value, errorMessage) {
    const parsed = Number(value);
    if (!Number.isFinite(parsed)) {
        throw new Error(errorMessage);
    }
    return parsed;
}

function getOptionLabel(select) {
    return select?.selectedOptions?.[0]?.textContent || '';
}

async function loadSavedFunctions() {
    if (!currentUser) {
        savedFunctions = [];
        refreshSavedDropdown();
        refreshFunctionDropdowns();
        return;
    }
    try {
        const response = await authorizedFetch('/ui/storage/functions');
        if (!response.ok) {
            const data = await response.json().catch(() => ({error: 'Не удалось загрузить сохранения'}));
            showError(data.error || 'Не удалось загрузить сохранения');
            return;
        }
        const data = await response.json();
        if (Array.isArray(data)) {
            savedFunctions = data;
        } else {
            savedFunctions = [];
        }
    } catch (e) {
        savedFunctions = [];
    }
    refreshSavedDropdown();
    refreshFunctionDropdowns();
}

function downloadFile(blob, filename) {
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    link.remove();
    URL.revokeObjectURL(url);
}

setFactory(selectedFactory);
refreshSavedDropdown();
refreshFunctionDropdowns();
hydrateAuth().then(() => {
    if (currentUser) {
        loadFunctions();
    }
    loadSavedFunctions();
});