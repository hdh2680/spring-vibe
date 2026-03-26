package springVibe.dev.users.amazonProduct.repository;

import springVibe.dev.users.amazonProduct.domain.AmazonCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AmazonCategoryRepository extends JpaRepository<AmazonCategory, Long> {
}

