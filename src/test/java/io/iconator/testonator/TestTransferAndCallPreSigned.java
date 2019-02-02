package io.iconator.testonator;

import org.junit.*;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.DynamicBytes;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Bytes4;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Hash;
import org.web3j.crypto.Sign;
import org.web3j.utils.Numeric;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Random;
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
        File contractFile1 = Paths.get(ClassLoader.getSystemResource("Inventory.sol").toURI()).toFile();
        Map<String, Contract> testContracts1 = compile(contractFile1);
        contracts.putAll(testContracts1);

        File contractFile2 = Paths.get(ClassLoader.getSystemResource("HashTest.sol").toURI()).toFile();
        Map<String, Contract> testContracts2 = compile(contractFile2);
        contracts.putAll(testContracts2);

        File contractFile3 = Paths.get(ClassLoader.getSystemResource("RecoverTests.sol").toURI()).toFile();
        Map<String, Contract> testContracts3 = compile(contractFile3);
        contracts.putAll(testContracts3);
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
    public void testBytesConversion() {
        //conversion from number to byte array can be reversed, sanity test
        byte[] s = Numeric.toBytesPadded(BigInteger.ONE, 32);
        BigInteger result = Numeric.toBigInt(s);
        Assert.assertEquals(result, BigInteger.ONE);

        Random r = new Random(42L);
        for(int i=0;i<1000;i++) {
            BigInteger input = new BigInteger(r.nextInt(256), r);
            s = Numeric.toBytesPadded(input, 32);
            result = Numeric.toBigInt(s);
            Assert.assertEquals(result, input);
        }
    }

    @Test
    public void testNumberConversion() {
        //web3j does not generate malleable addresses, sanity test
        for(int i=0;i<1000;i++) {
            Sign.SignatureData sigData = Sign.signMessage(("yes"+i).getBytes(), CREDENTIAL_0.getEcKeyPair(), true);
            final BigInteger HALF_CURVE_ORDER = Sign.CURVE_PARAMS.getN().shiftRight(1);
            if(Numeric.toBigInt(sigData.getS()).compareTo(HALF_CURVE_ORDER) == 1) {
                Assert.fail("high S");
            }
        }
    }

    @Test
    public void testMalleability() throws InterruptedException, ExecutionException, IOException, NoSuchMethodException, InstantiationException, IllegalAccessException, ConvertException, InvocationTargetException {
        DeployedContract deployedHash = blockchain.deploy(CREDENTIAL_0, contracts.get("RecoverTests"));

        String hash = Hash.sha3("0x123");

        Sign.SignatureData sigData = Sign.signMessage(
                Numeric.hexStringToByteArray(hash),
                CREDENTIAL_0.getEcKeyPair(),
                false);

        byte[] sig = new byte[65];
        System.arraycopy(sigData.getR(),0,sig,0,32);
        System.arraycopy(sigData.getS(),0,sig,32,32);
        sig[64] = sigData.getV();
        //sig[0] = 1;

        List<Type> ret = blockchain.callConstant(
                deployedHash,
                "recoverMalleable",
                Numeric.hexStringToByteArray(hash),
                sig);

        Assert.assertEquals("0x3572f8c373c15df4042d38c1b3b67d70429ca65a", ret.get(0).getValue());

        sig[64] = 0;
        ret = blockchain.callConstant(
                deployedHash,
                "recoverMalleable",
                Numeric.hexStringToByteArray(hash),
                sig);

        Assert.assertEquals("0x3572f8c373c15df4042d38c1b3b67d70429ca65a", ret.get(0).getValue());

        sig[64] = 0;
        ret = blockchain.callConstant(
                deployedHash,
                "recover",
                Numeric.hexStringToByteArray(hash),
                sig);

        Assert.assertEquals("0x0000000000000000000000000000000000000000", ret.get(0).getValue());

        //higer S

        BigInteger upper = Numeric.toBigInt("0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEBAAEDCE6AF48A03BBFD25E8CD0364141");
        BigInteger s = Numeric.toBigInt(sigData.getS());
        BigInteger s2 = upper.subtract(s);

        System.arraycopy(sigData.getR(),0,sig,0,32);
        System.arraycopy(Numeric.toBytesPadded(s2, 32),0,sig,32,32);
        sig[64] = 1;

        ret = blockchain.callConstant(
                deployedHash,
                "recoverMalleable",
                Numeric.hexStringToByteArray(hash),
                sig);

        Assert.assertEquals("0x3572f8c373c15df4042d38c1b3b67d70429ca65a", ret.get(0).getValue());

        ret = blockchain.callConstant(
                deployedHash,
                "recover",
                Numeric.hexStringToByteArray(hash),
                sig);

        Assert.assertEquals("0x0000000000000000000000000000000000000000", ret.get(0).getValue());

    }

    @Test
    public void testTransferPreSignedHash() throws InterruptedException, ExecutionException, IOException, NoSuchMethodException, InstantiationException, IllegalAccessException, ConvertException, InvocationTargetException {
        DeployedContract deployedHash = blockchain.deploy(CREDENTIAL_0, contracts.get("HashTest"));

        String s1 =
                padRight("15420b71")
                + pad32(Numeric.toBigInt(CREDENTIAL_0.getAddress()))
                + pad32(Numeric.toBigInt(CREDENTIAL_1.getAddress()))
                + pad32(new BigInteger("500"))
                + pad32(new BigInteger("1"))
                + pad32(new BigInteger("2"));

        String keccak1 = Hash.sha3(s1);

        String encoded = io.iconator.testonator.Utils.encodeParameters(0,
                new Bytes4(Numeric.hexStringToByteArray("0x15420b71")),
                new Address(CREDENTIAL_0.getAddress()),
                new Address(CREDENTIAL_1.getAddress()),
                new Uint256(new BigInteger("500")),
                new Uint256(new BigInteger("1")),
                new Uint256(new BigInteger("2")));

        String keccak2 = Hash.sha3(encoded);

        List<Type> ret = blockchain.callConstant(
                deployedHash,
                "transferPreSignedHashing",
                CREDENTIAL_0.getAddress(),
                CREDENTIAL_1.getAddress(),
                500,
                1,
                2);

        Assert.assertEquals(keccak2, keccak1);
        Assert.assertEquals(keccak1, Numeric.toHexString((byte[])ret.get(0).getValue()));

    }

    @Test
    public void testTransferAndCallPreSignedHashing() throws InterruptedException, ExecutionException, IOException, NoSuchMethodException, InstantiationException, IllegalAccessException, ConvertException, InvocationTargetException {
        DeployedContract deployedHash = blockchain.deploy(CREDENTIAL_0, contracts.get("HashTest"));

        Bytes4 b4 = new Bytes4(Numeric.hexStringToByteArray("0x12345667"));
        Random r = new Random(42);
        byte[] bb = new byte[1000];
        r.nextBytes(bb);
        String encoded = io.iconator.testonator.Utils.encodeParameters(0,
                new Bytes4(Numeric.hexStringToByteArray("0x38980f82")),
                new Address(CREDENTIAL_0.getAddress()),
                new Address(CREDENTIAL_1.getAddress()),
                new Uint256(new BigInteger("500")),
                new Uint256(new BigInteger("1")),
                new Uint256(new BigInteger("2")),
                b4,
                new DynamicBytes(bb));

        String keccak2 = Hash.sha3(encoded);

        List<Type> ret = blockchain.callConstant(
                deployedHash,
                "transferAndCallPreSignedHashing",
                CREDENTIAL_0.getAddress(),
                CREDENTIAL_1.getAddress(),
                500,
                1,
                2,
                b4.getValue(),
                bb);

        Assert.assertEquals(keccak2, Numeric.toHexString((byte[])ret.get(0).getValue()));

    }

    @Test
    public void testTransferPreSigned() throws InterruptedException, ExecutionException, IOException, NoSuchMethodException, InstantiationException, IllegalAccessException, ConvertException, InvocationTargetException {
        DeployedContract deployed = blockchain.deploy(CREDENTIAL_0, contracts.get("DOS"));
        TestUtils.mint(blockchain, deployed, CREDENTIAL_0.getAddress(), CREDENTIAL_1.getAddress(), CREDENTIAL_2.getAddress(), 10000, 1000, 10);

        //now we sign from cred0 offline to send 9999 DOS to cred1. This will be done by cred3

        String encoded = io.iconator.testonator.Utils.encodeParameters(0,
                new Bytes4(Numeric.hexStringToByteArray("0x15420b71")),
                new Address(deployed.contractAddress()),
                new Address(CREDENTIAL_1.getAddress()),
                new Uint256(new BigInteger("9999")),
                new Uint256(new BigInteger("1")),
                new Uint256(new BigInteger("2")));

        String keccak = Hash.sha3(encoded);

        System.out.println("hash:" + keccak);

        Sign.SignatureData sigData = Sign.signMessage(
                Numeric.hexStringToByteArray(keccak),
                CREDENTIAL_0.getEcKeyPair(),
                false);

        byte[] sig = new byte[65];
        System.arraycopy(sigData.getR(),0,sig,0,32);
        System.arraycopy(sigData.getS(),0,sig,32,32);
        sig[64] = sigData.getV();

        List<Event> event = blockchain.call(CREDENTIAL_3, deployed,
                "transferPreSigned",
                sig,
                CREDENTIAL_1.getAddress(),
                new BigInteger("9999"),
                new BigInteger("1"),
                new BigInteger("2"));

        List<Type> ret = blockchain.callConstant(deployed, "balanceOf", CREDENTIAL_3.getAddress());
        Assert.assertEquals(new BigInteger("1"), ret.get(0).getValue());
        ret = blockchain.callConstant(deployed, "balanceOf", CREDENTIAL_0.getAddress());
        Assert.assertEquals(new BigInteger("0"), ret.get(0).getValue());
        ret = blockchain.callConstant(deployed, "balanceOf", CREDENTIAL_1.getAddress());
        Assert.assertEquals(new BigInteger("10999"), ret.get(0).getValue());

    }

    @Test
    public void testTransferAndCallPreSigned() throws InterruptedException, ExecutionException, IOException, NoSuchMethodException, InstantiationException, IllegalAccessException, ConvertException, InvocationTargetException {
        DeployedContract deployed = blockchain.deploy(CREDENTIAL_0, contracts.get("DOS"));
        DeployedContract deployedInventory = blockchain.deploy(CREDENTIAL_5, contracts.get("Inventory").constructor(Cb.constructor("address", deployed.contractAddress())));
        TestUtils.mint(blockchain, deployed, CREDENTIAL_0.getAddress(), CREDENTIAL_1.getAddress(), CREDENTIAL_2.getAddress(), 10000, 1000, 10);

        //cred 1 stores entries in the Inventory contract via cred 3. cred0 gets the token and ends up with 10001

        String invParam = io.iconator.testonator.Utils.encodeParameters(2,
                new Utf8String("serial"),
                new Utf8String("description"));

        byte[] b4 = Numeric.hexStringToByteArray("0x5c28b451");
        byte[] args = Numeric.hexStringToByteArray(invParam);
        String encoded = io.iconator.testonator.Utils.encodeParameters(0,
                new Bytes4(Numeric.hexStringToByteArray("0x38980f82")),
                new Address(deployed.contractAddress()),
                new Address(deployedInventory.contractAddress()),
                new Uint256(new BigInteger("1")),
                new Uint256(new BigInteger("1")),
                new Uint256(new BigInteger("2")),
                new Bytes4(b4),
                new DynamicBytes(args));

        String keccak = Hash.sha3(encoded);

        System.out.println("hash:" + keccak);

        Sign.SignatureData sigData = Sign.signMessage(
                Numeric.hexStringToByteArray(keccak),
                CREDENTIAL_1.getEcKeyPair(),
                false);

        byte[] sig = new byte[65];
        System.arraycopy(sigData.getR(),0,sig,0,32);
        System.arraycopy(sigData.getS(),0,sig,32,32);
        sig[64] = sigData.getV();

        List<Event> event = blockchain.call(CREDENTIAL_3, deployed,
                "transferAndCallPreSigned",
                sig,
                deployedInventory.contractAddress(),
                new BigInteger("1"),
                new BigInteger("1"),
                new BigInteger("2"),
                b4,
                args);

        List<Type> ret = blockchain.callConstant(deployed, "balanceOf", CREDENTIAL_3.getAddress());
        Assert.assertEquals(new BigInteger("1"), ret.get(0).getValue());
        ret = blockchain.callConstant(deployed, "balanceOf", CREDENTIAL_0.getAddress());
        Assert.assertEquals(new BigInteger("10000"), ret.get(0).getValue());
        ret = blockchain.callConstant(deployed, "balanceOf", CREDENTIAL_1.getAddress());
        Assert.assertEquals(new BigInteger("998"), ret.get(0).getValue());
        ret = blockchain.callConstant(deployed, "balanceOf", deployedInventory.contractAddress());
        Assert.assertEquals(new BigInteger("1"), ret.get(0).getValue());

        ret = blockchain.callConstant(deployedInventory, "itemSerialAt", CREDENTIAL_1.getAddress(), 0);
        Assert.assertEquals("serial", ret.get(0).getValue());

        blockchain.call(CREDENTIAL_5, deployedInventory, "payout", CREDENTIAL_6.getAddress());

        ret = blockchain.callConstant(deployed, "balanceOf", CREDENTIAL_6.getAddress());
        Assert.assertEquals(new BigInteger("1"), ret.get(0).getValue());

    }

    private static String padRight(String big) {
        return big+"0000000000000000000000000000000000000000000000000000000000000000".substring(0,64-big.length());
    }

    private static String pad32(BigInteger big) {
        return Numeric.toHexStringNoPrefixZeroPadded(big, 64);
    }

    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
}
