package io.github.supernoobchallenge.nasserver.batch.service;

import io.github.supernoobchallenge.nasserver.file.core.entity.VirtualDirectory;
import io.github.supernoobchallenge.nasserver.file.core.repository.VirtualDirectoryRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DirectoryService {
    private final VirtualDirectoryRepository directoryRepository;
    private final BatchJobService batchJobService; // [주입] 배치 등록 서비스

    /**
     * 폴더 삭제 요청 (사용자가 호출)
     */
    @Transactional
    public void deleteDirectory(Long directoryId, Long userId) {

        // 1. 해당 폴더 존재 확인
        VirtualDirectory dir = directoryRepository.findById(directoryId)
                .orElseThrow(() -> new IllegalArgumentException("폴더가 없습니다."));

        // 2. 권한 체크 (생략)

        // 3. [핵심] 배치 작업 등록 (직접 지우지 않음!)
        // "나중에 스케줄러가 이 폴더랑 하위 폴더 싹 다 지워주세요"라고 쪽지 남김

        Map<String, Object> jobParams = new HashMap<>();
        jobParams.put("reason", "USER_REQUEST");
        jobParams.put("requestedAt", System.currentTimeMillis());

        batchJobService.registerJob(
                "DIRECTORY_DELETE",     // Job Type (핸들러와 일치해야 함)
                "virtual_directories",  // Target Table
                directoryId,            // Target ID
                jobParams,              // Params (JSON)
                userId                  // User ID
        );

        // 4. (선택) 사용자 경험을 위해 현재 폴더만 일단 '숨김' 처리할 수도 있음
        // dir.setVisible(false);
    }
}
