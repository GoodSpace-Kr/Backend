package goodspace.backend.admin.image;

import goodspace.backend.fixture.ImageFixture;
import goodspace.backend.testUtil.ImageUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;

class ImageManagerTest {
    static final String PREFIX_URL = "test";
    static final String FILE_NAME = "test_file";
    static final String EXTENSION = ".png";
    static final String FULL_URL = PREFIX_URL + "/" + FILE_NAME + EXTENSION;

    static final String IMAGE_1 = ImageFixture.KOTLIN.encodedImage;
    static final String IMAGE_2 = ImageFixture.CHOLOG.encodedImage;

    final ImageUtil imageUtil = new ImageUtil();

    @TempDir
    Path basePath;
    ImageManager imageManager;

    @BeforeEach
    void resetImageManager() {
        imageManager = new ImageManagerImpl(basePath.toString());
    }

    @Nested
    class createImageUrl {
        @Test
        @DisplayName("이미지 파일을 생성한다")
        void createImageFile() throws Exception {
            String imageUrl = imageManager.createImageUrl(PREFIX_URL, FILE_NAME, IMAGE_1);

            boolean isImageCreated = imageUtil.isSameImage(imageUrl, IMAGE_1);
            assertThat(isImageCreated).isTrue();
        }

        @Test
        @DisplayName("지정한 경로에 생성한다")
        void createInDesignatePath() {
            String imageUrl = imageManager.createImageUrl(PREFIX_URL, FILE_NAME, IMAGE_1);

            assertThatCode(() -> imageUtil.getImageFromUrl(imageUrl))
                    .doesNotThrowAnyException();
            assertThat(imageUrl).endsWith(FULL_URL);
        }
    }

    @Nested
    class deleteImage {
        @Test
        @DisplayName("파일을 삭제한다")
        void removeFile() {
            // given
            String imageUrl = imageManager.createImageUrl(PREFIX_URL, FILE_NAME, IMAGE_1);

            // when
            imageManager.deleteImage(imageUrl);

            // then
            assertThatThrownBy(() -> imageUtil.getImageFromUrl(imageUrl))
                    .isInstanceOf(NoSuchFileException.class);
        }
    }

    @Nested
    class updateImage {
        @Test
        @DisplayName("기존 이미지 파일의 내용을 수정한다")
        void updateImageContent() throws Exception {
            // given
            String imageUrl = imageManager.createImageUrl(PREFIX_URL, FILE_NAME, IMAGE_1);

            // when
            imageManager.updateImage(IMAGE_2, imageUrl);

            // then
            boolean isUpdated = imageUtil.isSameImage(imageUrl, IMAGE_2);
            assertThat(isUpdated).isTrue();
        }
    }
}
