package io.github.supernoobchallenge.nasserver.repository;

import io.github.supernoobchallenge.nasserver.config.JpaConfig;
import io.github.supernoobchallenge.nasserver.entity.FilePermissionKey;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static io.github.supernoobchallenge.nasserver.entity.FilePermissionKey.builder;

@SpringBootTest
@Transactional
@Import(JpaConfig.class)
class AuditEntityTest {
    @Autowired
    FilePermissionKeyRepository repository;
    @Autowired
    EntityManager em;

    @Test
    @DisplayName("객체가 수정되면 updated_at 또한 수정되어야 한다.")
    public void atTest() throws Exception{
        //given
        FilePermissionKey key = builder()
                .parentPermission(null)
                .ownerType(FilePermissionKey.OwnerType.USER).build();
        repository.save(key);

        //when
        Thread.sleep(2000);
        key.adjustTotalCapacity(1000L);
        em.flush();
        em.clear();

        FilePermissionKey key1 = repository.findById(key.getId()).get();

        //then
        Assertions.assertThat(key1.getCreatedAt()).isNotEqualTo(key1.getUpdatedAt());
    }
}