package com.ecolightcline.gallery.service;

import net.coobird.thumbnailator.Thumbnails;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class ImageProcessingService {

    @Value("${gallery.upload-dir:./uploads}")
    private String uploadDir;

    @Value("${gallery.thumbnail.width:300}")
    private int thumbnailWidth;

    @Value("${gallery.thumbnail.height:300}")
    private int thumbnailHeight;

    @Value("${gallery.thumbnail.quality:0.8}")
    private double thumbnailQuality;

    public ImageProcessingResult processImage(MultipartFile file) throws IOException {
        // Create upload directory if it doesn't exist
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String fileExtension = FilenameUtils.getExtension(originalFilename);
        String baseFilename = UUID.randomUUID().toString();
        String imageFilename = baseFilename + "." + fileExtension;
        String thumbnailFilename = baseFilename + "_thumb." + fileExtension;

        // Save original image
        Path imagePath = uploadPath.resolve(imageFilename);
        Files.copy(file.getInputStream(), imagePath);

        // Process image to get dimensions and format
        BufferedImage originalImage = ImageIO.read(file.getInputStream());
        int width = originalImage.getWidth();
        int height = originalImage.getHeight();
        long size = file.getSize();
        String format = fileExtension.toUpperCase();

        // Create thumbnail
        Path thumbnailPath = uploadPath.resolve(thumbnailFilename);
        Thumbnails.of(file.getInputStream())
                .size(thumbnailWidth, thumbnailHeight)
                .outputQuality(thumbnailQuality)
                .toFile(thumbnailPath.toFile());

        return new ImageProcessingResult(
            imagePath.toString(),
            thumbnailPath.toString(),
            width,
            height,
            size,
            format
        );
    }

    public static class ImageProcessingResult {
        private final String imageUrl;
        private final String thumbnailUrl;
        private final int width;
        private final int height;
        private final long size;
        private final String format;

        public ImageProcessingResult(String imageUrl, String thumbnailUrl, int width, int height, long size, String format) {
            this.imageUrl = imageUrl;
            this.thumbnailUrl = thumbnailUrl;
            this.width = width;
            this.height = height;
            this.size = size;
            this.format = format;
        }

        public String getImageUrl() {
            return imageUrl;
        }

        public String getThumbnailUrl() {
            return thumbnailUrl;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

        public long getSize() {
            return size;
        }

        public String getFormat() {
            return format;
        }
    }

    public void deleteImageFiles(String imageUrl, String thumbnailUrl) throws IOException {
        Path imagePath = Paths.get(imageUrl);
        Path thumbnailPath = Paths.get(thumbnailUrl);

        Files.deleteIfExists(imagePath);
        Files.deleteIfExists(thumbnailPath);
    }

    public boolean validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false;
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return false;
        }

        // Check file size (max 10MB)
        if (file.getSize() > 10 * 1024 * 1024) {
            return false;
        }

        // Check file extension
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            return false;
        }

        String extension = FilenameUtils.getExtension(originalFilename).toLowerCase();
        return extension.equals("jpg") || extension.equals("jpeg") || 
               extension.equals("png") || extension.equals("gif") || 
               extension.equals("webp") || extension.equals("bmp");
    }
}
