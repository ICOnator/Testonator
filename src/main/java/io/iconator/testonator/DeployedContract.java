package io.iconator.testonator;

import org.web3j.crypto.Credentials;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.EthSendTransaction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DeployedContract {
    final private EthSendTransaction tx;
    final private String contractAddress;
    final private Credentials owner;
    final private EthGetTransactionReceipt receipt;
    final private Contract contract;
    final private List<Contract> referencedContracts = new ArrayList<>();

    private Credentials from;

    public DeployedContract(
            EthSendTransaction tx,
            String contractAddress,
            Credentials owner,
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

    public Credentials owner() {
        return owner;
    }

    public EthGetTransactionReceipt receipt() {
        return receipt;
    }

    public Contract contract() {
        return contract;
    }

    public Credentials from() {
        return from;
    }
    public DeployedContract from(Credentials from) {
        this.from = from; return this;
    }

    /**
     * Add a contract with emited events of interest. E.g., if contract A calls contract B, and contract B
     * emits an event, then contract a needs to add contract B as a referenced contract.
     *
     * @param referencedContract
     * @return
     */
    public DeployedContract addReferencedContract(Contract referencedContract) {
        referencedContracts.add(referencedContract);
        return this;
    }

    public DeployedContract addAllReferencedContract(Collection<Contract> contrats) {
        referencedContracts.addAll(contrats);
        return this;
    }

    public List<Contract> referencedContracts() {
        return referencedContracts;
    }
}
