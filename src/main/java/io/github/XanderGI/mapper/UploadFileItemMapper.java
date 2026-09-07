package io.github.XanderGI.mapper;

import io.github.XanderGI.dto.internal.UploadFileItem;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Component
public class UploadFileItemMapper {

    public List<UploadFileItem> toUploadFileItems(List<MultipartFile> files) {
        return files.stream()
                .map(this::toUploadFileItem)
                .toList();
    }

    public UploadFileItem toUploadFileItem(MultipartFile file) {
        try {
            return new UploadFileItem(
                    file.getOriginalFilename(),
                    file.getInputStream(),
                    file.getSize()
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}