package io.github.XanderGI.dto.internal;

import io.github.XanderGI.service.ResourceStream;

public record DownloadResult(String filename, ResourceStream resourceStream) {
}