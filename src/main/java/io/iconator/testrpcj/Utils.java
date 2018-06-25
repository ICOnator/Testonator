package io.iconator.testrpcj;

import org.bouncycastle.util.encoders.Hex;
import org.ethereum.core.CallTransaction;
import org.ethereum.crypto.ECKey;
import org.ethereum.solidity.compiler.CompilationResult;
import org.ethereum.solidity.compiler.SolidityCompiler;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.crypto.*;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCompileSolidity;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.tuples.generated.Tuple2;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.math.BigInteger;
import java.util.*;

import static org.ethereum.solidity.compiler.SolidityCompiler.Options.*;

public class Utils {

    public static final BigInteger GAS_PRICE = BigInteger.valueOf(10_000_000_000L);
    public static final BigInteger GAS_LIMIT = BigInteger.valueOf(8_300_000);

    public static Credentials create(ECKey ecKey) {
        ECKeyPair pair = ECKeyPair.create(ecKey.getPrivKey());
        return Credentials.create(pair);
    }

    public static List<Type> callConstant(
            Web3j web3j, ECKey account, String contractAddress, Function function)
            throws IOException {
        Credentials credentials = create(account);
        String encodedFunction = FunctionEncoder.encode(function);
        org.web3j.protocol.core.methods.response.EthCall ethCall = web3j.ethCall(
                Transaction.createEthCallTransaction(
                        credentials.getAddress(), contractAddress, encodedFunction),
                DefaultBlockParameterName.LATEST).send();

        if (ethCall.hasError()) {
            throw new IOException(ethCall.getError().toString());
        }
        String value = ethCall.getValue();
        System.out.println(value);
        return FunctionReturnDecoder.decode(value, function.getOutputParameters());

    }

    public static String call(Web3j web3j, ECKey account, String contractAddress, BigInteger weiValue, Function function, BigInteger gasLimit) throws IOException {
        Credentials credentials = create(account);
        String encodedFunction = FunctionEncoder.encode(function);
        EthGetTransactionCount ethGetTransactionCount = web3j.ethGetTransactionCount(
                credentials.getAddress(), DefaultBlockParameterName.LATEST).send();
        BigInteger nonce = ethGetTransactionCount.getTransactionCount();
        RawTransaction rawTransaction = RawTransaction.createTransaction(
                nonce,
                GAS_PRICE,
                gasLimit,
                contractAddress,
                weiValue,
                encodedFunction);

        byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials);

        String hexValue = Numeric.toHexString(signedMessage);

        EthSendTransaction ret = web3j.ethSendRawTransaction(hexValue).send();
        if (ret.hasError()) {
            throw new IOException(ret.getError().toString());
        }
        return ret.getResult();
    }


    public static Tuple2<String, EthSendTransaction> deploy(
            Web3j web3j, ECKey account, String contract) throws IOException {
        return deploy(web3j, account, BigInteger.ZERO, contract);
    }

    public static Tuple2<String, EthSendTransaction> deploy(
            Web3j web3j, ECKey account, BigInteger value, String contract) throws IOException {
        Credentials credentials = create(account);

        // get the next available nonce
        EthGetTransactionCount ethGetTransactionCount = web3j.ethGetTransactionCount(
                credentials.getAddress(), DefaultBlockParameterName.LATEST).send();
        BigInteger nonce = ethGetTransactionCount.getTransactionCount();

        // create our transaction
        RawTransaction rawTransaction = RawTransaction.createContractTransaction(
                nonce, GAS_PRICE, GAS_LIMIT, value, contract);

        // sign & send our transaction
        byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
        String hexValue = Hex.toHexString(signedMessage);
        String contractAddress = ContractUtils.generateContractAddress(credentials.getAddress(), nonce);
        return new Tuple2(contractAddress, web3j.ethSendRawTransaction(hexValue).send());
    }

    public static Map<String, Tuple2<EthCompileSolidity.Code, List<Tuple2<CallTransaction.Function, BigInteger>>>> compile(String contractSrc) throws IOException {
        SolidityCompiler.Result result = TestBlockchain.compiler().compile(
                contractSrc.getBytes(), true, ABI, BIN, INTERFACE, METADATA, GAS);
        if (result.isFailed()) {
            throw new IOException(result.errors);
        }

        CompilationResult parsed = CompilationResult.parse(result.output);
        //TODO: make multicontract
        Map<String, Map<String, Integer>> gas = Utils.parseGasEstimation(result.output);

        Map<String, Tuple2<EthCompileSolidity.Code, List<Tuple2<CallTransaction.Function, BigInteger>>>> retVal = new HashMap<>();
        for (String key : parsed.getContractKeys()) {
            String name = key.substring(key.lastIndexOf(58) + 1);
            Map<String, Integer> contractGas = gas.get(name);
            CompilationResult.ContractMetadata meta = parsed.getContract(name);

            CallTransaction.Contract details = new CallTransaction.Contract(meta.abi);

            List<Tuple2<CallTransaction.Function, BigInteger>> functionList = new ArrayList<>();
            if (contractGas.containsKey("()")) {
                functionList.add(new Tuple2<>(details.getConstructor(), BigInteger.valueOf(contractGas.get("()"))));
            }

            for (CallTransaction.Function f : details.functions) {
                Tuple2<CallTransaction.Function, BigInteger> contractDetails
                        = new Tuple2<>(f, BigInteger.valueOf(contractGas.get(f.formatSignature())));
                functionList.add(contractDetails);
            }

            retVal.put(name, new Tuple2<>(new EthCompileSolidity.Code(meta.bin), functionList));
        }


        //CompilationResult result = CompilationResult.parse(r);
        //CompilationResult.ContractMetadata a = result.getContract("Example1");
        //CallTransaction.Contract contract = new CallTransaction.Contract(a.abi);

        return retVal;
    }

    public static Map<String, Map<String, Integer>> parseGasEstimation(String output) {
        Map<String, Map<String, Integer>> retVal = new HashMap<>();
        while (true) {
            int start = output.indexOf("======= <stdin>:");
            if (start < 0) {
                return retVal;
            }
            int end = output.indexOf(" =======\n", start);
            if (end < 0) {
                return retVal;
            }
            String name = output.substring(start + "======= <stdin>:".length(), end);
            Map<String, Integer> result = new HashMap<>();
            retVal.put(name, result);

            output = output.substring(end + " =======\n".length());

            int startGas = output.indexOf("Gas estimation:\nconstruction:\n");
            if (startGas < 0) {
                return retVal;
            }
            int equalsSign = output.indexOf("=", startGas);
            if (equalsSign < 0) {
                return retVal;
            }
            int lineBreak = output.indexOf("\n", equalsSign);
            if (lineBreak < 0) {
                return retVal;
            }

            int construction = Integer.parseInt(output.substring(equalsSign + 1, lineBreak).trim());
            result.put("()", construction);

            output = output.substring(lineBreak + 1);
            int functionStart = output.indexOf("external:\n");
            if (functionStart < 0) {
                return retVal;
            }
            functionStart += "external:\n".length();
            output = output.substring(functionStart);
            while (!output.trim().startsWith("======= <stdin>") && !output.trim().isEmpty()) {
                lineBreak = output.indexOf("\n");
                if (lineBreak < 0) {
                    return Collections.emptyMap();
                }
                int split = output.indexOf(":");
                if (split < 0) {
                    output = output.substring(lineBreak + 1);
                    continue;
                }
                String key = output.substring(0, split).trim();
                String value = output.substring(split + 1, lineBreak).trim();
                try {
                    result.put(key, Integer.parseInt(value));
                } catch (NumberFormatException nfe) {
                    nfe.printStackTrace();
                }
                output = output.substring(lineBreak + 1);
            }

        }
    }
}
