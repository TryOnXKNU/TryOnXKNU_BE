package org.example.tryonx.storage.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

@Service
@ConditionalOnProperty(name="app.storage", havingValue="local", matchIfMissing = true)
public class LocalStorageService implements StorageService {
    @Value("${app.upload.dir:upload}") private String uploadRoot;
    @Override public String store(MultipartFile file, String path) throws Exception {
        Path root = Paths.get(uploadRoot).toAbsolutePath();
        Path dir = root.resolve(path);
        Files.createDirectories(dir);
        String safe = System.currentTimeMillis()+"_"+ Objects.requireNonNull(file.getOriginalFilename());
        Path out = dir.resolve(safe);
        Files.copy(file.getInputStream(), out, StandardCopyOption.REPLACE_EXISTING);
        return "/" + root.getFileName() + "/" + path + safe;
    }
}
