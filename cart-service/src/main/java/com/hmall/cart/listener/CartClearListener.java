package com.hmall.cart.listener;

import com.hmall.cart.service.ICartService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.stereotype.Component;

import java.util.Set;


@Component
@RequiredArgsConstructor
public class CartClearListener {
    private final ICartService cartService;
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "cart.clear.queue",durable = "true",
                            arguments =@Argument(name = "x-queue-mode",value = "lazy")),
            exchange = @Exchange(name = "trade.topic",type = "topic"),
            key = "order.create"
    ))
    public void listenerCartClear(Set<Long> itemIds){
        cartService.removeByItemIds(itemIds);
    }

}
