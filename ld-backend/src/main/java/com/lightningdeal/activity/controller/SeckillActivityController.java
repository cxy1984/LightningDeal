package com.lightningdeal.activity.controller;

import com.lightningdeal.activity.model.ActivityVO;
import com.lightningdeal.activity.service.SeckillActivityService;
import com.lightningdeal.common.response.R;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * 秒杀活动控制器
 */
@Tag(name = "秒杀活动", description = "活动 CRUD、库存预热")
@RestController
@RequestMapping("/activity")
@RequiredArgsConstructor
public class SeckillActivityController {

    private final SeckillActivityService activityService;

    @Operation(summary = "活动列表（分页）")
    @GetMapping("/list")
    public R<IPage<ActivityVO>> list(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "状态筛选") @RequestParam(required = false) Integer status) {
        return R.ok(activityService.listActivities(page, size, status));
    }

    @Operation(summary = "活动详情")
    @GetMapping("/detail/{id}")
    public R<ActivityVO> detail(@PathVariable Long id) {
        return R.ok(activityService.getActivityDetail(id));
    }

    @Operation(summary = "创建活动（需登录）")
    @PostMapping("/create")
    public R<ActivityVO> create(@RequestBody com.lightningdeal.activity.entity.SeckillActivity activity,
                                Authentication authentication) {
        return R.ok(activityService.createActivity(activity));
    }

    @Operation(summary = "更新活动（需登录）")
    @PutMapping("/update")
    public R<ActivityVO> update(@RequestBody com.lightningdeal.activity.entity.SeckillActivity activity) {
        return R.ok(activityService.updateActivity(activity));
    }

    @Operation(summary = "预热库存到 Redis")
    @PostMapping("/preheat/{id}")
    public R<String> preheat(@PathVariable Long id) {
        activityService.preheatStock(id);
        return R.ok("库存预热成功");
    }
}
