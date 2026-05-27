package com.alcohol.controller;

import com.alcohol.common.Result;
import com.alcohol.common.SecuredApi;
import com.alcohol.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
@SecuredApi
@Tag(name = "上传", description = "酒照与 AI 卡片图片上传（multipart/form-data）")
public class UploadController {

    private final FileStorageService fileStorageService;

    @PostMapping(value = "/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传酒照", description = "支持 jpg/png/webp/gif，最大 10MB。返回相对路径 url。")
    public Result<Map<String, String>> uploadPhoto(
            @Parameter(description = "图片文件", required = true,
                    content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            schema = @Schema(type = "string", format = "binary")))
            @RequestParam("file") MultipartFile file) {
        String url = fileStorageService.store(file, "photos");
        return Result.success(Map.of("url", url));
    }

    @PostMapping(value = "/card", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传生成卡片图")
    public Result<Map<String, String>> uploadCard(
            @Parameter(description = "卡片图片", required = true,
                    content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            schema = @Schema(type = "string", format = "binary")))
            @RequestParam("file") MultipartFile file) {
        String url = fileStorageService.store(file, "cards");
        return Result.success(Map.of("url", url));
    }

    @PostMapping(value = "/voice", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传语音记录", description = "支持 webm/mp3/mp4/wav/ogg")
    public Result<Map<String, String>> uploadVoice(
            @Parameter(description = "语音文件", required = true,
                    content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            schema = @Schema(type = "string", format = "binary")))
            @RequestParam("file") MultipartFile file) {
        String url = fileStorageService.storeVoice(file);
        return Result.success(Map.of("url", url));
    }
}
