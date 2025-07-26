package goodspace.backend.client.dto;

import goodspace.backend.global.domain.Item;
import lombok.Builder;

import java.util.List;

@Builder
public record ItemBriefInfoResponseDto(
        Long id,
        String name,
        String landingPageDescription,
        String titleImageUrl,
        List<String> imageUrls
) {
    public static ItemBriefInfoResponseDto from(Item item) {
        return ItemBriefInfoResponseDto.builder()
                .id(item.getId())
                .name(item.getName())
                .landingPageDescription(item.getLandingPageDescription())
                .titleImageUrl(item.getTitleImageUrl())
                .imageUrls(item.getImageUrls())
                .build();
    }
}
