package com.workoutsmart.nutrition.repository;

import com.workoutsmart.nutrition.entity.FoodItem;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface FoodItemRepository extends JpaRepository<FoodItem, Long> {

    @Query("SELECT f FROM FoodItem f WHERE f.deletedAt IS NULL "
            + "AND (f.source = 'system' OR f.createdBy = :userId) "
            + "AND LOWER(f.name) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<FoodItem> searchVisible(String query, Long userId, Pageable pageable);

    List<FoodItem> findByCreatedByAndDeletedAtIsNull(Long userId);
}
