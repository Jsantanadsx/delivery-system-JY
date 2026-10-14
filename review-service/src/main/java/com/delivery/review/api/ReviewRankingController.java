package com.delivery.review.api;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import com.delivery.review.model.ReviewSummary;
import com.delivery.review.repository.ReviewSummaryRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ReviewRankingController {
    private final ReviewSummaryRepository summaries;

    public ReviewRankingController(ReviewSummaryRepository summaries) { this.summaries = summaries; }

    @GetMapping("/reviews/ranking")
    public List<RankingEntry> ranking() {
        return summaries.findAll().stream()
                .filter(summary -> summary.getRatingCount() > 0)
                .map(this::toEntry)
                .sorted(Comparator.comparingDouble(RankingEntry::average).reversed()
                        .thenComparing(RankingEntry::dishName))
                .toList();
    }

    private RankingEntry toEntry(ReviewSummary summary) {
        double average = BigDecimal.valueOf(summary.getRatingSum())
                .divide(BigDecimal.valueOf(summary.getRatingCount()), 2, RoundingMode.HALF_UP)
                .doubleValue();
        return new RankingEntry(summary.getDishId(), summary.getDishName(), average, summary.getRatingCount());
    }
}
