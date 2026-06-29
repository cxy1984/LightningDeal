package com.lightningdeal.search.controller;

import com.lightningdeal.common.response.R;
import com.lightningdeal.search.entity.ActivityIndex;
import com.lightningdeal.search.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

/**
 * 搜索控制器
 */
@Tag(name = "商品搜索", description = "ES 全文搜索（ik 分词）")
@RestController
@RequestMapping("/search")
public class SearchController {

    private final ObjectProvider<SearchService> searchServiceProvider;

    public SearchController(ObjectProvider<SearchService> searchServiceProvider) {
        this.searchServiceProvider = searchServiceProvider;
    }

    @Operation(summary = "搜索活动商品")
    @GetMapping("/activity")
    public R<?> search(
            @Parameter(description = "关键词") @RequestParam String keyword,
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size) {
        SearchService searchService = searchServiceProvider.getIfAvailable();
        if (searchService == null) {
            return R.fail(400, "搜索服务未启用（需安装 Elasticsearch）");
        }
        return R.ok(searchService.search(keyword, PageRequest.of(page, size)));
    }
}
