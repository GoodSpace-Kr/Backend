package goodspace.backend.global.domain;

import goodspace.backend.client.domain.Client;
import goodspace.backend.client.domain.RegisterStatus;
import goodspace.backend.order.domain.Order;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class Item extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private Integer price;
    private String shortDescription;
    private String landingPageDescription;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private RegisterStatus status;

    @ManyToOne
    @JoinColumn(name = "client_id")
    @Setter
    private Client client;

    @ManyToOne
    @JoinColumn(name = "order_id")
    @Setter
    private Order order;

    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private final List<ItemImage> itemImages = new ArrayList<>();

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    private ItemImage titleImage;

    public List<String> getImageUrls() {
        return itemImages.stream()
                .map(ItemImage::getImageUrl)
                .toList();
    }

    public String getTitleImageUrl() {
        return titleImage.getImageUrl();
    }

    public boolean isPublic() {
        return status == RegisterStatus.PUBLIC;
    }

    public void update(
            String name,
            Integer price,
            String shortDescription,
            String landingPageDescription,
            RegisterStatus status
    ) {
        this.name = name;
        this.price = price;
        this.shortDescription = shortDescription;
        this.landingPageDescription = landingPageDescription;
        this.status = status;
    }

    /**
     * Item - ItemImage 연관관계 편의 메서드
     */
    public void addItemImages(List<ItemImage> itemImages) {
        for (ItemImage itemImage : itemImages) {
            this.itemImages.add(itemImage);
            itemImage.setItem(this);
        }
    }

    public void setTitleImage(ItemImage itemImage) {
        this.titleImage = itemImage;
        itemImage.setItem(this);
    }

    public void removeTitleImage() {
        titleImage.setItem(null);
        titleImage = null;
    }

    public void removeItemImage(ItemImage itemImage) {
        itemImages.remove(itemImage);
        itemImage.setItem(null);
    }

    public void removeEveryImages() {
        itemImages.clear();
    }
}
