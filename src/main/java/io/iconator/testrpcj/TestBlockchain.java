package io.iconator.testrpcj;

import com.googlecode.jsonrpc4j.spring.JsonServiceExporter;
import io.iconator.testrpcj.jsonrpc.EthJsonRpcImpl;
import io.iconator.testrpcj.jsonrpc.JsonRpc;
import org.ethereum.config.SystemProperties;
import org.ethereum.crypto.ECKey;
import org.ethereum.solidity.compiler.SolidityCompiler;
import org.ethereum.util.blockchain.EtherUtil;
import org.ethereum.util.blockchain.StandaloneBlockchain;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import static org.springframework.boot.SpringApplication.run;

//testrpc for Java applications - to test:
//curl --silent -X POST -H "Content-Type: application/json" --data '{"jsonrpc":"2.0","method":"eth_getBlockByNumber","params":["latest", true],"id":1}' http://localhost:8081/rpc
@SpringBootApplication
public class TestBlockchain {

    static { System.setProperty("spring.config.name", "testrpcj.application"); }

    @Autowired
    private StandaloneBlockchain standaloneBlockchain;

    //public and private keys
    public final static ECKey ACCOUNT_0 = ECKey.fromPrivate(Hex.decode("1b865950b17a065c79b11ecb39650c377b4963d6387b2fb97d71744b89a7295e"));
    public final static ECKey ACCOUNT_1 = ECKey.fromPrivate(Hex.decode("c77ee832f3e5d7624ce9dab0eeb2958ad550e534952b79bb705e63b3989d4d1d"));
    public final static ECKey ACCOUNT_2 = ECKey.fromPrivate(Hex.decode("ba7ffe9dee14b3626211b2d056eacc30e7a634f7e11eeb4dde6ee6d50d0c81ab"));
    public final static ECKey ACCOUNT_3 = ECKey.fromPrivate(Hex.decode("64b1a16bb773bc2a6665967923cfd68f369e34f66ecd19c302995f8635598b1c"));
    public final static ECKey ACCOUNT_4 = ECKey.fromPrivate(Hex.decode("399c34e860be1f2740297fcadd3546fdd4f5ba4c06d13882da1e48527df3acca"));
    public final static ECKey ACCOUNT_5 = ECKey.fromPrivate(Hex.decode("a2a3abebd9160a2b2940970d848161008e3ea528aeaa927fb8b8370d3675f5f5"));
    public final static ECKey ACCOUNT_6 = ECKey.fromPrivate(Hex.decode("e728d9667a27b7f6164309fc3809c00fd8d782d9343c0b73ea1f5a150ec3d05b"));
    public final static ECKey ACCOUNT_7 = ECKey.fromPrivate(Hex.decode("d58fd771caefbdcca0c23fbc440fd03dacdee29cc4668cc9fc5acf29b4219f41"));
    public final static ECKey ACCOUNT_8 = ECKey.fromPrivate(Hex.decode("649f638d220fd6319ca4af8f5e0e261d15a66172830077126fef21fdbdd95410"));
    public final static ECKey ACCOUNT_9 = ECKey.fromPrivate(Hex.decode("ea8f71fc4690e0733f3478c3d8e53790988b9e51deabd10185364bc59c58fdba"));

    public static void main(String[] args) {
        new TestBlockchain().start(args);
    }

    public void start(String[] args) {
        try {
            run(TestBlockchain.class, args);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public void start() {
        start(new String[]{});
    }

    public static SolidityCompiler compiler() {
        return new SolidityCompiler(SystemProperties.getDefault());
    }

    @Bean
    public StandaloneBlockchain createStandaloneBlockchain() {
        return new StandaloneBlockchain()
                .withAccountBalance(ACCOUNT_0.getAddress(), EtherUtil.convert(10, EtherUtil.Unit.ETHER))
                .withAccountBalance(ACCOUNT_1.getAddress(), EtherUtil.convert(10, EtherUtil.Unit.ETHER))
                .withAccountBalance(ACCOUNT_2.getAddress(), EtherUtil.convert(10, EtherUtil.Unit.ETHER))
                .withAccountBalance(ACCOUNT_3.getAddress(), EtherUtil.convert(10, EtherUtil.Unit.ETHER))
                .withAccountBalance(ACCOUNT_4.getAddress(), EtherUtil.convert(10, EtherUtil.Unit.ETHER))
                .withAccountBalance(ACCOUNT_5.getAddress(), EtherUtil.convert(10, EtherUtil.Unit.ETHER))
                .withAccountBalance(ACCOUNT_6.getAddress(), EtherUtil.convert(10, EtherUtil.Unit.ETHER))
                .withAccountBalance(ACCOUNT_7.getAddress(), EtherUtil.convert(10, EtherUtil.Unit.ETHER))
                .withAccountBalance(ACCOUNT_8.getAddress(), EtherUtil.convert(10, EtherUtil.Unit.ETHER))
                .withAccountBalance(ACCOUNT_9.getAddress(), EtherUtil.convert(10, EtherUtil.Unit.ETHER))
                .withAutoblock(true);  //after each transaction, a new block will be created
    }

    @Bean(name = "/rpc")
    public JsonServiceExporter jsonServiceExporter() {
        JsonServiceExporter exporter = new JsonServiceExporter();
        exporter.setService(new EthJsonRpcImpl(standaloneBlockchain));
        exporter.setServiceInterface(JsonRpc.class);
        return exporter;
    }
}
