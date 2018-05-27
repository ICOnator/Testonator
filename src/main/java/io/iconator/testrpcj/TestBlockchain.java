package io.iconator.testrpcj;

import org.ethereum.config.SystemProperties;
import org.ethereum.crypto.ECKey;
import org.ethereum.solidity.compiler.SolidityCompiler;
import org.spongycastle.util.encoders.Hex;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import static org.springframework.boot.SpringApplication.run;

@Configuration
@ComponentScan(basePackages = "io.iconator.testrpcj")
public class TestBlockchain {

    static {
        System.setProperty("spring.config.name", "testrpcj.application");
    }

    // public and private keys
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

    public TestBlockchain start() {
        start(new String[]{});
        return this;
    }

    public static SolidityCompiler compiler() {
        return new SolidityCompiler(SystemProperties.getDefault());
    }

}
