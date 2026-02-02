package io.github.supernoobchallenge.nasserver.batch.handler;

import io.github.supernoobchallenge.nasserver.batch.entity.BatchJobQueue;

import java.util.List;

public interface BatchJobHandler {
    /**
     * 핸들러가 처리할 작업 타입 (예: "DIRECTORY_DELETE")
     */
    String getJobType();

    /**
     * 작업 뭉텅이(List)를 한 번에 받아서 처리
     * 구현체 내부에서 'WHERE IN' 쿼리를 쓰거나, 필요하면 반복문을 돔.
     */
    void handle(List<BatchJobQueue> jobs);
}
