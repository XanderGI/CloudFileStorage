package io.github.XanderGI.config.minio;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "minio")
@Validated
public record MinioProperties(
        @NotNull EndpointProperties endpoint,
        @NotBlank String bucket,
        @NotBlank String accessKey,
        @NotBlank String secretKey,
        @NotNull PrefixProperties prefix
) {
    public record EndpointProperties(@NotBlank String url) {
    }

    public record PrefixProperties(@NotBlank String template) {
    }
}