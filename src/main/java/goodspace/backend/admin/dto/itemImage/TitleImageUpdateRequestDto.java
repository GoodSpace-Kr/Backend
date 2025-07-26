package goodspace.backend.admin.dto.itemImage;

import lombok.Builder;

@Builder
public record TitleImageUpdateRequestDto(
        Long itemId,
        String encodedImage
) {
}
