package springVibe.dev.users.amazonProduct.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import springVibe.dev.common.jpa.BooleanTrueFalseConverter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "amazon_product")
public class AmazonProduct {

    @Id
    @Column(name = "asin", nullable = false, length = 32)
    private String asin;

    @Column(name = "title", length = 2000)
    private String title;

    @Column(name = "product_name_ko", length = 2000)
    private String productNameKo;

    @Column(name = "img_url", length = 2000)
    private String imgUrl;

    @Column(name = "product_url", length = 2000)
    private String productUrl;

    @Column(name = "stars")
    private Double stars;

    @Column(name = "reviews")
    private Integer reviews;

    @Column(name = "price")
    private Double price;

    @Column(name = "list_price")
    private Double listPrice;

    @Column(name = "category_id")
    private Long categoryId;

    @Convert(converter = BooleanTrueFalseConverter.class)
    @Column(name = "is_best_seller", length = 5)
    private Boolean isBestSeller;

    @Column(name = "bought_in_last_month")
    private Integer boughtInLastMonth;
}
