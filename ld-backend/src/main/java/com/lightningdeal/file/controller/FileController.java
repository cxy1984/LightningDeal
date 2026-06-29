package com.lightningdeal.file.controller;

import com.lightningdeal.common.response.R;
import com.lightningdeal.file.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传控制器
 */
@Tag(name = "文件管理", description = "MinIO 文件上传/下载")
@RestController
@RequestMapping("/file")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @Operation(summary = "上传商品图片")
    @PostMapping("/upload")
    public R<String> upload(@RequestParam("file") MultipartFile file) {
        String url = fileService.upload(file);
        return R.ok("上传成功", url);
    }

    @Operation(summary = "获取文件访问链接")
    @GetMapping("/url")
    public R<String> getFileUrl(@RequestParam String filename) {
        return R.ok(fileService.getFileUrl(filename));
    }

    @Operation(summary = "删除文件")
    @DeleteMapping("/delete")
    public R<String> delete(@RequestParam String filename) {
        fileService.delete(filename);
        return R.ok("删除成功");
    }
}
