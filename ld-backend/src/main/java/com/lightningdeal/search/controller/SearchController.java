package com.lightningdeal.search.controller;

import com.lightningdeal.activity.entity.SeckillActivity;
import com.lightningdeal.activity.service.SeckillActivityService;
import com.lightningdeal.common.response.R;
import com.lightningdeal.search.entity.ActivityIndex;
import com.lightningdeal.search.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 搜索控制器
 */
@Tag(name = "商品搜索", description = "ES 全文搜索（ik 分词）")
@RestController
@RequestMapping("/search")
public class SearchController {

    private final ObjectProvider<SearchService> searchServiceProvider;
    private final SeckillActivityService activityService;

    public SearchController(ObjectProvider<SearchService> searchServiceProvider,
                           SeckillActivityService activityService) {
        this.searchServiceProvider = searchServiceProvider;
        this.activityService = activityService;
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

    @Operation(summary = "批量同步活动数据到 ES（需管理员）")
    @PostMapping("/sync")
    @PreAuthorize("hasRole('ADMIN')")
    public R<String> syncAll() {
        SearchService searchService = searchServiceProvider.getIfAvailable();
        if (searchService == null) {
            return R.fail(400, "搜索服务未启用（需安装 Elasticsearch）");
        }
        List<SeckillActivity> activities = activityService.lambdaQuery()
                .eq(SeckillActivity::getDeleted, 0)
                .list();
        searchService.syncActivities(activities);
        return R.ok("同步成功，共 " + activities.size() + " 个活动");
    }
}
