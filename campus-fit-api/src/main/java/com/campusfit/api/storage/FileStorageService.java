package com.campusfit.api.storage;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@SuppressWarnings("null")
@Service
public class FileStorageService {

    private final Path uploadDir;

    public FileStorageService(@Value("${app.file.upload-dir:uploads/}") String uploadDirStr) {
        this.uploadDir = Paths.get(uploadDirStr).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadDir);
        } catch (IOException e) {
            throw new RuntimeException("업로드 디렉토리를 생성할 수 없습니다.", e);
        }
    }

    public String store(MultipartFile file) {
        try {
            String ext = "";
            String original = file.getOriginalFilename();
            if (original != null && original.contains(".")) {
                ext = original.substring(original.lastIndexOf('.'));
            }
            String fileName = UUID.randomUUID() + ext;
            Path target = uploadDir.resolve(fileName);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return uploadDir.relativize(target).toString();
        } catch (IOException e) {
            throw new RuntimeException("파일 저장 실패", e);
        }
    }

    public Resource loadAsResource(String storedPath) {
        try {
            Path filePath = uploadDir.resolve(storedPath).normalize();
            // 경로 타뎠 방지: 파일 경로가 uploadDir 내부여야만 허용
            if (!filePath.startsWith(uploadDir)) {
                throw new RuntimeException("접근 불가한 경로입니다.");
            }
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new RuntimeException("파일을 찾을 수 없습니다: " + storedPath);
            }
            return resource;
        } catch (MalformedURLException e) {
            throw new RuntimeException("파일 로드 실패", e);
        }
    }
}
