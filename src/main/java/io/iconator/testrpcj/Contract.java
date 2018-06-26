package io.iconator.testrpcj;

import org.ethereum.core.CallTransaction;
import org.web3j.protocol.core.methods.response.EthCompileSolidity;

import java.util.ArrayList;
import java.util.List;

public class Contract {

    final private EthCompileSolidity.Code code;
    final private List<CallTransaction.Function> functions = new ArrayList<>();

    public Contract(EthCompileSolidity.Code code) {
        this.code = code;
    }

    public Contract addFunction(CallTransaction.Function function) {
        functions.add(function);
        return this;
    }

    public EthCompileSolidity.Code code() {
        return code;
    }

    public List<CallTransaction.Function> functions() {
        return functions;
    }

}
