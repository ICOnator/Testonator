package io.iconator.testrpcj;

import org.ethereum.core.CallTransaction;
import org.ethereum.solidity.compiler.CompilationResult;
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
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.Web3ClientVersion;
import org.web3j.protocol.http.HttpService;
import org.web3j.tuples.generated.Tuple2;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.ethereum.solidity.compiler.SolidityCompiler.Options.*;
import static org.ethereum.solidity.compiler.SolidityCompiler.Result;
public class TestDeploy {

    private static TestBlockchain testBlockchain;
    private static Web3j web3j;

    @BeforeClass
    public static void setup() throws Exception {
        testBlockchain = TestBlockchain.start();
        web3j = Web3j.build(new HttpService("http://localhost:8545/rpc"));
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
        Result r = TestBlockchain.compiler().compile(
                contractSrc.getBytes(), true, ABI, BIN, INTERFACE, METADATA, GAS);
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
        Map<String, Tuple2<EthCompileSolidity.Code, List<Tuple2<CallTransaction.Function, BigInteger>>>> ret =
                Utils.compile(contractSrc);

        System.out.println(ret);
        //Map<String, Integer> gas = Utils.parseGasEstimation(r);

        //CompilationResult result = CompilationResult.parse(r);
        //CompilationResult.ContractMetadata a = result.getContract("Example1");
        //CallTransaction.Contract contract = new CallTransaction.Contract(a.abi);
        //contract.getConstructor();

    }

    @Test
    public void testDeploy() throws IOException {
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
        Map<String, Tuple2<EthCompileSolidity.Code, List<Tuple2<CallTransaction.Function, BigInteger>>>> ret =
                Utils.compile(contractSrc);

        EthCompileSolidity.Code c = ret.get("Exampl2").getValue1();
        Tuple2<String, EthSendTransaction> res = Utils.deploy(web3j, TestBlockchain.ACCOUNT_0, c.getCode());
        System.out.println(res.getValue2().hasError());

        final Function function = new Function("counter",
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));

        String value = Utils.callConstant(web3j, TestBlockchain.ACCOUNT_0, res.getValue1(), function).get(0).getValue().toString();
        Assert.assertEquals("12", value);

        final Function function2 = new Function("set",
                Arrays.<Type>asList(new Uint256(13L)),
                Arrays.<TypeReference<?>>asList());

        String re = Utils.call(web3j, TestBlockchain.ACCOUNT_0, res.getValue1(), BigInteger.ZERO, function2, ret.get("Exampl2").getValue2().get(2).getValue2());
        System.out.println(re);
    }

    @Test
    public void testCall() {
        //Contract
    }
}
