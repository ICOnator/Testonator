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

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import org.apache.commons.collections4.map.LRUMap;
import org.ethereum.config.CommonConfig;
import org.ethereum.core.*;
import org.ethereum.crypto.ECKey;
import org.ethereum.crypto.HashUtil;
import org.ethereum.db.BlockStore;
import org.ethereum.db.ByteArrayWrapper;
import org.ethereum.listener.EthereumListenerAdapter;
import org.ethereum.listener.LogFilter;
import org.ethereum.mine.MinerIfc;
import org.ethereum.mine.MinerListener;
import org.ethereum.solidity.compiler.SolidityCompiler;
import org.ethereum.util.BuildInfo;
import org.ethereum.util.ByteUtil;
import org.ethereum.util.blockchain.StandaloneBlockchain;
import org.ethereum.vm.DataWord;
import org.ethereum.vm.LogInfo;
import org.ethereum.vm.program.invoke.ProgramInvokeFactory;
import org.ethereum.vm.program.invoke.ProgramInvokeFactoryImpl;
import org.spongycastle.util.encoders.Hex;
import org.springframework.stereotype.Service;

import java.lang.reflect.Modifier;
import java.math.BigInteger;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.ethereum.util.ByteUtil.*;

/**
 * @author Anton Nashatyrev
 */
@Service
// renamed to not conflict with class from core
// wait for core class to be removed
public class EthJsonRpcImpl implements JsonRpc {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(EthJsonRpcImpl.class);

    private static final String BLOCK_LATEST = "latest";

    private volatile String hashrate;

    public class BinaryCallArguments {
        public long nonce;
        public long gasPrice;
        public long gasLimit;
        public String toAddress;
        public String fromAddress;
        public long value;
        public byte[] data;
        public void setArguments(CallArguments args) throws Exception {
            nonce = 0;
            if (args.nonce != null && args.nonce.length() != 0)
                nonce = JSonHexToLong(args.nonce);

            gasPrice = 0;
            if (args.gasPrice != null && args.gasPrice.length()!=0)
                gasPrice = JSonHexToLong(args.gasPrice);

            gasLimit = 4_000_000;
            if (args.gas != null && args.gas.length()!=0)
                gasLimit = JSonHexToLong(args.gas);

            toAddress = null;
            if (args.to != null && !args.to.isEmpty())
                toAddress = JSonHexToHex(args.to);

            fromAddress = null;
            if (args.from != null && !args.from.isEmpty())
                fromAddress = JSonHexToHex(args.from);

            value=0;
            if (args.value != null && args.value.length()!=0)
                value = JSonHexToLong(args.value);

            data = null;

            if (args.data != null && args.data.length()!=0)
                data = TypeConverter.StringHexToByteArray(args.data);
        }
    }

    final private StandaloneBlockchain standaloneBlockchain;
    final private BlockchainImpl blockchain;
    final private PendingState pendingState;
    final private BlockStore blockStore;
    final private ProgramInvokeFactory programInvokeFactory = new ProgramInvokeFactoryImpl();
    final private CommonConfig commonConfig = CommonConfig.getDefault();

    /**
     * State fields
     */
    protected volatile long initialBlockNumber;

    private final Map<String, ECKey> accounts = new ConcurrentHashMap<>();

    public void addAccount(String address, ECKey pair) {
        accounts.put(address, pair);
    }

    AtomicInteger filterCounter = new AtomicInteger(1);
    Map<Integer, Filter> installedFilters = new Hashtable<>();
    Map<ByteArrayWrapper, TransactionReceipt> pendingReceipts = Collections.synchronizedMap(new LRUMap<>(1024));

    Map<ByteArrayWrapper, Block> miningBlocks = new ConcurrentHashMap<>();

    volatile Block miningBlock;

    volatile SettableFuture<MinerIfc.MiningResult> miningTask;

    final MinerIfc externalMiner = new MinerIfc() {
        @Override
        public ListenableFuture<MiningResult> mine(Block block) {
            miningBlock = block;
            miningTask = SettableFuture.create();
            return miningTask;
        }

        @Override
        public void setListeners(Collection<MinerListener> var1) {

        }

        @Override
        public boolean validate(BlockHeader blockHeader) {
            return false;
        }
    };

    boolean minerInitialized = false;

    public EthJsonRpcImpl(StandaloneBlockchain standaloneBlockchain) {
        this.standaloneBlockchain = standaloneBlockchain;
        this.blockchain = standaloneBlockchain.getBlockchain();
        this.pendingState = standaloneBlockchain.getPendingState();
        this.blockStore = standaloneBlockchain.getBlockchain().getBlockStore();
        init();
    }



    private void init() {
        initialBlockNumber = blockchain.getBestBlock().getNumber();

        standaloneBlockchain.addEthereumListener(new EthereumListenerAdapter() {
            @Override
            public void onBlock(Block block, List<TransactionReceipt> receipts) {
                for (Filter filter : installedFilters.values()) {
                    filter.newBlockReceived(block);
                }
            }

            @Override
            public void onPendingTransactionsReceived(List<Transaction> transactions) {
                for (Filter filter : installedFilters.values()) {
                    for (Transaction tx : transactions) {
                        filter.newPendingTx(tx);
                    }
                }
            }

            @Override
            public void onPendingTransactionUpdate(TransactionReceipt txReceipt, PendingTransactionState state, Block block) {
                ByteArrayWrapper txHashW = new ByteArrayWrapper(txReceipt.getTransaction().getHash());
                if (state.isPending() || state == PendingTransactionState.DROPPED) {
                    pendingReceipts.put(txHashW, txReceipt);
                } else {
                    pendingReceipts.remove(txHashW);
                }
            }
        });

    }

    private long JSonHexToLong(String x) throws Exception {
        if (!x.startsWith("0x"))
            throw new Exception("Incorrect hex syntax");
        x = x.substring(2);
        return Long.parseLong(x, 16);
    }

    private int JSonHexToInt(String x) throws Exception {
        if (!x.startsWith("0x"))
            throw new Exception("Incorrect hex syntax");
        x = x.substring(2);
        return Integer.parseInt(x, 16);
    }

    private String JSonHexToHex(String x) {
        if (!x.startsWith("0x"))
            throw new RuntimeException("Incorrect hex syntax");
        x = x.substring(2);
        return x;
    }

    private Block getBlockByJSonHash(String blockHash) throws Exception {
        byte[] bhash = TypeConverter.StringHexToByteArray(blockHash);
        return blockchain.getBlockByHash(bhash);
    }

    private Block getByJsonBlockId(String id) {
        if ("earliest".equalsIgnoreCase(id)) {
            return blockchain.getBlockByNumber(0);
        } else if ("latest".equalsIgnoreCase(id)) {
            return blockchain.getBestBlock();
        } else if ("pending".equalsIgnoreCase(id)) {
            return null;
        } else {
            long blockNumber = TypeConverter.StringHexToBigInteger(id).longValue();
            return blockchain.getBlockByNumber(blockNumber);
        }
    }

    private Repository getRepoByJsonBlockId(String id) {
        if ("pending".equalsIgnoreCase(id)) {
            return pendingState.getRepository();
        } else {
            Block block = getByJsonBlockId(id);
            return blockchain.getRepository().getSnapshotTo(block.getStateRoot());
        }
    }

    private List<Transaction> getTransactionsByJsonBlockId(String id) {
        if ("pending".equalsIgnoreCase(id)) {
            return pendingState.getPendingTransactions();
        } else {
            Block block = getByJsonBlockId(id);
            return block != null ? block.getTransactionsList() : null;
        }
    }

    public String web3_clientVersion() {
        Pattern shortVersion = Pattern.compile("(\\d\\.\\d).*");
        Matcher matcher = shortVersion.matcher(System.getProperty("java.version"));
        matcher.matches();

        return Arrays.asList(
                "TestRPC-J/Harmony", "v1",
                System.getProperty("os.name"),
                "Java" + matcher.group(1),
                "-" + BuildInfo.buildHash).stream()
                .collect(Collectors.joining("/"));
    }

    public String web3_sha3(String data) throws Exception {
        byte[] result = HashUtil.sha3(data.getBytes());
        return TypeConverter.toJsonHex(result);
    }



    public String eth_protocolVersion(){
        return "not sure what to put here...";
    }

    public Object eth_syncing() {
        SyncingResult sr = new SyncingResult();
        sr.startingBlock = TypeConverter.toJsonHex(initialBlockNumber);
        sr.currentBlock = TypeConverter.toJsonHex(blockchain.getBestBlock().getNumber());
        sr.highestBlock = TypeConverter.toJsonHex(blockchain.getBestBlock().getNumber());
        return sr;
    }

    public String eth_coinbase() {
        return TypeConverter.toJsonHex(blockchain.getMinerCoinbase());
    }

    public boolean eth_mining() {
        return false;
    }

    public String eth_hashrate() {
        return hashrate;
    }

    public String eth_gasPrice(){
        return TypeConverter.toJsonHex(50_000_000_000L);
    }

    public String[] eth_accounts() {
        return accounts.keySet().toArray(new String[10]);
    }

    public String eth_blockNumber() {
        return TypeConverter.toJsonHex(blockchain.getBestBlock().getNumber());
    }

    public String eth_getBalance(String address, String blockId) throws Exception {
        Objects.requireNonNull(address, "address is required");
        blockId = blockId == null ? BLOCK_LATEST : blockId;

        byte[] addressAsByteArray = TypeConverter.StringHexToByteArray(address);
        BigInteger balance = getRepoByJsonBlockId(blockId).getBalance(addressAsByteArray);
        return TypeConverter.toJsonHex(balance);
    }

    public String eth_getLastBalance(String address) throws Exception {
        return eth_getBalance(address, BLOCK_LATEST);
    }

    @Override
    public String eth_getStorageAt(String address, String storageIdx, String blockId) throws Exception {
        byte[] addressAsByteArray = TypeConverter.StringHexToByteArray(address);
        DataWord storageValue = getRepoByJsonBlockId(blockId).
                getStorageValue(addressAsByteArray, new DataWord(TypeConverter.StringHexToByteArray(storageIdx)));
        return storageValue != null ? TypeConverter.toJsonHex(storageValue.getData()) : null;
    }

    @Override
    public String eth_getTransactionCount(String address, String blockId) throws Exception {
        byte[] addressAsByteArray = TypeConverter.StringHexToByteArray(address);
        BigInteger nonce = getRepoByJsonBlockId(blockId).getNonce(addressAsByteArray);
        return TypeConverter.toJsonHex(nonce);
    }

    public String eth_getBlockTransactionCountByHash(String blockHash) throws Exception {
        Block b = getBlockByJSonHash(blockHash);
        if (b == null) return null;
        long n = b.getTransactionsList().size();
        return TypeConverter.toJsonHex(n);
    }

    public String eth_getBlockTransactionCountByNumber(String bnOrId) throws Exception {
        List<Transaction> list = getTransactionsByJsonBlockId(bnOrId);
        if (list == null) return null;
        long n = list.size();
        return TypeConverter.toJsonHex(n);
    }

    public String eth_getUncleCountByBlockHash(String blockHash) throws Exception {
        Block b = getBlockByJSonHash(blockHash);
        if (b == null) return null;
        long n = b.getUncleList().size();
        return TypeConverter.toJsonHex(n);
    }

    public String eth_getUncleCountByBlockNumber(String bnOrId) throws Exception {
        Block b = getByJsonBlockId(bnOrId);
        if (b == null) return null;
        long n = b.getUncleList().size();
        return TypeConverter.toJsonHex(n);
    }

    public String eth_getCode(String address, String blockId) throws Exception {
        byte[] addressAsByteArray = TypeConverter.StringHexToByteArray(address);
        byte[] code = getRepoByJsonBlockId(blockId).getCode(addressAsByteArray);
        return TypeConverter.toJsonHex(code);
    }

    /**
     * Sign message hash with key to produce Elliptic Curve Digital Signature (ECDSA) signature.
     *
     * Note: implementation may be different to other Ethereum node implementations.
     *
     * @param address - address to sign. Account must be unlocked
     * @param messageHash - sha3 of message
     * @return ECDSA signature (in hex)
     * @throws Exception
     */
    public String eth_sign(String address, String messageHash) throws Exception {
        String ha = JSonHexToHex(address);
        ECKey key = accounts.get(ha);


        ECKey.ECDSASignature signature = key.sign(Hex.decode(JSonHexToHex(messageHash)));
        byte[] signatureBytes = toByteArray(signature);

        return TypeConverter.toJsonHex(signatureBytes);
    }

    private byte[] toByteArray(ECKey.ECDSASignature signature) {
        final byte fixedV = signature.v >= 27
                ? (byte) (signature.v - 27)
                :signature.v;

        return ByteUtil.merge(
                bigIntegerToBytes(signature.r),
                bigIntegerToBytes(signature.s),
                new byte[]{fixedV});
    }

    public String eth_sendTransaction(CallArguments args) throws Exception {
        String ha = JSonHexToHex(args.from);
        ECKey key = accounts.get(ha);

        return sendTransaction(args, key);
    }

    private String sendTransaction(CallArguments args, ECKey account) {
        if (args.data != null && args.data.startsWith("0x"))
            args.data = args.data.substring(2);

        // convert zero to empty byte array
        // TEMP, until decide for better behavior
        final BigInteger valueBigInt = args.value != null ? TypeConverter.StringHexToBigInteger(args.value) : BigInteger.ZERO;
        final byte[] value = !valueBigInt.equals(BigInteger.ZERO) ? bigIntegerToBytes(valueBigInt) : EMPTY_BYTE_ARRAY;

        final Transaction tx = new Transaction(
                args.nonce != null ? TypeConverter.StringHexToByteArray(args.nonce) : bigIntegerToBytes(pendingState.getRepository().getNonce(account.getAddress())),
                args.gasPrice != null ? TypeConverter.StringHexToByteArray(args.gasPrice) : ByteUtil.longToBytesNoLeadZeroes(50_000_000_000L),
                args.gas != null ? TypeConverter.StringHexToByteArray(args.gas) : longToBytes(90_000),
                args.to != null ? TypeConverter.StringHexToByteArray(args.to) : EMPTY_BYTE_ARRAY,
                value,
                args.data != null ? TypeConverter.StringHexToByteArray(args.data) : EMPTY_BYTE_ARRAY);

        tx.sign(account);

        validateAndSubmit(tx);

        return TypeConverter.toJsonHex(tx.getHash());
    }

    public String eth_sendTransactionArgs(String from, String to, String gas,
                                      String gasPrice, String value, String data, String nonce) throws Exception {

        String ha = JSonHexToHex(from);
        ECKey key = accounts.get(ha);

        CallArguments ca = new CallArguments();
        ca.from=from;
        ca.to=to;
        ca.gas=gas;
        ca.gasPrice=gasPrice;
        ca.value=value;
        ca.data=data;
        ca.nonce=nonce;

        return sendTransaction(ca, key);
    }

    public String eth_sendRawTransaction(String rawData) throws Exception {
        Transaction tx = new Transaction(TypeConverter.StringHexToByteArray(rawData));

        tx.rlpParse();
        validateAndSubmit(tx);

        return TypeConverter.toJsonHex(tx.getHash());
    }

    protected void validateAndSubmit(Transaction tx) {
        if (tx.getValue().length > 0 && tx.getValue()[0] == 0) {
            // zero value should be sent as empty byte array
            // otherwise tx will be accepted by core, but never included in block
//            throw new RuntimeException("Field 'value' should not have leading zero");
            byte[] txRaw = tx.getValue();
            log.warn("Transaction might use incorrect zero value: "+txRaw);
        }
        standaloneBlockchain.submitTransaction(tx);
    }

    protected TransactionReceipt createCallTxAndExecute(CallArguments args, Block block) throws Exception {
        Repository repository = blockchain.getRepository()
                .getSnapshotTo(block.getStateRoot())
                .startTracking();

        return createCallTxAndExecute(args, block, repository, blockchain.getBlockStore());
    }

    protected TransactionReceipt createCallTxAndExecute(CallArguments args, Block block, Repository repository, BlockStore blockStore) throws Exception {
        BinaryCallArguments bca = new BinaryCallArguments();
        bca.setArguments(args);
        Transaction rawTransaction = CallTransaction.createRawTransaction(0,
                bca.gasPrice,
                bca.gasLimit,
                bca.toAddress,
                bca.value,
                bca.data);
        LocalTransaction tx = new LocalTransaction(rawTransaction.getEncoded());

        // handle from address without signing
        if (args.from != null) {
            tx.setSender(hexStringToBytes(args.from));
        } else {
            // put mock signature if not present
            tx.sign(ECKey.fromPrivate(new byte[32]));
        }

        try {
            TransactionExecutor executor = new TransactionExecutor(
                    tx, block.getCoinbase(), repository, blockStore,
                    programInvokeFactory, block, new EthereumListenerAdapter(), 0)
                    .withCommonConfig(commonConfig)
                    .setLocalCall(true);

            executor.init();
            executor.execute();
            executor.go();
            executor.finalization();

            return executor.getReceipt();
        } finally {
            repository.rollback();
        }
    }

    public String eth_call(CallArguments args, String bnOrId) throws Exception {
        TransactionReceipt res;
        if ("pending".equals(bnOrId)) {
            Block pendingBlock = blockchain.createNewBlock(blockchain.getBestBlock(), pendingState.getPendingTransactions(), Collections.<BlockHeader>emptyList());
            res = createCallTxAndExecute(args, pendingBlock, pendingState.getRepository(), blockchain.getBlockStore());
        } else {
            res = createCallTxAndExecute(args, getByJsonBlockId(bnOrId));
        }
        return TypeConverter.toJsonHex(res.getExecutionResult());
    }

    public String eth_estimateGas(CallArguments args) throws Exception {
        TransactionReceipt res = createCallTxAndExecute(args, blockchain.getBestBlock());
        return TypeConverter.toJsonHex(res.getGasUsed());
    }


    protected BlockResult getBlockResult(Block block, boolean fullTx) {
        if (block==null)
            return null;
        boolean isPending = ByteUtil.byteArrayToLong(block.getNonce()) == 0;
        BlockResult br = new BlockResult();
        br.number = isPending ? null : TypeConverter.toJsonHex(block.getNumber());
        br.hash = isPending ? null : TypeConverter.toJsonHex(block.getHash());
        br.parentHash = TypeConverter.toJsonHex(block.getParentHash());
        br.nonce = isPending ? null : TypeConverter.toJsonHex(block.getNonce());
        br.sha3Uncles= TypeConverter.toJsonHex(block.getUnclesHash());
        br.logsBloom = isPending ? null : TypeConverter.toJsonHex(block.getLogBloom());
        br.transactionsRoot = TypeConverter.toJsonHex(block.getTxTrieRoot());
        br.stateRoot = TypeConverter.toJsonHex(block.getStateRoot());
        br.receiptRoot = TypeConverter.toJsonHex(block.getReceiptsRoot());
        br.miner = isPending ? null : TypeConverter.toJsonHex(block.getCoinbase());
        br.difficulty = TypeConverter.toJsonHex(block.getDifficultyBI());
        br.totalDifficulty = TypeConverter.toJsonHex(blockStore.getTotalDifficultyForHash(block.getHash()));
        if (block.getExtraData() != null)
            br.extraData = TypeConverter.toJsonHex(block.getExtraData());
        br.size = TypeConverter.toJsonHex(block.getEncoded().length);
        br.gasLimit = TypeConverter.toJsonHex(block.getGasLimit());
        br.gasUsed = TypeConverter.toJsonHex(block.getGasUsed());
        br.timestamp = TypeConverter.toJsonHex(block.getTimestamp());

        List<Object> txes = new ArrayList<>();
        if (fullTx) {
            for (int i = 0; i < block.getTransactionsList().size(); i++) {
                txes.add(new TransactionResultDTO(block, i, block.getTransactionsList().get(i)));
            }
        } else {
            for (Transaction tx : block.getTransactionsList()) {
                txes.add(TypeConverter.toJsonHex(tx.getHash()));
            }
        }
        br.transactions = txes.toArray();

        List<String> ul = new ArrayList<>();
        for (BlockHeader header : block.getUncleList()) {
            ul.add(TypeConverter.toJsonHex(header.getHash()));
        }
        br.uncles = ul.toArray(new String[ul.size()]);

        return br;
    }

    public BlockResult eth_getBlockByHash(String blockHash,Boolean fullTransactionObjects) throws Exception {
        final Block b = getBlockByJSonHash(blockHash);
        return getBlockResult(b, fullTransactionObjects);
    }

    public BlockResult eth_getBlockByNumber(String bnOrId, Boolean fullTransactionObjects) throws Exception {
        final Block b;
        if ("pending".equalsIgnoreCase(bnOrId)) {
            b = blockchain.createNewBlock(blockchain.getBestBlock(), pendingState.getPendingTransactions(), Collections.<BlockHeader>emptyList());
        } else {
            b = getByJsonBlockId(bnOrId);
        }
        return (b == null ? null : getBlockResult(b, fullTransactionObjects));
    }

    public TransactionResultDTO eth_getTransactionByHash(String transactionHash) throws Exception {
        final byte[] txHash = TypeConverter.StringHexToByteArray(transactionHash);

        final TransactionInfo txInfo = blockchain.getTransactionInfo(txHash);
        if (txInfo == null) {
            return null;
        }

        final Block block = blockchain.getBlockByHash(txInfo.getBlockHash());
        // need to return txes only from main chain
        final Block mainBlock = blockchain.getBlockByNumber(block.getNumber());
        if (!Arrays.equals(block.getHash(), mainBlock.getHash())) {
            return null;
        }
        txInfo.setTransaction(block.getTransactionsList().get(txInfo.getIndex()));

        return new TransactionResultDTO(block, txInfo.getIndex(), txInfo.getReceipt().getTransaction());
    }

    public TransactionResultDTO eth_getTransactionByBlockHashAndIndex(String blockHash,String index) throws Exception {
        Block b = getBlockByJSonHash(blockHash);
        if (b == null) return null;
        int idx = JSonHexToInt(index);
        if (idx >= b.getTransactionsList().size()) return null;
        Transaction tx = b.getTransactionsList().get(idx);
        return new TransactionResultDTO(b, idx, tx);
    }

    public TransactionResultDTO eth_getTransactionByBlockNumberAndIndex(String bnOrId, String index) throws Exception {
        Block b = getByJsonBlockId(bnOrId);
        List<Transaction> txs = getTransactionsByJsonBlockId(bnOrId);
        if (txs == null) return null;
        int idx = JSonHexToInt(index);
        if (idx >= txs.size()) return null;
        Transaction tx = txs.get(idx);
        return new TransactionResultDTO(b, idx, tx);
    }

    public TransactionReceiptDTO eth_getTransactionReceipt(String transactionHash) throws Exception {
        final byte[] hash = TypeConverter.StringHexToByteArray(transactionHash);

        final TransactionInfo txInfo = blockchain.getTransactionInfo(hash);

        if (txInfo == null)
            return null;

        final Block block = blockchain.getBlockByHash(txInfo.getBlockHash());
        final Block mainBlock = blockchain.getBlockByNumber(block.getNumber());

        // need to return txes only from main chain
        if (!Arrays.equals(block.getHash(), mainBlock.getHash())) {
            return null;
        }

        return new TransactionReceiptDTO(block, txInfo);
    }

    @Override
    public TransactionReceiptDTOExt ethj_getTransactionReceipt(String transactionHash) throws Exception {
        byte[] hash = TypeConverter.StringHexToByteArray(transactionHash);

        TransactionReceipt pendingReceipt = pendingReceipts.get(new ByteArrayWrapper(hash));

        TransactionInfo txInfo;
        Block block;

        if (pendingReceipt != null) {
            txInfo = new TransactionInfo(pendingReceipt);
            block = null;
        } else {
            txInfo = blockchain.getTransactionInfo(hash);

            if (txInfo == null)
                return null;

            block = blockchain.getBlockByHash(txInfo.getBlockHash());

            // need to return txes only from main chain
            Block mainBlock = blockchain.getBlockByNumber(block.getNumber());
            if (!Arrays.equals(block.getHash(), mainBlock.getHash())) {
                return null;
            }
        }

        return new TransactionReceiptDTOExt(block, txInfo);
    }

    @Override
    public BlockResult eth_getUncleByBlockHashAndIndex(String blockHash, String uncleIdx) throws Exception {
        Block block = blockchain.getBlockByHash(TypeConverter.StringHexToByteArray(blockHash));
        if (block == null) return null;
        int idx = JSonHexToInt(uncleIdx);
        if (idx >= block.getUncleList().size()) return null;
        BlockHeader uncleHeader = block.getUncleList().get(idx);
        Block uncle = blockchain.getBlockByHash(uncleHeader.getHash());
        if (uncle == null) {
            uncle = new Block(uncleHeader, Collections.<Transaction>emptyList(), Collections.<BlockHeader>emptyList());
        }
        return getBlockResult(uncle, false);
    }

    @Override
    public BlockResult eth_getUncleByBlockNumberAndIndex(String blockId, String uncleIdx) throws Exception {
        Block block = getByJsonBlockId(blockId);
        return block == null ? null :
                eth_getUncleByBlockHashAndIndex(TypeConverter.toJsonHex(block.getHash()), uncleIdx);
    }

    @Override
    public String[] eth_getCompilers() {
        return new String[] {"solidity"};
    }

//    @Override
//    public CompilationResult eth_compileLLL(String contract) {
//        throw new UnsupportedOperationException("LLL compiler not supported");
//    }

    @Override
    public CompilationResult eth_compileSolidity(String contract) throws Exception {
        SolidityCompiler.Result res = SolidityCompiler.compile(
                contract.getBytes(), true, SolidityCompiler.Options.ABI, SolidityCompiler.Options.BIN, SolidityCompiler.Options.INTERFACE);
        if (res.isFailed()) {
            throw new RuntimeException("Compilation error: " + res.errors);
        }
        org.ethereum.solidity.compiler.CompilationResult result = org.ethereum.solidity.compiler.CompilationResult.parse(res.output);
        CompilationResult ret = new CompilationResult();
        org.ethereum.solidity.compiler.CompilationResult.ContractMetadata contractMetadata = result.getContracts().iterator().next();
        ret.code = TypeConverter.toJsonHex(contractMetadata.bin);
        ret.info = new CompilationInfo();
        ret.info.source = contract;
        ret.info.language = "Solidity";
        ret.info.languageVersion = "0";
        ret.info.compilerVersion = result.version;
        ret.info.abiDefinition = new CallTransaction.Contract(contractMetadata.abi).functions;
        return ret;
    }

//    @Override
//    public CompilationResult eth_compileSerpent(String contract){
//        throw new UnsupportedOperationException("Serpent compiler not supported");
//    }
//
//    @Override
//    public String eth_resend() {
//        throw new UnsupportedOperationException("JSON RPC method eth_resend not implemented yet");
//    }
//
//    @Override
//    public String eth_pendingTransactions() {
//        throw new UnsupportedOperationException("JSON RPC method eth_pendingTransactions not implemented yet");
//    }

    static class Filter {
        static final int MAX_EVENT_COUNT = 1024; // prevent OOM when Filers are forgotten
        private int pollStart = 0;
        static abstract class FilterEvent {
            public abstract Object getJsonEventObject();
        }
        List<FilterEvent> events = new LinkedList<>();

        public synchronized boolean hasNew() { return !events.isEmpty();}

        public synchronized Object[] poll() {
            Object[] ret = new Object[events.size() - pollStart];
            for (int i = 0; i < ret.length; i++) {
                ret[i] = events.get(i + pollStart).getJsonEventObject();
            }
            pollStart += ret.length;
            return ret;
        }

        public synchronized Object[] getAll() {
            Object[] ret = new Object[events.size()];
            for (int i = 0; i < ret.length; i++) {
                ret[i] = events.get(i).getJsonEventObject();
            }
            return ret;
        }

        protected synchronized void add(FilterEvent evt) {
            events.add(evt);
            if (events.size() > MAX_EVENT_COUNT) {
                events.remove(0);
                if (pollStart > 0) {
                    --pollStart;
                }
            }
        }

        public void newBlockReceived(Block b) {}
        public void newPendingTx(Transaction tx) {}
    }

    static class NewBlockFilter extends Filter {
        class NewBlockFilterEvent extends FilterEvent {
            public final Block b;
            NewBlockFilterEvent(Block b) {this.b = b;}

            @Override
            public String getJsonEventObject() {
                return TypeConverter.toJsonHex(b.getHash());
            }
        }

        public void newBlockReceived(Block b) {
            add(new NewBlockFilterEvent(b));
        }
    }

    static class PendingTransactionFilter extends Filter {
        class PendingTransactionFilterEvent extends FilterEvent {
            private final Transaction tx;

            PendingTransactionFilterEvent(Transaction tx) {this.tx = tx;}

            @Override
            public String getJsonEventObject() {
                return TypeConverter.toJsonHex(tx.getHash());
            }
        }

        public void newPendingTx(Transaction tx) {
            add(new PendingTransactionFilterEvent(tx));
        }
    }

    class JsonLogFilter extends Filter {
        class LogFilterEvent extends FilterEvent {
            private final LogFilterElement el;

            LogFilterEvent(LogFilterElement el) {
                this.el = el;
            }

            @Override
            public LogFilterElement getJsonEventObject() {
                return el;
            }
        }

        LogFilter logFilter;
        boolean onNewBlock;
        boolean onPendingTx;

        public JsonLogFilter(LogFilter logFilter) {
            this.logFilter = logFilter;
        }

        void onLogMatch(LogInfo logInfo, Block b, int txIndex, Transaction tx, int logIdx) {
            add(new LogFilterEvent(new LogFilterElement(logInfo, b, txIndex, tx, logIdx)));
        }

        void onTransactionReceipt(TransactionReceipt receipt, Block b, int txIndex) {
            if (logFilter.matchBloom(receipt.getBloomFilter())) {
                int logIdx = 0;
                for (LogInfo logInfo : receipt.getLogInfoList()) {
                    if (logFilter.matchBloom(logInfo.getBloom()) && logFilter.matchesExactly(logInfo)) {
                        onLogMatch(logInfo, b, txIndex, receipt.getTransaction(), logIdx);
                    }
                    logIdx++;
                }
            }
        }

        void onTransaction(Transaction tx, Block b, int txIndex) {
            if (logFilter.matchesContractAddress(tx.getReceiveAddress())) {
                TransactionInfo txInfo = blockchain.getTransactionInfo(tx.getHash());
                onTransactionReceipt(txInfo.getReceipt(), b, txIndex);
            }
        }

        void onBlock(Block b) {
            if (logFilter.matchBloom(new Bloom(b.getLogBloom()))) {
                int txIdx = 0;
                for (Transaction tx : b.getTransactionsList()) {
                    onTransaction(tx, b, txIdx);
                    txIdx++;
                }
            }
        }

        @Override
        public void newBlockReceived(Block b) {
            if (onNewBlock) onBlock(b);
        }

        @Override
        public void newPendingTx(Transaction tx) {
            // TODO add TransactionReceipt for PendingTx
//            if (onPendingTx)
        }
    }

    @Override
    public String eth_newFilter(FilterRequest fr) throws Exception {
        LogFilter logFilter = new LogFilter();

        if (fr.address instanceof String) {
            logFilter.withContractAddress(TypeConverter.StringHexToByteArray((String) fr.address));
        } else if (fr.address instanceof String[]) {
            List<byte[]> addr = new ArrayList<>();
            for (String s : ((String[]) fr.address)) {
                addr.add(TypeConverter.StringHexToByteArray(s));
            }
            logFilter.withContractAddress(addr.toArray(new byte[0][]));
        }

        if (fr.topics != null) {
            for (Object topic : fr.topics) {
                if (topic == null) {
                    logFilter.withTopic((byte[][]) null);
                } else if (topic instanceof String) {
                    logFilter.withTopic(new DataWord(TypeConverter.StringHexToByteArray((String) topic)).getData());
                } else if (topic instanceof String[]) {
                    List<byte[]> t = new ArrayList<>();
                    for (String s : ((String[]) topic)) {
                        t.add(new DataWord(TypeConverter.StringHexToByteArray(s)).getData());
                    }
                    logFilter.withTopic(t.toArray(new byte[0][]));
                }
            }
        }

        JsonLogFilter filter = new JsonLogFilter(logFilter);
        int id = filterCounter.getAndIncrement();
        installedFilters.put(id, filter);

        final Block blockFrom = fr.fromBlock == null ? null : getByJsonBlockId(fr.fromBlock);
        Block blockTo = fr.toBlock == null ? null : getByJsonBlockId(fr.toBlock);

        if (blockFrom != null) {
            // need to add historical data
            blockTo = blockTo == null ? blockchain.getBestBlock() : blockTo;
            for (long blockNum = blockFrom.getNumber(); blockNum <= blockTo.getNumber(); blockNum++) {
                filter.onBlock(blockchain.getBlockByNumber(blockNum));
            }
        }

        // the following is not precisely documented
        if ("pending".equalsIgnoreCase(fr.fromBlock) || "pending".equalsIgnoreCase(fr.toBlock)) {
            filter.onPendingTx = true;
        } else if (fr.toBlock == null || "latest".equalsIgnoreCase(fr.fromBlock) || "latest".equalsIgnoreCase(fr.toBlock)) {
            filter.onNewBlock = true;
        }

        return TypeConverter.toJsonHex(id);
    }

    @Override
    public String eth_newBlockFilter() {
        int id = filterCounter.getAndIncrement();
        installedFilters.put(id, new NewBlockFilter());
        return TypeConverter.toJsonHex(id);
    }

    @Override
    public String eth_newPendingTransactionFilter() {
        int id = filterCounter.getAndIncrement();
        installedFilters.put(id, new PendingTransactionFilter());
        return TypeConverter.toJsonHex(id);
    }

    @Override
    public boolean eth_uninstallFilter(String id) {
        if (id == null) return false;
        return installedFilters.remove(TypeConverter.StringHexToBigInteger(id).intValue()) != null;
    }

    @Override
    public Object[] eth_getFilterChanges(String id) {
        Filter filter = installedFilters.get(TypeConverter.StringHexToBigInteger(id).intValue());
        if (filter == null) return null;
        return filter.poll();
    }

    @Override
    public Object[] eth_getFilterLogs(String id) {
        Filter filter = installedFilters.get(TypeConverter.StringHexToBigInteger(id).intValue());
        if (filter == null) return null;
        return filter.getAll();
    }

    @Override
    public Object[] eth_getLogs(FilterRequest filterRequest) throws Exception {
        log.debug("eth_getLogs ...");
        String id = eth_newFilter(filterRequest);
        Object[] ret = eth_getFilterChanges(id);
        eth_uninstallFilter(id);
        return ret;
    }

    @Override
    public boolean eth_submitWork(String nonceHex, String headerHex, String digestHex) throws Exception {
        try {
            final long nonce = TypeConverter.HexToLong(nonceHex);
            final byte[] digest = TypeConverter.StringHexToByteArray(digestHex);
            final byte[] header = TypeConverter.StringHexToByteArray(headerHex);

            final Block block = miningBlocks.remove(new ByteArrayWrapper(header));

            if (block != null && miningTask != null) {
                block.setNonce(longToBytes(nonce));
                block.setMixHash(digest);

                miningTask.set(new MinerIfc.MiningResult(nonce, digest, block));
                miningTask = null;
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            log.error("eth_submitWork", e);
            return false;
        }
    }

    @Override
    public boolean eth_submitHashrate(String hashrate, String id) {
        this.hashrate = hashrate;
        return true;
    }


    /**
     * List method names for client side terminal competition.
     * @return array in format: `["methodName arg1 arg2", "methodName2"]`
     */
    @Override
    public String[] ethj_listAvailableMethods() {
        final Set<String> ignore = Arrays.asList(Object.class.getMethods()).stream()
                .map(method -> method.getName())
                .collect(Collectors.toSet());

        return Arrays.asList(EthJsonRpcImpl.class.getMethods()).stream()
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .filter(method -> !ignore.contains(method.getName()))
                .map(method -> {
                     List<String> params = Arrays.asList(method.getParameters())
                            .stream()
                            .map(parameter ->
                                    parameter.isNamePresent() ? parameter.getName() : parameter.getType().getSimpleName())
                            .collect(Collectors.toList());
                    params.add(0, method.getName());
                    return params.stream().collect(Collectors.joining(" "));
                })
                .sorted(String::compareTo)
                .toArray(size -> new String[size]);
    }
}