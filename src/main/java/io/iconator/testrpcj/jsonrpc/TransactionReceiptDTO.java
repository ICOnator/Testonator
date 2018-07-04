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
import org.ethereum.core.TransactionInfo;
import org.ethereum.core.TransactionReceipt;
import org.ethereum.vm.LogInfo;

/**
 * Created by Ruben on 5/1/2016.
 */
public class TransactionReceiptDTO {

    public String transactionHash;          // hash of the transaction.
    public String transactionIndex;         // integer of the transactions index position in the block.
    public String blockHash;                // hash of the block where this transaction was in.
    public String blockNumber;              // block number where this transaction was in.
    public String cumulativeGasUsed;        // The total amount of gas used when this transaction was executed in the block.
    public String gasUsed;                  // The amount of gas used by this specific transaction alone.
    public String contractAddress;          // The contract address created, if the transaction was a contract creation, otherwise  null .
    public String status;
//    public String from;
//    public String to;
    public JsonRpc.LogFilterElement[] logs;         // Array of log objects, which this transaction generated.

    public TransactionReceiptDTO(Block block, TransactionInfo txInfo){
        TransactionReceipt receipt = txInfo.getReceipt();

        transactionHash = TypeConverter.toJsonHex(receipt.getTransaction().getHash());
        transactionIndex = TypeConverter.toJsonHex(new Integer(txInfo.getIndex()).longValue());
        cumulativeGasUsed = TypeConverter.toJsonHex(receipt.getCumulativeGas());
        gasUsed = TypeConverter.toJsonHex(receipt.getGasUsed());
        if (receipt.getTransaction().getContractAddress() != null) {
            contractAddress = TypeConverter.toJsonHex(receipt.getTransaction().getContractAddress());
        } else {
            contractAddress = null;
        }
        logs = new JsonRpc.LogFilterElement[receipt.getLogInfoList().size()];
        if (block != null) {
            blockNumber = TypeConverter.toJsonHex(block.getNumber());
            blockHash = TypeConverter.toJsonHex(txInfo.getBlockHash());
        } else {
            blockNumber = null;
            blockHash = null;
        }
        for (int i = 0; i < logs.length; i++) {
            LogInfo logInfo = receipt.getLogInfoList().get(i);
            logs[i] = new JsonRpc.LogFilterElement(logInfo, block, txInfo.getIndex(),
                    txInfo.getReceipt().getTransaction(), i);
        }
        if(receipt.getError().isEmpty()) {
            status = "0x1";
        } else {
            status = "0x0";
        }
    }

    public String getTransactionHash() {
        return this.transactionHash;
    }

    public String getTransactionIndex() {
        return this.transactionIndex;
    }

    public String getBlockHash() {
        return this.blockHash;
    }

    public String getBlockNumber() {
        return this.blockNumber;
    }

    public String getCumulativeGasUsed() {
        return this.cumulativeGasUsed;
    }

    public String getGasUsed() {
        return this.gasUsed;
    }

    public String getContractAddress() {
        return this.contractAddress;
    }

    public String status() {
        return this.status;
    }

    public JsonRpc.LogFilterElement[] getLogs() {
        return this.logs;
    }

    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof TransactionReceiptDTO)) return false;
        final TransactionReceiptDTO other = (TransactionReceiptDTO) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$transactionHash = this.getTransactionHash();
        final Object other$transactionHash = other.getTransactionHash();
        if (this$transactionHash == null ? other$transactionHash != null : !this$transactionHash.equals(other$transactionHash))
            return false;
        final Object this$transactionIndex = this.getTransactionIndex();
        final Object other$transactionIndex = other.getTransactionIndex();
        if (this$transactionIndex == null ? other$transactionIndex != null : !this$transactionIndex.equals(other$transactionIndex))
            return false;
        final Object this$blockHash = this.getBlockHash();
        final Object other$blockHash = other.getBlockHash();
        if (this$blockHash == null ? other$blockHash != null : !this$blockHash.equals(other$blockHash)) return false;
        final Object this$blockNumber = this.getBlockNumber();
        final Object other$blockNumber = other.getBlockNumber();
        if (this$blockNumber == null ? other$blockNumber != null : !this$blockNumber.equals(other$blockNumber))
            return false;
        final Object this$cumulativeGasUsed = this.getCumulativeGasUsed();
        final Object other$cumulativeGasUsed = other.getCumulativeGasUsed();
        if (this$cumulativeGasUsed == null ? other$cumulativeGasUsed != null : !this$cumulativeGasUsed.equals(other$cumulativeGasUsed))
            return false;
        final Object this$gasUsed = this.getGasUsed();
        final Object other$gasUsed = other.getGasUsed();
        if (this$gasUsed == null ? other$gasUsed != null : !this$gasUsed.equals(other$gasUsed)) return false;
        final Object this$contractAddress = this.getContractAddress();
        final Object other$contractAddress = other.getContractAddress();
        if (this$contractAddress == null ? other$contractAddress != null : !this$contractAddress.equals(other$contractAddress))
            return false;
        if (!java.util.Arrays.deepEquals(this.getLogs(), other.getLogs())) return false;
        return true;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $transactionHash = this.getTransactionHash();
        result = result * PRIME + ($transactionHash == null ? 43 : $transactionHash.hashCode());
        final Object $transactionIndex = this.getTransactionIndex();
        result = result * PRIME + ($transactionIndex == null ? 43 : $transactionIndex.hashCode());
        final Object $blockHash = this.getBlockHash();
        result = result * PRIME + ($blockHash == null ? 43 : $blockHash.hashCode());
        final Object $blockNumber = this.getBlockNumber();
        result = result * PRIME + ($blockNumber == null ? 43 : $blockNumber.hashCode());
        final Object $cumulativeGasUsed = this.getCumulativeGasUsed();
        result = result * PRIME + ($cumulativeGasUsed == null ? 43 : $cumulativeGasUsed.hashCode());
        final Object $gasUsed = this.getGasUsed();
        result = result * PRIME + ($gasUsed == null ? 43 : $gasUsed.hashCode());
        final Object $contractAddress = this.getContractAddress();
        result = result * PRIME + ($contractAddress == null ? 43 : $contractAddress.hashCode());
        result = result * PRIME + java.util.Arrays.deepHashCode(this.getLogs());
        return result;
    }

    protected boolean canEqual(Object other) {
        return other instanceof TransactionReceiptDTO;
    }

    public String toString() {
        return "TransactionReceiptDTO(transactionHash=" + this.getTransactionHash() + ", transactionIndex=" + this.getTransactionIndex() + ", blockHash=" + this.getBlockHash() + ", blockNumber=" + this.getBlockNumber() + ", cumulativeGasUsed=" + this.getCumulativeGasUsed() + ", gasUsed=" + this.getGasUsed() + ", contractAddress=" + this.getContractAddress() + ", logs=" + java.util.Arrays.deepToString(this.getLogs()) + ")";
    }
}
