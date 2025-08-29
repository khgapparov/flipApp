package com.ecolightcline.gallery.repository;

import com.ecolightcline.gallery.entity.Image;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ImageRepository extends JpaRepository<Image, String> {

    Page<Image> findByOwnerId(String ownerId, Pageable pageable);

    @Query("SELECT i FROM Image i WHERE i.ownerId = :ownerId AND :albumId MEMBER OF i.albumIds")
    Page<Image> findByOwnerIdAndAlbumId(
            @Param("ownerId") String ownerId,
            @Param("albumId") String albumId,
            Pageable pageable);

    @Query("SELECT i FROM Image i WHERE :tag MEMBER OF i.tags")
    Page<Image> findByTag(@Param("tag") String tag, Pageable pageable);

    @Query("SELECT i FROM Image i WHERE i.ownerId = :ownerId AND :tag MEMBER OF i.tags")
    Page<Image> findByOwnerIdAndTag(
            @Param("ownerId") String ownerId,
            @Param("tag") String tag,
            Pageable pageable);

    @Query("SELECT i FROM Image i WHERE i.format = :format")
    Page<Image> findByFormat(@Param("format") Image.ImageFormat format, Pageable pageable);

    @Query("SELECT i FROM Image i WHERE i.ownerId = :ownerId AND i.format = :format")
    Page<Image> findByOwnerIdAndFormat(
            @Param("ownerId") String ownerId,
            @Param("format") Image.ImageFormat format,
            Pageable pageable);

    @Query("SELECT i FROM Image i WHERE i.title LIKE %:keyword% OR i.description LIKE %:keyword%")
    Page<Image> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT i FROM Image i WHERE i.ownerId = :ownerId AND (i.title LIKE %:keyword% OR i.description LIKE %:keyword%)")
    Page<Image> searchByOwnerIdAndKeyword(
            @Param("ownerId") String ownerId,
            @Param("keyword") String keyword,
            Pageable pageable);

    @Query("SELECT i FROM Image i WHERE :albumId MEMBER OF i.albumIds")
    Page<Image> findByAlbumId(@Param("albumId") String albumId, Pageable pageable);

    @Query("SELECT COUNT(i) FROM Image i WHERE i.ownerId = :ownerId")
    long countByOwnerId(@Param("ownerId") String ownerId);

    @Query("SELECT COUNT(i) FROM Image i WHERE :albumId MEMBER OF i.albumIds")
    long countByAlbumId(@Param("albumId") String albumId);

    @Query("SELECT COUNT(i) FROM Image i WHERE i.ownerId = :ownerId AND :albumId MEMBER OF i.albumIds")
    long countByOwnerIdAndAlbumId(
            @Param("ownerId") String ownerId,
            @Param("albumId") String albumId);

    @Query("SELECT i FROM Image i WHERE i.ownerId = :ownerId ORDER BY i.createdAt DESC")
    List<Image> findLatestByOwnerId(@Param("ownerId") String ownerId, Pageable pageable);

    @Query("SELECT i FROM Image i ORDER BY i.createdAt DESC")
    Page<Image> findAllOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT i FROM Image i WHERE i.ownerId = :ownerId ORDER BY i.createdAt DESC")
    Page<Image> findByOwnerIdOrderByCreatedAtDesc(@Param("ownerId") String ownerId, Pageable pageable);

    // Removed findPublicImages and findPublicImagesByOwnerId queries as Image entity doesn't have isPublic field

    @Query("SELECT DISTINCT t FROM Image i JOIN i.tags t WHERE i.ownerId = :ownerId")
    List<String> findDistinctTagsByOwnerId(@Param("ownerId") String ownerId);

    @Query("SELECT DISTINCT t FROM Image i JOIN i.tags t")
    List<String> findAllDistinctTags();

    @Query("SELECT i FROM Image i WHERE i.ownerId = :ownerId AND SIZE(i.albumIds) = 0")
    Page<Image> findUncategorizedImages(@Param("ownerId") String ownerId, Pageable pageable);

    @Query("SELECT i FROM Image i WHERE i.ownerId = :ownerId AND i.imageId IN (SELECT a.coverImageId FROM Album a WHERE a.ownerId = :ownerId AND a.coverImageId IS NOT NULL)")
    Page<Image> findCoverImages(@Param("ownerId") String ownerId, Pageable pageable);

    boolean existsByImageIdAndOwnerId(String imageId, String ownerId);

    @Query("SELECT i FROM Image i WHERE i.ownerId = :ownerId AND i.width > :minWidth AND i.height > :minHeight")
    Page<Image> findByOwnerIdAndMinDimensions(
            @Param("ownerId") String ownerId,
            @Param("minWidth") int minWidth,
            @Param("minHeight") int minHeight,
            Pageable pageable);

    @Query("SELECT i FROM Image i WHERE i.ownerId = :ownerId AND i.size > :minSize")
    Page<Image> findByOwnerIdAndMinSize(
            @Param("ownerId") String ownerId,
            @Param("minSize") long minSize,
            Pageable pageable);
}
