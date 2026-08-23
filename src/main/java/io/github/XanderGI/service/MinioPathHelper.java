package io.github.XanderGI.service;

import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class MinioPathHelper {
    private static final String KEY_DELIMITER = "/";
    private static final String ROOT_PATH = "/";

    @Value("${minio.prefix.template}")
    private String templatePrefix;

    public String buildRootPrefix(Long userId) {
        return templatePrefix.formatted(userId);
    }

    public boolean isFolder(String pathOrKey) {
        if (pathOrKey == null || pathOrKey.isBlank()) {
            throw new IllegalArgumentException("path must be not null or empty");
        }

        return pathOrKey.endsWith(KEY_DELIMITER);
    }

    public String buildMinioKey(Long userId, String path) {
        if (path == null || path.isBlank()) {
            return buildRootPrefix(userId);
        }

        String cleanPath = normalizeAndSanitizePath(path);

        return buildRootPrefix(userId).concat(cleanPath);
    }

    public String getName(String path) {
        String normalizePath = normalizeForParsing(path);
        return FilenameUtils.getName(normalizePath);
    }

    public String getContextPath(String path) {
        String normalizePath = normalizeForParsing(path);
        String contextPath = FilenameUtils.getFullPath(normalizePath);

        if (contextPath.isBlank()) {
            return ROOT_PATH;
        }

        return contextPath;
    }

    public String getContextPathFromKey(Long userId, String key) {
        String extractPath = extractFilePath(userId, key);

        return getContextPath(extractPath);
    }

    public String buildFilePath(String directoryPath, String originalFilename) {
        if (directoryPath == null || directoryPath.isBlank()) {
            throw new IllegalArgumentException("path must be not null or empty");
        }

        String contextPath = directoryPath.concat(originalFilename);

        return normalizeAndSanitizePath(contextPath);
    }

    public List<String> splitContextPath(String contextPath) {
        Path path = Path.of(contextPath);
        List<String> accumulated = new ArrayList<>();

        while (path != null && path.getFileName() != null) {
            String segment = path.toString().replace("\\", KEY_DELIMITER);
            accumulated.add(0, segment.concat(KEY_DELIMITER));
            path = path.getParent();
        }

        return accumulated;
    }

    private String extractFilePath(Long userId, String path) {
        if (path == null || path.isBlank()) {
            return ROOT_PATH;
        }

        String userPrefix = buildRootPrefix(userId);
        String structural = path.replaceAll("/+", "/");

        if (structural.startsWith(KEY_DELIMITER)) {
            structural = structural.substring(KEY_DELIMITER.length());
        }

        if (structural.equals(userPrefix)) {
            return ROOT_PATH;
        }

        if (!structural.startsWith(userPrefix)) {
            throw new IllegalArgumentException("Key does not belong to user: " + path);
        }

        String suffix = structural.substring(userPrefix.length());

        return normalizeAndSanitizePath(suffix);
    }

    private String normalizeAndSanitizePath(String path) {
        String normalizePath = StringUtils.cleanPath(path);

        if (normalizePath.isEmpty() ||
                Arrays.asList(normalizePath.split(KEY_DELIMITER)).contains("..")) {
            throw new IllegalArgumentException("Invalid path segments: " + path);
        }

        String sanitized = normalizePath.replaceAll("/+", KEY_DELIMITER);

        return sanitized.replaceAll("^/", "");
    }

    private String normalizeForParsing(String path) {
        String normalizePath = normalizeAndSanitizePath(path);

        if (isFolder(normalizePath)) {
            normalizePath = StringUtils.trimTrailingCharacter(normalizePath, '/');
        }

        return normalizePath;
    }
}