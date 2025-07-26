package goodspace.backend.admin.service.itemImage;

import goodspace.backend.admin.dto.itemImage.*;
import goodspace.backend.admin.image.ImageManager;
import goodspace.backend.global.domain.Item;
import goodspace.backend.global.domain.ItemImage;
import goodspace.backend.global.repository.ItemImageRepository;
import goodspace.backend.global.repository.ItemRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class ItemImageManageServiceImpl implements ItemImageManageService {
    private static final Supplier<EntityNotFoundException> ITEM_NOT_FOUND = () -> new EntityNotFoundException("상품을 찾을 수 없습니다.");
    private static final Supplier<EntityNotFoundException> ITEM_IMAGE_NOT_FOUND = () -> new EntityNotFoundException("상품 이미지를 찾을 수 없습니다.");

    private static final String TITLE_IMAGE = "title";

    private final ImageManager imageManager;

    private final ItemRepository itemRepository;
    private final ItemImageRepository itemImageRepository;

    @Override
    @Transactional(readOnly = true)
    public TotalItemImageResponseDto findByItem(long itemId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(ITEM_NOT_FOUND);

        return TotalItemImageResponseDto.from(item);
    }

    @Override
    @Transactional
    public ItemImageInfoResponseDto register(ItemImageRegisterRequestDto requestDto) {
        Item item = itemRepository.findById(requestDto.itemId())
                .orElseThrow(ITEM_NOT_FOUND);

        ItemImage itemImage = createEmptyItemImage(item);
        itemImageRepository.save(itemImage);

        String urlPrefix = getImageUrlPrefix(
                requestDto.clientId(),
                requestDto.itemId()
        );
        String imageUrl = imageManager.createImageUrl(urlPrefix, itemImage.getId(), requestDto.encodedImage());

        itemImage.setImageUrl(imageUrl);

        return ItemImageInfoResponseDto.from(itemImage);
    }

    @Override
    @Transactional
    public TitleImageInfoResponseDto registerTitleImage(ItemImageRegisterRequestDto requestDto) {
        Item item = itemRepository.findById(requestDto.itemId())
                .orElseThrow(ITEM_NOT_FOUND);

        String urlPrefix = getTitleUrlPrefix(requestDto.clientId());
        String imageUrl = imageManager.createImageUrl(urlPrefix, TITLE_IMAGE, requestDto.encodedImage());

        ItemImage itemImage = ItemImage.from(imageUrl);
        item.setTitleImage(itemImage);

        return TitleImageInfoResponseDto.from(itemImage);
    }

    @Override
    @Transactional(readOnly = true)
    public TitleImageInfoResponseDto updateTitleImage(TitleImageUpdateRequestDto requestDto) {
        Item item = itemRepository.findById(requestDto.itemId())
                .orElseThrow(ITEM_NOT_FOUND);

        imageManager.updateImage(requestDto.encodedImage(), item.getTitleImageUrl());

        return TitleImageInfoResponseDto.from(item.getTitleImage());
    }

    @Override
    @Transactional
    public void delete(ItemImageDeleteRequestDto requestDto) {
        Item item = itemRepository.findById(requestDto.itemId())
                .orElseThrow(ITEM_NOT_FOUND);
        ItemImage itemImage = findItemImageFromItem(item, requestDto.itemImageId());

        imageManager.deleteImage(itemImage.getImageUrl());
        item.removeItemImage(itemImage);
    }

    private ItemImage createEmptyItemImage(Item item) {
        return ItemImage.builder()
                .item(item)
                .build();
    }

    private String getTitleUrlPrefix(long clientId) {
        return clientId + "/item/";
    }

    private String getImageUrlPrefix(long clientId, long itemId) {
        return clientId + "/item/" + itemId + "/";
    }

    private ItemImage findItemImageFromItem(Item item, long itemImageId) {
        return item.getItemImages().stream()
                .filter(itemImage -> itemImage.getId().equals(itemImageId))
                .findAny()
                .orElseThrow(ITEM_IMAGE_NOT_FOUND);
    }
}
