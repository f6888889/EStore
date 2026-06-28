package com.hmall.search.listener;

import cn.hutool.json.JSONUtil;
import com.hmall.search.domain.dto.OrderDetailDTO;
import com.hmall.search.domain.po.ItemDoc;
import lombok.RequiredArgsConstructor;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.xcontent.XContentType;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class UpdateElasticListener {
    private final RestHighLevelClient client;
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "item.add"),
            exchange = @Exchange(name = "search.exchange"),
            key = "item.add"
    ))
    public void add(ItemDoc itemDoc) throws IOException {
        IndexRequest request = new IndexRequest("items").id(itemDoc.getId());
        String jsonStr = JSONUtil.toJsonStr(itemDoc);
        request.source(jsonStr, XContentType.JSON);
        client.index(request, RequestOptions.DEFAULT);
    }
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "item.update"),
            exchange = @Exchange(name = "search.exchange"),
            key = "item.update"
    ))
    public void update(ItemDoc itemDoc) throws IOException {
        IndexRequest request = new IndexRequest("items").id(itemDoc.getId());
        String jsonStr = JSONUtil.toJsonStr(itemDoc);
        request.source(jsonStr, XContentType.JSON);
        client.index(request, RequestOptions.DEFAULT);
    }
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "item.delete"),
            exchange = @Exchange(name = "search.exchange"),
            key = "item.delete"
    ))
    public void delete(Long id) throws IOException {
        DeleteRequest request = new DeleteRequest("items", id.toString());
        client.delete(request, RequestOptions.DEFAULT);
    }

}
