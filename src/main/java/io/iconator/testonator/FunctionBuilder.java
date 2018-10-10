package io.iconator.testonator;

import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

public class FunctionBuilder {
    final private String name;
    final private List<Type> inputParameters = new ArrayList<>();
    final private List<TypeReference<?>> outputParameters = new ArrayList<>();

    public FunctionBuilder(String name) {
        this.name = name;
    }

    public Function build() {
        return new Function(name, inputParameters, outputParameters);
    }

    public FunctionBuilder addInput(String type, Object value) throws InvocationTargetException, NoSuchMethodException, InstantiationException, ConvertException, IllegalAccessException {
        Type<?> t = Utils.convertTypes(type, value);
        inputParameters.add(t);
        return this;
    }

    public FunctionBuilder outputs(String... types) {
        for(String type: types) {
            TypeReference<Type> t = Utils.getType(type);
            outputParameters.add(t);
        }
        return this;
    }
}
