package io.iconator.testonator;

import org.junit.*;
import org.web3j.abi.datatypes.Type;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static io.iconator.testonator.TestBlockchain.*;

public class TestERC20 {
    private static TestBlockchain blockchain;
    private static Map<String, Contract> contracts;

    @BeforeClass
    public static void setup() throws Exception {
        blockchain = TestBlockchain.runLocal();
        contracts = TestUtils.setup();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        blockchain.shutdown();
    }

    @After
    public void afterTests() {
        blockchain.reset();
    }

    //********************* ERC20 Regular Transfers
    @Test
    public void testReverse() throws InterruptedException, ExecutionException, IOException {
        //transfers: ether transfer should be reversed
        DeployedContract deployed = blockchain.deploy(CREDENTIAL_0, contracts.get("DOS"));
        List<Event> events = blockchain.call(CREDENTIAL_0, deployed, BigInteger.ONE);
        Assert.assertNull(events);
    }

    @Test
    public void testTransferFailFlag() throws InterruptedException, ExecutionException, IOException, NoSuchMethodException, InstantiationException, IllegalAccessException, ConvertException, InvocationTargetException {
        //transfers: should transfer 10000 to accounts[1] with accounts[0] having 10000
        DeployedContract deployed = blockchain.deploy(CREDENTIAL_0, contracts.get("DOS"));

        TestUtils.mint(blockchain, deployed, CREDENTIAL_0.getAddress(), CREDENTIAL_1.getAddress(), CREDENTIAL_2.getAddress(), 10000, 1000, 10, null,false);
        List<Event> events = blockchain.call(deployed,
                new FunctionBuilder("transfer")
                        .addInput("address", CREDENTIAL_1.getAddress())
                        .addInput("uint256", new BigInteger("10000"))
                        .outputs("bool"));
        Assert.assertNull(events);
    }

    @Test
    public void testTransfer() throws InterruptedException, ExecutionException, IOException, NoSuchMethodException, InstantiationException, IllegalAccessException, ConvertException, InvocationTargetException {
        //transfers: should transfer 10000 to accounts[1] with accounts[0] having 10000
        DeployedContract deployed = blockchain.deploy(CREDENTIAL_0, contracts.get("DOS"));

        TestUtils.mint(blockchain, deployed, CREDENTIAL_0.getAddress(), CREDENTIAL_1.getAddress(), CREDENTIAL_2.getAddress(), 10000, 1000, 10);
        List<Event> events = blockchain.call(deployed,
                new FunctionBuilder("transfer")
                        .addInput("address", CREDENTIAL_1.getAddress())
                        .addInput("uint256", new BigInteger("10000"))
                        .outputs("bool"));
        Assert.assertEquals(1, events.size());
    }

    @Test
    public void testTransferFail() throws NoSuchMethodException, InterruptedException, ExecutionException, InstantiationException, ConvertException, IllegalAccessException, InvocationTargetException, IOException {
        //transfers: should fail when trying to transfer 10001 to accounts[1] with accounts[0] having 10000
        DeployedContract deployed = blockchain.deploy(CREDENTIAL_0, contracts.get("DOS"));

        TestUtils.mint(blockchain, deployed, CREDENTIAL_0.getAddress(), CREDENTIAL_1.getAddress(), CREDENTIAL_2.getAddress(), 10000, 1000, 10);
        List<Event> events = blockchain.call(deployed,
                new FunctionBuilder("transfer")
                        .addInput("address", CREDENTIAL_1.getAddress())
                        .addInput("uint256", new BigInteger("10001"))
                        .outputs("bool"));
        Assert.assertNull(events);
    }

    @Test
    public void testTransferFailContract() throws InterruptedException, ExecutionException, IOException, InvocationTargetException, NoSuchMethodException, InstantiationException, ConvertException, IllegalAccessException {
        //transfers: should fail when trying to transfer to contract address
        DeployedContract deployed = blockchain.deploy(CREDENTIAL_0, contracts.get("DOS"));

        TestUtils.mint(blockchain, deployed, CREDENTIAL_0.getAddress(), CREDENTIAL_1.getAddress(), CREDENTIAL_2.getAddress(), 10000, 1000, 10);
        List<Event> events = blockchain.call(deployed,
                new FunctionBuilder("transfer")
                        .addInput("address", deployed.contractAddress())
                        .addInput("uint256", new BigInteger("10001"))
                        .outputs("bool"));
        Assert.assertNull(events);
    }

    @Test
    public void testTransferFailZero() throws NoSuchMethodException, InterruptedException, ExecutionException, InstantiationException, ConvertException, IllegalAccessException, InvocationTargetException, IOException {
        //transfers: should fail when trying to transfer to 0x0
        DeployedContract deployed = blockchain.deploy(CREDENTIAL_0, contracts.get("DOS"));

        TestUtils.mint(blockchain, deployed, CREDENTIAL_0.getAddress(), CREDENTIAL_1.getAddress(), CREDENTIAL_2.getAddress(), 10000, 1000, 10);
        List<Event> events = blockchain.call(deployed,
                new FunctionBuilder("transfer")
                        .addInput("address", "0x0")
                        .addInput("uint256", new BigInteger("10001"))
                        .outputs("bool"));
        Assert.assertNull(events);
    }

    //********************* ERC20 Approvals

    @Test
    public void testApprove() throws NoSuchMethodException, InterruptedException, ExecutionException, InstantiationException, ConvertException, IllegalAccessException, InvocationTargetException, IOException {
        //approvals: msg.sender should approve 100 to accounts[1]
        DeployedContract deployed = blockchain.deploy(CREDENTIAL_0, contracts.get("DOS"));

        TestUtils.mint(blockchain, deployed, CREDENTIAL_0.getAddress(), CREDENTIAL_1.getAddress(), CREDENTIAL_2.getAddress(), 10000, 1000, 10);
        List<Event> events = blockchain.call(CREDENTIAL_0, deployed,
                new FunctionBuilder("approve")
                        .addInput("address", CREDENTIAL_1.getAddress())
                        .addInput("uint256", new BigInteger("100"))
                        .outputs("bool"));
        Assert.assertEquals(1, events.size());
        List<Type> results = blockchain.callConstant(deployed, new FunctionBuilder("allowance")
                .addInput("address", CREDENTIAL_0.getAddress())
                .addInput("address", CREDENTIAL_1.getAddress())
                .outputs("uint256"));
        Assert.assertEquals("100", results.get(0).getValue().toString());
    }

    @Test
    public void testApproveWithdraw() throws NoSuchMethodException, InterruptedException, ExecutionException, InstantiationException, ConvertException, IllegalAccessException, InvocationTargetException, IOException {
        //approvals: msg.sender approves accounts[1] of 100 & withdraws 20 once.
        DeployedContract deployed = blockchain.deploy(CREDENTIAL_0, contracts.get("DOS"));
        TestUtils.mint(blockchain, deployed, CREDENTIAL_0.getAddress(), CREDENTIAL_1.getAddress(), CREDENTIAL_2.getAddress(), 10000, 0, 0);

        List<Type> results = blockchain.callConstant(deployed, new FunctionBuilder("balanceOf")
                .addInput("address", CREDENTIAL_0.getAddress())
                .outputs("uint256"));
        Assert.assertEquals("10000", results.get(0).getValue().toString());

        List<Event> events = blockchain.call(CREDENTIAL_0, deployed,
                new FunctionBuilder("approve")
                        .addInput("address", CREDENTIAL_1.getAddress())
                        .addInput("uint256", new BigInteger("100"))
                        .outputs("bool"));
        results = blockchain.callConstant(deployed, new FunctionBuilder("balanceOf")
                .addInput("address", CREDENTIAL_2.getAddress())
                .outputs("uint256"));
        Assert.assertEquals("0", results.get(0).getValue().toString());

        results = blockchain.callConstant(deployed, new FunctionBuilder("allowance")
                .addInput("address", CREDENTIAL_0.getAddress())
                .addInput("address", CREDENTIAL_1.getAddress())
                .outputs("uint256"));
        Assert.assertEquals("100", results.get(0).getValue().toString());

        //        results = blockchain.callConstant(deployed, new FunctionBuilder("allowance")
        events = blockchain.call(CREDENTIAL_1, deployed,
                new FunctionBuilder("transferFrom")
                        .addInput("address", CREDENTIAL_0.getAddress())
                        .addInput("address", CREDENTIAL_2.getAddress())
                        .addInput("uint256", new BigInteger("20"))
                        .outputs("bool"));

        results = blockchain.callConstant(deployed, new FunctionBuilder("allowance")
                .addInput("address", CREDENTIAL_0.getAddress())
                .addInput("address", CREDENTIAL_1.getAddress())
                .outputs("uint256"));
        Assert.assertEquals("80", results.get(0).getValue().toString());

        results = blockchain.callConstant(deployed, new FunctionBuilder("balanceOf")
                .addInput("address", CREDENTIAL_0.getAddress())
                .outputs("uint256"));
        Assert.assertEquals("9980", results.get(0).getValue().toString());

    }

    @Test
    public void testApproveWithdrawTwice() throws NoSuchMethodException, InterruptedException, ExecutionException, InstantiationException, ConvertException, IllegalAccessException, InvocationTargetException, IOException {
        //approvals: msg.sender approves accounts[1] of 100 & withdraws 20 twice.
        DeployedContract deployed = blockchain.deploy(CREDENTIAL_0, contracts.get("DOS"));
        TestUtils.mint(blockchain, deployed, CREDENTIAL_0.getAddress(), CREDENTIAL_1.getAddress(), CREDENTIAL_2.getAddress(), 10000, 0, 0);

        List<Event> events = blockchain.call(CREDENTIAL_0, deployed,
                new FunctionBuilder("approve")
                        .addInput("address", CREDENTIAL_1.getAddress())
                        .addInput("uint256", new BigInteger("100"))
                        .outputs("bool"));

        events = blockchain.call(CREDENTIAL_1, deployed,
                new FunctionBuilder("transferFrom")
                        .addInput("address", CREDENTIAL_0.getAddress())
                        .addInput("address", CREDENTIAL_2.getAddress())
                        .addInput("uint256", new BigInteger("20"))
                        .outputs("bool"));

        List<Type> results = blockchain.callConstant(deployed, new FunctionBuilder("allowance")
                .addInput("address", CREDENTIAL_0.getAddress())
                .addInput("address", CREDENTIAL_1.getAddress())
                .outputs("uint256"));
        Assert.assertEquals("80", results.get(0).getValue().toString());

        // FIRST tx done.
        // onto next.
        events = blockchain.call(CREDENTIAL_1, deployed,
                new FunctionBuilder("transferFrom")
                        .addInput("address", CREDENTIAL_0.getAddress())
                        .addInput("address", CREDENTIAL_2.getAddress())
                        .addInput("uint256", new BigInteger("20"))
                        .outputs("bool"));

        results = blockchain.callConstant(deployed, new FunctionBuilder("allowance")
                .addInput("address", CREDENTIAL_0.getAddress())
                .addInput("address", CREDENTIAL_1.getAddress())
                .outputs("uint256"));
        Assert.assertEquals("60", results.get(0).getValue().toString());

        results = blockchain.callConstant(deployed, new FunctionBuilder("balanceOf")
                .addInput("address", CREDENTIAL_0.getAddress())
                .outputs("uint256"));
        Assert.assertEquals("9960", results.get(0).getValue().toString());

    }

    @Test
    public void testApproveWithdrawFailTwice() throws NoSuchMethodException, InterruptedException, ExecutionException, InstantiationException, ConvertException, IllegalAccessException, InvocationTargetException, IOException {
        //approvals: msg.sender approves accounts[1] of 100 & withdraws 50 & 60 (2nd tx should fail)
        DeployedContract deployed = blockchain.deploy(CREDENTIAL_0, contracts.get("DOS"));
        TestUtils.mint(blockchain, deployed, CREDENTIAL_0.getAddress(), CREDENTIAL_1.getAddress(), CREDENTIAL_2.getAddress(), 10000, 0, 0);

        List<Event> events = blockchain.call(CREDENTIAL_0, deployed,
                new FunctionBuilder("approve")
                        .addInput("address", CREDENTIAL_1.getAddress())
                        .addInput("uint256", new BigInteger("100"))
                        .outputs("bool"));

        events = blockchain.call(CREDENTIAL_1, deployed,
                new FunctionBuilder("transferFrom")
                        .addInput("address", CREDENTIAL_0.getAddress())
                        .addInput("address", CREDENTIAL_2.getAddress())
                        .addInput("uint256", new BigInteger("50"))
                        .outputs("bool"));

        List<Type> results = blockchain.callConstant(deployed, new FunctionBuilder("allowance")
                .addInput("address", CREDENTIAL_0.getAddress())
                .addInput("address", CREDENTIAL_1.getAddress())
                .outputs("uint256"));
        Assert.assertEquals("50", results.get(0).getValue().toString());

        results = blockchain.callConstant(deployed, new FunctionBuilder("balanceOf")
                .addInput("address", CREDENTIAL_0.getAddress())
                .outputs("uint256"));
        Assert.assertEquals("9950", results.get(0).getValue().toString());

        events = blockchain.call(CREDENTIAL_1, deployed,
                new FunctionBuilder("transferFrom")
                        .addInput("address", CREDENTIAL_0.getAddress())
                        .addInput("address", CREDENTIAL_2.getAddress())
                        .addInput("uint256", new BigInteger("60"))
                        .outputs("bool"));

        Assert.assertNull(events);
    }

    @Test
    public void testApproveWithdrawNoAllowance() throws NoSuchMethodException, InterruptedException, ExecutionException, InstantiationException, ConvertException, IllegalAccessException, InvocationTargetException, IOException {
        //approvals: attempt withdrawal from account with no allowance (should fail)
        DeployedContract deployed = blockchain.deploy(CREDENTIAL_0, contracts.get("DOS"));
        TestUtils.mint(blockchain, deployed, CREDENTIAL_0.getAddress(), CREDENTIAL_1.getAddress(), CREDENTIAL_2.getAddress(), 10000, 0, 0);

        List<Event> events = blockchain.call(CREDENTIAL_1, deployed,
                new FunctionBuilder("transferFrom")
                        .addInput("address", CREDENTIAL_0.getAddress())
                        .addInput("address", CREDENTIAL_2.getAddress())
                        .addInput("uint256", new BigInteger("60"))
                        .outputs("bool"));

        Assert.assertNull(events);
    }

    @Test
    public void testApproveWithdrawTransfer() throws NoSuchMethodException, InterruptedException, ExecutionException, InstantiationException, ConvertException, IllegalAccessException, InvocationTargetException, IOException {
        //approvals: allow accounts[1] 100 to withdraw from accounts[0]. Withdraw 60 and then approve 0 & attempt transfer.
        DeployedContract deployed = blockchain.deploy(CREDENTIAL_0, contracts.get("DOS"));
        TestUtils.mint(blockchain, deployed, CREDENTIAL_0.getAddress(), CREDENTIAL_1.getAddress(), CREDENTIAL_2.getAddress(), 10000, 0, 0);

        List<Event> events = blockchain.call(CREDENTIAL_0, deployed,
                new FunctionBuilder("approve")
                        .addInput("address", CREDENTIAL_1.getAddress())
                        .addInput("uint256", new BigInteger("100"))
                        .outputs("bool"));

        events = blockchain.call(CREDENTIAL_1, deployed,
                new FunctionBuilder("transferFrom")
                        .addInput("address", CREDENTIAL_0.getAddress())
                        .addInput("address", CREDENTIAL_2.getAddress())
                        .addInput("uint256", new BigInteger("60"))
                        .outputs("bool"));

        events = blockchain.call(CREDENTIAL_0, deployed,
                new FunctionBuilder("approve")
                        .addInput("address", CREDENTIAL_1.getAddress())
                        .addInput("uint256", new BigInteger("0"))
                        .outputs("bool"));

        events = blockchain.call(CREDENTIAL_1, deployed,
                new FunctionBuilder("transferFrom")
                        .addInput("address", CREDENTIAL_0.getAddress())
                        .addInput("address", CREDENTIAL_2.getAddress())
                        .addInput("uint256", new BigInteger("10"))
                        .outputs("bool"));

        Assert.assertNull(events);
    }

    @Test
    public void testApproveMax() throws NoSuchMethodException, InterruptedException, ExecutionException, InstantiationException, ConvertException, IllegalAccessException, InvocationTargetException, IOException {
        //approvals: approve max (2^256 - 1)
        DeployedContract deployed = blockchain.deploy(CREDENTIAL_0, contracts.get("DOS"));
        TestUtils.mint(blockchain, deployed, CREDENTIAL_0.getAddress(), CREDENTIAL_1.getAddress(), CREDENTIAL_2.getAddress(), 10000, 0, 0);


        List<Event> events = blockchain.call(CREDENTIAL_0, deployed,
                new FunctionBuilder("approve")
                        .addInput("address", CREDENTIAL_1.getAddress())
                        .addInput("uint256", new BigInteger("115792089237316195423570985008687907853269984665640564039457584007913129639935"))
                        .outputs("bool"));

        events = blockchain.call(CREDENTIAL_1, deployed,
                new FunctionBuilder("transferFrom")
                        .addInput("address", CREDENTIAL_0.getAddress())
                        .addInput("address", CREDENTIAL_2.getAddress())
                        .addInput("uint256", new BigInteger("60"))
                        .outputs("bool"));

        List<Type> results = blockchain.callConstant(deployed, new FunctionBuilder("allowance")
                .addInput("address", CREDENTIAL_0.getAddress())
                .addInput("address", CREDENTIAL_1.getAddress())
                .outputs("uint256"));
        Assert.assertEquals("115792089237316195423570985008687907853269984665640564039457584007913129639875", results.get(0).getValue().toString());
    }

    @Test
    public void testApproveFailZero() throws NoSuchMethodException, InterruptedException, ExecutionException, InstantiationException, ConvertException, IllegalAccessException, InvocationTargetException, IOException {
        //approve: should fail when trying to transfer to 0x0
        DeployedContract deployed = blockchain.deploy(CREDENTIAL_0, contracts.get("DOS"));
        TestUtils.mint(blockchain, deployed, CREDENTIAL_0.getAddress(), CREDENTIAL_1.getAddress(), CREDENTIAL_2.getAddress(), 10000, 0, 0);

        List<Event> events = blockchain.call(CREDENTIAL_0, deployed,
                new FunctionBuilder("approve")
                        .addInput("address", "0x0")
                        .addInput("uint256", new BigInteger("115792089237316195423570985008687907853269984665640564039457584007913129639935"))
                        .outputs("bool"));

        Assert.assertNull(events);
    }

    //********************* ERC20 Events

    @Test
    public void testEventMinting() throws NoSuchMethodException, InstantiationException, IllegalAccessException, ConvertException, InvocationTargetException, InterruptedException, ExecutionException, IOException {
        //events: minting should fire Transfer event properly
        DeployedContract deployed = blockchain.deploy(CREDENTIAL_0, contracts.get("DOS"));

        List<String> addresses = new ArrayList<>(1);
        List<BigInteger> values = new ArrayList<>(1);

        addresses.add(CREDENTIAL_5.getAddress());
        values.add(BigInteger.valueOf(2222));

        List<Event> events = blockchain.call(deployed,
                new FunctionBuilder("mint").addInput("address[]", addresses)
                        .addInput("uint256[]", values));

        Assert.assertEquals(1, events.size());
        Assert.assertEquals("Transfer", events.get(0).name());
        Assert.assertEquals("0x0000000000000000000000000000000000000000", events.get(0).values().get(0).getValue().toString());
        Assert.assertEquals(CREDENTIAL_5.getAddress(), events.get(0).values().get(1).getValue().toString());
        Assert.assertEquals("2222", events.get(0).values().get(2).getValue().toString());
    }

    @Test
    public void testEventTransfer() throws NoSuchMethodException, InterruptedException, ExecutionException, InstantiationException, ConvertException, IllegalAccessException, InvocationTargetException, IOException {
        //events: should fire Transfer event properly
        DeployedContract deployed = blockchain.deploy(CREDENTIAL_0, contracts.get("DOS"));
        TestUtils.mint(blockchain, deployed, CREDENTIAL_0.getAddress(), CREDENTIAL_1.getAddress(), CREDENTIAL_2.getAddress(), 10000, 0, 0);

        List<Event> events = blockchain.call(deployed,
                new FunctionBuilder("transfer")
                        .addInput("address", CREDENTIAL_1.getAddress())
                        .addInput("uint256", new BigInteger("10000"))
                        .outputs("bool"));

        Assert.assertEquals(1, events.size());
        Assert.assertEquals("Transfer", events.get(0).name());
        Assert.assertEquals(CREDENTIAL_0.getAddress(), events.get(0).values().get(0).getValue().toString());
        Assert.assertEquals(CREDENTIAL_1.getAddress(), events.get(0).values().get(1).getValue().toString());
        Assert.assertEquals("10000", events.get(0).values().get(2).getValue().toString());
    }

    @Test
    public void testEventTransferZero() throws NoSuchMethodException, InterruptedException, ExecutionException, InstantiationException, ConvertException, IllegalAccessException, InvocationTargetException, IOException {
        //events: should generate an event on zero-transfers
        DeployedContract deployed = blockchain.deploy(CREDENTIAL_0, contracts.get("DOS"));
        TestUtils.mint(blockchain, deployed, CREDENTIAL_0.getAddress(), CREDENTIAL_1.getAddress(), CREDENTIAL_2.getAddress(), 10000, 0, 0);

        List<Event> events = blockchain.call(deployed,
                new FunctionBuilder("transfer")
                        .addInput("address", CREDENTIAL_1.getAddress())
                        .addInput("uint256", new BigInteger("0"))
                        .outputs("bool"));

        Assert.assertEquals(1, events.size());
        Assert.assertEquals("Transfer", events.get(0).name());
        Assert.assertEquals(CREDENTIAL_0.getAddress(), events.get(0).values().get(0).getValue().toString());
        Assert.assertEquals(CREDENTIAL_1.getAddress(), events.get(0).values().get(1).getValue().toString());
        Assert.assertEquals("0", events.get(0).values().get(2).getValue().toString());
    }

    @Test
    public void testEventMintAllFail() throws NoSuchMethodException, InstantiationException, IllegalAccessException, ConvertException, InvocationTargetException, InterruptedException, ExecutionException, IOException {
        //events: should fail on minting max tokens
        DeployedContract deployed = blockchain.deploy(CREDENTIAL_0, contracts.get("DOS"));

        List<String> addresses = new ArrayList<>(1);
        List<BigInteger> values = new ArrayList<>(1);

        addresses.add(CREDENTIAL_5.getAddress());
        values.add(new BigInteger("900000000000000000000000001"));

        List<Event> events = blockchain.call(deployed,
                new FunctionBuilder("mint").addInput("address[]", addresses)
                        .addInput("uint256[]", values));

        Assert.assertNull(events);
    }

    @Test
    public void testEventMintAll() throws NoSuchMethodException, InstantiationException, IllegalAccessException, ConvertException, InvocationTargetException, InterruptedException, ExecutionException, IOException {
        //events: should not fail on minting max tokens
        DeployedContract deployed = blockchain.deploy(CREDENTIAL_0, contracts.get("DOS"));

        List<String> addresses = new ArrayList<>(1);
        List<BigInteger> values = new ArrayList<>(1);

        addresses.add(CREDENTIAL_2.getAddress());
        values.add(new BigInteger("900000000000000000000000000"));

        List<Event> events = blockchain.call(deployed,
                new FunctionBuilder("mint").addInput("address[]", addresses)
                        .addInput("uint256[]", values));

        Assert.assertEquals(1, events.size());
        Assert.assertEquals("Transfer", events.get(0).name());
        Assert.assertEquals("0x0000000000000000000000000000000000000000", events.get(0).values().get(0).getValue().toString());
        Assert.assertEquals(CREDENTIAL_2.getAddress(), events.get(0).values().get(1).getValue().toString());
        Assert.assertEquals("900000000000000000000000000", events.get(0).values().get(2).getValue().toString());
    }

    @Test
    public void testEventApprove() throws NoSuchMethodException, InterruptedException, ExecutionException, InstantiationException, ConvertException, IllegalAccessException, InvocationTargetException, IOException {
        //events: should fire Approval event properly
        DeployedContract deployed = blockchain.deploy(CREDENTIAL_0, contracts.get("DOS"));
        TestUtils.mint(blockchain, deployed, CREDENTIAL_0.getAddress(), CREDENTIAL_1.getAddress(), CREDENTIAL_2.getAddress(), 10000, 0, 0);


        List<Event> events = blockchain.call(CREDENTIAL_0, deployed,
                new FunctionBuilder("approve")
                        .addInput("address", CREDENTIAL_1.getAddress())
                        .addInput("uint256", new BigInteger("115792089237316195423570985008687907853269984665640564039457584007913129639935"))
                        .outputs("bool"));

        Assert.assertEquals(1, events.size());
        Assert.assertEquals("Approval", events.get(0).name());
        Assert.assertEquals(CREDENTIAL_0.getAddress(), events.get(0).values().get(0).getValue().toString());
        Assert.assertEquals(CREDENTIAL_1.getAddress(), events.get(0).values().get(1).getValue().toString());
        Assert.assertEquals("115792089237316195423570985008687907853269984665640564039457584007913129639935", events.get(0).values().get(2).getValue().toString());
    }

    @Test
    public void testEventTransferFrom() throws NoSuchMethodException, InterruptedException, ExecutionException, InstantiationException, ConvertException, IllegalAccessException, InvocationTargetException, IOException {
        //events: should fire transferFrom event properly
        DeployedContract deployed = blockchain.deploy(CREDENTIAL_0, contracts.get("DOS"));
        TestUtils.mint(blockchain, deployed, CREDENTIAL_0.getAddress(), CREDENTIAL_1.getAddress(), CREDENTIAL_2.getAddress(), 10000, 0, 0);

        List<Event> events = blockchain.call(CREDENTIAL_0, deployed,
                new FunctionBuilder("approve")
                        .addInput("address", CREDENTIAL_1.getAddress())
                        .addInput("uint256", new BigInteger("115792089237316195423570985008687907853269984665640564039457584007913129639935"))
                        .outputs("bool"));

        events = blockchain.call(CREDENTIAL_1, deployed,
                new FunctionBuilder("transferFrom")
                        .addInput("address", CREDENTIAL_0.getAddress())
                        .addInput("address", CREDENTIAL_2.getAddress())
                        .addInput("uint256", new BigInteger("60"))
                        .outputs("bool"));

        Assert.assertEquals(2, events.size());
        Assert.assertEquals("Transfer", events.get(0).name());
        Assert.assertEquals(CREDENTIAL_0.getAddress(), events.get(0).values().get(0).getValue().toString());
        Assert.assertEquals(CREDENTIAL_2.getAddress(), events.get(0).values().get(1).getValue().toString());
        Assert.assertEquals("60", events.get(0).values().get(2).getValue().toString());

        Assert.assertEquals("Approval", events.get(1).name());
        Assert.assertEquals(CREDENTIAL_0.getAddress(), events.get(1).values().get(0).getValue().toString());
        Assert.assertEquals(CREDENTIAL_1.getAddress(), events.get(1).values().get(1).getValue().toString());
        Assert.assertEquals(new BigInteger("115792089237316195423570985008687907853269984665640564039457584007913129639935").subtract(BigInteger.valueOf(60)).toString(), events.get(1).values().get(2).getValue().toString());
    }

    //********************* ERC20 Lockups

    @Test
    public void testLockup() throws NoSuchMethodException, InstantiationException, IllegalAccessException, ConvertException, InvocationTargetException, InterruptedException, ExecutionException, IOException {
        //should not be able to withdraw funds before lockup is over with transfer

        DeployedContract deployed = blockchain.deploy(CREDENTIAL_0, contracts.get("DOS"));

        List<String> addresses = new ArrayList<>(1);
        List<BigInteger> values = new ArrayList<>(1);
        List<BigInteger> timeouts = new ArrayList<>(1);

        addresses.add(CREDENTIAL_5.getAddress());
        values.add(BigInteger.valueOf(2222));
        timeouts.add(BigInteger.valueOf(2));

        List<Event> events = blockchain.call(deployed,
                new FunctionBuilder("mint").addInput("address[]", addresses)
                        .addInput("uint256[]", values));

        events = blockchain.call(deployed,
                new FunctionBuilder("lockTokens").addInput("address[]", addresses)
                        .addInput("uint256[]", timeouts));

        Assert.assertEquals(1, events.size());
        Assert.assertEquals("TokensLocked", events.get(0).name());
        Assert.assertEquals(CREDENTIAL_5.getAddress(), events.get(0).values().get(0).getValue().toString());
        Assert.assertEquals(""+((60*60*24*30*6*2) + 1548979200), events.get(0).values().get(1).getValue().toString());

        events = blockchain.call(deployed,
                new FunctionBuilder("setAdmin").addInput("address", TestBlockchain.CREDENTIAL_1.getAddress())
                        .addInput("address", TestBlockchain.CREDENTIAL_2.getAddress()));

        events = blockchain.call(deployed,
                new FunctionBuilder("finishMinting"));
        Assert.assertEquals(0, events.size());

        events = blockchain.call(CREDENTIAL_5, deployed,
                new FunctionBuilder("transfer")
                        .addInput("address", CREDENTIAL_1.getAddress())
                        .addInput("uint256", new BigInteger("2222"))
                        .outputs("bool"));

        Assert.assertNull(events);

    }

    @Test
    public void testWithdrawTransferFrom() throws InterruptedException, ExecutionException, IOException, NoSuchMethodException, InstantiationException, IllegalAccessException, ConvertException, InvocationTargetException {
        //should not be able to withdraw funds before lockup is over with transferFrom

        DeployedContract deployed = blockchain.deploy(CREDENTIAL_0, contracts.get("DOS"));

        List<String> addresses = new ArrayList<>(1);
        List<BigInteger> values = new ArrayList<>(1);

        addresses.add(CREDENTIAL_5.getAddress());
        values.add(BigInteger.valueOf(2222));

        List<Event> events = blockchain.call(deployed,
                new FunctionBuilder("mint").addInput("address[]", addresses)
                        .addInput("uint256[]", values));

        events = blockchain.call(deployed,
                new FunctionBuilder("lockTokens").addInput("address[]", addresses)
                        .addInput("uint256[]", values));

        events = blockchain.call(deployed,
                new FunctionBuilder("setAdmin").addInput("address", TestBlockchain.CREDENTIAL_1.getAddress())
                        .addInput("address", TestBlockchain.CREDENTIAL_2.getAddress()));

        events = blockchain.call(deployed,
                new FunctionBuilder("finishMinting"));
        Assert.assertEquals(0, events.size());

        events = blockchain.call(CREDENTIAL_5, deployed,
                new FunctionBuilder("approve")
                        .addInput("address", CREDENTIAL_1.getAddress())
                        .addInput("uint256", new BigInteger("50"))
                        .outputs("bool"));

        events = blockchain.call(CREDENTIAL_1, deployed,
                new FunctionBuilder("transferFrom")
                        .addInput("address", CREDENTIAL_5.getAddress())
                        .addInput("address", CREDENTIAL_2.getAddress())
                        .addInput("uint256", new BigInteger("50"))
                        .outputs("bool"));

        Assert.assertNull(events);
    }

    @Test
    public void testWithdraw() throws InterruptedException, ExecutionException, IOException, NoSuchMethodException, InstantiationException, IllegalAccessException, ConvertException, InvocationTargetException {
        //should be able to withdraw funds after lockup expires

        DeployedContract deployed = blockchain.deploy(CREDENTIAL_0, contracts.get("DOS"));

        List<String> addresses = new ArrayList<>(1);
        List<BigInteger> values = new ArrayList<>(1);
        List<BigInteger> timeouts = new ArrayList<>(1);

        addresses.add(CREDENTIAL_5.getAddress());
        values.add(BigInteger.valueOf(2222));
        timeouts.add(BigInteger.valueOf(1));

        List<Event> events = blockchain.call(deployed,
                new FunctionBuilder("mint").addInput("address[]", addresses)
                        .addInput("uint256[]", values));

        events = blockchain.call(deployed,
                new FunctionBuilder("lockTokens").addInput("address[]", addresses)
                        .addInput("uint256[]", timeouts));

        events = blockchain.call(deployed,
                new FunctionBuilder("setAdmin").addInput("address", TestBlockchain.CREDENTIAL_1.getAddress())
                        .addInput("address", TestBlockchain.CREDENTIAL_2.getAddress()));

        events = blockchain.call(deployed,
                new FunctionBuilder("finishMinting"));
        Assert.assertEquals(0, events.size());

        blockchain.setTime((60 * 60 * 24 * 30 * 6) + (1548979200 - 27));

        events = blockchain.call(CREDENTIAL_5, deployed,
                new FunctionBuilder("transfer")
                        .addInput("address", CREDENTIAL_1.getAddress())
                        .addInput("uint256", new BigInteger("2222"))
                        .outputs("bool"));

        Assert.assertNull(events);

        blockchain.setTime((60 * 60 * 24 * 30 * 6) + (1548979200 - 26)); //26 is 2x13, the time increment
        events = blockchain.call(CREDENTIAL_5, deployed,
                new FunctionBuilder("transfer")
                        .addInput("address", CREDENTIAL_1.getAddress())
                        .addInput("uint256", new BigInteger("2222"))
                        .outputs("bool"));

        Assert.assertEquals(1, events.size());
    }

    @Test
    public void testTransferOwnership() throws InterruptedException, ExecutionException, IOException, NoSuchMethodException, InstantiationException, IllegalAccessException, ConvertException, InvocationTargetException {
        //should be able to withdraw funds after lockup expires

        DeployedContract deployed = blockchain.deploy(CREDENTIAL_0, contracts.get("DOS"));
        TestUtils.mint(blockchain, deployed, CREDENTIAL_0.getAddress(), CREDENTIAL_1.getAddress(), CREDENTIAL_2.getAddress(), 10000, 0, 0);

        List<Event> events = blockchain.call(CREDENTIAL_5, deployed, "transferOwnership", CREDENTIAL_0.getAddress());
        Assert.assertEquals(null, events);

        events = blockchain.call(deployed, "transferOwnership", CREDENTIAL_1.getAddress());
        Assert.assertEquals(null, events); //cannot be admin1

        events = blockchain.call(deployed, "transferOwnership", CREDENTIAL_3.getAddress());
        Assert.assertEquals(0, events.size());

        events = blockchain.call(CREDENTIAL_3, deployed, "claimOwnership");
        Assert.assertEquals(0, events.size());

        events = blockchain.call(CREDENTIAL_3,deployed, "transferOwnership", CREDENTIAL_4.getAddress());
        Assert.assertEquals(0, events.size());
    }
}
