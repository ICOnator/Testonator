package io.iconator.testrpcj;

import org.ethereum.crypto.ECKey;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.EthSendTransaction;

public class DeployedContract {
    final private EthSendTransaction tx;
    final private String contractAddress;
    final private ECKey owner;
    final private EthGetTransactionReceipt receipt;
    final private Contract contract;
    public DeployedContract(
            EthSendTransaction tx,
            String contractAddress,
            ECKey owner,
            EthGetTransactionReceipt receipt,
            Contract contract) {
        this.tx = tx;
        this.contractAddress = contractAddress;
        this.owner = owner;
        this.receipt = receipt;
        this.contract = contract;
    }
    public EthSendTransaction tx() {
        return tx;
    }

    public String contractAddress() {
        return contractAddress;
    }

    public ECKey owner() {
        return owner;
    }

    public EthGetTransactionReceipt receipt() {
        return receipt;
    }

    public Contract contract() {
        return contract;
    }
}
