package io.github.supernoobchallenge.nasserver.batch.handler.impl;

import io.github.supernoobchallenge.nasserver.batch.handler.BatchJobHandler;
import io.github.supernoobchallenge.nasserver.batch.entity.BatchJobQueue;
import io.github.supernoobchallenge.nasserver.file.core.repository.VirtualDirectoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DirectoryDeleteHandler implements BatchJobHandler {

    private final VirtualDirectoryRepository directoryRepository;

    @Override
    public String getJobType() {
        return "DIRECTORY_DELETE"; // DB에 저장되는 jobType 문자열과 일치해야 함
    }

    @Override
    public void handle(BatchJobQueue job) {
        // 1. 데이터 꺼내기
        Long directoryId = job.getTargetId();
        String reason = (String) job.getJobData().get("reason");

        log.info(">>> [배치 시작] 폴더 삭제 작업. ID: {}, 사유: {}", directoryId, reason);

        // 2. 실제 로직 수행 (가상)
        // directoryRepository.deleteRecursive(directoryId);

        try {
            Thread.sleep(2000); // 오래 걸리는 작업 흉내
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        log.info(">>> [배치 종료] 폴더 삭제 완료.");
    }
}
