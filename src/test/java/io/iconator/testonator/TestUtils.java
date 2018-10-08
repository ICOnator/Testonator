package io.iconator.testonator;

import org.junit.Assert;

import java.io.File;
import java.nio.file.Paths;
import java.util.Map;

import static io.iconator.testonator.TestBlockchain.compile;

public class TestUtils {

    private static Map<String, Contract> contracts = null;

    public static Map<String, Contract> setup() throws Exception {
        if(contracts != null) {
            return contracts;
        }
        File contractFile1 = Paths.get(ClassLoader.getSystemResource("SafeMath.sol").toURI()).toFile();
        File contractFile2 = Paths.get(ClassLoader.getSystemResource("Utils.sol").toURI()).toFile();
        File contractFile3 = Paths.get(ClassLoader.getSystemResource("Eureka.sol").toURI()).toFile();
        Map<String, Contract> contracts = compile(contractFile3, contractFile1, contractFile2);
        Assert.assertEquals(5, contracts.size());
        for(String name:contracts.keySet()) {
            System.out.println("Available contract names: " + name);
        }
        TestUtils.contracts = contracts;
        return contracts;
    }


}
