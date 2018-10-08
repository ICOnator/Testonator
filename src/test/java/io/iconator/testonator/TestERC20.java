package io.iconator.testonator;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Map;

public class TestERC20 {
    private static TestBlockchain blockchain;
    private static Map<String, Contract> contracts;

    @BeforeClass
    public static void setup() throws Exception {
        blockchain = TestBlockchain.run();
        contracts = TestUtils.setup();
    }

    @After
    public void afterTests() {
        blockchain.reset();
    }

    //********************* ERC20 Regular Transfers
    @Test
    public void testReverse() {
        //transfers: ether transfer should be reversed
    }

    @Test
    public void testTransfer() {
        //transfers: should transfer 10000 to accounts[1] with accounts[0] having 10000
    }

    @Test
    public void testTransferFail() {
        //transfers: should fail when trying to transfer 10001 to accounts[1] with accounts[0] having 10000
    }

    @Test
    public void testTransferFailContract() {
        //transfers: should fail when trying to transfer to contract address
    }

    @Test
    public void testTransferFailZero() {
        //transfers: should fail when trying to transfer to 0x0
    }

    //********************* ERC20 Approvals

    @Test
    public void testApprove() {
        //approvals: msg.sender should approve 100 to accounts[1]
    }

    @Test
    public void testApproveWithdraw() {
        //approvals: msg.sender approves accounts[1] of 100 & withdraws 20 once.
    }

    @Test
    public void testApproveWithdrawTwice() {
        //approvals: msg.sender approves accounts[1] of 100 & withdraws 20 twice.
    }

    @Test
    public void testApproveWithdrawFailTwice() {
        //approvals: msg.sender approves accounts[1] of 100 & withdraws 50 & 60 (2nd tx should fail)
    }

    @Test
    public void testApproveWithdrawNoAllowance() {
        //approvals: attempt withdrawal from acconut with no allowance (should fail)
    }

    @Test
    public void testApproveWithdrawTransfer() {
        //approvals: allow accounts[1] 100 to withdraw from accounts[0]. Withdraw 60 and then approve 0 & attempt transfer.
    }

    @Test
    public void testApproveMax() {
        //approvals: approve max (2^256 - 1)
    }

    @Test
    public void testApproveFailContractAddress() {
        //approve: should fail when trying to transfer to contract address
    }

    @Test
    public void testApproveFailZero() {
        //approve: should fail when trying to transfer to 0x0
    }

    //********************* ERC20 Events

    @Test
    public void testEventMinting() {
        //events: minting should fire Transfer event properly
    }

    @Test
    public void testEventTransfer() {
        //events: should fire Transfer event properly
    }

    @Test
    public void testEventTransferZero() {
        //events: should generate an event on zero-transfers
    }

    @Test
    public void testEventMintAllFail() {
        //events: should fail on minting max tokens
    }

    @Test
    public void testEventMintAll() {
        //events: should not fail on minting max tokens
    }

    @Test
    public void testEventApprove() {
        //events: should fire Approval event properly
    }

    @Test
    public void testEventTransferFrom() {
        //events: should fire transferFrom event properly
    }

    //********************* ERC20 Lockups

    @Test
    public void testLockup() {
        //should not be able to withdraw funds before lockup is over with transfer
    }

    @Test
    public void testWithdraw() {
        //should be able to withdraw funds after lockup expires
    }

    @Test
    public void testWithdrawTransferFrom() {
        //should not be able to withdraw funds before lockup is over with transferFrom
    }
}
