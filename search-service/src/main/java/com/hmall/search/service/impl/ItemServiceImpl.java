package com.hmall.search.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmall.common.domain.PageDTO;

import com.hmall.common.utils.CollUtils;
import com.hmall.search.domain.dto.FilterDTO;
import com.hmall.search.domain.dto.ItemDTO;
import com.hmall.search.domain.po.Item;
import com.hmall.search.domain.po.ItemDoc;
import com.hmall.search.domain.query.ItemPageQuery;
import com.hmall.search.mapper.ItemMapper;
import com.hmall.search.service.IItemService;
import lombok.RequiredArgsConstructor;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.lucene.search.function.CombineFunction;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.elasticsearch.search.sort.SortOrder.ASC;
import static org.elasticsearch.search.sort.SortOrder.DESC;

/**
 * <p>
 * 商品表 服务实现类
 * </p>
 *
 * @author 虎哥
 */
@Service
@RequiredArgsConstructor
public class ItemServiceImpl extends ServiceImpl<ItemMapper, Item> implements IItemService {
    private final RestHighLevelClient client;
    @Override
    public PageDTO<ItemDTO> search(ItemPageQuery query) throws IOException {
        SearchRequest request = new SearchRequest("items");
        BoolQueryBuilder bool = QueryBuilders.boolQuery();
        if (StrUtil.isNotBlank(query.getKey())) {
            bool.must(QueryBuilders.matchQuery("name", query.getKey()));
        }
        if (StrUtil.isNotBlank(query.getBrand())) {
            bool.filter(QueryBuilders.termQuery("brand", query.getBrand()));
        }
        if (StrUtil.isNotBlank(query.getCategory())) {
            bool.filter(QueryBuilders.termQuery("category", query.getCategory()));
        }
        if (query.getMinPrice() != null&&query.getMaxPrice() != null) {
            bool.filter(QueryBuilders.rangeQuery("price").gte(query.getMinPrice()).lte(query.getMaxPrice()));
        }
        if (StrUtil.isNotBlank(query.getSortBy())) {
            request.source().sort(query.getSortBy(), query.getIsAsc() ? ASC : DESC);
        }
        // 2. 集成function_score查询提升广告商品排名
        FunctionScoreQueryBuilder functionScoreQuery = QueryBuilders.functionScoreQuery(
                        bool, // 基础查询条件
                        new FunctionScoreQueryBuilder.FilterFunctionBuilder[]{
                                // 对isAD=true的商品提升权重
                                new FunctionScoreQueryBuilder.FilterFunctionBuilder(
                                        QueryBuilders.termQuery("isAD", true),
                                        ScoreFunctionBuilders.weightFactorFunction(10) // 权重因子，可根据需求调整
                                )
                        })
                .boostMode(CombineFunction.MULTIPLY);
        request.source()
                .query(functionScoreQuery)
                .from(query.from())
                .size(query.getPageSize());
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        return handleResponse(response);
    }

    @Override
    public FilterDTO getFilters(ItemPageQuery query) throws IOException {
        SearchRequest request = new SearchRequest("items");
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

        // 2.构建布尔查询
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

        // 关键字搜索
        if (query.getKey() != null && !query.getKey().isEmpty()) {
            boolQuery.must(QueryBuilders.matchQuery("name", query.getKey()));
        }

        // 分类过滤
        if (query.getCategory() != null && !query.getCategory().isEmpty()) {
            boolQuery.filter(QueryBuilders.termQuery("category", query.getCategory()));
        }

        // 品牌过滤
        if (query.getBrand() != null && !query.getBrand().isEmpty()) {
            boolQuery.filter(QueryBuilders.termQuery("brand", query.getBrand()));
        }

        // 价格范围过滤
        if (query.getMinPrice() != null || query.getMaxPrice() != null) {
            BoolQueryBuilder priceQuery = QueryBuilders.boolQuery();
            if (query.getMinPrice() != null) {
                priceQuery.filter(QueryBuilders.rangeQuery("price").gte(query.getMinPrice()));
            }
            if (query.getMaxPrice() != null) {
                priceQuery.filter(QueryBuilders.rangeQuery("price").lte(query.getMaxPrice()));
            }
            boolQuery.filter(priceQuery);
        }

        sourceBuilder.query(boolQuery);
        // 3.添加聚合查询
        // 分类聚合
        sourceBuilder.aggregation(AggregationBuilders.terms("category_agg").field("category").size(100));
        // 品牌聚合
        sourceBuilder.aggregation(AggregationBuilders.terms("brand_agg").field("brand").size(100));

        // 只需要聚合结果，不需要文档内容
        sourceBuilder.size(0);

        request.source(sourceBuilder);

        // 4.执行查询
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        // 5.解析聚合结果
        FilterDTO filterDTO = getFilterDTO(response);

        return filterDTO;

    }

    private static FilterDTO getFilterDTO(SearchResponse response) {
        FilterDTO filterDTO = new FilterDTO();

        // 解析分类聚合
        Terms categoryTerms = response.getAggregations().get("category_agg");
        List<String> categories = categoryTerms.getBuckets().stream()
                .map(bucket -> bucket.getKeyAsString())
                .collect(Collectors.toList());
        filterDTO.setCategory(categories);

        // 解析品牌聚合
        Terms brandTerms = response.getAggregations().get("brand_agg");
        List<String> brands = brandTerms.getBuckets().stream()
                .map(bucket -> bucket.getKeyAsString())
                .collect(Collectors.toList());
        filterDTO.setBrand(brands);
        return filterDTO;
    }

    private PageDTO<ItemDTO> handleResponse(SearchResponse response) {
        SearchHits searchHits = response.getHits();
        // 1.获取总条数
        long total = searchHits.getTotalHits().value;
        // 2.遍历结果数组
        SearchHit[] hits = searchHits.getHits();
        List<ItemDTO> pageList = new ArrayList<>();
        PageDTO<ItemDTO> page = new PageDTO<>();
        page.setTotal(total);
        for (SearchHit hit : hits) {
            // 3.得到_source，也就是原始json文档
            String source = hit.getSourceAsString();
            // 4.反序列化
            ItemDTO item = JSONUtil.toBean(source, ItemDTO.class);
            // 5.获取高亮结果
            Map<String, HighlightField> hfs = hit.getHighlightFields();
            if (CollUtils.isNotEmpty(hfs)) {
                // 5.1.有高亮结果，获取name的高亮结果
                HighlightField hf = hfs.get("name");
                if (hf != null) {
                    // 5.2.获取第一个高亮结果片段，就是商品名称的高亮值
                    String hfName = hf.getFragments()[0].string();
                    item.setName(hfName);
                }
            }
            pageList.add( item);
        }
        page.setList(pageList);
        return page;
    }
}
