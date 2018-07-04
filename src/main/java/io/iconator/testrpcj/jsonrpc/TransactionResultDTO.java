/*
 * Copyright 2015, 2016 Ether.Camp Inc. (US)
 * This file is part of Ethereum Harmony.
 *
 * Ethereum Harmony is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Ethereum Harmony is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Ethereum Harmony.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.iconator.testrpcj.jsonrpc;

import org.ethereum.core.Block;
import org.ethereum.core.Transaction;

/**
 * Created by Ruben on 8/1/2016.
 */
public class TransactionResultDTO {

    public String hash;
    public String nonce;
    public String blockHash;
    public String blockNumber;
    public String transactionIndex;

    public String from;
    public String to;
    public String gas;
    public String gasPrice;
    public String value;
    public String input;

    public TransactionResultDTO(Block b, int index, Transaction tx) {
        hash = TypeConverter.toJsonHex(tx.getHash());
        nonce = TypeConverter.toJsonHex(tx.getNonce());
        blockHash = TypeConverter.toJsonHex(b.getHash());
        blockNumber = TypeConverter.toJsonHex(b.getNumber());
        transactionIndex = TypeConverter.toJsonHex(index);
        from = TypeConverter.toJsonHexAddress(tx.getSender());
        to = TypeConverter.toJsonHexAddress(tx.getReceiveAddress());
        gas = TypeConverter.toJsonHex(tx.getGasLimit());
        gasPrice = TypeConverter.toJsonHex(tx.getGasPrice());
        value = TypeConverter.toJsonHexNumber(tx.getValue());
        input = tx.getData() != null ? TypeConverter.toJsonHex(tx.getData()) : null;
    }

    public String getHash() {
        return this.hash;
    }

    public String getNonce() {
        return this.nonce;
    }

    public String getBlockHash() {
        return this.blockHash;
    }

    public String getBlockNumber() {
        return this.blockNumber;
    }

    public String getTransactionIndex() {
        return this.transactionIndex;
    }

    public String getFrom() {
        return this.from;
    }

    public String getTo() {
        return this.to;
    }

    public String getGas() {
        return this.gas;
    }

    public String getGasPrice() {
        return this.gasPrice;
    }

    public String getValue() {
        return this.value;
    }

    public String getInput() {
        return this.input;
    }

}