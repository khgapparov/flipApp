package com.lovablecline.service;

import com.lovablecline.entity.GalleryItem;
import com.lovablecline.entity.Project;
import com.lovablecline.repository.GalleryItemRepository;
import com.lovablecline.repository.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class GalleryItemService {

    @Autowired
    private GalleryItemRepository galleryItemRepository;

    @Autowired
    private ProjectRepository projectRepository;

    public List<GalleryItem> getAllGalleryItemsByProjectId(String userId, String projectId) {
        return galleryItemRepository.findByUserIdAndProjectId(userId, projectId);
    }

    public Optional<GalleryItem> getGalleryItemById(String userId, String projectId, String itemId) {
        return galleryItemRepository.findByIdAndProjectId(itemId, projectId);
    }

    public GalleryItem createGalleryItem(String userId, String projectId, GalleryItem galleryItem) {
        Optional<Project> projectOptional = projectRepository.findByUserIdAndProjectId(userId, projectId);
        if (projectOptional.isEmpty()) {
            throw new RuntimeException("Project not found");
        }

        galleryItem.setProject(projectOptional.get());
        return galleryItemRepository.save(galleryItem);
    }

    public GalleryItem updateGalleryItem(String userId, String projectId, String itemId, GalleryItem itemDetails) {
        Optional<GalleryItem> itemOptional = galleryItemRepository.findByIdAndProjectId(itemId, projectId);
        if (itemOptional.isEmpty()) {
            throw new RuntimeException("Gallery item not found");
        }

        GalleryItem item = itemOptional.get();
        
        if (itemDetails.getImageUrl() != null) {
            item.setImageUrl(itemDetails.getImageUrl());
        }
        if (itemDetails.getTitle() != null) {
            item.setTitle(itemDetails.getTitle());
        }
        if (itemDetails.getDescription() != null) {
            item.setDescription(itemDetails.getDescription());
        }
        if (itemDetails.getRoom() != null) {
            item.setRoom(itemDetails.getRoom());
        }
        if (itemDetails.getStage() != null) {
            item.setStage(itemDetails.getStage());
        }

        return galleryItemRepository.save(item);
    }

    public void deleteGalleryItem(String userId, String projectId, String itemId) {
        Optional<GalleryItem> itemOptional = galleryItemRepository.findByIdAndProjectId(itemId, projectId);
        if (itemOptional.isEmpty()) {
            throw new RuntimeException("Gallery item not found");
        }

        galleryItemRepository.delete(itemOptional.get());
    }

    public List<GalleryItem> getAllGalleryItemsByUserId(String userId) {
        return galleryItemRepository.findAllByUserId(userId);
    }

    public List<GalleryItem> getGalleryItemsByProjectIdOrdered(String projectId) {
        return galleryItemRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
    }

    public List<GalleryItem> getGalleryItemsByRoom(String projectId, String room) {
        return galleryItemRepository.findByProjectIdAndRoom(projectId, room);
    }

    public List<GalleryItem> getGalleryItemsByStage(String projectId, String stage) {
        return galleryItemRepository.findByProjectIdAndStage(projectId, stage);
    }
}
