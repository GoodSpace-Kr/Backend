package goodspace.backend.admin.dto.itemImage;

import goodspace.backend.global.domain.ItemImage;
import lombok.Builder;

@Builder
public record TitleImageInfoResponseDto(
        String imageUrl
) {
    public static TitleImageInfoResponseDto from(ItemImage itemImage) {
        return TitleImageInfoResponseDto.builder()
                .imageUrl(itemImage.getImageUrl())
                .build();
    }
}
