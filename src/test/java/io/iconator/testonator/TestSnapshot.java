package io.iconator.testonator;

import org.junit.*;
import org.web3j.abi.datatypes.Type;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static io.iconator.testonator.TestBlockchain.CREDENTIAL_0;
import static io.iconator.testonator.TestBlockchain.CREDENTIAL_1;

public class TestSnapshot {
    private static TestBlockchain blockchain;
    private static Map<String, Contract> contracts;

    @BeforeClass
    public static void setup() throws Exception {
        blockchain = TestBlockchain.runLocal();
        contracts = TestUtils.setupSnapshot();
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
    public void testTransfer() throws InterruptedException, ExecutionException, IOException, NoSuchMethodException, InstantiationException, IllegalAccessException, ConvertException, InvocationTargetException {
        //transfers: should transfer 10000 to accounts[1] with accounts[0] having 10000
        DeployedContract deployed = blockchain.deploy(CREDENTIAL_0, contracts.get("ERC20Snapshot"));
        DeployedContract deployedVoting = blockchain.deploy(CREDENTIAL_0, contracts.get("Voting"));
        mint(blockchain, deployed, deployedVoting, deployed.contractAddress());

        blockchain.call(deployedVoting,
                new FunctionBuilder("vote").addInput("bool", true));

        List<Type> ret = blockchain.callConstant(deployedVoting, new FunctionBuilder("yay").outputs("uint256"));

        Assert.assertEquals("10000", ret.get(0).getValue().toString());

        blockchain.call(deployed,
                new FunctionBuilder("transfer")
                        .addInput("address", CREDENTIAL_1.getAddress())
                        .addInput("uint256", new BigInteger("9999"))
                        .outputs("bool"));

        //credential 0 has voting power 1, as he transferred the rest to credential 1
        blockchain.call(deployedVoting,
                new FunctionBuilder("vote").addInput("bool", true));
        ret = blockchain.callConstant(deployedVoting, new FunctionBuilder("yay").outputs("uint256"));
        Assert.assertEquals("10001", ret.get(0).getValue().toString());

        blockchain.call(deployed,
                new FunctionBuilder("transfer")
                        .addInput("address", CREDENTIAL_1.getAddress())
                        .addInput("uint256", new BigInteger("1"))
                        .outputs("bool"));

        //credential 0 has still voting power 1 even though he transferred 1 to credential 1, since its after block 8
        blockchain.call(deployedVoting,
                new FunctionBuilder("vote").addInput("bool", true));
        ret = blockchain.callConstant(deployedVoting, new FunctionBuilder("yay").outputs("uint256"));
        Assert.assertEquals("10002", ret.get(0).getValue().toString());

    }

    private static void mint(TestBlockchain blockchain, DeployedContract deployed, DeployedContract deployedVoting, String contractAddress) throws NoSuchMethodException, InstantiationException, IllegalAccessException, ConvertException, InvocationTargetException, InterruptedException, ExecutionException, IOException {
        blockchain.call(deployed,
                new FunctionBuilder("mint").addInput("address", TestBlockchain.CREDENTIAL_1.getAddress()));
        blockchain.call(deployedVoting,
                new FunctionBuilder("set").addInput("address", contractAddress));
    }
}
