package io.github.supernoobchallenge.nasserver.connection;

import io.github.supernoobchallenge.nasserver.global.config.JpaConfig;
import io.github.supernoobchallenge.nasserver.file.core.entity.FilePermissionKey;
import io.github.supernoobchallenge.nasserver.user.entity.User;
import io.github.supernoobchallenge.nasserver.user.entity.UserPermission;
import org.junit.jupiter.api.Test;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(JpaConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class MySQLConnectionTest {

    @Test
    public void basicInsertTest() throws Exception{
        //given
        // 1. 파일 권한 키 먼저 생성
        FilePermissionKey key = FilePermissionKey.builder()
                .ownerType(FilePermissionKey.OwnerType.USER)
                .build(); // 용량은 자동으로 0 세팅됨

        // 2. 유저 생성 (권한 키 넣어서)
        User user = User.builder()
                .loginId("tester")
                .password("1234")
                .email("test@test.com")
                .filePermission(key) // 여기서 UserPermission은 안 넣음 (못 넣음)
                .build();
        

        // 3. 유저 권한 생성 (만들어진 유저를 넣어서)
        UserPermission permission = UserPermission.builder()
                .user(user) // User 객체 주입
                .canShareFile(true)
                .canCreateGroup(false)
                .canStorePersonal(false)
                .canInvitePerson(false)
                .build();

        // 4. (선택) 유저 객체에도 권한을 알려줌 (JPA Cascade 저장 시 필요할 수 있음)
        user.assignPermission(permission);
        //when

        //then
    }

}
