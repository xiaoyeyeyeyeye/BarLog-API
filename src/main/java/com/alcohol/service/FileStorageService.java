package com.alcohol.service;

import com.alcohol.common.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class FileStorageService {

    private static final Set<String> IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp", "image/gif");
    private static final Set<String> VOICE_TYPES = Set.of("audio/webm", "audio/mpeg", "audio/mp4", "audio/wav", "audio/ogg");

    @Value("${alcohol.upload-dir:./uploads}")
    private String uploadDir;

    public String store(MultipartFile file, String subDir) {
        return store(file, subDir, IMAGE_TYPES, "仅支持 jpg/png/webp/gif 图片");
    }

    public String storeVoice(MultipartFile file) {
        return store(file, "voice", VOICE_TYPES, "仅支持 webm/mp3/mp4/wav/ogg 音频");
    }

    private String store(MultipartFile file, String subDir, Set<String> allowed, String typeError) {
        if (file == null || file.isEmpty()) {
            throw new BizException("请选择文件");
        }
        String contentType = file.getContentType();
        if (contentType == null || !allowed.contains(contentType)) {
            throw new BizException(typeError);
        }
        String ext = extensionFor(contentType);
        try {
            Path dir = Paths.get(uploadDir, subDir).toAbsolutePath().normalize();
            Files.createDirectories(dir);
            String filename = UUID.randomUUID() + ext;
            Path target = dir.resolve(filename);
            file.transferTo(target);
            return "/uploads/" + subDir + "/" + filename;
        } catch (IOException e) {
            log.error("upload failed", e);
            throw new BizException("文件上传失败");
        }
    }

    private String extensionFor(String contentType) {
        return switch (contentType) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            case "audio/webm" -> ".webm";
            case "audio/mpeg" -> ".mp3";
            case "audio/mp4" -> ".m4a";
            case "audio/wav" -> ".wav";
            case "audio/ogg" -> ".ogg";
            default -> ".jpg";
        };
    }
}
