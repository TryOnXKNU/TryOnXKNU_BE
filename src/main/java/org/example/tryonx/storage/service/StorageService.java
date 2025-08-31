package org.example.tryonx.storage.service;

public interface StorageService {
    /** @param path 예: "product/123/" */
    String store(org.springframework.web.multipart.MultipartFile file, String path) throws Exception;
}
