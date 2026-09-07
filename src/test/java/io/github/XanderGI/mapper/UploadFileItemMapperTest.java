package io.github.XanderGI.mapper;

import io.github.XanderGI.exception.FileReadException;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UploadFileItemMapperTest {
    private final UploadFileItemMapper mapper = new UploadFileItemMapper();

    @Test
    void shouldThrowFileReadExceptionWhenIOExceptionThrow() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("brokenFile.txt");
        when(file.getInputStream()).thenThrow(new IOException("stream closed"));

        assertThatThrownBy(() -> mapper.toUploadFileItem(file))
                .isInstanceOf(FileReadException.class)
                .hasMessageContaining("brokenFile.txt")
                .hasCauseInstanceOf(IOException.class);
    }
}