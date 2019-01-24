package io.iconator.testonator;

import org.ethereum.core.CallTransaction;
import org.web3j.protocol.core.methods.response.EthCompileSolidity;

import java.util.ArrayList;
import java.util.List;

public class Contract {

    final private EthCompileSolidity.Code code;
    private String encodedConstructor = "";
    final private List<CallTransaction.Function> functions = new ArrayList<>();

    public Contract(EthCompileSolidity.Code code) {
        this.code = code;
    }

    public Contract addFunction(CallTransaction.Function function) {
        functions.add(function);
        return this;
    }

    public Contract constructor(Cb cb) {
        this.encodedConstructor = cb.build();
        return this;
    }

    public String constructor() {
        return encodedConstructor;
    }

    public EthCompileSolidity.Code code() {
        return code;
    }

    public List<CallTransaction.Function> functions() {
        return functions;
    }

}
