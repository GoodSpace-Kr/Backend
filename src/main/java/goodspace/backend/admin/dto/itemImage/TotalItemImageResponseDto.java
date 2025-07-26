package goodspace.backend.admin.dto.itemImage;

import goodspace.backend.global.domain.Item;
import lombok.Builder;

import java.util.List;

@Builder
public record TotalItemImageResponseDto(
        String titleImageUrl,
        List<ItemImageInfoResponseDto> images
) {
    public static TotalItemImageResponseDto from(Item item) {
        List<ItemImageInfoResponseDto> images = item.getItemImages().stream()
                .map(ItemImageInfoResponseDto::from)
                .toList();

        return TotalItemImageResponseDto.builder()
                .titleImageUrl(item.getTitleImageUrl())
                .images(images)
                .build();
    }
}
