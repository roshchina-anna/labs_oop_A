const { createApp } = Vue;
function clonePoints(points = []) {
    return points.map((point) => ({ x: point.x, y: point.y }));
}

createApp({
    data() {
        return {
            factories: [
                { value: "array", label: "ArrayTabulatedFunctionFactory (массив)" },
                { value: "list", label: "LinkedListTabulatedFunctionFactory (список)" },
            ],
            selectedFactory: "array",
            functionOptions: [],
            results: [],
            savedFunctions: [],
            arrayForm: {
                name: '',
                name: "",
                size: 3,
                points: [
                    { x: "", y: "" },
                    { x: "", y: "" },
                    { x: "", y: "" },
                ],
            },
            functionForm: {
                name: "",
                functionName: "",
                from: "",
                to: "",
                count: 8,
            },
            integralForm: {
                resultId: "",
                from: "",
                to: "",
                threads: 2,
            },
            integralResult: null,
            message: null,
            error: null,
            loading: false,
            saveModal: {
                visible: false,
                source: "local",
                selectedLocalId: "",
                selectedSavedId: "",
                form: {
                    id: null,
                    name: "",
                    points: [
                        { x: "", y: "" },
                        { x: "", y: "" },
                    ],
                },
            },
        };
    },
    computed: {
        currentFactoryLabel() {
            const factory = this.factories.find((f) => f.value === this.selectedFactory);            return factory ? factory.label : this.selectedFactory;
            return factory ? factory.label : this.selectedFactory;
        },
        hasResults() {
            return this.results.length > 0;
        },
        hasSavedFunctions() {
            return this.savedFunctions.length > 0;
        },
    },
    mounted() {
        this.loadFunctions();
        this.loadSavedFunctions();
    },
    methods: {
        scrollTo(anchor) {
            const target = document.getElementById(anchor);
            if (target) {
                target.scrollIntoView({ behavior: "smooth" });
            }
        },
        syncArraySize() {
            const size = Math.max(2, Math.min(500, Number(this.arrayForm.size) || 0));
            this.arrayForm.size = size;
            if (size > this.arrayForm.points.length) {
                while (this.arrayForm.points.length < size) {
                    this.arrayForm.points.push({ x: "", y: "" });
                }
            } else {
                this.arrayForm.points.splice(size);
            }
        },
        addArrayRow() {
            this.arrayForm.points.push({ x: "", y: "" });
            this.arrayForm.size = this.arrayForm.points.length;
        },
        removeArrayRow() {
            if (this.arrayForm.points.length > 2) {
                this.arrayForm.points.pop();
                this.arrayForm.size = this.arrayForm.points.length;
            }
        },
            async loadFunctions() {
                try {
                    const response = await fetch("/ui/tabulated/functions");
                    if (!response.ok) {
                        throw new Error("Не удалось загрузить список функций");
                    }
                }
                this.functionOptions = await response.json();
            } catch (e) {
                this.error = e.message;
                }
            },
            async loadSavedFunctions() {
                try {
                    const response = await fetch("/ui/storage/functions");
                    if (response.status === 401) {
                        return;
                    }
                if (!response.ok) {
                    throw new Error("Не удалось загрузить сохранённые функции");
                }

                this.savedFunctions = await response.json();
            } catch (e) {
                this.error = e.message;
            }
        },
        async createFromTable() {
            this.resetAlerts();
            if (this.arrayForm.points.length < 2) {
                this.error = "Нужно минимум две точки";
                return;
            }
            const payload = {
                xValues: this.arrayForm.points.map((p) => p.x.toString()),
                yValues: this.arrayForm.points.map((p) => p.y.toString()),
                name: this.arrayForm.name,
                factoryType: this.selectedFactory,
            };
            const data = await this.safeRequest(
                "/ui/tabulated/arrays",
                payload,
                "Функция создана из таблицы",
            );
            if (data) {
                this.addResult(data, payload.name || "Таблица точек");
            }
        },
        async createFromFunction() {
            this.resetAlerts();
            const payload = {
                functionName: this.functionForm.functionName,
                from: this.functionForm.from,
                to: this.functionForm.to,
                count: this.functionForm.count?.toString(),
                name: this.functionForm.name,
                factoryType: this.selectedFactory,
            };
            const data = await this.safeRequest(
                "/ui/tabulated/from-function",
                payload,
                "Функция создана из источника",
            );
            if (data) {
                this.addResult(data, payload.name || payload.functionName || "Функция");
            }
        },
        async calculateIntegral() {
            this.resetAlerts();
            const entry = this.results.find((r) => r.id === this.integralForm.resultId);
            if (!entry) {
                this.error = "Выберите функцию для интегрирования";
                return;
            }
            const payload = {
                from: this.integralForm.from,
                to: this.integralForm.to,
                threads: this.integralForm.threads?.toString(),
                factoryType: this.selectedFactory,
                points: entry.points,
            };
            const data = await this.safeRequest(
                "/ui/tabulated/integral",
                payload,
                "Интеграл вычислен",
            );
            if (data) {
                this.integralResult = {
                    label: data.label,
                    value: data.result,
                };

            }
        },
        async safeRequest(url, body, successMessage, method = "POST") {
            this.loading = true;
            try {
                const response = await fetch(url, {
                    method,
                    headers: { "Content-Type": "application/json" },
                    body: method === "GET" ? undefined : JSON.stringify(body),
                });
                const text = await response.text();
                if (!response.ok) {
                    throw new Error(text || "Произошла ошибка при выполнении запроса");                }
                const data = text ? JSON.parse(text) : null;
                if (successMessage) {
                    this.message = successMessage;
                }
                return data;
            } catch (e) {
                this.error = e.message;
                return null;
            } finally {
                this.loading = false;
            }
        },
        addResult(response, title) {
            const entry = {
                id: crypto.randomUUID(),
                title,
                source: response.source,
                points: response.points || [],
            };
            this.results.unshift(entry);
            this.integralForm.resultId = this.integralForm.resultId || entry.id;
        },
        resetAlerts() {
            this.error = null;
            this.message = null;
        },
        openSaveModal(source = "local", entryId = null) {
            this.saveModal.visible = true;
            this.saveModal.source = source;
            if (source === "local") {
                this.saveModal.selectedLocalId = entryId || this.results[0]?.id || "";
            } else {
                this.saveModal.selectedSavedId = entryId || this.savedFunctions[0]?.id || "";
            }
            this.applySelection();
        },
        closeSaveModal() {
            this.saveModal.visible = false;
        },
        switchSource(source) {
            this.saveModal.source = source;
            this.applySelection();
        },
        applySelection() {
            let entry = null;
            if (this.saveModal.source === "local") {
                entry = this.results.find((r) => r.id === this.saveModal.selectedLocalId);
            } else {
                entry = this.savedFunctions.find((r) => r.id === this.saveModal.selectedSavedId);
            }
            if (!entry) {
                this.saveModal.form = {
                    id: null,
                    name: "",
                    points: [
                        { x: "", y: "" },
                        { x: "", y: "" },
                    ],
                };
                return;
            }
            this.saveModal.form = {
                id: entry.id,
                name: entry.title || entry.name || "",
                points: clonePoints(entry.points),
            };
        },
        addEditablePoint() {
            this.saveModal.form.points.push({ x: "", y: "" });
        },
        removeEditablePoint(index) {
            if (this.saveModal.form.points.length <= 2) {
                return;
            }
            this.saveModal.form.points.splice(index, 1);
        },
        async saveEditedFunction() {
            this.resetAlerts();
            const { form } = this.saveModal;
            if (!form.name || form.name.trim().length === 0) {
                this.error = "Введите имя функции";
                return;
            }
            if (form.points.length < 2) {
                this.error = "Нужно минимум две точки";
                return;
            }
            const normalizedPoints = form.points.map((p, index) => {
                const x = Number(p.x);
                const y = Number(p.y);
                if (Number.isNaN(x) || Number.isNaN(y)) {
                    throw new Error(`Точка №${index + 1} должна содержать числовые значения`);
                }
                return { x, y };
            });
            try {
                const payload = {
                    name: form.name,
                    points: normalizedPoints,
                    factoryType: this.selectedFactory,
                };
                const response = await this.safeRequest(
                    "/ui/storage/functions",
                    payload,
                    "Функция сохранена в одно действие",
                );
                if (response) {
                    const existingIndex = this.savedFunctions.findIndex((f) => f.id === response.id);
                    if (existingIndex >= 0) {
                        this.savedFunctions.splice(existingIndex, 1, response);
                    } else {
                        this.savedFunctions.unshift(response);
                    }
                    if (this.saveModal.source === "local") {
                        const localIndex = this.results.findIndex((r) => r.id === form.id);
                        if (localIndex >= 0) {
                            this.results[localIndex].title = form.name;
                            this.results[localIndex].points = clonePoints(normalizedPoints);
                        }
                    }
                    this.closeSaveModal();
                }
            } catch (e) {
                this.error = e.message;
            }
        },
    },
}).mount("#app");