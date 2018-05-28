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
        hash =  TypeConverter.toJsonHex(tx.getHash());
        nonce = TypeConverter.toJsonHex(tx.getNonce());
        blockHash = TypeConverter.toJsonHex(b.getHash());
        blockNumber = TypeConverter.toJsonHex(b.getNumber());
        transactionIndex = TypeConverter.toJsonHex(index);
        from= TypeConverter.toJsonHex(tx.getSender());
        to = tx.getReceiveAddress() == null ? null : TypeConverter.toJsonHex(tx.getReceiveAddress());
        gas = TypeConverter.toJsonHex(tx.getGasLimit());
        gasPrice = TypeConverter.toJsonHex(tx.getGasPrice());
        value = TypeConverter.toJsonHex(tx.getValue());
        input  = tx.getData() != null ? TypeConverter.toJsonHex(tx.getData()) : null;
    }

    @Override
    public String toString() {
        return "TransactionResultDTO{" +
                "hash='" + hash + '\'' +
                ", nonce='" + nonce + '\'' +
                ", blockHash='" + blockHash + '\'' +
                ", blockNumber='" + blockNumber + '\'' +
                ", transactionIndex='" + transactionIndex + '\'' +
                ", from='" + from + '\'' +
                ", to='" + to + '\'' +
                ", gas='" + gas + '\'' +
                ", gasPrice='" + gasPrice + '\'' +
                ", value='" + value + '\'' +
                ", input='" + input + '\'' +
                '}';
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

    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof TransactionResultDTO)) return false;
        final TransactionResultDTO other = (TransactionResultDTO) o;
        final Object this$hash = this.getHash();
        final Object other$hash = other.getHash();
        if (this$hash == null ? other$hash != null : !this$hash.equals(other$hash)) return false;
        final Object this$nonce = this.getNonce();
        final Object other$nonce = other.getNonce();
        if (this$nonce == null ? other$nonce != null : !this$nonce.equals(other$nonce)) return false;
        final Object this$blockHash = this.getBlockHash();
        final Object other$blockHash = other.getBlockHash();
        if (this$blockHash == null ? other$blockHash != null : !this$blockHash.equals(other$blockHash)) return false;
        final Object this$blockNumber = this.getBlockNumber();
        final Object other$blockNumber = other.getBlockNumber();
        if (this$blockNumber == null ? other$blockNumber != null : !this$blockNumber.equals(other$blockNumber))
            return false;
        final Object this$transactionIndex = this.getTransactionIndex();
        final Object other$transactionIndex = other.getTransactionIndex();
        if (this$transactionIndex == null ? other$transactionIndex != null : !this$transactionIndex.equals(other$transactionIndex))
            return false;
        final Object this$from = this.getFrom();
        final Object other$from = other.getFrom();
        if (this$from == null ? other$from != null : !this$from.equals(other$from)) return false;
        final Object this$to = this.getTo();
        final Object other$to = other.getTo();
        if (this$to == null ? other$to != null : !this$to.equals(other$to)) return false;
        final Object this$gas = this.getGas();
        final Object other$gas = other.getGas();
        if (this$gas == null ? other$gas != null : !this$gas.equals(other$gas)) return false;
        final Object this$gasPrice = this.getGasPrice();
        final Object other$gasPrice = other.getGasPrice();
        if (this$gasPrice == null ? other$gasPrice != null : !this$gasPrice.equals(other$gasPrice)) return false;
        final Object this$value = this.getValue();
        final Object other$value = other.getValue();
        if (this$value == null ? other$value != null : !this$value.equals(other$value)) return false;
        final Object this$input = this.getInput();
        final Object other$input = other.getInput();
        if (this$input == null ? other$input != null : !this$input.equals(other$input)) return false;
        return true;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $hash = this.getHash();
        result = result * PRIME + ($hash == null ? 43 : $hash.hashCode());
        final Object $nonce = this.getNonce();
        result = result * PRIME + ($nonce == null ? 43 : $nonce.hashCode());
        final Object $blockHash = this.getBlockHash();
        result = result * PRIME + ($blockHash == null ? 43 : $blockHash.hashCode());
        final Object $blockNumber = this.getBlockNumber();
        result = result * PRIME + ($blockNumber == null ? 43 : $blockNumber.hashCode());
        final Object $transactionIndex = this.getTransactionIndex();
        result = result * PRIME + ($transactionIndex == null ? 43 : $transactionIndex.hashCode());
        final Object $from = this.getFrom();
        result = result * PRIME + ($from == null ? 43 : $from.hashCode());
        final Object $to = this.getTo();
        result = result * PRIME + ($to == null ? 43 : $to.hashCode());
        final Object $gas = this.getGas();
        result = result * PRIME + ($gas == null ? 43 : $gas.hashCode());
        final Object $gasPrice = this.getGasPrice();
        result = result * PRIME + ($gasPrice == null ? 43 : $gasPrice.hashCode());
        final Object $value = this.getValue();
        result = result * PRIME + ($value == null ? 43 : $value.hashCode());
        final Object $input = this.getInput();
        result = result * PRIME + ($input == null ? 43 : $input.hashCode());
        return result;
    }
}