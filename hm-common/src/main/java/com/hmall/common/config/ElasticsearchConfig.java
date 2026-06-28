package com.hmall.common.config;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration // 标记为配置类，Spring会扫描并加载
@ConditionalOnClass(RestHighLevelClient.class)
public class ElasticsearchConfig {
    @Bean // 将方法返回的对象注册为Spring Bean，默认单例
    public RestHighLevelClient client() {
        return new RestHighLevelClient(RestClient.builder(
                HttpHost.create("http://192.168.11.135:9200")
        ));
    }
}
