package io.github.XanderGI.dto;

import io.github.XanderGI.service.ResourceStream;

public record DownloadResult(String filename, ResourceStream resourceStream) {
}