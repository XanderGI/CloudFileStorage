package io.github.XanderGI.service;

import io.github.XanderGI.TestcontainersConfiguration;
import io.github.XanderGI.dto.internal.DownloadResult;
import io.github.XanderGI.dto.response.ResourceResponseDto;
import io.github.XanderGI.dto.response.ResourceType;
import io.github.XanderGI.dto.internal.UploadFileItem;
import io.github.XanderGI.exception.ResourceAlreadyExistsException;
import io.github.XanderGI.exception.ResourceNotFoundException;
import io.github.XanderGI.storage.StorageClient;
import io.github.XanderGI.storage.StorageItem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
public class ResourceServiceTest {
    private static final Long FIRST_USER_ID = 1L;
    private static final Long SECOND_USER_ID = 2L;
    private static final String ROOT_PATH = "/";
    private static final String DEFAULT_CONTENT = "test content";

    @Autowired
    private ResourceService resourceService;

    @Autowired
    private StorageClient storageClient;

    @Autowired
    private MinioPathHelper helper;

    @AfterEach
    void clearBucket() {
        List<String> firstUserKeys = storageClient.listObjects(firstUserKey(""), true).stream()
                .map(StorageItem::key)
                .toList();

        List<String> secondUserKeys = storageClient.listObjects(secondUserKey(""), true).stream()
                .map(StorageItem::key)
                .toList();

        storageClient.removeObjects(firstUserKeys);
        storageClient.removeObjects(secondUserKeys);
    }

    @Nested
    class UploadOperation {
        @Test
        void shouldUploadFileToRootDirectory() {
            String firstUserKeyFile = firstUserKey("test.txt");
            UploadFileItem fileItem = createFileItem("test.txt", DEFAULT_CONTENT);

            List<ResourceResponseDto> response = resourceService.uploadResources(FIRST_USER_ID, ROOT_PATH, List.of(fileItem));

            ResourceResponseDto expected = new ResourceResponseDto("/", "test.txt", fileItem.size(), ResourceType.FILE);
            assertThat(response).isNotEmpty();
            assertThat(storageClient.isExist(firstUserKeyFile)).isTrue();
            assertThat(response)
                    .element(0)
                    .usingRecursiveComparison()
                    .isEqualTo(expected);
        }

        @Test
        void shouldCreateParentDirectoryWhenUploadingFileToNestedPath() {
            String firstUserKeySubfolder = firstUserKey("subfolder/");
            String firstUserKeyFile = firstUserKey("subfolder/test.txt");

            UploadFileItem fileItem = createFileItem("subfolder/test.txt", DEFAULT_CONTENT);

            List<ResourceResponseDto> response = resourceService.uploadResources(FIRST_USER_ID, ROOT_PATH, List.of(fileItem));

            assertThat(response).isNotEmpty();
            assertThat(response).hasSize(2);
            assertThat(storageClient.isExist(firstUserKeySubfolder)).isTrue();
            assertThat(storageClient.isExist(firstUserKeyFile)).isTrue();

            assertThat(response).extracting(
                    ResourceResponseDto::path, ResourceResponseDto::name,
                    ResourceResponseDto::size, ResourceResponseDto::type
            ).containsExactlyInAnyOrder(
                    tuple("subfolder/", "test.txt", fileItem.size(), ResourceType.FILE),
                    tuple("/", "subfolder", null, ResourceType.DIRECTORY)
            );
        }

        @Test
        void shouldThrowResourceAlreadyExistsExceptionWhenRequestContainsDuplicateFileNames() {
            String firstUserKeyFile = firstUserKey("test.txt");

            List<UploadFileItem> files = List.of(
                    createFileItem("test.txt", DEFAULT_CONTENT),
                    createFileItem("test.txt", "test content duplicate")
            );

            assertThatThrownBy(() -> resourceService.uploadResources(FIRST_USER_ID, ROOT_PATH, files))
                    .isInstanceOf(ResourceAlreadyExistsException.class);
            assertThat(storageClient.isExist(firstUserKeyFile)).isFalse();
        }

        @Test
        void shouldThrowResourceAlreadyExistsExceptionWhenFileAlreadyExistsInStorage() {
            String firstUserKeyFile = firstUserKey("test.txt");
            UploadFileItem firstFile = createFileItem("test.txt", DEFAULT_CONTENT);
            UploadFileItem secondFile = createFileItem("test.txt", DEFAULT_CONTENT);

            resourceService.uploadResources(FIRST_USER_ID, ROOT_PATH, List.of(firstFile));

            assertThatThrownBy(() -> resourceService.uploadResources(FIRST_USER_ID, ROOT_PATH, List.of(secondFile)))
                    .isInstanceOf(ResourceAlreadyExistsException.class);
            assertThat(storageClient.isExist(firstUserKeyFile)).isTrue();
        }
    }

    @Nested
    class ListDirectoryOperation {

        @Test
        void shouldReturnAllResourcesInDirectory() {
            UploadFileItem firstFileItem = createFileItem("firstFile.txt", "first");
            UploadFileItem secondFileItem = createFileItem("secondFile.md", "second");
            List<UploadFileItem> files = List.of(
                    firstFileItem,
                    secondFileItem
            );
            resourceService.uploadResources(FIRST_USER_ID, ROOT_PATH, files);

            List<ResourceResponseDto> response = resourceService.getDirectoryContent(FIRST_USER_ID, ROOT_PATH);

            assertThat(response).hasSize(2);
            assertThat(response).extracting(
                    ResourceResponseDto::path, ResourceResponseDto::name,
                    ResourceResponseDto::size, ResourceResponseDto::type
            ).containsExactlyInAnyOrder(
                    tuple("/", "firstFile.txt", firstFileItem.size(), ResourceType.FILE),
                    tuple("/", "secondFile.md", secondFileItem.size(), ResourceType.FILE)
            );
        }

        @Test
        void shouldReturnOnlyDirectChildrenWhenListingDirectory() {
            UploadFileItem file = createFileItem("nested/deep/test.txt", "deep content");
            resourceService.uploadResources(FIRST_USER_ID, ROOT_PATH, List.of(file));

            List<ResourceResponseDto> response = resourceService.getDirectoryContent(FIRST_USER_ID, "nested/");

            ResourceResponseDto expected = new ResourceResponseDto("nested/", "deep", null, ResourceType.DIRECTORY);
            assertThat(response).hasSize(1);
            assertThat(response)
                    .element(0)
                    .usingRecursiveComparison()
                    .isEqualTo(expected);
        }

        @Test
        void shouldReturnEmptyListWhenDirectoryIsEmpty() {
            resourceService.createDirectory(FIRST_USER_ID, "emptyDir/");

            assertThat(resourceService.getDirectoryContent(FIRST_USER_ID, "emptyDir/")).isEmpty();
        }

        @Test
        void shouldThrowNotFoundExceptionWhenDirectoryDoesNotExist() {
            assertThatThrownBy(() -> resourceService.getDirectoryContent(FIRST_USER_ID, "notExistFolder/"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    class CreateDirectoryOperation {

        @Test
        void shouldCreateDirectorySuccessfully() {
            String firstUserKeyFolder = firstUserKey("testFolder/");

            ResourceResponseDto expected = new ResourceResponseDto(ROOT_PATH, "testFolder", null, ResourceType.DIRECTORY);
            assertThat(resourceService.createDirectory(FIRST_USER_ID, "testFolder/"))
                    .usingRecursiveComparison()
                    .isEqualTo(expected);
            assertThat(storageClient.isExist(firstUserKeyFolder)).isTrue();
        }

        @Test
        void shouldThrowAlreadyExistsExceptionWhenDirectoryAlreadyExists() {
            resourceService.createDirectory(FIRST_USER_ID, "testFolder/");

            assertThatThrownBy(() -> resourceService.createDirectory(FIRST_USER_ID, "testFolder/"))
                    .isInstanceOf(ResourceAlreadyExistsException.class);
        }

        @Test
        void shouldThrowNotFoundExceptionWhenParentDirectoryDoesNotExist() {
            assertThatThrownBy(() -> resourceService.createDirectory(FIRST_USER_ID, "notExistFolder/nestedFolder/"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    class DeleteOperation {

        @Test
        void shouldDeleteFileSuccessfully() {
            String firstUserKeyFile = firstUserKey("test.txt");
            UploadFileItem file = createFileItem("test.txt", DEFAULT_CONTENT);
            resourceService.uploadResources(FIRST_USER_ID, ROOT_PATH, List.of(file));

            resourceService.deleteResource(FIRST_USER_ID, "test.txt");

            assertThat(storageClient.isExist(firstUserKeyFile)).isFalse();
        }

        @Test
        void shouldDeleteDirectoryAndAllNestedResources() {
            String firstUserKeyFile = firstUserKey("nested/deep/test.txt");
            String firstUserKeyFolder = firstUserKey("nested/");
            UploadFileItem file = createFileItem("nested/deep/test.txt", DEFAULT_CONTENT);
            resourceService.uploadResources(FIRST_USER_ID, ROOT_PATH, List.of(file));

            resourceService.deleteResource(FIRST_USER_ID, "nested/");

            assertThat(storageClient.isExist(firstUserKeyFile)).isFalse();
            assertThat(storageClient.isExist(firstUserKeyFolder)).isFalse();
        }

        @Test
        void shouldThrowNotFoundExceptionWhenResourceDoesNotExist() {
            assertThatThrownBy(() -> resourceService.deleteResource(FIRST_USER_ID, "notExistFolder/"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    class MoveOperation {

        @Test
        void shouldRenameResourceWithinSameDirectory() {
            String firstUserKeyNotRenamedFile = firstUserKey("notRenamed.txt");
            String firstUserKeyRenamedFile = firstUserKey("renamed.txt");
            UploadFileItem file = createFileItem("notRenamed.txt", DEFAULT_CONTENT);
            resourceService.uploadResources(FIRST_USER_ID, ROOT_PATH, List.of(file));

            ResourceResponseDto response = resourceService.moveResource(FIRST_USER_ID, "notRenamed.txt", "renamed.txt");

            ResourceResponseDto expected = new ResourceResponseDto(ROOT_PATH, "renamed.txt", file.size(), ResourceType.FILE);
            assertThat(storageClient.isExist(firstUserKeyNotRenamedFile)).isFalse();
            assertThat(storageClient.isExist(firstUserKeyRenamedFile)).isTrue();
            assertThat(response)
                    .usingRecursiveComparison()
                    .isEqualTo(expected);
        }

        @Test
        void shouldMoveFileToDifferentDirectory() {
            String firstUserKeyFile = firstUserKey("test.txt");
            String firstUserKeyNestedFile = firstUserKey("target/test.txt");
            UploadFileItem file = createFileItem("test.txt", DEFAULT_CONTENT);
            resourceService.createDirectory(FIRST_USER_ID, "target/");
            resourceService.uploadResources(FIRST_USER_ID, ROOT_PATH, List.of(file));

            ResourceResponseDto response = resourceService.moveResource(FIRST_USER_ID, "test.txt", "target/test.txt");

            ResourceResponseDto expected = new ResourceResponseDto("target/", "test.txt", file.size(), ResourceType.FILE);
            assertThat(storageClient.isExist(firstUserKeyFile)).isFalse();
            assertThat(storageClient.isExist(firstUserKeyNestedFile)).isTrue();
            assertThat(response)
                    .usingRecursiveComparison()
                    .isEqualTo(expected);
        }

        @Test
        void shouldMoveDirectoryAndAllNestedResources() {
            String oldFolderKey = firstUserKey("oldFolder/");
            String oldFirstNestedFileKey = firstUserKey("oldFolder/first.txt");
            String oldSecondNestedFileKey = firstUserKey("oldFolder/second.txt");
            String newFirstNestedFileKey = firstUserKey("newFolder/first.txt");
            String newSecondNestedFileKey = firstUserKey("newFolder/second.txt");
            String newFolderKey = firstUserKey("newFolder/");
            UploadFileItem firstFile = createFileItem("first.txt", DEFAULT_CONTENT);
            UploadFileItem secondFile = createFileItem("second.txt", DEFAULT_CONTENT);
            List<UploadFileItem> files = List.of(
                    firstFile,
                    secondFile
            );
            resourceService.uploadResources(FIRST_USER_ID, "oldFolder/", files);

            resourceService.moveResource(FIRST_USER_ID, "oldFolder/", "newFolder/");

            assertThat(storageClient.isExist(oldFolderKey)).isFalse();
            assertThat(storageClient.isExist(oldFirstNestedFileKey)).isFalse();
            assertThat(storageClient.isExist(oldSecondNestedFileKey)).isFalse();
            assertThat(storageClient.isExist(newFolderKey)).isTrue();
            assertThat(storageClient.isExist(newFirstNestedFileKey)).isTrue();
            assertThat(storageClient.isExist(newSecondNestedFileKey)).isTrue();
        }

        @Test
        void shouldThrowIllegalArgumentExceptionWhenMovingFileToDirectoryPath() {
            UploadFileItem file = createFileItem("test.txt", DEFAULT_CONTENT);
            resourceService.uploadResources(FIRST_USER_ID, ROOT_PATH, List.of(file));

            assertThatThrownBy(() -> resourceService.moveResource(FIRST_USER_ID, "test.txt", "folder/"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void shouldThrowAlreadyExistsExceptionWhenTargetPathAlreadyExists() {
            String firstFileKey = firstUserKey("source.txt");
            UploadFileItem firstFile = createFileItem("source.txt", DEFAULT_CONTENT);
            UploadFileItem secondFile = createFileItem("target.txt", DEFAULT_CONTENT);
            List<UploadFileItem> files = List.of(
                    firstFile,
                    secondFile
            );
            resourceService.uploadResources(FIRST_USER_ID, ROOT_PATH, files);

            assertThatThrownBy(() -> resourceService.moveResource(FIRST_USER_ID, "source.txt", "target.txt"))
                    .isInstanceOf(ResourceAlreadyExistsException.class);
            assertThat(storageClient.isExist(firstFileKey)).isTrue();
        }

        @Test
        void shouldThrowNotFoundExceptionWhenSourceResourceDoesNotExist() {
            assertThatThrownBy(() -> resourceService.moveResource(FIRST_USER_ID, "from/", "to/"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        void shouldThrowNotFoundExceptionWhenTargetParentDirectoryDoesNotExist() {
            UploadFileItem file = createFileItem("test.txt", DEFAULT_CONTENT);
            resourceService.uploadResources(FIRST_USER_ID, ROOT_PATH, List.of(file));

            assertThatThrownBy(() -> resourceService.moveResource(FIRST_USER_ID, "test.txt", "notExistFolder/target.txt"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    class SearchOperation {

        @Test
        void shouldFindMatchingResourcesAcrossAllNestedDirectories() {
            UploadFileItem firstFile = createFileItem("test.txt", DEFAULT_CONTENT);
            UploadFileItem secondFile = createFileItem("folder/source.txt", "source content");
            UploadFileItem thirdFile = createFileItem("folder/nested/deep/tooDeep/target.txt", "");
            resourceService.createDirectory(FIRST_USER_ID, "txt/");
            resourceService.uploadResources(FIRST_USER_ID, ROOT_PATH, List.of(firstFile));
            resourceService.uploadResources(FIRST_USER_ID, ROOT_PATH, List.of(secondFile));
            resourceService.uploadResources(FIRST_USER_ID, ROOT_PATH, List.of(thirdFile));

            List<ResourceResponseDto> response = resourceService.search(FIRST_USER_ID, ".txt");

            assertThat(response).hasSize(3);
            assertThat(response).extracting(
                    ResourceResponseDto::path, ResourceResponseDto::name,
                    ResourceResponseDto::size, ResourceResponseDto::type
            ).containsExactlyInAnyOrder(
                    tuple(ROOT_PATH, "test.txt", firstFile.size(), ResourceType.FILE),
                    tuple("folder/", "source.txt", secondFile.size(), ResourceType.FILE),
                    tuple("folder/nested/deep/tooDeep/", "target.txt", thirdFile.size(), ResourceType.FILE)
            );
        }

        @Test
        void shouldNotReturnResourcesBelongingToAnotherUser() {
            UploadFileItem firstUserFile = createFileItem("firstUserFile.txt", DEFAULT_CONTENT);
            UploadFileItem secondUserFile = createFileItem("secondUserFile.txt", DEFAULT_CONTENT);
            resourceService.uploadResources(FIRST_USER_ID, ROOT_PATH, List.of(firstUserFile));
            resourceService.uploadResources(SECOND_USER_ID, ROOT_PATH, List.of(secondUserFile));

            List<ResourceResponseDto> response = resourceService.search(FIRST_USER_ID, "UserFile");

            assertThat(response).hasSize(1)
                    .extracting(ResourceResponseDto::name)
                    .containsExactly("firstUserFile.txt");
        }
    }

    @Nested
    class DownloadOperation {

        @Test
        void shouldDownloadFileWithCorrectContent() throws IOException {
            byte[] expectedBytes = DEFAULT_CONTENT.getBytes(StandardCharsets.UTF_8);
            UploadFileItem file = createFileItem("test.txt", DEFAULT_CONTENT);
            resourceService.uploadResources(FIRST_USER_ID, ROOT_PATH, List.of(file));

            DownloadResult result = resourceService.downloadResource(FIRST_USER_ID, "test.txt");
            byte[] actualBytes = readAllBytes(result);

            assertThat(result.filename()).isEqualTo("test.txt");
            assertThat(actualBytes).isEqualTo(expectedBytes);
        }

        @Test
        void shouldDownloadDirectoryAsZipWithCorrectEntries() throws IOException {
            resourceService.createDirectory(FIRST_USER_ID, "folder/");
            String firstContent = "first content";
            String secondContent = "second content";
            UploadFileItem firstFile = createFileItem("first.txt", firstContent);
            UploadFileItem secondFile = createFileItem("second.txt", secondContent);
            List<UploadFileItem> files = List.of(
                    firstFile,
                    secondFile
            );
            resourceService.uploadResources(FIRST_USER_ID, "folder/", files);

            DownloadResult result = resourceService.downloadResource(FIRST_USER_ID, "folder/");
            byte[] zipBytes = readAllBytes(result);

            try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
                ZipEntry entry;
                Map<String, byte[]> entries = new HashMap<>();

                while ((entry = zipInputStream.getNextEntry()) != null) {
                    byte[] entryBytes = zipInputStream.readAllBytes();
                    entries.put(entry.getName(), entryBytes);
                    zipInputStream.closeEntry();
                }

                assertThat(entries).containsOnlyKeys("first.txt", "second.txt");
                assertThat(entries.get("first.txt"))
                        .isEqualTo(firstContent.getBytes(StandardCharsets.UTF_8));
                assertThat(entries.get("second.txt"))
                        .isEqualTo(secondContent.getBytes(StandardCharsets.UTF_8));
            }

            assertThat(result.filename()).isEqualTo("folder.zip");
        }
    }

    private UploadFileItem createFileItem(String filename, String content) {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        InputStream inputStream = new ByteArrayInputStream(bytes);

        return new UploadFileItem(filename, inputStream, bytes.length);
    }

    private String firstUserKey(String path) {
        return helper.buildMinioKey(FIRST_USER_ID, path);
    }

    private String secondUserKey(String path) {
        return helper.buildMinioKey(SECOND_USER_ID, path);
    }

    private byte[] readAllBytes(DownloadResult result) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        result.resourceStream().writeTo(outputStream);

        return outputStream.toByteArray();
    }
}