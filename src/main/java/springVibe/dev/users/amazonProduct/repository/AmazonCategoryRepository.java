package springVibe.dev.users.amazonProduct.repository;

import springVibe.dev.users.amazonProduct.domain.AmazonCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface AmazonCategoryRepository extends JpaRepository<AmazonCategory, Long> {
    List<AmazonCategory> findByIdIn(Collection<Long> ids);
}
