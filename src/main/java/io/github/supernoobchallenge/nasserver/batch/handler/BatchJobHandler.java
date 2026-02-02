package io.github.supernoobchallenge.nasserver.batch.handler;

import io.github.supernoobchallenge.nasserver.batch.entity.BatchJobQueue;

public interface BatchJobHandler {
    /**
     * 이 핸들러가 처리할 작업 타입 (예: "DIRECTORY_DELETE")
     */
    String getJobType();

    /**
     * 실제 비즈니스 로직 수행
     */
    void handle(BatchJobQueue job);
}
