package springVibe.etc.velog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;

public interface VelogPostRepository extends JpaRepository<VelogPostEntity, String> {
    Page<VelogPostSummary> findAllByOrderByReleasedAtDesc(Pageable pageable);

    Page<VelogPostSummary> findByTitleContainingOrShortDescriptionContainingOrderByReleasedAtDesc(
        String title,
        String shortDescription,
        Pageable pageable
    );

    List<VelogPostSummary> findByIdIn(Collection<String> ids);
}
