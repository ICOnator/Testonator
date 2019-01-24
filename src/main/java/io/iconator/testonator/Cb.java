package io.iconator.testonator;

import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

public class Cb {
    final private List<Type> inputParameters = new ArrayList<>();

    public static Cb constructor() {
        return new Cb();
    }

    public static Cb constructor(String type, Object value) throws NoSuchMethodException, InstantiationException, IllegalAccessException, ConvertException, InvocationTargetException {
        return (new Cb()).input(type, value);
    }

    public String build() {
        return FunctionEncoder.encodeConstructor(inputParameters);
    }

    public Cb input(String type, Object value) throws InvocationTargetException, NoSuchMethodException, InstantiationException, ConvertException, IllegalAccessException {
        Type<?> t = Utils.convertTypes(type, value);
        inputParameters.add(t);
        return this;
    }
}
