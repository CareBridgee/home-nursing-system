package iti.jets.java.homenursing.controller;

import iti.jets.java.homenursing.service.CloudinaryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/upload")
public class FileUploadController {

    private final CloudinaryService cloudinaryService;

    public FileUploadController(CloudinaryService cloudinaryService) {
        this.cloudinaryService = cloudinaryService;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        String url = cloudinaryService.upload(file);
        return ResponseEntity.ok(Map.of("url", url));
    }
}
