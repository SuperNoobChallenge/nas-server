package io.github.supernoobchallenge.nasserver.file.core.integration;

import io.github.supernoobchallenge.nasserver.batch.entity.BatchJobQueue;
import io.github.supernoobchallenge.nasserver.batch.repository.BatchJobQueueRepository;
import io.github.supernoobchallenge.nasserver.batch.scheduler.BatchJobWorker;
import io.github.supernoobchallenge.nasserver.file.core.entity.VirtualDirectory;
import io.github.supernoobchallenge.nasserver.file.core.repository.VirtualDirectoryRepository;
import io.github.supernoobchallenge.nasserver.user.service.UserService;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@SpringBootTest
@Transactional
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class VirtualDirectoryControllerIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserService userService;

    @Autowired
    private VirtualDirectoryRepository virtualDirectoryRepository;

    @Autowired
    private BatchJobQueueRepository batchJobQueueRepository;

    @Autowired
    private BatchJobWorker batchJobWorker;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        mockMvc = webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("가상 디렉터리를 생성하고 부모 기준으로 하위 목록을 조회할 수 있다")
    void createDirectory_AndListChildren_Succeeds() throws Exception {
        userService.register("dir_user_1", "pass-1234", "dir_user_1@test.com");
        MockHttpSession session = login("dir_user_1", "pass-1234");

        Long rootDirectoryId = createDirectory(session, null, "docs", 0, 1);

        mockMvc.perform(get("/api/directories").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].directoryId").value(rootDirectoryId))
                .andExpect(jsonPath("$[0].name").value("docs"))
                .andExpect(jsonPath("$[0].depthLevel").value(0));

        createDirectory(session, rootDirectoryId, "images", 0, 1);

        mockMvc.perform(get("/api/directories")
                        .param("parentDirectoryId", String.valueOf(rootDirectoryId))
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("images"))
                .andExpect(jsonPath("$[0].depthLevel").value(1));
    }

    @Test
    @DisplayName("디렉터리 이름 변경과 부모 이동을 순차적으로 수행할 수 있다")
    void renameDirectory_AndMoveDirectory_Succeeds() throws Exception {
        userService.register("dir_user_2", "pass-1234", "dir_user_2@test.com");
        MockHttpSession session = login("dir_user_2", "pass-1234");

        Long parentAId = createDirectory(session, null, "parent-a", 0, 1);
        Long parentBId = createDirectory(session, null, "parent-b", 0, 1);
        Long childId = createDirectory(session, parentAId, "child", 0, 1);

        mockMvc.perform(patch("/api/directories/{directoryId}/name", childId)
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"child-renamed"}
                                """))
                .andExpect(status().isNoContent());

        mockMvc.perform(patch("/api/directories/{directoryId}/parent", childId)
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"newParentDirectoryId":%d}
                                """.formatted(parentBId)))
                .andExpect(status().isNoContent());

        VirtualDirectory moved = virtualDirectoryRepository.findActiveById(childId).orElseThrow();
        assertThat(moved.getName()).isEqualTo("child-renamed");
        assertThat(moved.getParentDirectory()).isNotNull();
        assertThat(moved.getParentDirectory().getId()).isEqualTo(parentBId);
    }

    @Test
    @DisplayName("로그인 없이 디렉터리 생성 요청 시 403을 반환한다")
    void createDirectory_WithoutLogin_ReturnsForbidden() throws Exception {
        mockMvc.perform(post("/api/directories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"docs",
                                  "readLevel":0,
                                  "writeLevel":1
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("디렉터리 트리 조회는 로그인한 사용자 소유 디렉터리만 반환한다")
    void listDirectoryTree_ReturnsOnlyRequesterDirectories() throws Exception {
        userService.register("dir_tree_user_a", "pass-1234", "dir_tree_user_a@test.com");
        userService.register("dir_tree_user_b", "pass-1234", "dir_tree_user_b@test.com");

        MockHttpSession sessionA = login("dir_tree_user_a", "pass-1234");
        MockHttpSession sessionB = login("dir_tree_user_b", "pass-1234");

        createDirectory(sessionA, null, "tree-a-root", 0, 1);
        createDirectory(sessionB, null, "tree-b-root", 0, 1);

        mockMvc.perform(get("/api/directories/tree").session(sessionA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("tree-a-root"));
    }

    @Test
    @DisplayName("디렉터리 삭제 요청은 배치 큐에 DIRECTORY_DELETE 작업을 등록한다")
    void requestDeleteDirectory_RegistersBatchJob() throws Exception {
        userService.register("dir_user_3", "pass-1234", "dir_user_3@test.com");
        MockHttpSession session = login("dir_user_3", "pass-1234");
        Long directoryId = createDirectory(session, null, "to-delete", 0, 1);

        Long baselineJobId = batchJobQueueRepository.findTopByOrderByIdDesc()
                .map(BatchJobQueue::getId)
                .orElse(0L);

        mockMvc.perform(delete("/api/directories/{directoryId}", directoryId)
                        .session(session))
                .andExpect(status().isNoContent());

        List<BatchJobQueue> newJobs = batchJobQueueRepository
                .findByIdGreaterThanAndJobTypeAndTargetIdOrderByIdAsc(
                        baselineJobId,
                        "DIRECTORY_DELETE",
                        directoryId
                );
        assertThat(newJobs).isNotEmpty();
    }

    @Test
    @DisplayName("디렉터리 삭제 요청 후 워커를 실행하면 배치 작업이 성공 처리되고 디렉터리가 soft delete 된다")
    void requestDeleteDirectory_WhenWorkerRuns_ProcessesDeleteJob() throws Exception {
        excludePreExistingRunnableJobs();

        userService.register("dir_user_4", "pass-1234", "dir_user_4@test.com");
        MockHttpSession session = login("dir_user_4", "pass-1234");
        Long directoryId = createDirectory(session, null, "to-process-delete", 0, 1);

        Long baselineJobId = batchJobQueueRepository.findTopByOrderByIdDesc()
                .map(BatchJobQueue::getId)
                .orElse(0L);

        mockMvc.perform(delete("/api/directories/{directoryId}", directoryId)
                        .session(session))
                .andExpect(status().isNoContent());

        List<BatchJobQueue> newJobs = batchJobQueueRepository
                .findByIdGreaterThanAndJobTypeAndTargetIdOrderByIdAsc(
                        baselineJobId,
                        "DIRECTORY_DELETE",
                        directoryId
                );
        assertThat(newJobs).isNotEmpty();

        Long queuedJobId = newJobs.get(0).getId();
        forceJobRunnableNow(queuedJobId);

        batchJobWorker.processPendingJobs();
        entityManager.flush();
        entityManager.clear();

        BatchJobQueue processedJob = batchJobQueueRepository.findById(queuedJobId).orElseThrow();
        assertThat(processedJob.getStatus()).isEqualTo("success");
        assertThat(virtualDirectoryRepository.findActiveById(directoryId)).isEmpty();
    }

    private void excludePreExistingRunnableJobs() {
        while (true) {
            List<Long> preExistingRunnableJobIds = batchJobQueueRepository
                    .findTop200ByStatusInOrderByIdAsc(List.of("wait", "retry_wait"))
                    .stream()
                    .map(BatchJobQueue::getId)
                    .toList();
            if (preExistingRunnableJobIds.isEmpty()) {
                break;
            }

            batchJobQueueRepository.updateStatusToProcessing(preExistingRunnableJobIds, "in_progress");
            batchJobQueueRepository.markAsSuccess(preExistingRunnableJobIds);
            entityManager.flush();
            entityManager.clear();
        }
    }

    private void forceJobRunnableNow(Long jobId) {
        entityManager.createQuery("UPDATE BatchJobQueue b SET b.nextRunAt = :nextRunAt WHERE b.id = :id")
                .setParameter("nextRunAt", LocalDateTime.now().minusSeconds(5))
                .setParameter("id", jobId)
                .executeUpdate();
        entityManager.flush();
        entityManager.clear();
    }

    private Long createDirectory(
            MockHttpSession session,
            Long parentDirectoryId,
            String name,
            int readLevel,
            int writeLevel
    ) throws Exception {
        String parentField = parentDirectoryId == null
                ? "\"parentDirectoryId\":null,"
                : "\"parentDirectoryId\":" + parentDirectoryId + ",";

        String body = """
                {
                  %s
                  "name":"%s",
                  "readLevel":%d,
                  "writeLevel":%d
                }
                """.formatted(parentField, name, readLevel, writeLevel);

        MvcResult result = mockMvc.perform(post("/api/directories")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.directoryId").isNumber())
                .andReturn();

        return Long.parseLong(readField(result, "directoryId"));
    }

    private String readField(MvcResult result, String fieldName) throws Exception {
        String body = result.getResponse().getContentAsString();
        String pattern = "\"" + fieldName + "\":";
        int start = body.indexOf(pattern);
        if (start < 0) {
            throw new IllegalArgumentException("응답에서 필드를 찾을 수 없습니다. field=" + fieldName);
        }
        int valueStart = start + pattern.length();
        while (valueStart < body.length() && Character.isWhitespace(body.charAt(valueStart))) {
            valueStart++;
        }
        int end = valueStart;
        while (end < body.length() && Character.isDigit(body.charAt(end))) {
            end++;
        }
        return body.substring(valueStart, end);
    }

    private MockHttpSession login(String loginId, String password) throws Exception {
        String body = """
                {"loginId":"%s","password":"%s"}
                """.formatted(loginId, password);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();

        return (MockHttpSession) result.getRequest().getSession(false);
    }
}
