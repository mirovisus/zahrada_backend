package upce.fei.garden.service;

import upce.fei.garden.dto.review.CreateReview;
import upce.fei.garden.dto.review.ReviewResponse;
import upce.fei.garden.model.Demand;
import upce.fei.garden.model.Owner;
import upce.fei.garden.model.Review;
import upce.fei.garden.model.Worker;

/**
 * Převod mezi entitou {@link Review} a jejími DTO. Držen mimo {@link ReviewService},
 * aby servisní vrstva obsahovala pouze business logiku.
 */
final class ReviewMapper {

    private ReviewMapper() {
    }

    static Review toEntity(CreateReview request, Demand demand, Owner owner, Worker worker) {
        Review review = new Review();
        review.setDemand(demand);
        review.setOwner(owner);
        review.setWorker(worker);
        review.setRating(request.getRating());
        review.setComment(request.getComment());
        return review;
    }

    static ReviewResponse toResponse(Review review) {
        if (review == null) {
            return null;
        }
        return new ReviewResponse(review.getRating(), review.getComment(), review.getCreatedAt());
    }
}
