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
        if (!configured) {
            throw new RuntimeException("Cloudinary is not configured");
        }

        try {
            Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.emptyMap());
            return (String) uploadResult.get("secure_url");
        } catch (IOException e) {
            log.error("Cloudinary upload failed: {}", e.getMessage());
            throw new RuntimeException("File upload failed: " + e.getMessage());
        }
    }
}
