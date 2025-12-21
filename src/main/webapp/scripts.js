const { createApp } = Vue;

createApp({
    data() {
        return {
            factories: [
                { value: 'array', label: 'ArrayTabulatedFunctionFactory (массив)' },
                { value: 'list', label: 'LinkedListTabulatedFunctionFactory (список)' }
            ],
            selectedFactory: 'array',
            functionOptions: [],
            results: [],
            arrayForm: {
                name: '',
                size: 3,
                points: [
                    { x: '', y: '' },
                    { x: '', y: '' },
                    { x: '', y: '' }
                ]
            },
            functionForm: {
                name: '',
                functionName: '',
                from: '',
                to: '',
                count: 8
            },
            integralForm: {
                resultId: '',
                from: '',
                to: '',
                threads: 2
            },
            integralResult: null,
            message: null,
            error: null,
            loading: false
        };
    },
    computed: {
        currentFactoryLabel() {
            const factory = this.factories.find(f => f.value === this.selectedFactory);
            return factory ? factory.label : this.selectedFactory;
        }
    },
    mounted() {
        this.loadFunctions();
    },
    methods: {
        scrollTo(anchor) {
            const target = document.getElementById(anchor);
            if (target) {
                target.scrollIntoView({ behavior: 'smooth' });
            }
            },
            syncArraySize() {
                const size = Math.max(2, Math.min(500, Number(this.arrayForm.size) || 0));
                this.arrayForm.size = size;
                if (size > this.arrayForm.points.length) {
                    while (this.arrayForm.points.length < size) {
                        this.arrayForm.points.push({ x: '', y: '' });
                    }
                } else {
                    this.arrayForm.points.splice(size);
                }
        },
        addArrayRow() {
            this.arrayForm.points.push({ x: '', y: '' });
            this.arrayForm.size = this.arrayForm.points.length;
        },
        removeArrayRow() {
            if (this.arrayForm.points.length > 2) {
                this.arrayForm.points.pop();
                this.arrayForm.size = this.arrayForm.points.length;
        },
            async loadFunctions() {
                try {
                    const response = await fetch('/ui/tabulated/functions');
                    if (!response.ok) {
                         throw new Error('Не удалось загрузить список функций');
                    }
                    this.functionOptions = await response.json();
                } catch (e) {
                    this.error = e.message;
                }
            },
            async createFromTable() {
                this.resetAlerts();
                if (this.arrayForm.points.length < 2) {
                    this.error = 'Нужно минимум две точки';
                    return;
                }
                const payload = {
                    xValues: this.arrayForm.points.map(p => p.x.toString()),
                    yValues: this.arrayForm.points.map(p => p.y.toString()),
                    name: this.arrayForm.name,
                    factoryType: this.selectedFactory
                };
                const data = await this.safeRequest('/ui/tabulated/arrays', payload, 'Функция создана из таблицы');
                if (data) {
                    this.addResult(data, payload.name || 'Таблица точек');
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
                    factoryType: this.selectedFactory
                };
                const data = await this.safeRequest('/ui/tabulated/from-function', payload, 'Функция создана из источника');
                if (data) {
                    this.addResult(data, payload.name || payload.functionName || 'Функция');
                }
            },
            async calculateIntegral() {
                this.resetAlerts();
                const entry = this.results.find(r => r.id === this.integralForm.resultId);
                if (!entry) {
                    this.error = 'Выберите функцию для интегрирования';
                    return;
                }
                const payload = {
                    from: this.integralForm.from,
                    to: this.integralForm.to,
                    threads: this.integralForm.threads?.toString(),
                    factoryType: this.selectedFactory,
                    points: entry.points
                };
                const data = await this.safeRequest('/ui/tabulated/integral', payload, 'Интеграл вычислен');
                if (data) {
                    this.integralResult = {
                        label: data.label,
                        value: data.result
                    };
                }
            },
            async safeRequest(url, body, successMessage) {
                this.loading = true;
                try {
                    const response = await fetch(url, {
                        method: 'POST',
                    }
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(body)
                });
                const text = await response.text();
                if (!response.ok) {
                    throw new Error(text || 'Произошла ошибка при выполнении запроса');
                }
                const data = text ? JSON.parse(text) : null;
                this.message = successMessage;
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
                points: response.points || []
            };
            this.results.unshift(entry);
            this.integralForm.resultId = this.integralForm.resultId || entry.id;
        },
        resetAlerts() {
            this.error = null;
            this.message = null;
        }
    }
}).mount('#app');