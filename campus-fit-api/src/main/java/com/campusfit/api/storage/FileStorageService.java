package com.campusfit.api.storage;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

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
}
