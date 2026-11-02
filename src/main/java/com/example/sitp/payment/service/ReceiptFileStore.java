package com.example.sitp.payment.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

@Component
public class ReceiptFileStore {

    private final Path receiptsDir;

    public ReceiptFileStore(@Value("${app.receipts.dir:./receipts}") String receiptsDir) {
        this.receiptsDir = Path.of(receiptsDir).toAbsolutePath().normalize();
    }

    public String save(MultipartFile receipt) {
        try {
            Files.createDirectories(receiptsDir);
            String original = Optional.ofNullable(receipt.getOriginalFilename()).orElse("receipt");
            String safeName = original.replaceAll("[^a-zA-Z0-9._-]", "_");
            String storedName = UUID.randomUUID() + "-" + safeName;
            Path target = receiptsDir.resolve(storedName);
            receipt.transferTo(target.toFile());
            return "receipts/" + storedName;
        } catch (IOException e) {
            throw new IllegalStateException("The receipt could not be stored.", e);
        }
    }

    public Resource resolve(String reference) {
        Path file = receiptsDir.resolve(Path.of(reference).getFileName().toString());
        if (!Files.exists(file)) {
            throw new IllegalStateException("The receipt file is gone from storage: " + reference);
        }
        return new FileSystemResource(file);
    }

    public String probeContentType(String reference) {
        try {
            return Optional.ofNullable(
                            Files.probeContentType(receiptsDir.resolve(Path.of(reference).getFileName().toString())))
                    .orElse("application/octet-stream");
        } catch (IOException e) {
            return "application/octet-stream";
        }
    }
}
