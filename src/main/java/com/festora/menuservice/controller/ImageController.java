package com.festora.menuservice.controller;

import com.festora.menuservice.service.SupabaseStorageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.concurrent.atomic.AtomicReference;

@RestController
@RequestMapping("/menu")
@RequestMapping("/api/images")
public class ImageController {

    private final SupabaseStorageService storage;
    private final S3Client s3Client;

    public ImageController(SupabaseStorageService storage, S3Client s3Client) {
        this.storage = storage;
        this.s3Client = s3Client;
    }

    @PostMapping("/upload")
    public ResponseEntity<String> upload(@RequestParam("file") MultipartFile file) {
        try {
            String key = storage.upload(file);
            return ResponseEntity.ok(key);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{key}")
    public ResponseEntity<String> getSignedUrl(@PathVariable String key) {

        return ResponseEntity.ok(storage.getSignedUrl(key));
    }
}