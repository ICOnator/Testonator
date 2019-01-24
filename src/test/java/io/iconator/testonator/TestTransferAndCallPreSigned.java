package io.iconator.testonator;

import org.junit.*;
import org.web3j.abi.datatypes.Type;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
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
        contracts = TestUtils.setupTransferAndCall();
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
    public void testTransferAndCall() throws InterruptedException, ExecutionException, IOException, NoSuchMethodException, InstantiationException, IllegalAccessException, ConvertException, InvocationTargetException {
        //transfers: should transfer 10000 to accounts[1] with accounts[0] having 10000
        DeployedContract deployed = blockchain.deploy(CREDENTIAL_0, contracts.get("DOS"));
        DeployedContract deployedVoting = blockchain.deploy(CREDENTIAL_0, contracts.get("TransferVoting").constructor(Cb.constructor("address", deployed.contractAddress())));
        TestUtils.mint(blockchain, deployed, CREDENTIAL_0.getAddress(), CREDENTIAL_1.getAddress(), CREDENTIAL_2.getAddress(), 10000, 1000, 10);

        List<Type> retGetAddress = blockchain.callConstant(deployedVoting, Fb.name("getAddress").output("address"));

        blockchain.call(deployed,
                Fb.name("transferAndCall")
                        .input("address", CREDENTIAL_1.getAddress())
                        .input("uint256", new BigInteger("9999"))
                        .output("bool"));



    }
}
