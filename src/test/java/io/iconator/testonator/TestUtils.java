package io.iconator.testonator;

import org.junit.Assert;
import org.web3j.abi.datatypes.Type;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static io.iconator.testonator.TestBlockchain.compile;

public class TestUtils {

    private static Map<String, Contract> contracts = null;
    private static Map<String, Contract> contractsSnapshot = null;
    private static Map<String, Contract> contractsTransferAndCall = null;

    public static Map<String, Contract> setup() throws Exception {
        if(contracts != null) {
            return contracts;
        }
        File contractFile1 = Paths.get(ClassLoader.getSystemResource("SafeMath.sol").toURI()).toFile();
        File contractFile2 = Paths.get(ClassLoader.getSystemResource("Utils.sol").toURI()).toFile();
        File contractFile3 = Paths.get(ClassLoader.getSystemResource("DOS.sol").toURI()).toFile();
        Map<String, Contract> contracts = compile(contractFile3, contractFile1, contractFile2);
        Assert.assertEquals(5, contracts.size()); //DOS.sol has 3 (ERC20, DOS, ERC865Plus677ish), plus the other 2 are 5
        for(String name:contracts.keySet()) {
            System.out.println("Available contract names: " + name);
        }
        TestUtils.contracts = contracts;
        return contracts;
    }

    public static Map<String, Contract> setupSnapshot() throws Exception {
        if(contractsSnapshot != null) {
            return contractsSnapshot;
        }
        File contractFile1 = Paths.get(ClassLoader.getSystemResource("ERC20Snapshot.sol").toURI()).toFile();
        File contractFile2 = Paths.get(ClassLoader.getSystemResource("Voting.sol").toURI()).toFile();
        Map<String, Contract> contracts = compile(contractFile1);
        contracts.putAll(compile(contractFile2));
        Assert.assertEquals(3, contracts.size());
        for(String name:contracts.keySet()) {
            System.out.println("Available contract names: " + name);
        }
        TestUtils.contractsSnapshot = contracts;
        return contractsSnapshot;
    }

    public static void mint(TestBlockchain blockchain, DeployedContract deployed, String address1, String address2,
                            String address3, int value1, int value2, int value3)
            throws NoSuchMethodException, InterruptedException, ExecutionException, InstantiationException, ConvertException, IllegalAccessException, InvocationTargetException, IOException {
        mint(blockchain, deployed, address1, address2, address3, value1, value2, value3, null, true);
    }

    public static void mint(TestBlockchain blockchain, DeployedContract deployed, String address1, String address2,
                            String address3, int value1, int value2, int value3, String whitelist)
            throws NoSuchMethodException, InterruptedException, ExecutionException, InstantiationException, ConvertException, IllegalAccessException, InvocationTargetException, IOException {
        mint(blockchain, deployed, address1, address2, address3, value1, value2, value3, whitelist, true);
    }


    public static void mint(TestBlockchain blockchain, DeployedContract deployed, String address1, String address2,
                            String address3, int value1, int value2, int value3, String whitelist, boolean setFlag)
            throws NoSuchMethodException, InstantiationException, IllegalAccessException, ConvertException,
            InvocationTargetException, InterruptedException, ExecutionException, IOException {
        List<String> addresses = new ArrayList<>(3);
        List<BigInteger> values = new ArrayList<>(3);
        int counter = 0;

        if(value1 > 0) {
            addresses.add(address1);
            values.add(BigInteger.valueOf(value1));
            counter ++;
        }

        if(value2 > 0) {
            addresses.add(address2);
            values.add(BigInteger.valueOf(value2));
            counter ++;
        }

        if(value3 > 0) {
            addresses.add(address3);
            values.add(BigInteger.valueOf(value3));
            counter ++;
        }

        List<Event> events = blockchain.call(deployed,
                Fb.name("mint")
                        .input("address[]", addresses)
                        .input("uint256[]", values));

        Assert.assertEquals(counter, events.size());
        if(value1 > 0) {
            Assert.assertEquals("" + value1, events.get(0).values().get(2).getValue().toString());
        }

        events = blockchain.call(deployed,
                Fb.name("setAdmin")
                        .input("address", TestBlockchain.CREDENTIAL_1.getAddress())
                        .input("address", TestBlockchain.CREDENTIAL_2.getAddress()));

        if(whitelist != null) {
            events = blockchain.call(deployed,
                    Fb.name("addWhitelist")
                            .input("address", whitelist));
        }

        if(setFlag) {
            events = blockchain.call(deployed, Fb.name("finishMinting"));
            Assert.assertEquals(0, events.size());

            List<Type> result = blockchain.callConstant(deployed, Fb.name("mintingDone").output("bool"));
            Assert.assertEquals("true", result.get(0).getValue().toString());
        }

    }
}
