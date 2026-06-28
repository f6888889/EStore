package com.hmall.common.config;

import com.hmall.common.utils.UserContext;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
@ConditionalOnClass(RabbitTemplate.class)
public class MqConfig {

    @Bean
    public MessageConverter messageConverter() {
        // 1.定义消息转换器
        Jackson2JsonMessageConverter jackson2JsonMessageConverter = new Jackson2JsonMessageConverter();
        // 2.配置自动创建消息id，用于识别不同消息，也可以在业务中基于ID判断是否是重复消息
        jackson2JsonMessageConverter.setCreateMessageIds(true);//666
        return new AuthMessageConverter(jackson2JsonMessageConverter);
    }

    static class AuthMessageConverter implements MessageConverter {
        private final MessageConverter delegate;

        public AuthMessageConverter(MessageConverter delegate) {
            this.delegate = delegate;
        }

        @Override
        public Message toMessage(Object o, MessageProperties messageProperties) throws MessageConversionException {
            Map<String, Object> headers = messageProperties.getHeaders();
            Long user = UserContext.getUser();
            if (user != null) {
                headers.put("user-info", UserContext.getUser());
            }
            return delegate.toMessage(o, messageProperties);
        }

        @Override
        public Object fromMessage(Message message) throws MessageConversionException {
            Map<String, Object> headers = message.getMessageProperties().getHeaders();
            Object o = headers.get("user-info");
            if (o != null) {
                UserContext.setUser(Long.valueOf(o.toString()));
            }
            return delegate.fromMessage(message);
        }
    }
}
