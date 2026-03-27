package springVibe.dev.users.amazonProduct.search;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName = "amazon-products", createIndex = false)
public class AmazonProductDocument {

    @Id
    private String asin;

    // fielddata=true allows sorting on text (dev/local usage only).
    @Field(type = FieldType.Text, fielddata = true)
    private String title;

    @Field(type = FieldType.Keyword)
    private Long categoryId;

    @Field(type = FieldType.Double)
    private Double stars;

    @Field(type = FieldType.Integer)
    private Integer reviews;

    @Field(type = FieldType.Double)
    private Double price;

    @Field(type = FieldType.Boolean)
    private Boolean isBestSeller;

    public String getAsin() {
        return asin;
    }

    public void setAsin(String asin) {
        this.asin = asin;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public Double getStars() {
        return stars;
    }

    public void setStars(Double stars) {
        this.stars = stars;
    }

    public Integer getReviews() {
        return reviews;
    }

    public void setReviews(Integer reviews) {
        this.reviews = reviews;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Boolean getIsBestSeller() {
        return isBestSeller;
    }

    public void setIsBestSeller(Boolean isBestSeller) {
        this.isBestSeller = isBestSeller;
    }
}
