package io.iconator.testonator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.googlecode.jsonrpc4j.JsonRpcServer;
import io.iconator.testonator.jsonrpc.AddContentTypeFilter;
import io.iconator.testonator.jsonrpc.EthJsonRpcImpl;
import io.iconator.testonator.jsonrpc.JsonRpc;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.ethereum.config.SystemProperties;
import org.ethereum.config.blockchain.ByzantiumConfig;
import org.ethereum.config.blockchain.DaoHFConfig;
import org.ethereum.core.BlockHeader;
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
import org.web3j.abi.datatypes.*;
import org.web3j.crypto.*;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.*;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Files;
import org.web3j.utils.Numeric;

import javax.servlet.DispatcherType;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    public final static String DEFAULT_PATH = "/";
    public static final BigInteger GAS_PRICE = BigInteger.valueOf(10_000_000_000L);
    public static final BigInteger GAS_LIMIT = BigInteger.valueOf(8_300_000);

    private Server server = null;
    private StandaloneBlockchain standaloneBlockchain = null;
    private Web3j web3j;
    private ServletHolder holder;
    private Map<String, DeployedContract> cacheDeploy = new HashMap<>();

    public static void main(String[] args) throws Exception {
        TestBlockchain t = new TestBlockchain();
        t.start();
        LOG.info("Server running.");
    }

    public static TestBlockchain run() throws Exception {
        return run(DEFAULT_PORT, DEFAULT_PATH);
    }

    public static TestBlockchain run(int port, String path) throws Exception {
        TestBlockchain t = new TestBlockchain();
        return t.start(port, path);
    }

    public TestBlockchain start() throws Exception {
        return start(DEFAULT_PORT, DEFAULT_PATH);
    }

    public TestBlockchain start(int port) throws Exception {
        return start(port, DEFAULT_PATH);
    }

    public TestBlockchain start(int port, String path) throws Exception {
        return start(port, Web3j.build(new HttpService("http://localhost:8545"+path)), path);
    }

    public TestBlockchain start(int port, Web3j web3j, String path) throws Exception {
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
        context.addServlet(holder, path);
        context.addFilter(AddContentTypeFilter.class, path, EnumSet.of(DispatcherType.REQUEST));
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
                .withAutoblock(true) //after each transaction, a new block will be created
                .withNetConfig(new ByzantiumConfig(new DaoHFConfig()){
                    @Override
                    public BigInteger calcDifficulty(BlockHeader curBlock, BlockHeader parent) {
                        //We don't want to mine
                        return BigInteger.ONE;
                    }
                });
        standaloneBlockchain.createBlock();
        EthJsonRpcImpl ethJsonRpcImpl = new EthJsonRpcImpl(standaloneBlockchain);

        ethJsonRpcImpl.addAccount(CREDENTIAL_0.getAddress().substring(2), ACCOUNT_0);
        ethJsonRpcImpl.addAccount(CREDENTIAL_1.getAddress().substring(2), ACCOUNT_1);
        ethJsonRpcImpl.addAccount(CREDENTIAL_2.getAddress().substring(2), ACCOUNT_2);
        ethJsonRpcImpl.addAccount(CREDENTIAL_3.getAddress().substring(2), ACCOUNT_3);
        ethJsonRpcImpl.addAccount(CREDENTIAL_4.getAddress().substring(2), ACCOUNT_4);
        ethJsonRpcImpl.addAccount(CREDENTIAL_5.getAddress().substring(2), ACCOUNT_5);
        ethJsonRpcImpl.addAccount(CREDENTIAL_6.getAddress().substring(2), ACCOUNT_6);
        ethJsonRpcImpl.addAccount(CREDENTIAL_7.getAddress().substring(2), ACCOUNT_7);
        ethJsonRpcImpl.addAccount(CREDENTIAL_8.getAddress().substring(2), ACCOUNT_8);
        ethJsonRpcImpl.addAccount(CREDENTIAL_9.getAddress().substring(2), ACCOUNT_9);
        JsonRpcServer rpcServer = new JsonRpcServer(new ObjectMapper(), ethJsonRpcImpl, JsonRpc.class);
        return new RPCServlet(rpcServer);
    }

    public void reset() {
        if(holder != null) {
            RPCServlet rpcServlet = createBlockchainServlet();
            holder.setServlet(rpcServlet);
        }
        cacheDeploy.clear();
    }

    public TestBlockchain stop() throws Exception {
        server.stop();
        server.destroy();
        server = null;
        standaloneBlockchain = null;
        cacheDeploy.clear();
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
            throws IOException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, ExecutionException, InterruptedException, ConvertException {
        Function function = Utils.createFunction(contract.contract(), name, parameters);
        if(function == null) {
            throw new RuntimeException("could not create/find function with name: "+name);
        }
        return callConstant(contract.from() == null? contract.owner() : contract.from(), contract.contractAddress(), function);
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
            throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, IOException, ExecutionException, InterruptedException, ConvertException {

        return call(contract.from() == null? contract.owner() : contract.from(), contract, BigInteger.ZERO, name, parameters);
    }

    public List<Event> call(Credentials credential, DeployedContract contract,
                            String name, Object... parameters)
            throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, IOException, ExecutionException, InterruptedException, ConvertException {

        Function function =  Utils.createFunction(contract.contract(), name, parameters);
        return call(credential, contract, BigInteger.ZERO, function);
    }

    public List<Event> call(Credentials credential, DeployedContract contract, BigInteger weiValue,
                           String name, Object... parameters)
            throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, IOException, ExecutionException, InterruptedException, ConvertException {

        Function function =  Utils.createFunction(contract.contract(), name, parameters);
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

        if(!receipt.getResult().isStatusOK()) {
            LOG.warn("Function [{}] failed", function.getName());
        }

        List<Contract> contracts = new ArrayList<>();
        contracts.add(contract.contract());
        for(Contract c:contract.referencedContracts()) {
            contracts.add(c);
        }

        List<Event> events = new ArrayList<>();
        //get logs and return them
        for(Log log:receipt.getResult().getLogs()) {
            //search topic
            for(Contract c:contracts) {
                for (CallTransaction.Function f : c.functions()) {
                    for (String topic : log.getTopics()) {
                        if (Hash.sha3String(f.formatSignature()).equals(topic)) {
                            //match! now we now the parameters

                            //first indexed parameters
                            Map<Integer, TypeReference<Type>> outputIndexed = Utils.createEventIndexed(f, true);
                            List<TypeReference<Type>> tmp = new ArrayList<>(outputIndexed.values());
                            List<Type> valuesIndexed = new ArrayList<>(tmp.size());
                            //add 1 as first entry is the topic
                            int logTopicSize = log.getTopics().size();
                            if(logTopicSize != tmp.size() + 1) {
                                throw new IOException("did not get the right number of logs back: "+logTopicSize + "/" + tmp.size());
                            }
                            if(!log.getTopics().get(0).equals(topic)) {
                                throw new IOException("did not get the right topic: ["+log.getTopics().get(0)+ "]/["+ topic+"]");
                            }

                            for(int j = 1; j<logTopicSize; j++) {
                                valuesIndexed.add(FunctionReturnDecoder.decodeIndexedValue(log.getTopics().get(j), tmp.get(j - 1)));
                            }

                            //then non-indexed parameters
                            Map<Integer, TypeReference<Type>> outputNonIndexed = Utils.createEventIndexed(f, false);
                            tmp = new ArrayList<>(outputNonIndexed.values());
                            List<Type> valuesNonIndexed = FunctionReturnDecoder.decode(log.getData(), tmp);

                            //merge everything in the right order
                            List<Type> values = new ArrayList<>();
                            int len = valuesIndexed.size() + valuesNonIndexed.size();
                            for (int i = 0; i < len; i++) {
                                if (outputIndexed.containsKey(i)) {
                                    values.add(valuesIndexed.remove(0));
                                } else if (outputNonIndexed.containsKey(i)) {
                                    values.add(valuesNonIndexed.remove(0));
                                } else {
                                    throw new RuntimeException("cannot happen");
                                }
                            }

                            events.add(new Event(c, values, f.name, f.formatSignature(), topic));
                        }
                    }
                }
            }
        }
        return events;
    }

    public DeployedContract deploy(Credentials credential, Contract contract)
            throws IOException, ExecutionException, InterruptedException {
        return deploy(credential, contract, BigInteger.ZERO, Collections.emptyMap()).get(0);
    }

    public DeployedContract deploy(Credentials credential, String contractName, Map<String,Contract> contracts)
            throws InterruptedException, ExecutionException, IOException {
        return deploy(credential, contracts.get(contractName), contracts);
    }

    public DeployedContract deploy(Credentials credential, Contract contract, Map<String,Contract> contracts)
            throws IOException, ExecutionException, InterruptedException {
        return deploy(credential, contract, BigInteger.ZERO, contracts).get(0);
    }

    public List<DeployedContract> deploy(Credentials credential, Contract contract, BigInteger value,
                                         Map<String,Contract> contracts)
            throws IOException, ExecutionException, InterruptedException {
        return deploy(credential, contract, value, contracts, new ArrayList<>());
    }



    public List<DeployedContract> deploy(Credentials credential, Contract contract, BigInteger value,
                                         Map<String,Contract> contracts, List<DeployedContract> retVal)
            throws IOException, ExecutionException, InterruptedException {

        if(contract.code().getCode().contains("__")) {
            Pattern p = Pattern.compile("__<[^>]*>:([^_]*)[_]*__");
            Matcher m = p.matcher(contract.code().getCode());
            int prevStart = 0;
            StringBuilder sb = new StringBuilder();
            while(m.find(prevStart)) {
                String partOne = contract.code().getCode().substring(prevStart, m.start());
                sb.append(partOne);
                String depName = m.group(1);
                Contract dep = contracts.get(depName);
                if(dep == null) {
                    throw new RuntimeException("cannot find dependency: "+depName);
                }
                DeployedContract otherContract = cacheDeploy.get(depName);
                if(otherContract == null) {
                    otherContract = deploy(
                            credential, dep, BigInteger.ZERO, contracts, retVal)
                            .get(retVal.size() - 1);
                    cacheDeploy.put(m.group(1), otherContract);
                }
                sb.append(otherContract.contractAddress().substring(2)); //we don't want 0x
                prevStart = m.end();
            }
            sb.append(contract.code().getCode().substring(prevStart));
            contract.code().setCode(sb.toString());
            retVal.add(0, deploy(credential, contract, value).addAllReferencedContract(contracts.values()));
        } else {
            retVal.add(0, deploy(credential, contract, value).addAllReferencedContract(contracts.values()));
        }
        return retVal;
    }

    public DeployedContract deploy(Credentials credential, Contract contract, BigInteger value)
            throws IOException, ExecutionException, InterruptedException {

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
        LOG.info("Contract deployed at {}, {}", contractAddress, contract.code().getCode());
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

    public static Map<String, Contract> compile(File... contracts) throws IOException {
        if(contracts.length == 0) {
            throw new RuntimeException("need files as input");
        }
        String contractSrc = Files.readString(contracts[0]);
        Map<String, String> dependencies = new HashMap<>();
        for(int i=1;i<contracts.length;i++) {
            dependencies.put("./"+contracts[i].getName(), Files.readString(contracts[i]));
        }
        return compile(contractSrc, dependencies);
    }

    public static Map<String, Contract> compile(String contractSrc, Map<String, String> dependencies) throws IOException {
        Pattern p = Pattern.compile("\\s*import\\s*\"([^\"]*)\"\\s*;");
        Matcher m = p.matcher(contractSrc);
        StringBuilder sb = new StringBuilder();

        int prevStart = 0;
        while(m.find(prevStart)) {
            sb.append(contractSrc.substring(prevStart, m.start()));
            System.out.println("AA"+m.group(1)+" de "+dependencies.keySet());
            sb.append(stripPragma(dependencies.get(m.group(1))));
            prevStart = m.end();
        }
        sb.append(contractSrc.substring(prevStart));
        return compile(sb.toString());
    }

    private static String stripPragma(String contractSrc) {
        return contractSrc.replaceAll("\\s*pragma\\s*solidity.*;", "");
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

}
