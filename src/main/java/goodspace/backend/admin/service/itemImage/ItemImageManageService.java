package goodspace.backend.admin.service.itemImage;

import goodspace.backend.admin.dto.itemImage.*;

public interface ItemImageManageService {
    TotalItemImageResponseDto findByItem(long itemId);

    ItemImageInfoResponseDto register(ItemImageRegisterRequestDto requestDto);

    TitleImageInfoResponseDto registerTitleImage(ItemImageRegisterRequestDto requestDto);

    TitleImageInfoResponseDto updateTitleImage(TitleImageUpdateRequestDto requestDto);

    void delete(ItemImageDeleteRequestDto requestDto);
}
