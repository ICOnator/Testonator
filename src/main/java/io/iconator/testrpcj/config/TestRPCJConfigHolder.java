package io.iconator.testrpcj.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TestRPCJConfigHolder {

    @Value("${io.iconator.testrpcj.server.port}")
    private Integer port;

    public Integer getPort() {
        return port;
    }
}
