package io.github.XanderGI.dto.internal;

import java.io.InputStream;

public record UploadFileItem(String originalFilename, InputStream inputStream, long size) {
}