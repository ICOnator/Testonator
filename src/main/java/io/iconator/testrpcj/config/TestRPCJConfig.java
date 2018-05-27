package io.iconator.testrpcj.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.googlecode.jsonrpc4j.JsonRpcServer;
import io.iconator.testrpcj.RPCServlet;
import io.iconator.testrpcj.TestBlockchain;
import io.iconator.testrpcj.jsonrpc.EthJsonRpcImpl;
import io.iconator.testrpcj.jsonrpc.JsonRpc;
import org.ethereum.util.blockchain.EtherUtil;
import org.ethereum.util.blockchain.StandaloneBlockchain;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

@Configuration
@Import(TestRPCJConfigHolder.class)
public class TestRPCJConfig {

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertyPlaceholderConfigurer() {

        return new PropertySourcesPlaceholderConfigurer();
    }

    @Bean
    public StandaloneBlockchain createStandaloneBlockchain() {
        StandaloneBlockchain standaloneBlockchain = new StandaloneBlockchain()
                .withAccountBalance(TestBlockchain.ACCOUNT_0.getAddress(), EtherUtil.convert(10, EtherUtil.Unit.ETHER))
                .withAccountBalance(TestBlockchain.ACCOUNT_1.getAddress(), EtherUtil.convert(10, EtherUtil.Unit.ETHER))
                .withAccountBalance(TestBlockchain.ACCOUNT_2.getAddress(), EtherUtil.convert(10, EtherUtil.Unit.ETHER))
                .withAccountBalance(TestBlockchain.ACCOUNT_3.getAddress(), EtherUtil.convert(10, EtherUtil.Unit.ETHER))
                .withAccountBalance(TestBlockchain.ACCOUNT_4.getAddress(), EtherUtil.convert(10, EtherUtil.Unit.ETHER))
                .withAccountBalance(TestBlockchain.ACCOUNT_5.getAddress(), EtherUtil.convert(10, EtherUtil.Unit.ETHER))
                .withAccountBalance(TestBlockchain.ACCOUNT_6.getAddress(), EtherUtil.convert(10, EtherUtil.Unit.ETHER))
                .withAccountBalance(TestBlockchain.ACCOUNT_7.getAddress(), EtherUtil.convert(10, EtherUtil.Unit.ETHER))
                .withAccountBalance(TestBlockchain.ACCOUNT_8.getAddress(), EtherUtil.convert(10, EtherUtil.Unit.ETHER))
                .withAccountBalance(TestBlockchain.ACCOUNT_9.getAddress(), EtherUtil.convert(10, EtherUtil.Unit.ETHER))
                .withAutoblock(true);  //after each transaction, a new block will be created
        standaloneBlockchain.createBlock();
        return standaloneBlockchain;
    }

    @Bean
    public EthJsonRpcImpl ethJsonRpcImpl(StandaloneBlockchain standaloneBlockchain) {
        return new EthJsonRpcImpl(standaloneBlockchain);
    }

    @Bean
    public ServletRegistrationBean servletRegistrationBean(EthJsonRpcImpl ethJsonRpcImpl) {
        JsonRpcServer server = new JsonRpcServer(new ObjectMapper(), ethJsonRpcImpl, JsonRpc.class);
        RPCServlet rpcServlet = new RPCServlet(server);
        return new ServletRegistrationBean(rpcServlet, "/rpc/*");
    }

    @Bean
    public ServletWebServerFactory servletWebServerFactory(TestRPCJConfigHolder testRPCJConfigHolder) {
        return new TomcatServletWebServerFactory(testRPCJConfigHolder.getPort());
    }

}
