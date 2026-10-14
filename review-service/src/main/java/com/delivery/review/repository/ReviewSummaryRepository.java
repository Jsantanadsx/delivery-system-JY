package com.delivery.review.repository;

import com.delivery.review.model.ReviewSummary;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewSummaryRepository extends JpaRepository<ReviewSummary, Long> { }
