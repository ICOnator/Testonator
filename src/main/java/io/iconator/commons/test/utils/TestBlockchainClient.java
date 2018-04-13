package io.iconator.commons.test.utils;

import io.iconator.commons.test.jsonrpc.TypeConverter;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.exceptions.TransactionException;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.Transfer;
import org.web3j.utils.Convert;

import java.math.BigDecimal;
import java.math.BigInteger;

public class TestBlockchainClient {
    public static void main(String[] args) throws Exception {
        Web3j web3j = Web3j.build(new HttpService("http://localhost:8081/rpc"));

        Credentials credentials = Credentials.create(ECKeyPair.create(TestBlockchain.ACCOUNT_0.getPrivKeyBytes()));

        TransactionReceipt r = Transfer.sendFunds(
                web3j,
                credentials,
                TypeConverter.toJsonHex(TestBlockchain.ACCOUNT_1.getAddress()),
                BigDecimal.valueOf(1.0),
                Convert.Unit.ETHER).send();


        EthGetBalance ethGetBalance = web3j
                .ethGetBalance(TypeConverter.toJsonHex(TestBlockchain.ACCOUNT_1.getAddress()), DefaultBlockParameterName.LATEST)
                .send();

        BigInteger wei = ethGetBalance.getBalance();

        System.out.println(r.getBlockNumber()+" / "+ wei+ " / " + ethGetBalance.hasError());
    }
}
