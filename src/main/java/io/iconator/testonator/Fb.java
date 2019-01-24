package io.iconator.testonator;

import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

public class Fb implements Builder {
    final private String name;
    final private List<Type> inputParameters = new ArrayList<>();
    final private List<TypeReference<?>> outputParameters = new ArrayList<>();

    public static Fb name(String name) {
        return new Fb(name);
    }

    private Fb(String name) {
        this.name = name;
    }

    public Function build() {
        return new Function(name, inputParameters, outputParameters);
    }

    public Fb input(String type, Object value) throws InvocationTargetException, NoSuchMethodException, InstantiationException, ConvertException, IllegalAccessException {
        Type<?> t = Utils.convertTypes(type, value);
        inputParameters.add(t);
        return this;
    }

    public Fb output(String... types) {
        for(String type: types) {
            TypeReference<Type> t = Utils.getType(type);
            outputParameters.add(t);
        }
        return this;
    }
}
