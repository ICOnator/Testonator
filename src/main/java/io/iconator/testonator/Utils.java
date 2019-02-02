package io.iconator.testonator;

import org.ethereum.core.CallTransaction;
import org.ethereum.crypto.cryptohash.Keccak256;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.DynamicArray;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.AbiTypes;
import org.web3j.abi.datatypes.generated.Uint160;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.utils.Numeric;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Utils {

    public static Function createFunction(Contract contract, String name, Object... input)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, ConvertException {
        for (CallTransaction.Function f : contract.functions()) {
            if (f != null && f.name.equals(name)) {
                if (f.inputs.length != input.length) {
                    continue;
                }
                List<Type> inputParameters = new ArrayList<>();
                int len = f.inputs.length;
                for (int i = 0; i < len; i++) {
                    CallTransaction.Param p = f.inputs[i];
                    Type<?> t = convertTypes(p.getType(), input[i]);
                    inputParameters.add(t);
                }

                List<TypeReference<?>> outputParameters = new ArrayList<>();
                len = f.outputs.length;
                for (int i = 0; i < len; i++) {
                    CallTransaction.Param p = f.outputs[i];
                    TypeReference<Type> t = Utils.getType(p.getType());
                    outputParameters.add(t);
                }
                return new Function(name, inputParameters, outputParameters);
            }
        }
        return null;
    }

    public static Map<Integer, TypeReference<Type>> createEventIndexed(CallTransaction.Function f, boolean indexed) {
        Map<Integer, TypeReference<Type>> outputParameters = new LinkedHashMap<>();
        int len = f.inputs.length;
        for (int i = 0; i < len; i++) {

            CallTransaction.Param p = f.inputs[i];
            if(!(p.indexed ^ indexed)) {
                TypeReference<Type> t = Utils.getType(p.getType());
                outputParameters.put(i, t);
            }
        }
        return outputParameters;
    }

    public static Type<?> convertTypes(String type, Object param)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, ConvertException {

        if(type.contains("[]")) {
            if(!(param instanceof List)) {
                throw new ConvertException(
                        "expected List for an array type got "
                                + param.getClass());
            }
            List<Type<?>> retVal = new ArrayList<>();
            for(Object o:((List)param)) {
                Type<?> value = convertTypes(type.replace("[]",""), o);
                retVal.add(value);
            }
            return new DynamicArray(retVal);
        } else {
            Class c = AbiTypes.getType(type);

            if (type.startsWith("uint") || type.startsWith("int")) {
                if (!(param instanceof Integer || param instanceof Long || param instanceof BigInteger)) {
                    throw new ConvertException(
                            "expected Long or BigInteger for uint, but got "
                                    + param.getClass());
                }
            } else if (type.startsWith("bytes")) {
                if (!(param instanceof byte[] || param instanceof String)) {
                    throw new ConvertException(
                            "expected byte[] for bytes*, but got "
                                    + param.getClass());
                }
            } else if (type.startsWith("address")) {
                if (!(param instanceof Uint160 || param instanceof BigInteger || param instanceof String || param instanceof Address)) {
                    throw new ConvertException(
                            "expected Uint160, BigInteger, String, or Address for address, but got "
                                    + param.getClass());
                }
            } else if (type.startsWith("bool")) {
                if (!(param instanceof Boolean)) {
                    throw new ConvertException(
                            "expected Boolean for bool, but got "
                                    + param.getClass());
                }
            } else if (type.startsWith("string")) {
                if (!(param instanceof String)) {
                    throw new ConvertException(
                            "expected String for string, but got "
                                    + param.getClass());
                }
            } else {
                throw new ConvertException(
                        "expected something known, this is unkown "
                                + type);
            }
            if (param instanceof Integer) {
                return (Type<?>) c.getDeclaredConstructor(long.class).newInstance(((Integer) param).longValue());
            } else if (param instanceof Long) {
                return (Type<?>) c.getDeclaredConstructor(long.class).newInstance(((Long) param).longValue());
            } else if (param instanceof Boolean) {
                return (Type<?>) c.getDeclaredConstructor(boolean.class).newInstance(((Boolean) param).booleanValue());
            } else if (param instanceof String && type.startsWith("bytes")) {
                return (Type<?>) c.getDeclaredConstructor(byte[].class).newInstance(Numeric.hexStringToByteArray((String)param));
            } else {
                return (Type<?>) c.getDeclaredConstructor(param.getClass()).newInstance(param);
            }
        }
    }

    public static TypeReference<Type> getType(String type) {
        if(type.endsWith("[]")) {
            return new TypeReference<Type>() {
                @Override
                public java.lang.reflect.Type getType() {
                    return new ParameterizedType() {
                        public java.lang.reflect.Type getRawType() {
                            return DynamicArray.class;
                        }

                        public java.lang.reflect.Type getOwnerType() {
                            return null;
                        }

                        public java.lang.reflect.Type[] getActualTypeArguments() {
                            return new java.lang.reflect.Type[] { AbiTypes.getType(type.replace("[]","")) };
                        }
                    };
                }
            };
        } else {
            return TypeReference.create((Class<Type>) AbiTypes.getType(type));
        }
    }

    public static String encodeParameters(int offsetUint256, Type... objects) {
        List<Type> params = new ArrayList<Type>(objects.length + offsetUint256);

        //these are dummy values, so that the encoded offset is correct
        for(int i=0;i<offsetUint256;i++) {
            params.add(new Uint256(0));
        }

        for(Type t:objects) {
            params.add(t);
        }
        String encoded = FunctionEncoder.encodeConstructor(params);
        //32bits are represented in textual way by 64bits
        return encoded.substring(offsetUint256 * 64);
    }

    public static Type createArray(Type... types) {
        List<Type> tmp = new ArrayList<Type>(types.length);
        for(Type type:types) {
            tmp.add(type);
        }
        return new DynamicArray(tmp);
    }

    public static String functionHash(String abiSig) {
        byte[] hash = new Keccak256().digest(abiSig.getBytes());
        byte[] name = new byte[4];
        System.arraycopy(hash, 0, name, 0, 4);
        return Numeric.toHexString(name);
    }
}
