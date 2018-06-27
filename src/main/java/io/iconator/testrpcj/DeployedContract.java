package io.iconator.testrpcj;

import org.ethereum.crypto.ECKey;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.EthSendTransaction;

public class DeployedContract {
    final private EthSendTransaction tx;
    final private String contractAddress;
    final private Credentials credential;
    final private EthGetTransactionReceipt receipt;
    final private Contract contract;
    public DeployedContract(
            EthSendTransaction tx,
            String contractAddress,
            Credentials credential,
            EthGetTransactionReceipt receipt,
            Contract contract) {
        this.tx = tx;
        this.contractAddress = contractAddress;
        this.credential = credential;
        this.receipt = receipt;
        this.contract = contract;
    }
    public EthSendTransaction tx() {
        return tx;
    }

    public String contractAddress() {
        return contractAddress;
    }

    public Credentials credential() {
        return credential;
    }

    public EthGetTransactionReceipt receipt() {
        return receipt;
    }

    public Contract contract() {
        return contract;
    }
}
