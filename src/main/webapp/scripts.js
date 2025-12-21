const MAX_POINTS = 500;
const STORAGE_KEY = 'tabulated_saved_functions';
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
let selectedFactory = localStorage.getItem('factoryType') || 'array';
let savedFunctions = [];
let entryCounter = 0;
const functionLibrary = [];

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
    await sendRequest('/ui/tabulated/arrays', {xValues, yValues}, arrayModal);
});

submitFunctionButton.addEventListener('click', async () => {
    const body = {
        functionName: functionSelect.value,
        from: fromInput.value.trim(),
        to: toInput.value.trim(),
        count: functionCountInput.value.trim()
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
saveFunctionButton.addEventListener('click', () => {
    const entry = resolveEntry(saveFunctionSelect.value);
    const name = saveNameInput.value.trim();
    if (!entry) {
        return showError('Выберите функцию для сохранения.');
    }
    if (!name) {
        return showError('Введите название сохранения.');
    }
    const payload = {id: `saved-${Date.now()}`, name, points: entry.points};
    savedFunctions = savedFunctions.filter(f => f.name !== name);
    savedFunctions.push(payload);
    persistSaved();
    refreshSavedDropdown();
});

loadFunctionButton.addEventListener('click', () => {
    const entry = savedFunctions.find(f => f.id === loadFunctionSelect.value);
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
        const response = await fetch('/ui/storage/export', {
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
        const response = await fetch(`/ui/storage/import?format=${format}&factoryType=${selectedFactory}`, {
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

clearSavedButton.addEventListener('click', () => {
    if (!savedFunctions.length) {
        return;
    }
    if (confirm('Удалить все сохранения?')) {
        savedFunctions = [];
        persistSaved();
        refreshSavedDropdown();
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
        const response = await fetch('/ui/tabulated/integral', {
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
    try {
        const payload = {...body, factoryType: selectedFactory};
        const response = await fetch(url, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(payload)
        });
        if (!response.ok) {
            const data = await response.json().catch(() => ({error: 'Ошибка обработки запроса'}));
            return showError(data.error || 'Неизвестная ошибка');
        }
        const data = await response.json();
        addResultCard(data);
        closeModal(modalToClose);
    } catch (e) {
        showError('Не удалось выполнить запрос. Проверьте соединение.');
    }
}

function renderTableBody(tbody, size, points = []) {
    tbody.innerHTML = '';
    for (let i = 0; i < size; i++) {
        const row = document.createElement('tr');
        const xCell = document.createElement('td');
        const yCell = document.createElement('td');

        const xInput = document.createElement('input');
        xInput.placeholder = `x${i + 1}`;
        const yInput = document.createElement('input');
        yInput.placeholder = `y${i + 1}`;
        if (points[i]) {
                    xInput.value = points[i].x;
                    yInput.value = points[i].y;
                }
        xCell.appendChild(xInput);
        yCell.appendChild(yInput);
        row.appendChild(xCell);
        row.appendChild(yCell);
        tbody.appendChild(row);
    }
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
        const response = await fetch('/ui/tabulated/functions');
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
    if (!savedFunctions.length) {
        const opt = document.createElement('option');
        opt.textContent = 'Нет сохранений';
        opt.disabled = true;
        opt.selected = true;
        loadFunctionSelect.appendChild(opt);
        loadFunctionSelect.disabled = true;
    } else {
        savedFunctions.forEach(entry => {
            const opt = document.createElement('option');
            opt.value = entry.id;
            opt.textContent = entry.name;
            loadFunctionSelect.appendChild(opt);
        });
        loadFunctionSelect.disabled = false;
    }
}

function getAvailableSources() {
    const savedEntries = savedFunctions.map(item => ({
        id: item.id,
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

function persistSaved() {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(savedFunctions));
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

function hydrateSaved() {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) {
        savedFunctions = [];
        return;
    }
    try {
        const parsed = JSON.parse(raw);
        if (Array.isArray(parsed)) {
            savedFunctions = parsed;
        }
    } catch (e) {
        savedFunctions = [];
    }
}

setFactory(selectedFactory);
hydrateSaved();
refreshSavedDropdown();
refreshFunctionDropdowns();
loadFunctions();