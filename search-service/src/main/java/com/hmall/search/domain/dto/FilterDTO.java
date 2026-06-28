package com.hmall.search.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel(description = "搜索过滤器结果")
public class FilterDTO {
    @ApiModelProperty("分类列表")
    private List<String> category;
    @ApiModelProperty("品牌列表")
    private List<String> brand;
}