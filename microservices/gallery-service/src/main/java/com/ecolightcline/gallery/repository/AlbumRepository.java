package com.ecolightcline.gallery.repository;

import com.ecolightcline.gallery.entity.Album;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlbumRepository extends JpaRepository<Album, String> {

    Page<Album> findByOwnerId(String ownerId, Pageable pageable);

    @Query("SELECT a FROM Album a WHERE a.ownerId = :ownerId AND a.isPublic = :isPublic")
    Page<Album> findByOwnerIdAndIsPublic(
            @Param("ownerId") String ownerId,
            @Param("isPublic") Boolean isPublic,
            Pageable pageable);

    @Query("SELECT a FROM Album a WHERE a.isPublic = true")
    Page<Album> findPublicAlbums(Pageable pageable);

    @Query("SELECT a FROM Album a WHERE a.title LIKE %:keyword% OR a.description LIKE %:keyword%")
    Page<Album> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT a FROM Album a WHERE a.ownerId = :ownerId AND (a.title LIKE %:keyword% OR a.description LIKE %:keyword%)")
    Page<Album> searchByOwnerIdAndKeyword(
            @Param("ownerId") String ownerId,
            @Param("keyword") String keyword,
            Pageable pageable);

    @Query("SELECT a FROM Album a WHERE :tag MEMBER OF a.tags")
    Page<Album> findByTag(@Param("tag") String tag, Pageable pageable);

    @Query("SELECT a FROM Album a WHERE a.ownerId = :ownerId AND :tag MEMBER OF a.tags")
    Page<Album> findByOwnerIdAndTag(
            @Param("ownerId") String ownerId,
            @Param("tag") String tag,
            Pageable pageable);

    @Query("SELECT COUNT(a) FROM Album a WHERE a.ownerId = :ownerId")
    long countByOwnerId(@Param("ownerId") String ownerId);

    @Query("SELECT a FROM Album a WHERE a.ownerId = :ownerId ORDER BY a.createdAt DESC")
    List<Album> findLatestByOwnerId(@Param("ownerId") String ownerId, Pageable pageable);

    @Query("SELECT a FROM Album a ORDER BY a.createdAt DESC")
    Page<Album> findAllOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT a FROM Album a WHERE a.ownerId = :ownerId ORDER BY a.createdAt DESC")
    Page<Album> findByOwnerIdOrderByCreatedAtDesc(@Param("ownerId") String ownerId, Pageable pageable);

    @Query("SELECT DISTINCT t FROM Album a JOIN a.tags t WHERE a.ownerId = :ownerId")
    List<String> findDistinctTagsByOwnerId(@Param("ownerId") String ownerId);

    @Query("SELECT DISTINCT t FROM Album a JOIN a.tags t")
    List<String> findAllDistinctTags();

    @Query("SELECT a FROM Album a WHERE a.ownerId = :ownerId AND SIZE(a.imageIds) > 0")
    Page<Album> findNonEmptyAlbums(@Param("ownerId") String ownerId, Pageable pageable);

    @Query("SELECT a FROM Album a WHERE a.ownerId = :ownerId AND SIZE(a.imageIds) = 0")
    Page<Album> findEmptyAlbums(@Param("ownerId") String ownerId, Pageable pageable);

    @Query("SELECT a FROM Album a WHERE a.coverImageId IS NOT NULL")
    Page<Album> findAlbumsWithCoverImages(Pageable pageable);

    @Query("SELECT a FROM Album a WHERE a.ownerId = :ownerId AND a.coverImageId IS NOT NULL")
    Page<Album> findAlbumsWithCoverImagesByOwnerId(@Param("ownerId") String ownerId, Pageable pageable);

    boolean existsByAlbumIdAndOwnerId(String albumId, String ownerId);

    @Query("SELECT a FROM Album a WHERE a.ownerId = :ownerId AND :imageId MEMBER OF a.imageIds")
    List<Album> findByOwnerIdAndImageId(
            @Param("ownerId") String ownerId,
            @Param("imageId") String imageId);

    @Query("SELECT a FROM Album a WHERE :imageId MEMBER OF a.imageIds")
    List<Album> findByImageId(@Param("imageId") String imageId);

    @Query("SELECT COUNT(a) FROM Album a WHERE :imageId MEMBER OF a.imageIds")
    long countAlbumsContainingImage(@Param("imageId") String imageId);

    @Query("SELECT a FROM Album a WHERE a.ownerId = :ownerId AND SIZE(a.imageIds) > :minImageCount")
    Page<Album> findByOwnerIdAndMinImageCount(
            @Param("ownerId") String ownerId,
            @Param("minImageCount") int minImageCount,
            Pageable pageable);
}
