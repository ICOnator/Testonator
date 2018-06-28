package io.iconator.testrpcj;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.googlecode.jsonrpc4j.JsonRpcServer;
import io.iconator.testrpcj.jsonrpc.AddContentTypeFilter;
import io.iconator.testrpcj.jsonrpc.EthJsonRpcImpl;
import io.iconator.testrpcj.jsonrpc.JsonRpc;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.ethereum.config.SystemProperties;
import org.ethereum.core.CallTransaction;
import org.ethereum.crypto.ECKey;
import org.ethereum.solidity.compiler.CompilationResult;
import org.ethereum.solidity.compiler.SolidityCompiler;
import org.ethereum.util.blockchain.EtherUtil;
import org.ethereum.util.blockchain.StandaloneBlockchain;
import org.spongycastle.util.encoders.Hex;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.AbiTypes;
import org.web3j.abi.datatypes.generated.Uint160;
import org.web3j.crypto.*;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.*;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Numeric;

import javax.servlet.DispatcherType;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.ExecutionException;

import static org.ethereum.solidity.compiler.SolidityCompiler.Options.*;

public class TestBlockchain {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(TestBlockchain.class);

    // public and private keys
    public final static ECKey ACCOUNT_0 = ECKey.fromPrivate(Hex.decode("1b865950b17a065c79b11ecb39650c377b4963d6387b2fb97d71744b89a7295e"));
    public final static ECKey ACCOUNT_1 = ECKey.fromPrivate(Hex.decode("c77ee832f3e5d7624ce9dab0eeb2958ad550e534952b79bb705e63b3989d4d1d"));
    public final static ECKey ACCOUNT_2 = ECKey.fromPrivate(Hex.decode("ba7ffe9dee14b3626211b2d056eacc30e7a634f7e11eeb4dde6ee6d50d0c81ab"));
    public final static ECKey ACCOUNT_3 = ECKey.fromPrivate(Hex.decode("64b1a16bb773bc2a6665967923cfd68f369e34f66ecd19c302995f8635598b1c"));
    public final static ECKey ACCOUNT_4 = ECKey.fromPrivate(Hex.decode("399c34e860be1f2740297fcadd3546fdd4f5ba4c06d13882da1e48527df3acca"));
    public final static ECKey ACCOUNT_5 = ECKey.fromPrivate(Hex.decode("a2a3abebd9160a2b2940970d848161008e3ea528aeaa927fb8b8370d3675f5f5"));
    public final static ECKey ACCOUNT_6 = ECKey.fromPrivate(Hex.decode("e728d9667a27b7f6164309fc3809c00fd8d782d9343c0b73ea1f5a150ec3d05b"));
    public final static ECKey ACCOUNT_7 = ECKey.fromPrivate(Hex.decode("d58fd771caefbdcca0c23fbc440fd03dacdee29cc4668cc9fc5acf29b4219f41"));
    public final static ECKey ACCOUNT_8 = ECKey.fromPrivate(Hex.decode("649f638d220fd6319ca4af8f5e0e261d15a66172830077126fef21fdbdd95410"));
    public final static ECKey ACCOUNT_9 = ECKey.fromPrivate(Hex.decode("ea8f71fc4690e0733f3478c3d8e53790988b9e51deabd10185364bc59c58fdba"));

    public final static Credentials CREDENTIAL_0 = create(ACCOUNT_0);
    public final static Credentials CREDENTIAL_1 = create(ACCOUNT_1);
    public final static Credentials CREDENTIAL_2 = create(ACCOUNT_2);
    public final static Credentials CREDENTIAL_3 = create(ACCOUNT_3);
    public final static Credentials CREDENTIAL_4 = create(ACCOUNT_4);
    public final static Credentials CREDENTIAL_5 = create(ACCOUNT_5);
    public final static Credentials CREDENTIAL_6 = create(ACCOUNT_6);
    public final static Credentials CREDENTIAL_7 = create(ACCOUNT_7);
    public final static Credentials CREDENTIAL_8 = create(ACCOUNT_8);
    public final static Credentials CREDENTIAL_9 = create(ACCOUNT_9);

    public final static Integer DEFAULT_PORT = 8545;
    public static final BigInteger GAS_PRICE = BigInteger.valueOf(10_000_000_000L);
    public static final BigInteger GAS_LIMIT = BigInteger.valueOf(8_300_000);

    private Server server = null;
    private StandaloneBlockchain standaloneBlockchain = null;
    private Web3j web3j;
    private ServletHolder holder;

    public static void main(String[] args) throws Exception {
        Integer port = null;
        TestBlockchain t = new TestBlockchain();
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException nfe) {
                LOG.info("The given parameter can't be parsed as a number: {}", args[0]);
                port = DEFAULT_PORT;
            }
        }
        LOG.info("Using port: {}", port);
        t.start(port);
    }

    public static TestBlockchain start() throws Exception {
        TestBlockchain t = new TestBlockchain();
        return t.start(DEFAULT_PORT);
    }

    public TestBlockchain start(int port) throws Exception {
        return start(port, Web3j.build(new HttpService("http://localhost:8545/rpc")));
    }

    public TestBlockchain start(int port, Web3j web3j) throws Exception {
        if (server != null) {
            stop();
        }
        this.web3j = web3j;
        server = new Server(port);

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);

        RPCServlet rpcServlet = createBlockchainServlet();
        holder = new ServletHolder(rpcServlet);
        context.addServlet(holder, "/rpc");
        context.addFilter(AddContentTypeFilter.class, "/rpc", EnumSet.of(DispatcherType.REQUEST));
        server.start();

        return this;
    }

    private RPCServlet createBlockchainServlet() {
        standaloneBlockchain = new StandaloneBlockchain()
                .withAccountBalance(TestBlockchain.ACCOUNT_0.getAddress(),
                        EtherUtil.convert(10, EtherUtil.Unit.ETHER))
                .withAccountBalance(TestBlockchain.ACCOUNT_1.getAddress(),
                        EtherUtil.convert(10, EtherUtil.Unit.ETHER))
                .withAccountBalance(TestBlockchain.ACCOUNT_2.getAddress(),
                        EtherUtil.convert(10, EtherUtil.Unit.ETHER))
                .withAccountBalance(TestBlockchain.ACCOUNT_3.getAddress(),
                        EtherUtil.convert(10, EtherUtil.Unit.ETHER))
                .withAccountBalance(TestBlockchain.ACCOUNT_4.getAddress(),
                        EtherUtil.convert(10, EtherUtil.Unit.ETHER))
                .withAccountBalance(TestBlockchain.ACCOUNT_5.getAddress(),
                        EtherUtil.convert(10, EtherUtil.Unit.ETHER))
                .withAccountBalance(TestBlockchain.ACCOUNT_6.getAddress(),
                        EtherUtil.convert(10, EtherUtil.Unit.ETHER))
                .withAccountBalance(TestBlockchain.ACCOUNT_7.getAddress(),
                        EtherUtil.convert(10, EtherUtil.Unit.ETHER))
                .withAccountBalance(TestBlockchain.ACCOUNT_8.getAddress(),
                        EtherUtil.convert(10, EtherUtil.Unit.ETHER))
                .withAccountBalance(TestBlockchain.ACCOUNT_9.getAddress(),
                        EtherUtil.convert(10, EtherUtil.Unit.ETHER))
                .withAutoblock(true);  //after each transaction, a new block will be created
        standaloneBlockchain.createBlock();
        EthJsonRpcImpl ethJsonRpcImpl = new EthJsonRpcImpl(standaloneBlockchain);

        JsonRpcServer rpcServer = new JsonRpcServer(new ObjectMapper(), ethJsonRpcImpl, JsonRpc.class);
        return new RPCServlet(rpcServer);
    }

    public void reset() {
        if(holder != null) {
            RPCServlet rpcServlet = createBlockchainServlet();
            holder.setServlet(rpcServlet);
        }
    }

    public TestBlockchain stop() throws Exception {
        server.stop();
        server.destroy();
        server = null;
        standaloneBlockchain = null;
        return this;
    }

    public Web3j web3j() {
        return web3j;
    }

    public BigInteger balance(Credentials credential) throws IOException, ExecutionException, InterruptedException {
        return web3j.ethGetBalance(credential.getAddress(), DefaultBlockParameterName.LATEST).sendAsync().get().getBalance();
    }

    public BigInteger balance(String address) throws IOException, ExecutionException, InterruptedException {
        return web3j.ethGetBalance(address, DefaultBlockParameterName.LATEST).sendAsync().get().getBalance();
    }

    public BigInteger nonce(Credentials credentials) throws IOException, ExecutionException, InterruptedException {
        EthGetTransactionCount ethGetTransactionCount = web3j.ethGetTransactionCount(
                credentials.getAddress(), DefaultBlockParameterName.LATEST).sendAsync().get();
        return ethGetTransactionCount.getTransactionCount();
    }

    public static Credentials create(ECKey ecKey) {
        BigInteger privKey = ecKey.getPrivKey();
        ECKeyPair pair = new ECKeyPair(privKey, Sign.publicKeyFromPrivate(privKey));
        return Credentials.create(pair);
    }

    public List<Type> callConstant(DeployedContract contract, String name, Object... parameters)
            throws IOException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, ExecutionException, InterruptedException {
        Function function = createFunction(contract.contract(), name, parameters);
        return callConstant(contract.credential(), contract.contractAddress(), function);
    }

    public List<Type> callConstant(Credentials credential, String contractAddress, Function function)
            throws IOException, ExecutionException, InterruptedException {
        String encodedFunction = FunctionEncoder.encode(function);
        org.web3j.protocol.core.methods.response.EthCall ethCall = web3j.ethCall(
                Transaction.createEthCallTransaction(
                        credential.getAddress(), contractAddress, encodedFunction),
                DefaultBlockParameterName.LATEST).sendAsync().get();

        if (ethCall.hasError()) {
            throw new IOException(ethCall.getError().toString());
        }
        String value = ethCall.getValue();
        return FunctionReturnDecoder.decode(value, function.getOutputParameters());
    }

    public List<Event> call(DeployedContract contract, String name, Object... parameters)
            throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, IOException, ExecutionException, InterruptedException {

        return call(contract.credential(), contract, BigInteger.ZERO, name, parameters);
    }

    public List<Event> call(Credentials credential, DeployedContract contract, BigInteger weiValue,
                           String name, Object... parameters)
            throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, IOException, ExecutionException, InterruptedException {

        Function function =  createFunction(contract.contract(), name, parameters);
        return call(credential, contract, weiValue, function);
    }

    public List<Event> call(Credentials credential, DeployedContract contract, BigInteger weiValue, Function function) throws IOException, ExecutionException, InterruptedException {
        BigInteger nonce = nonce(credential);
        String encodedFunction = FunctionEncoder.encode(function);
        RawTransaction rawTransaction = RawTransaction.createTransaction(
                nonce,
                GAS_PRICE,
                GAS_LIMIT,
                contract.contractAddress(),
                weiValue,
                encodedFunction);

        byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, credential);

        String hexValue = Numeric.toHexString(signedMessage);

        EthSendTransaction ret = web3j.ethSendRawTransaction(hexValue).sendAsync().get();
        if (ret.hasError()) {
            throw new IOException(ret.getError().toString());
        }

        EthGetTransactionReceipt receipt = web3j.ethGetTransactionReceipt(ret.getTransactionHash()).sendAsync().get();

        List<Event> events = new ArrayList<>();
        //get logs and return them
        for(Log log:receipt.getResult().getLogs()) {
            //search topic
            for(String topic:log.getTopics()) {
                for(CallTransaction.Function f:contract.contract().functions()) {
                    if(Hash.sha3String(f.formatSignature()).equals(topic)) {
                        //match! now we now the parameters
                        List<TypeReference<Type>> output = createEvent(f);
                        List<Type> values = FunctionReturnDecoder.decode(log.getData(), output);
                        events.add(new Event(values, f.name, f.formatSignature(), topic));
                    }
                }
            }
        }
        return events;
    }

    public DeployedContract deploy(Credentials credential, Contract contract) throws IOException, ExecutionException, InterruptedException {
        return deploy(credential, contract, BigInteger.ZERO);
    }

    public DeployedContract deploy(Credentials credential, Contract contract, BigInteger value) throws IOException, ExecutionException, InterruptedException {
        BigInteger nonce = nonce(credential);
        // create our transaction
        RawTransaction rawTransaction = RawTransaction.createContractTransaction(
                nonce, GAS_PRICE, GAS_LIMIT, value, contract.code().getCode());

        // sign & send our transaction
        byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, credential);
        String hexValue = org.bouncycastle.util.encoders.Hex.toHexString(signedMessage);
        String contractAddress = ContractUtils.generateContractAddress(credential.getAddress(), nonce);

        EthSendTransaction tx = web3j.ethSendRawTransaction(hexValue).sendAsync().get();
        EthGetTransactionReceipt receipt = web3j.ethGetTransactionReceipt(tx.getTransactionHash()).sendAsync().get();

        return new DeployedContract(tx, contractAddress, credential, receipt, contract);
    }

    public static Map<String, Contract> compile(File source) throws IOException {
        SolidityCompiler.Result result = new SolidityCompiler(SystemProperties.getDefault()).compileSrc(
                source, true, true, ABI, BIN, INTERFACE, METADATA);
        if (result.isFailed()) {
            throw new IOException(result.errors);
        }
        CompilationResult parsed = CompilationResult.parse(result.output);
        return compile(parsed);
    }

    public static Map<String, Contract> compile(String contractSrc) throws IOException {
        SolidityCompiler.Result result = new SolidityCompiler(SystemProperties.getDefault()).compile(
                contractSrc.getBytes(), true, ABI, BIN, INTERFACE, METADATA);
        if (result.isFailed()) {
            throw new IOException(result.errors);
        }

        CompilationResult parsed = CompilationResult.parse(result.output);
        return compile(parsed);
    }

    private static Map<String, Contract> compile(CompilationResult parsed) throws IOException {
        Map<String, Contract> retVal = new HashMap<>();
        for (String key : parsed.getContractKeys()) {
            String name = key.substring(key.lastIndexOf(58) + 1);
            CompilationResult.ContractMetadata meta = parsed.getContract(name);
            CallTransaction.Contract details = new CallTransaction.Contract(meta.abi);
            Contract contract = new Contract(new EthCompileSolidity.Code(meta.bin));

            for (CallTransaction.Function f : details.functions) {
                contract.addFunction(f);
            }
            retVal.put(name, contract);
        }
        return retVal;
    }

    // ************************** Utils ***************************
    private static Function createFunction(Contract contract, String name, Object... input)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        for (CallTransaction.Function f : contract.functions()) {
            if (f != null && f.name.equals(name)) {
                if (f.inputs.length != input.length) {
                    throw new RuntimeException(
                            "contract input argument length: "
                                    + f.inputs.length
                                    + " does not match user input length: "
                                    + input.length);
                }
                List<Type> inputParameters = new ArrayList<>();
                int len = f.inputs.length;
                for (int i = 0; i < len; i++) {
                    CallTransaction.Param p = f.inputs[i];
                    Type<?> t = convertTypes(p, input[i]);
                    inputParameters.add(t);
                }

                List<TypeReference<?>> outputParameters = new ArrayList<>();
                len = f.outputs.length;
                for (int i = 0; i < len; i++) {
                    CallTransaction.Param p = f.outputs[i];
                    TypeReference<Type> t = TypeReference.<Type>create((Class<Type>) AbiTypes.getType(p.getType()));
                    outputParameters.add(t);
                }
                return new Function(name, inputParameters, outputParameters);
            }
        }
        return null;
    }

    private static List<TypeReference<Type>> createEvent(CallTransaction.Function f) {
        List<TypeReference<Type>> outputParameters = new ArrayList<>();
        int len = f.inputs.length;
        for (int i = 0; i < len; i++) {
            CallTransaction.Param p = f.inputs[i];
            TypeReference<Type> t = TypeReference.<Type>create((Class<Type>) AbiTypes.getType(p.getType()));
            outputParameters.add(t);
        }
        return outputParameters;
    }


    private static Type<?> convertTypes(CallTransaction.Param p, Object param)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Class c = AbiTypes.getType(p.getType());
        if (p.getType().startsWith("uint") || p.getType().startsWith("int")) {
            if (!(param instanceof Long) && !(param instanceof BigInteger)) {
                throw new RuntimeException(
                        "expected Long or BigInteger for uint, but got "
                                + param.getClass());
            }
        } else if (p.getType().startsWith("bytes")) {
            if (!(param instanceof byte[])) {
                throw new RuntimeException(
                        "expected byte[] for bytes*, but got "
                                + param.getClass());
            }
        } else if (p.getType().startsWith("address")) {
            if (!(param instanceof Uint160
                    && !(param instanceof BigInteger)
                    && !(param instanceof String))) {
                throw new RuntimeException(
                        "expected Uint160, BigInteger, or String for address, but got "
                                + param.getClass());
            }
        } else if (p.getType().startsWith("bool")) {
            if (!(param instanceof Boolean)) {
                throw new RuntimeException(
                        "expected Boolean for bool, but got "
                                + param.getClass());
            }
        } else if (p.getType().startsWith("string")) {
            if (!(param instanceof String)) {
                throw new RuntimeException(
                        "expected String for string, but got "
                                + param.getClass());
            }
        } else {
            throw new RuntimeException(
                    "expected something known, this is unkown "
                            + p.getType());
        }
        if (param instanceof Long) {
            return (Type<?>) c.getDeclaredConstructor(long.class).newInstance(((Long) param).longValue());
        } else if (param instanceof Boolean) {
            return (Type<?>) c.getDeclaredConstructor(boolean.class).newInstance(((Boolean) param).booleanValue());
        } else {
            return (Type<?>) c.getDeclaredConstructor(param.getClass()).newInstance(param);
        }
    }
}
