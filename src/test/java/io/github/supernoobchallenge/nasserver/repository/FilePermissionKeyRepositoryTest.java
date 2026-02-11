package io.github.supernoobchallenge.nasserver.repository;

import io.github.supernoobchallenge.nasserver.file.core.entity.FilePermissionKey;
import io.github.supernoobchallenge.nasserver.file.core.repository.FilePermissionKeyRepository;
import io.github.supernoobchallenge.nasserver.global.config.JpaConfig;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;


import static io.github.supernoobchallenge.nasserver.file.core.entity.FilePermissionKey.*;
import static io.github.supernoobchallenge.nasserver.file.core.entity.FilePermissionKey.OwnerType.*;
import static org.assertj.core.api.Assertions.*;
@SpringBootTest
@Transactional
@Import(JpaConfig.class)
class FilePermissionKeyRepositoryTest {
    @Autowired
    FilePermissionKeyRepository repository;
    @Autowired
    EntityManager em;
    @Test
    public void insertTest() throws Exception{
        FilePermissionKey key = builder()
                .parentPermission(null)
                .ownerType(USER).build();
        repository.save(key);
        FilePermissionKey key1 = repository.findById(key.getId()).get();
        assertThat(key1).isEqualTo(key);
    }
    
    @Test
    public void initValueTest() throws Exception{
        // give
        FilePermissionKey key = builder()
                .parentPermission(null)
                .ownerType(USER).build();
        repository.save(key);

        //when
        em.flush();
        em.clear();

        // than
        FilePermissionKey key1 = repository.findById(key.getId()).get();
        assertThat(key1.getParentPermission()).isNull();
        assertThat(key1.getOwnerType()).isEqualTo(USER);
        assertThat(key1.getTotalCapacity()).isEqualTo(0);
        assertThat(key1.getAvailableCapacity()).isEqualTo(0);
        assertThat(key1.getDeletedAt()).isNull();
        assertThat(key1.getCreatedBy()).isEqualTo(1);
        assertThat(key1.getUpdatedBy()).isEqualTo(1);
    }
    
    @Test
    @DisplayName("자식 객체 불러왔을 때 부모도 같이 불러와지는지 검증")
    public void HierarchyTest() throws Exception{
        // give
        FilePermissionKey parent = builder()
                .parentPermission(null)
                .ownerType(USER).build();
        repository.save(parent);
        FilePermissionKey child = builder()
                .parentPermission(parent)
                .ownerType(USER).build();
        repository.save(child);

        // when
        em.flush();
        em.clear();
        FilePermissionKey findChild = repository.findById(child.getId()).get();

        // then
        assertThat(findChild.getParentPermission().getId()).isEqualTo(parent.getId());
    }

    @Test
    public void softDeleteTest() throws Exception{
        // give
        FilePermissionKey key = builder()
                .parentPermission(null)
                .ownerType(USER).build();
        repository.save(key);
        key.delete();

        // when
        em.flush();
        em.clear();
        FilePermissionKey key1 = repository.findById(key.getId()).get();

        // then
        assertThat(key1.getDeletedAt()).isNotNull();
    }
}
