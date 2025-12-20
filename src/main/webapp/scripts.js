const MAX_POINTS = 500;

const overlay = document.getElementById('overlay');
const arrayModal = document.getElementById('array-modal');
const functionModal = document.getElementById('function-modal');
const errorModal = document.getElementById('error-modal');
const modals = [arrayModal, functionModal, errorModal];

const arraySizeInput = document.getElementById('array-size');
const buildTableButton = document.getElementById('build-table');
const arrayTableBody = document.getElementById('array-table-body');
const submitArrayButton = document.getElementById('submit-array');

const functionSelect = document.getElementById('function-select');
const fromInput = document.getElementById('from-value');
const toInput = document.getElementById('to-value');
const functionCountInput = document.getElementById('function-count');
const submitFunctionButton = document.getElementById('submit-function');

const resultContainer = document.getElementById('result-container');
const errorMessage = document.getElementById('error-message');

document.getElementById('open-array-modal').addEventListener('click', () => openModal(arrayModal));
document.getElementById('open-function-modal').addEventListener('click', () => openModal(functionModal));

document.querySelectorAll('[data-close]').forEach(button => {
    button.addEventListener('click', closeAllModals);
});

buildTableButton.addEventListener('click', () => {
    const sizeText = arraySizeInput.value.trim();
    if (!sizeText) {
        return showError('Введите количество точек для таблицы.');
    }
    const size = Number(sizeText);
    if (!Number.isFinite(size)) {
        return showError('Размер таблицы должен быть числом.');
    }
    if (!Number.isInteger(size)) {
        return showError('Размер таблицы должен быть целым числом.');
    }
    if (size < 2) {
        return showError('Нужно минимум две точки.');
    }
    if (size > MAX_POINTS) {
        return showError(`Слишком большое значение. Максимум точек: ${MAX_POINTS}.`);
    }
    if (arrayTableBody.children.length > 0 && !confirm('Текущие значения будут очищены. Продолжить?')) {
        return;
    }
    renderTable(size);
});

submitArrayButton.addEventListener('click', async () => {
    if (arrayTableBody.children.length === 0) {
        return showError('Постройте таблицу перед созданием функции.');
    }
    const {xValues, yValues} = collectTableValues();
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

async function sendRequest(url, body, modalToClose) {
    try {
        const response = await fetch(url, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(body)
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

function renderTable(size) {
    arrayTableBody.innerHTML = '';
    for (let i = 0; i < size; i++) {
        const row = document.createElement('tr');
        const xCell = document.createElement('td');
        const yCell = document.createElement('td');

        const xInput = document.createElement('input');
        xInput.placeholder = `x${i + 1}`;
        const yInput = document.createElement('input');
        yInput.placeholder = `y${i + 1}`;

        xCell.appendChild(xInput);
        yCell.appendChild(yInput);
        row.appendChild(xCell);
        row.appendChild(yCell);
        arrayTableBody.appendChild(row);
    }
}

function collectTableValues() {
    const xValues = [];
    const yValues = [];
    arrayTableBody.querySelectorAll('tr').forEach(row => {
        const inputs = row.querySelectorAll('input');
        xValues.push(inputs[0].value.trim());
        yValues.push(inputs[1].value.trim());
    });
    return {xValues, yValues};
}

function openModal(modal) {
    modals.forEach(m => m.classList.add('hidden'));
    modal.classList.remove('hidden');
    overlay.classList.remove('hidden');
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
    const {source, points} = data;
    if (!Array.isArray(points) || points.length === 0) {
        return;
    }
    const card = document.createElement('div');
    card.className = 'card';

    const title = document.createElement('div');
    title.className = 'tag';
    title.textContent = source || 'Табулированная функция';

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

    card.appendChild(title);
    card.appendChild(meta);
    card.appendChild(sampleList);

    resultContainer.prepend(card);
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

loadFunctions();