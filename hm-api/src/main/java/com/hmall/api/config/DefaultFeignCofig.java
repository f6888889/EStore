package com.hmall.api.config;

import com.hmall.api.fallback.ItemClientFallback;
import com.hmall.common.utils.UserContext;
import feign.Logger;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Bean;



public class DefaultFeignCofig {
    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.BASIC;
    }
    @Bean
    public RequestInterceptor userInfoRequestInterceptor() {
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate requestTemplate) {
                Long user = UserContext.getUser();
                if(user != null){
                    requestTemplate.header("user", user.toString());
                }

            }
        };
    }
    @Bean
    public ItemClientFallback itemClientFallback() {
        return new ItemClientFallback();
    }
}
