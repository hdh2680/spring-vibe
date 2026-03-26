package springVibe.dev.users.amazonProduct.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Amazon 카테고리(로컬 CSV 기반).
 *
 * id는 CSV의 고정 id를 그대로 사용한다.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "amazon_category")
public class AmazonCategory {

    @Id
    private Long id;

    @Column(name = "category_name", nullable = false, length = 255)
    private String categoryName;

    @Column(name = "category_name_ko", length = 255)
    private String categoryNameKo;

    public AmazonCategory(Long id, String categoryName, String categoryNameKo) {
        this.id = id;
        this.categoryName = categoryName;
        this.categoryNameKo = categoryNameKo;
    }
}

