package iti.jets.java.homenursing.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import iti.jets.java.homenursing.service.CloudinaryService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
public class CloudinaryServiceImpl implements CloudinaryService {

    private static final Logger log = LoggerFactory.getLogger(CloudinaryServiceImpl.class);

    @Value("${CLOUDINARY_CLOUD_NAME:}")
    private String cloudName;

    @Value("${CLOUDINARY_API_KEY:}")
    private String apiKey;

    @Value("${CLOUDINARY_API_SECRET:}")
    private String apiSecret;

    private Cloudinary cloudinary;

    private boolean configured = false;

    @PostConstruct
    public void init() {
        if (!cloudName.isBlank() && !apiKey.isBlank() && !apiSecret.isBlank()) {
            cloudinary = new Cloudinary(ObjectUtils.asMap(
                    "cloud_name", cloudName,
                    "api_key", apiKey,
                    "api_secret", apiSecret
            ));
            configured = true;
            log.info("Cloudinary service initialized");
        } else {
            log.warn("Cloudinary not configured — file uploads will not work");
        }
    }

    @Override
    public String upload(MultipartFile file) {
        return upload(file, Map.of());
    }

    @Override
    public void delete(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            return;
        }
        URI uri = URI.create(fileUrl);
        if (uri.getHost() == null || !uri.getHost().endsWith("res.cloudinary.com")) {
            log.debug("Skipping deletion for non-Cloudinary URL: {}", fileUrl);
            return;
        }
        ensureConfigured();

        String publicId = extractPublicId(uri);
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.asMap(
                    "resource_type", "image",
                    "invalidate", true
            ));
        } catch (IOException e) {
            log.error("Cloudinary deletion failed for {}: {}", publicId, e.getMessage());
            throw new RuntimeException("File deletion failed: " + e.getMessage(), e);
        }
    }

    private String upload(MultipartFile file, Map<String, Object> options) {
        ensureConfigured();

        try {
            Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(), options);
            return (String) uploadResult.get("secure_url");
        } catch (IOException e) {
            log.error("Cloudinary upload failed: {}", e.getMessage());
            throw new RuntimeException("File upload failed: " + e.getMessage(), e);
        }
    }

    private void ensureConfigured() {
        if (!configured) {
            throw new RuntimeException("Cloudinary is not configured");
        }
    }

    private String extractPublicId(URI uri) {
        String path = uri.getPath();
        int uploadMarker = path.indexOf("/upload/");
        if (uploadMarker < 0) {
            throw new IllegalArgumentException("Invalid Cloudinary URL");
        }

        String publicId = path.substring(uploadMarker + "/upload/".length());
        publicId = publicId.replaceFirst("^v\\d+/", "");
        int extensionIndex = publicId.lastIndexOf('.');
        if (extensionIndex > 0) {
            publicId = publicId.substring(0, extensionIndex);
        }
        return URLDecoder.decode(publicId, StandardCharsets.UTF_8);
    }
}
