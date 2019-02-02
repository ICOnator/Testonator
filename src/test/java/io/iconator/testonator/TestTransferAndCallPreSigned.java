package io.iconator.testonator;

import org.junit.*;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Bytes4;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Hash;
import org.web3j.utils.Numeric;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static io.iconator.testonator.TestBlockchain.*;

public class TestTransferAndCallPreSigned {
    private static TestBlockchain blockchain;
    private static Map<String, Contract> contracts;

    @BeforeClass
    public static void setup() throws Exception {
        blockchain = TestBlockchain.runLocal();
        contracts = TestUtils.setup();

        //compile test receiving contract
        File contractFile = Paths.get(ClassLoader.getSystemResource("Inventory.sol").toURI()).toFile();
        Map<String, Contract> testContracts = compile(contractFile);
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
    public void testTransferPreSigned() throws InterruptedException, ExecutionException, IOException, NoSuchMethodException, InstantiationException, IllegalAccessException, ConvertException, InvocationTargetException {
        //transfers: should transfer 10000 to accounts[1] with accounts[0] having 10000
        DeployedContract deployed = blockchain.deploy(CREDENTIAL_0, contracts.get("DOS"));
        DeployedContract deployedInventory = blockchain.deploy(CREDENTIAL_0, contracts.get("Inventory").constructor(Cb.constructor("address", deployed.contractAddress())));
        TestUtils.mint(blockchain, deployed, CREDENTIAL_0.getAddress(), CREDENTIAL_1.getAddress(), CREDENTIAL_2.getAddress(), 10000, 1000, 10);

        List<Type> retGetAddress = blockchain.callConstant(deployedInventory, Fb.name("getAddress").output("address"));

        //keccak256(abi.encodePacked(bytes4(0x5c4b4c12), _token, _to, _value, _fee));

        BigInteger addressToken = Numeric.toBigInt(deployed.contractAddress());
        BigInteger addressInventory = Numeric.toBigInt(deployedInventory.contractAddress());

        String s1 = "0x5c4b4c12" + pad32(addressToken) + pad32(addressInventory) + pad32(new BigInteger("500"))+ pad32(new BigInteger("1"));
        String keccak = Hash.sha3(s1);
        System.out.println("raw: "+s1);

        String encoded = io.iconator.testonator.Utils.encodeParameters(0,
                new Bytes4(Numeric.hexStringToByteArray("0x5c4b4c12")),
                new Address(deployed.contractAddress()),
                new Address(deployedInventory.contractAddress()),
                new Uint256(new BigInteger("500")),
                new Uint256(new BigInteger("1")));

        System.out.println("parameters: "+encoded);

        System.out.println("keccak: "+keccak);

        List<Type> ret = blockchain.callConstant(deployedInventory, "transferPreSignedHashing", deployed.contractAddress(), deployedInventory.contractAddress(), 500, 1);

        System.out.println("ret: "+Numeric.toHexString((byte[])ret.get(0).getValue()));

        List<Type> ret1 = blockchain.callConstant(deployedInventory, "transferPreSigned", deployed.contractAddress(), deployedInventory.contractAddress(), 500, 1);

        Assert.assertEquals(s1, Numeric.toHexString((byte[])ret1.get(0).getValue()));

        //System.out.println("string: "+);

        //function transferPreSigned(bytes memory _signature, address _to, uint256 _value, uint256 _fee) public returns (bool) {
        blockchain.call(deployed,
                Fb.name("transferPreSigned")
                        .input("bytes", "0x5c4b4c12")
                        .input("address", deployedInventory.contractAddress())
                        .input("uint256", new BigInteger("500"))
                        .input("uint256", new BigInteger("1"))
                        .output("bool"));



    }

    public String pad32(BigInteger big) {
        String hex = big.toString(16);
        return ("0000000000000000000000000000000000000000000000000000000000000000" + hex).substring(hex.length());
    }
}
