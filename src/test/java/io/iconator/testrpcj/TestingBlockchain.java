package io.iconator.testrpcj;

import org.junit.Test;

public class TestingBlockchain {

    @Test
    public void testReset() throws Exception {
        TestBlockchain blockchain = TestBlockchain.run();
        for(int i=0;i<100;i++) {
            blockchain.reset();
        }
    }
}
