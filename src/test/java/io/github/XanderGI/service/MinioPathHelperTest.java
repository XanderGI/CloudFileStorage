package io.github.XanderGI.service;

import io.github.XanderGI.config.minio.MinioProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

public class MinioPathHelperTest {
    private static final Long USER_ID = 42L;
    private static final String EXPECTED_ROOT = "user-42-files/";
    private static final String ROOT_PATH = "/";

    private MinioPathHelper helper;

    @BeforeEach
    void setUp() {
        MinioProperties.PrefixProperties prefixProperties = new MinioProperties.PrefixProperties("user-%s-files/");
        helper = new MinioPathHelper(minioProperties(prefixProperties));
    }

    @Test
    void shouldReturnCorrectRootPrefixWhenGetUserId() {
        assertThat(helper.buildRootPrefix(USER_ID)).isEqualTo(EXPECTED_ROOT);
    }

    @ParameterizedTest
    @CsvSource({
            "folder1/folder2/file.txt, false",
            "folder1/folder2/, true",
            "/, true"
    })
    void shouldIdentifyIfPathIsFolder(String path, boolean isFolder) {
        assertThat(helper.isFolder(path)).isEqualTo(isFolder);
    }

    @ParameterizedTest
    @NullSource
    @EmptySource
    @ValueSource(strings = {" "})
    void shouldThrowExceptionWhenPathIsInvalid(String invalidPath) {
        assertThatThrownBy(() -> helper.isFolder(invalidPath))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @NullSource
    @EmptySource
    @ValueSource(strings = {" "})
    void shouldReturnRootPrefixWhenPathIsEmpty(String invalidPath) {
        assertThat(helper.buildMinioKey(USER_ID, invalidPath)).isEqualTo(EXPECTED_ROOT);
    }

    @ParameterizedTest
    @CsvSource({
            "folder1/folder2/file.txt, folder1/folder2/file.txt",
            "/folder2/file.txt, folder2/file.txt",
            "///folder/file.md, folder/file.md",
            "/folder/../file.md, file.md",
            "./folder/file.md, folder/file.md",
            "/folder1/folder2/, folder1/folder2/",
            "./files.md, files.md"

    })
    void shouldReturnNormalizeAndConcatPath(String rawPath, String expectedPath) {
        assertThat(helper.buildMinioKey(USER_ID, rawPath)).isEqualTo(EXPECTED_ROOT.concat(expectedPath));
    }

    @ParameterizedTest
    @NullSource
    @EmptySource
    @ValueSource(strings = {" "})
    void shouldReturnRootPathWhenPathIsEmpty(String invalidPath) {
        assertThat(helper.extractFilePath(USER_ID, invalidPath)).isEqualTo(ROOT_PATH);
    }

    @Test
    void shouldReturnRootPathWhenKeyMatchesRootPrefix() {
        assertThat(helper.extractFilePath(USER_ID, EXPECTED_ROOT)).isEqualTo(ROOT_PATH);
    }

    @ParameterizedTest
    @CsvSource({
            "user-42-files/folder1/text.txt, folder1/text.txt",
            "user-42-files/folder1/folder2/, folder1/folder2/",
            "user-42-files//folder1/folder2/data.db, folder1/folder2/data.db",
            "/user-42-files//, /"
    })
    void shouldExtractPathCorrectly(String rawPath, String expectedPath) {
        assertThat(helper.extractFilePath(USER_ID, rawPath)).isEqualTo(expectedPath);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/user-666-files/",
            "/",
            "//user-666-files//folder1/"
    })
    void shouldThrowExceptionWhenKeyDoesNotBelongToUser(String rawPath) {
        assertThatThrownBy(() -> helper.extractFilePath(USER_ID, rawPath))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Key does not belong to user");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "../file.md",
            "folder1/../../secret.txt",
            "../../file.md",
            "folder1/..", // этот тест!
            "../"
    })
    void shouldThrowExceptionWhenPathContainsPathTraversal(String rawPath) {
        assertThatThrownBy(() -> helper.buildMinioKey(USER_ID, rawPath))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid path segments:");

        String unsafeKey = helper.buildRootPrefix(USER_ID).concat(rawPath);

        assertThatThrownBy(() -> helper.extractFilePath(USER_ID, unsafeKey))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid path segments:");
    }

    @ParameterizedTest
    @CsvSource({
            "folder1/folder2/file.txt, folder1/folder2/file.txt",
            "/folder2/file.txt, folder2/file.txt",
            "///folder/file.md, folder/file.md",
            "/folder/../file.md, file.md",
            "./folder/file.md, folder/file.md",
            "/folder1/folder2/, folder1/folder2/",
            "./files.md, files.md"
    })
    void shouldGetOriginalPathAfterBuildingAndExtractingKey(String rawPath, String expectedPath) {
        String minioKey = helper.buildMinioKey(USER_ID, rawPath);
        String extractedPath = helper.extractFilePath(USER_ID, minioKey);

        assertThat(extractedPath).isEqualTo(expectedPath);
    }

    @ParameterizedTest
    @CsvSource({
            "folder1/text.txt, text.txt",
            "/folder1/folder2/, folder2",
            "/folder1/folder2/folder3/, folder3",
            "folder1/, folder1"
    })
    void shouldReturnOnlyNameAfterGetMethodCall(String path, String expectedPath) {
        assertThat(helper.getName(path)).isEqualTo(expectedPath);
    }

    @ParameterizedTest
    @CsvSource({
            "/folder1/text.txt, folder1/",
            "/folder1/folder2/, folder1/",
            "folder3/, /",

    })
    void shouldReturnContextPathWhenCallGetContextPathMethod(String path, String expectedPath) {
        assertThat(helper.getContextPath(path)).isEqualTo(expectedPath);
    }

    private MinioProperties minioProperties(MinioProperties.PrefixProperties prefix) {
        return new MinioProperties(null, null, null, null, prefix);
    }
}