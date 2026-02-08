package com.side.shop.config;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TestConfig {

    @Bean
    public MockMvcBuilderCustomizer mockMvcBuilderCustomizer() {
        return builder -> builder.alwaysDo(print());
    }
}
