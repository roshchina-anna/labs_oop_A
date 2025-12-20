package ui.swing;

import functions.factory.ArrayTabulatedFunctionFactory;
import functions.factory.LinkedListTabulatedFunctionFactory;
import functions.factory.TabulatedFunctionFactory;
import operations.TabulatedDifferentialOperator;
import operations.TabulatedFunctionOperationService;

public class AppContext {
    private TabulatedFunctionFactory factory;
    private final TabulatedFunctionOperationService operationService;
    private final TabulatedDifferentialOperator differentialOperator;

    public AppContext() {
        this.factory = new ArrayTabulatedFunctionFactory();
        this.operationService = new TabulatedFunctionOperationService(factory);
        this.differentialOperator = new TabulatedDifferentialOperator(factory);
    }

    public TabulatedFunctionFactory getFactory() {
        return factory;
    }

    public void setFactory(TabulatedFunctionFactory factory) {
        this.factory = factory;
        this.operationService.setFactory(factory);
        this.differentialOperator.setFactory(factory);
    }

    public TabulatedFunctionFactory getArrayFactory() {
        return new ArrayTabulatedFunctionFactory();
    }

    public TabulatedFunctionFactory getLinkedListFactory() {
        return new LinkedListTabulatedFunctionFactory();
    }

    public TabulatedFunctionOperationService getOperationService() {
        return operationService;
    }

    public TabulatedDifferentialOperator getDifferentialOperator() {
        return differentialOperator;
    }
}