package springVibe.dev.users.amazonProduct.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import springVibe.dev.users.amazonProduct.domain.AmazonProduct;

import java.util.Collection;
import java.util.List;

public interface AmazonProductRepository extends JpaRepository<AmazonProduct, String> {

    // Slice avoids COUNT(*) on each page. Use deterministic ordering for stable paging.
    Slice<AmazonProduct> findAllByOrderByAsinAsc(Pageable pageable);

    interface AmazonProductCard {
        String getAsin();
        String getTitle();
        String getProductNameKo();
        String getImgUrl();
        String getProductUrl();
        Double getStars();
        Integer getReviews();
        Double getPrice();
        Double getListPrice();
        Boolean getIsBestSeller();
        Integer getBoughtInLastMonth();
        Long getCategoryId();
    }

    @Query("""
        select
          p.asin as asin,
          p.title as title,
          p.productNameKo as productNameKo,
          p.imgUrl as imgUrl,
          p.productUrl as productUrl,
          p.stars as stars,
          p.reviews as reviews,
          p.price as price,
          p.listPrice as listPrice,
          p.categoryId as categoryId,
          p.isBestSeller as isBestSeller,
          p.boughtInLastMonth as boughtInLastMonth
        from AmazonProduct p
        where (:categoryId is null or p.categoryId = :categoryId)
        """)
    Page<AmazonProductCard> listCards(
        @Param("categoryId") Long categoryId,
        Pageable pageable
    );

    @Query("""
        select
          p.asin as asin,
          p.title as title,
          p.productNameKo as productNameKo,
          p.imgUrl as imgUrl,
          p.productUrl as productUrl,
          p.stars as stars,
          p.reviews as reviews,
          p.price as price,
          p.listPrice as listPrice,
          p.categoryId as categoryId,
          p.isBestSeller as isBestSeller,
          p.boughtInLastMonth as boughtInLastMonth
        from AmazonProduct p
        where (:categoryId is null or p.categoryId = :categoryId)
          and (
            lower(p.title) like lower(concat('%', :q, '%')) or
            lower(coalesce(p.productNameKo, '')) like lower(concat('%', :q, '%'))
          )
        """)
    Page<AmazonProductCard> searchCards(
        @Param("q") String q,
        @Param("categoryId") Long categoryId,
        Pageable pageable
    );

    @Query("""
        select
          p.asin as asin,
          p.title as title,
          p.productNameKo as productNameKo,
          p.imgUrl as imgUrl,
          p.productUrl as productUrl,
          p.stars as stars,
          p.reviews as reviews,
          p.price as price,
          p.listPrice as listPrice,
          p.categoryId as categoryId,
          p.isBestSeller as isBestSeller,
          p.boughtInLastMonth as boughtInLastMonth
        from AmazonProduct p
        where p.asin in :asins
        """)
    List<AmazonProductCard> findCardsByAsinIn(@Param("asins") Collection<String> asins);
}
