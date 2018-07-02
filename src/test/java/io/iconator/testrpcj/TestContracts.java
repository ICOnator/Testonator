package io.iconator.testrpcj;

import org.ethereum.config.SystemProperties;
import org.ethereum.core.CallTransaction;
import org.ethereum.solidity.compiler.CompilationResult;
import org.ethereum.solidity.compiler.SolidityCompiler;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthCompileSolidity;
import org.web3j.protocol.core.methods.response.Web3ClientVersion;
import org.web3j.protocol.http.HttpService;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.ExecutionException;

import static org.ethereum.solidity.compiler.SolidityCompiler.Options.*;
import static org.ethereum.solidity.compiler.SolidityCompiler.Result;
public class TestContracts {

    private static TestBlockchain testBlockchain;
    private static Web3j web3j;

    private static long start = System.currentTimeMillis();

    @BeforeClass
    public static void setup() throws Exception {
        testBlockchain = TestBlockchain.start();
        web3j = Web3j.build(new HttpService("http://localhost:8545/rpc"));
        System.out.println("setup done: "+(System.currentTimeMillis()-start));
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if(testBlockchain != null) {
            testBlockchain.stop();
        }
    }

    @Test
    public void testConnect() throws IOException {
        Web3ClientVersion web3ClientVersion = web3j.web3ClientVersion().send();
        String clientVersion = web3ClientVersion.getWeb3ClientVersion();
        Assert.assertTrue(clientVersion.startsWith("TestRPC-J"));
        System.out.println("Version: "+clientVersion);
    }

    @Test
    public void testManualCompile() throws IOException {
        String contractSrc = "pragma solidity ^0.4.24;\n" +
                "\n" +
                "//a comment\n" +
                "contract Example1 {\n" +
                "\tuint counter;\n" +
                "}";
        Result r = new SolidityCompiler(SystemProperties.getDefault()).compile(
                contractSrc.getBytes(), true, ABI, BIN, INTERFACE, METADATA);
        Assert.assertTrue(!r.isFailed());
        CompilationResult result = CompilationResult.parse(r.output);
        CompilationResult.ContractMetadata a = result.getContract("Example1");
        CallTransaction.Contract contract = new CallTransaction.Contract(a.abi);
        Assert.assertTrue(contract.functions.length == 0);
    }

    @Test
    public void testCompile() throws IOException {
        String contractSrc = "pragma solidity ^0.4.24;\n" +
                "\n" +
                "//a comment\n" +
                "contract Example1 {\n" +
                "\tuint counter;\n" +
                "}";
        EthCompileSolidity request = web3j.ethCompileSolidity(contractSrc).send();
        EthCompileSolidity.Code c = request.getCompiledSolidity().get("code");
        Assert.assertTrue(c.getCode().startsWith("0x60806040"));
    }

    @Test
    public void testCompileGas() throws IOException {
        String contractSrc = "pragma solidity ^0.4.24;\n" +
                "\n" +
                "contract Example2 {\n" +
                "\tuint256 public counter;\n" +
                "\tfunction set(uint256 _counter) public returns (uint256) {\n" +
                "\t    uint256 tmp = counter;\n" +
                "\t    counter = _counter;\n" +
                "\t    return tmp;\n" +
                "\t}\n" +
                "\tfunction get() public view returns (uint256) {\n" +
                "\t    return counter;\n" +
                "\t}\n" +
                "}\n" +
                "contract Example3 {\n" +
                "    uint32 test;\n" +
                "}";
        Map<String, Contract> ret = TestBlockchain.compile(contractSrc);
        Assert.assertEquals(2, ret.size());
    }

    @Test
    public void testDeploy() throws IOException, ExecutionException, InterruptedException {
        String contractSrc = "pragma solidity ^0.4.24;\n" +
                "\n" +
                "contract Exampl2 {\n" +
                "\tuint256 public counter = 12;\n" +
                "\tfunction set(uint256 _counter) public returns (uint256) {\n" +
                "\t    uint256 tmp = counter;\n" +
                "\t    counter = _counter;\n" +
                "\t    return tmp;\n" +
                "\t}\n" +
                "\tfunction get() public view returns (uint256) {\n" +
                "\t    return counter;\n" +
                "\t}\n" +
                "}";
        Map<String, Contract> ret = TestBlockchain.compile(contractSrc);

        EthCompileSolidity.Code c = ret.get("Exampl2").code();

        DeployedContract deployed = testBlockchain.deploy(TestBlockchain.CREDENTIAL_0, ret.get("Exampl2"));

        final Function function = new Function("counter",
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));

        Type t = testBlockchain.callConstant( TestBlockchain.CREDENTIAL_0, deployed.contractAddress(), function).get(0);

        Assert.assertEquals("12", t.getValue().toString());

        final Function function2 = new Function("set",
                Arrays.<Type>asList(new Uint256(13L)),
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));


        List<Event> events = testBlockchain.call(
                TestBlockchain.CREDENTIAL_0, deployed, BigInteger.ZERO, function2);
        System.out.println(events);

        String value = testBlockchain.callConstant(
                TestBlockchain.CREDENTIAL_0, deployed.contractAddress(), function).get(0).getValue().toString();
        Assert.assertEquals(value, "13");

    }

    @Test
    public void testCall() throws IOException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException, ExecutionException, InterruptedException {
        String contractSrc = "pragma solidity ^0.4.24;\n" +
                "\n" +
                "contract Exampl2 {\n" +
                "\tuint256 public counter = 15;\n" +
                "\tfunction set(uint256 _counter) public returns (uint256) {\n" +
                "\t    uint256 tmp = counter;\n" +
                "\t    counter = _counter;\n" +
                "\t    return tmp;\n" +
                "\t}\n" +
                "\tfunction get() public view returns (uint256) {\n" +
                "\t    return counter;\n" +
                "\t}\n" +
                "}";
        Map<String, Contract> ret = TestBlockchain.compile(contractSrc);
        BigInteger balance = testBlockchain.balance(TestBlockchain.CREDENTIAL_0);
        System.out.println("balance1: "+balance);
        DeployedContract deployed = testBlockchain.deploy(TestBlockchain.CREDENTIAL_0, ret.get("Exampl2"));
        testBlockchain.call(deployed, "set", Long.valueOf(5));
        BigInteger balance2 = testBlockchain.balance(TestBlockchain.CREDENTIAL_0);
        Assert.assertNotEquals(balance, balance2);
        System.out.println("balance2: "+balance2);

        String value = testBlockchain.callConstant(deployed, "get").get(0).getValue().toString();
        Assert.assertEquals(value, "5");
        Assert.assertEquals(balance2, testBlockchain.balance(TestBlockchain.CREDENTIAL_0));
    }

    @Test
    public void testEvents() throws IOException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException, ExecutionException, InterruptedException {
        start = System.currentTimeMillis();
        String contractSrc ="pragma solidity ^0.4.24;\n" +
                "\n" +
                "contract ExampleEvent {\n" +
                "\tuint256 public counter=3;\n" +
                "\tevent Message(string, uint256);\n" +
                "\tfunction set(uint256 _counter) public returns (uint256) {\n" +
                "\t    uint256 tmp = counter;\n" +
                "\t    counter = _counter;\n" +
                "\t    emit Message(\"hey there1\", _counter);\n" +
                "\t    emit Message(\"hey there2\", tmp);\n" +
                "\t    return tmp;\n" +
                "\t}\n" +
                "\tfunction get() public view returns (uint256) {\n" +
                "\t    return counter;\n" +
                "\t}\n" +
                "}\n";
        Map<String, Contract> ret = TestBlockchain.compile(contractSrc);
        System.out.println("compile done: "+(System.currentTimeMillis()-start));
        start = System.currentTimeMillis();
        Contract contract = ret.get("ExampleEvent");
        DeployedContract deployed = testBlockchain.deploy(TestBlockchain.CREDENTIAL_0, contract);
        System.out.println("deploy done: "+(System.currentTimeMillis()-start));
        start = System.currentTimeMillis();
        List<Event> events = testBlockchain.call(deployed, "set", Long.valueOf(5));
        System.out.println("call done: "+(System.currentTimeMillis()-start));
        start = System.currentTimeMillis();
        Assert.assertEquals(2, events.size());
        Assert.assertEquals(events.get(0).values().get(0).getValue().toString(), "hey there1");

        Assert.assertEquals(events.get(0).values().get(1).getValue().toString(), "5");
        Assert.assertEquals(events.get(1).values().get(1).getValue().toString(), "3");

    }

    @Test
    public void testArrays() throws IOException, ExecutionException, InterruptedException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        String contractSrc = "contract Example2 {\n" +
                "uint8 public result;\n" +
                "    function mint(uint8[] numbers) public {\n" +
                "        for(uint8 i = 0;i<numbers.length;i++) {\n" +
                "            result += numbers[i];\n" +
                "        }\n" +
                "    }\n" +
                "}";
        Map<String, Contract> ret = TestBlockchain.compile(contractSrc);
        Contract contract = ret.get("Example2");
        DeployedContract deployed = testBlockchain.deploy(TestBlockchain.CREDENTIAL_0, contract);

        List<Long> list = new ArrayList<>();
        list.add(10L);
        list.add(20L);
        list.add(12L);
        List<Event> events = testBlockchain.call(deployed, "mint", list);

        Object result = testBlockchain.callConstant(deployed, "result").get(0).getValue();
        Assert.assertEquals(new BigInteger("42"), result);
    }

    @Test
    public void testLibrary() throws IOException, ExecutionException, InterruptedException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        String contractSrc1 = "pragma solidity ^0.4.24;\n" +
                "import \"./LibraryTest.sol\";\n" +
                "contract LibImport {\n" +
                "    using LibTest for uint256;\n" +
                "    function testMe() public view returns (uint256) {\n" +
                "        uint256 a = 0;\n" +
                "        return a.test();\n" +
                "    }\n" +
                "}";
        String contractSrc2 = "pragma solidity ^0.4.24;\n" +
                "library LibTest {\n" +
                "    function test(uint256 a) pure returns (uint256) {\n" +
                "        return 42;\n" +
                "    }\n" +
                "}";
        Map<String, String> contracts = new HashMap<>();
        contracts.put("./LibraryTest.sol", contractSrc2);
        Map<String, Contract> ret = TestBlockchain.compileInline(contractSrc1, contracts);
        Contract contract = ret.get("LibImport");
        DeployedContract deployed = testBlockchain.deploy(TestBlockchain.CREDENTIAL_0, contract, ret);
        Object result = testBlockchain.callConstant(deployed, "testMe").get(0).getValue();
        Assert.assertEquals(new BigInteger("42"), result);
    }

    @Test
    public void testLibrary2() throws IOException, ExecutionException, InterruptedException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        String contractSrc1 = "pragma solidity ^0.4.24;\n" +
                "import \"./LibraryTest.sol\";\n" +
                "contract LibImport {\n" +
                "    function testMe() public view returns (uint256) {\n" +
                "        return LibTest.test(0);\n" +
                "    }\n" +
                "}";
        String contractSrc2 = "pragma solidity ^0.4.24;\n" +
                "library LibTest {\n" +
                "    function test(uint256 a) pure returns (uint256) {\n" +
                "        return 42;\n" +
                "    }\n" +
                "}";
        Map<String, String> contracts = new HashMap<>();
        contracts.put("./LibraryTest.sol", contractSrc2);
        Map<String, Contract> ret = TestBlockchain.compileInline(contractSrc1, contracts);
        Contract contract = ret.get("LibTest");
        DeployedContract deployed = testBlockchain.deploy(TestBlockchain.CREDENTIAL_0, contract);
        Object result = testBlockchain.callConstant(deployed, "testMe").get(0).getValue();
        Assert.assertEquals(new BigInteger("42"), result);
    }
}
