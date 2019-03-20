package io.iconator.testonator;

import org.junit.*;
import org.web3j.abi.datatypes.*;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.utils.Numeric;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static io.iconator.testonator.TestBlockchain.*;

public class TestTransferAndCall {

    private static TestBlockchain blockchain;
    private static Map<String, Contract> contracts;

    @BeforeClass
    public static void setup() throws Exception {
        blockchain = TestBlockchain.runLocal();
        contracts = TestUtils.setup();

        //compile test receiving contract
        File contractFile = Paths.get(ClassLoader.getSystemResource("TestSomeContract.sol").toURI()).toFile();
        Map<String, Contract> testContracts = compile(contractFile);
        contracts.putAll(testContracts);

        //compile test receiving contract
        contractFile = Paths.get(ClassLoader.getSystemResource("EncodingTest.sol").toURI()).toFile();
        testContracts = compile(contractFile);
        contracts.putAll(testContracts);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        blockchain.shutdown();
    }

    @After
    public void afterTests() {
        blockchain.reset();
    }

    @Test
    public void testEncoding() throws InterruptedException, ExecutionException, IOException, NoSuchMethodException, InstantiationException, IllegalAccessException, ConvertException, InvocationTargetException {
        DeployedContract dcEncoding = blockchain.deploy(CREDENTIAL_0, contracts.get("EncodingTest"));

        byte[] methodName = Numeric.hexStringToByteArray("0x75b7e9f9");

        String encoded = io.iconator.testonator.Utils.encodeParameters(2, new DynamicBytes(Numeric.hexStringToByteArray("0x30783235")));

        byte[] arg = Numeric.hexStringToByteArray(encoded);

        List<Event> result = blockchain.call(
                dcEncoding, "encode",
                methodName,
                arg,
                CREDENTIAL_0.getAddress(),
                BigInteger.valueOf(99));

        System.out.println("FunctionSelector: " + Numeric.toHexString(methodName));
        System.out.println("FROM: " + CREDENTIAL_0.getAddress());
        System.out.println("VALUE: " + BigInteger.valueOf(99));
        System.out.println("ARGS: " + encoded);

        Assert.assertEquals(2, result.size());

        DynamicBytes chain = (DynamicBytes) result.get(0).values().get(0);
        DynamicBytes dezos = (DynamicBytes) result.get(1).values().get(0);

        System.out.println("ChainSolution: " + Numeric.toHexString(chain.getValue()));
        System.out.println("DezosSolution: " + Numeric.toHexString(dezos.getValue()));

        byte[] expected = Numeric.hexStringToByteArray("0x75b7e9f9" +
                "0000000000000000000000003572f8c373c15df4042d38c1b3b67d70429ca65a" +
                "0000000000000000000000000000000000000000000000000000000000000063" +
                "0000000000000000000000000000000000000000000000000000000000000060" +
                "0000000000000000000000000000000000000000000000000000000000000004" +
                "3078323500000000000000000000000000000000000000000000000000000000");

        Assert.assertArrayEquals(expected, dezos.getValue());
    }

    @Test
    public void testCallNoArgs() throws InterruptedException, ExecutionException, IOException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, ConvertException {
        DeployedContract dcDOS = blockchain.deploy(CREDENTIAL_0, contracts.get("DOS"));
        DeployedContract dcTest = blockchain.deploy(CREDENTIAL_0, contracts.get("TestSomeContract"));
        dcDOS.addReferencedContract(dcTest.contract());

        mint(dcDOS, dcTest.contractAddress());

        //in order to call function someName(address _from, uint256 _value, bool _testBoolean, string _testString, address[] _testArray)
        //we need to find the function hash first, use keccak for the signature: someName(address,uint256) -> fac42a59
        String methodName = io.iconator.testonator.Utils.functionHash("someName(address,uint256)");
        System.out.println("method name: "+methodName);

        List<Event> result = blockchain.call(CREDENTIAL_1, dcDOS, "transferAndCall", dcTest.contractAddress(), new BigInteger("100"), Numeric.hexStringToByteArray(methodName), new byte[0]);
        Assert.assertEquals(3, result.size());
        System.out.println(result.size());

        blockchain.call(dcDOS, "removeWhitelist", dcTest.contractAddress());

        result = blockchain.call(CREDENTIAL_1, dcDOS, "transferAndCall", dcTest.contractAddress(), new BigInteger("100"), Numeric.hexStringToByteArray(methodName), new byte[0]);
        Assert.assertEquals(null, result);
    }

    @Test
    public void testCallSimpleArgs() throws InterruptedException, ExecutionException, IOException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, ConvertException {
        DeployedContract dcDOS = blockchain.deploy(CREDENTIAL_0, contracts.get("DOS"));
        DeployedContract dcTest = blockchain.deploy(CREDENTIAL_0, contracts.get("TestSomeContract"));
        dcDOS.addReferencedContract(dcTest.contract());

        mint(dcDOS, dcTest.contractAddress());

        //in order to call function someName(address _from, uint256 _value, bool _testBoolean, string _testString, address[] _testArray)
        //we need to find the function hash first, use keccak for the signature: someName(address,uint256,uint256) -> a67045bf
        String methodName = io.iconator.testonator.Utils.functionHash("someName(address,uint256,uint256)");
        System.out.println("method name: "+methodName);

        String encoded = io.iconator.testonator.Utils.encodeParameters(2, new Uint256(new BigInteger("12345")));
        System.out.println("parameters: "+encoded);

        List<Event> result = blockchain.call(CREDENTIAL_1, dcDOS, "transferAndCall", dcTest.contractAddress(), new BigInteger("100"), Numeric.hexStringToByteArray(methodName), Numeric.hexStringToByteArray(encoded));
        Assert.assertEquals(3, result.size());
        Uint256 u1 = (Uint256) result.get(2).values().get(2);
        Uint256 u2 = (Uint256) result.get(2).values().get(3);
        Assert.assertEquals(new Uint256(new BigInteger("100")).getValue(), u1.getValue());
        Assert.assertEquals(new Uint256(new BigInteger("12345")).getValue(), u2.getValue());
        System.out.println(result.size());
    }

    @Test
    public void testCallComplexArgs() throws InterruptedException, ExecutionException, IOException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, ConvertException {
        DeployedContract dcDOS = blockchain.deploy(CREDENTIAL_0, contracts.get("DOS"));
        DeployedContract dcTest = blockchain.deploy(CREDENTIAL_0, contracts.get("TestSomeContract"));
        dcDOS.addReferencedContract(dcTest.contract());

        mint(dcDOS, dcTest.contractAddress());

        //in order to call function someName(address _from, uint256 _value, bool _testBoolean, string _testString, address[] _testArray)
        //we need to find the function hash first, use keccak for the signature: someName(address,uint256,bool,string,address[]) -> aef6af1c
        String methodName = io.iconator.testonator.Utils.functionHash("someName(address,uint256,bool,string,address[])");
        System.out.println("method name: "+methodName);

        List<Type> params = new ArrayList<Type>();

        String encoded = io.iconator.testonator.Utils.encodeParameters(2,
                new Bool(true),
                new Utf8String("testme"),
                io.iconator.testonator.Utils.createArray(
                        new Address(CREDENTIAL_2.getAddress()),
                        new Address(CREDENTIAL_3.getAddress()))
        );
        System.out.println("parameters: "+encoded);

        List<Event> result = blockchain.call(CREDENTIAL_1, dcDOS, "transferAndCall", dcTest.contractAddress(), new BigInteger("100"), Numeric.hexStringToByteArray(methodName), Numeric.hexStringToByteArray(encoded));

        Assert.assertEquals(3, result.size());
        Assert.assertEquals("testme", result.get(2).values().get(3).toString().trim());
        System.out.println(result.size());
    }

    private void mint(DeployedContract dc, String whitelist) throws NoSuchMethodException, InterruptedException, ExecutionException, InstantiationException, IllegalAccessException, InvocationTargetException, IOException, ConvertException {
        List<String> addresses = new ArrayList<>();
        List<BigInteger> values = new ArrayList<>();

        addresses.add(CREDENTIAL_1.getAddress());
        addresses.add(CREDENTIAL_2.getAddress());

        values.add(new BigInteger("20000"));
        values.add(new BigInteger("20000"));

        System.out.println(dc.contractAddress());
        List<Event> result1 = blockchain.call(dc, "mint", addresses, values);

        Assert.assertEquals(2, result1.size());
        Assert.assertEquals(new BigInteger("20000"), result1.get(1).values().get(2).getValue());

        result1 = blockchain.call(dc, "setAdmin", TestBlockchain.CREDENTIAL_1.getAddress(), TestBlockchain.CREDENTIAL_2.getAddress());
        Assert.assertEquals(0, result1.size());

        if(whitelist != null) {
            result1 = blockchain.call(dc, "addWhitelist", whitelist);
            Assert.assertEquals(0, result1.size());
        }

        List<Event> result2 = blockchain.call(dc, "finishMinting");
        Assert.assertEquals(0, result2.size());

        List<Type> result = blockchain.callConstant(dc, Fb.name("mintingDone").output("bool"));
        Assert.assertEquals("true", result.get(0).getValue().toString());
    }

}
