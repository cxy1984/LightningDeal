package com.lightningdeal.seckill.controller;

import com.lightningdeal.common.annotation.RateLimit;
import com.lightningdeal.common.response.R;
import com.lightningdeal.seckill.model.SeckillRequest;
import com.lightningdeal.seckill.model.SeckillResult;
import com.lightningdeal.seckill.service.SeckillService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 秒杀核心控制器
 */
@Tag(name = "秒杀核心", description = "高并发秒杀入口")
@RestController
@RequestMapping("/seckill")
@RequiredArgsConstructor
public class SeckillController {

    private final SeckillService seckillService;

    @Operation(summary = "执行秒杀")
    @PostMapping("/execute")
    @RateLimit(
            qps = 5,
            capacity = 10,
            key = "'seckill:' + #request.activityId + ':' + #authentication.principal",
            message = "秒杀太火爆啦，请稍后再试"
    )
    public R<SeckillResult> execute(@Valid @RequestBody SeckillRequest request,
                                    Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        SeckillResult result = seckillService.executeSeckill(userId, request);
        return R.ok(result);
    }

    @Operation(summary = "查询秒杀结果")
    @GetMapping("/result/{activityId}")
    public R<SeckillResult> getResult(@PathVariable Long activityId,
                                      Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        SeckillResult result = seckillService.getSeckillResult(userId, activityId);
        return result != null ? R.ok(result) : R.ok(SeckillResult.queuing(activityId));
    }
}
