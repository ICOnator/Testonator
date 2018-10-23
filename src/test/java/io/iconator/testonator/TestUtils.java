package io.iconator.testonator;

import org.junit.Assert;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.web3j.abi.datatypes.Type;


import static io.iconator.testonator.TestBlockchain.CREDENTIAL_0;
import static io.iconator.testonator.TestBlockchain.compile;

public class TestUtils {

    private static Map<String, Contract> contracts = null;

    public static Map<String, Contract> setup() throws Exception {
        if(contracts != null) {
            return contracts;
        }
        File contractFile1 = Paths.get(ClassLoader.getSystemResource("SafeMath.sol").toURI()).toFile();
        File contractFile2 = Paths.get(ClassLoader.getSystemResource("Utils.sol").toURI()).toFile();
        File contractFile3 = Paths.get(ClassLoader.getSystemResource("DOS.sol").toURI()).toFile();
        File contractFile4 = Paths.get(ClassLoader.getSystemResource("SafeMath192.sol").toURI()).toFile();
        Map<String, Contract> contracts = compile(contractFile3, contractFile1, contractFile2, contractFile4);
        Assert.assertEquals(6, contracts.size()); //DOS.sol has 3 (ERC20, DOS, ERC865Plus677ish), plus the other 3 are 6
        for(String name:contracts.keySet()) {
            System.out.println("Available contract names: " + name);
        }
        TestUtils.contracts = contracts;
        return contracts;
    }

    public static void mint(TestBlockchain blockchain, DeployedContract deployed, String address1, String address2, String address3, int value1, int value2, int value3) throws NoSuchMethodException, InterruptedException, ExecutionException, InstantiationException, ConvertException, IllegalAccessException, InvocationTargetException, IOException {
        mint(blockchain, deployed, address1, address2, address3, value1, value2, value3, true);
    }


    public static void mint(TestBlockchain blockchain, DeployedContract deployed, String address1, String address2, String address3, int value1, int value2, int value3, boolean setFlag) throws NoSuchMethodException, InstantiationException, IllegalAccessException, ConvertException, InvocationTargetException, InterruptedException, ExecutionException, IOException {
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
                new FunctionBuilder("mint").addInput("address[]", addresses)
                        .addInput("uint192[]", values));

        Assert.assertEquals(counter, events.size());
        if(value1 > 0) {
            Assert.assertEquals("" + value1, events.get(0).values().get(2).getValue().toString());
        }

        if(setFlag) {
            events = blockchain.call(deployed,
                    new FunctionBuilder("finishMinting"));
            Assert.assertEquals(0, events.size());

            List<Type> result = blockchain.callConstant(deployed, new FunctionBuilder("mintingDone").outputs("bool"));
            Assert.assertEquals("true", result.get(0).getValue().toString());
        }

    }
}
