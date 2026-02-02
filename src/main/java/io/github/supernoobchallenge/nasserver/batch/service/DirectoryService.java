package io.github.supernoobchallenge.nasserver.batch.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class DirectoryService {
    private final BatchJobService batchJobService; // 주입

    public void deleteDirectory(Long dirId) {
        // 1. 디렉터리 Soft Delete (생략)

        // 2. 배치 등록 (이제 복잡하게 Builder 안 써도 됨)
        batchJobService.registerJob(
                "DIRECTORY_DELETE",
                "virtual_directories",
                dirId,
                Map.of("reason", "User Requested")
        );
    }
}
