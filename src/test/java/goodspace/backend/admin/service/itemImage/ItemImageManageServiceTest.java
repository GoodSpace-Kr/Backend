package goodspace.backend.admin.service.itemImage;

import goodspace.backend.admin.dto.itemImage.*;
import goodspace.backend.admin.image.ImageManager;
import goodspace.backend.admin.image.ImageManagerImpl;
import goodspace.backend.client.domain.Client;
import goodspace.backend.global.domain.Item;
import goodspace.backend.global.domain.ItemImage;
import goodspace.backend.fixture.ClientFixture;
import goodspace.backend.fixture.ImageFixture;
import goodspace.backend.fixture.ItemFixture;
import goodspace.backend.client.repository.ClientRepository;
import goodspace.backend.global.repository.ItemImageRepository;
import goodspace.backend.global.repository.ItemRepository;
import goodspace.backend.testUtil.ImageUtil;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
class ItemImageManageServiceTest {
    final static Supplier<EntityNotFoundException> DTO_NOT_FOUND = () -> new EntityNotFoundException("DTO가 조회되지 않습니다.");
    final static Supplier<EntityNotFoundException> ITEM_IMAGE_NOT_FOUND = () -> new EntityNotFoundException("Item Image가 조회되지 않습니다.");
    final static String TITLE_IMAGE_FILE_NAME = "title";
    final static String DEFAULT_TITLE_IMAGE = ImageFixture.JAVA.encodedImage;
    final static String DEFAULT_IMAGE_A = ImageFixture.GDG.encodedImage;
    final static String DEFAULT_IMAGE_B = ImageFixture.KOTLIN.encodedImage;
    final static String NEW_IMAGE = ImageFixture.GOOD_SPACE.encodedImage;

    @Autowired
    ItemRepository itemRepository;
    @Autowired
    ItemImageRepository itemImageRepository;
    @Autowired
    ClientRepository clientRepository;
    ItemImageManageService itemImageManageService;

    @Autowired
    ImageUtil imageUtil;

    @TempDir
    Path basePath;
    ImageManager imageManager;

    Client client;
    Item item;
    ItemImage titleImage;
    ItemImage itemImageA;
    ItemImage itemImageB;
    List<ItemImage> existItemImages;

    @BeforeEach
    void resetEntities() {
        imageManager = new ImageManagerImpl(basePath.toString());
        itemImageManageService = new ItemImageManageServiceImpl(imageManager, itemRepository, itemImageRepository);

        client = clientRepository.save(ClientFixture.CREATOR.getInstance());
        item = itemRepository.save(ItemFixture.PUBLIC_A.getInstance());

        titleImage = ItemImage.from(imageManager.createImageUrl(item.getId(), TITLE_IMAGE_FILE_NAME, DEFAULT_TITLE_IMAGE));
        item.setTitleImage(titleImage);

        itemImageA = itemImageRepository.save(ItemImage.getEmptyInstance());
        itemImageB = itemImageRepository.save(ItemImage.getEmptyInstance());
        itemImageA.setImageUrl(imageManager.createImageUrl(item.getId(), itemImageA.getId(), DEFAULT_IMAGE_A));
        itemImageB.setImageUrl(imageManager.createImageUrl(item.getId(), itemImageB.getId(), DEFAULT_IMAGE_B));

        client.addItem(item);
        item.addItemImages(List.of(itemImageA, itemImageB));
        existItemImages = List.of(itemImageA, itemImageB);
    }

    @Nested
    class findByItem {
        @Test
        @DisplayName("특정 상품의 일반 이미지를 모두 반환한다")
        void returnEveryImageOfItem() {
            TotalItemImageResponseDto responseDto = itemImageManageService.findByItem(item.getId());

            assertThat(responseDto.images().size()).isEqualTo(existItemImages.size());

            for (ItemImage existItemImage : existItemImages) {
                ItemImageInfoResponseDto itemImageDto = findDtoById(existItemImage.getId(), responseDto.images());
                assertThat(isEqual(existItemImage, itemImageDto)).isTrue();
            }
        }

        @Test
        @DisplayName("특정 상품의 타이틀 이미지를 반환한다")
        void returnTitleImageOfItem() throws IOException {
            TotalItemImageResponseDto responseDto = itemImageManageService.findByItem(item.getId());

            String titleImageUrl = responseDto.titleImageUrl();

            assertThat(item.getTitleImageUrl()).isEqualTo(titleImageUrl);
            assertThat(imageUtil.isSameImage(titleImageUrl, DEFAULT_TITLE_IMAGE)).isTrue();
        }
    }

    @Nested
    class register {
        @Test
        @DisplayName("상품에 새로운 이미지 URL을 등록한다")
        void registerNewImageInItem() throws IOException {
            // given
            ItemImageRegisterRequestDto requestDto = ItemImageRegisterRequestDto.builder()
                    .clientId(client.getId())
                    .itemId(item.getId())
                    .encodedImage(NEW_IMAGE)
                    .build();

            // when
            ItemImageInfoResponseDto registeredItemImageDto = itemImageManageService.register(requestDto);

            // then
            ItemImage itemImage = itemImageRepository.findById(registeredItemImageDto.id())
                    .orElseThrow(ITEM_IMAGE_NOT_FOUND);

            assertThat(itemImage.getItem()).isEqualTo(item);
            assertThat(imageUtil.isSameImage(itemImage.getImageUrl(), NEW_IMAGE)).isTrue();
        }
    }

    @Nested
    class registerTitleImage {
        @Test
        @DisplayName("상품에 새로운 타이틀 이미지 URL을 등록한다")
        void registerNewTitleImageUrl() throws IOException {
            // given
            ItemImageRegisterRequestDto requestDto = ItemImageRegisterRequestDto.builder()
                    .clientId(client.getId())
                    .itemId(item.getId())
                    .encodedImage(NEW_IMAGE)
                    .build();

            // when
            itemImageManageService.registerTitleImage(requestDto);

            // then
            ItemImage itemImage = item.getTitleImage();

            assertThat(itemImage.getItem()).isEqualTo(item);
            assertThat(imageUtil.isSameImage(itemImage.getImageUrl(), NEW_IMAGE)).isTrue();
        }
    }

    @Nested
    class updateTitleImage {
        @Test
        @DisplayName("상품의 타이틀 이미지를 수정한다")
        void updateTitleImageOfItem() throws IOException {
            // given
            TitleImageUpdateRequestDto requestDto = TitleImageUpdateRequestDto.builder()
                    .itemId(item.getId())
                    .encodedImage(NEW_IMAGE)
                    .build();

            // when
            itemImageManageService.updateTitleImage(requestDto);

            // then
            ItemImage itemImage = item.getTitleImage();

            assertThat(itemImage.getItem()).isEqualTo(item);
            assertThat(imageUtil.isSameImage(itemImage.getImageUrl(), NEW_IMAGE)).isTrue();
        }
    }

    @Nested
    class delete {
        @Test
        @DisplayName("상품의 특정 이미지를 삭제한다")
        void removeImageOfItem() {
            // given
            ItemImageDeleteRequestDto requestDto = ItemImageDeleteRequestDto.builder()
                    .itemId(item.getId())
                    .itemImageId(itemImageA.getId())
                    .build();

            // when
            itemImageManageService.delete(requestDto);

            // then
            boolean imageDeleted = item.getItemImages().stream()
                    .noneMatch(itemImage -> itemImage.getId().equals(itemImageA.getId()));
            assertThat(imageDeleted).isTrue();
        }

        @Test
        @DisplayName("이미지 URL에 연결되어 있던 이미지 파일을 삭제한다")
        void removeImageFileConnectedUrl() {
            // given
            ItemImageDeleteRequestDto requestDto = ItemImageDeleteRequestDto.builder()
                    .itemId(item.getId())
                    .itemImageId(itemImageA.getId())
                    .build();
            assertThatCode(() -> imageUtil.getImageFromUrl(itemImageA.getImageUrl()))
                    .doesNotThrowAnyException();

            // when
            itemImageManageService.delete(requestDto);

            // then
            assertThatThrownBy(() -> imageUtil.getImageFromUrl(itemImageA.getImageUrl()))
                    .isInstanceOf(NoSuchFileException.class);
        }
    }

    private boolean isEqual(ItemImage itemImage, ItemImageInfoResponseDto dto) {
        return itemImage.getId().equals(dto.id()) &&
                itemImage.getImageUrl().equals(dto.imageUrl());
    }

    private ItemImageInfoResponseDto findDtoById(long imageId, List<ItemImageInfoResponseDto> dtos) {
        return dtos.stream()
                .filter(dto -> dto.id().equals(imageId))
                .findAny()
                .orElseThrow(DTO_NOT_FOUND);
    }
}
