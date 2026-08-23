package io.github.XanderGI.dto;

import java.io.InputStream;

public record UploadFileItem(String originalFilename, InputStream inputStream, long size) {
}