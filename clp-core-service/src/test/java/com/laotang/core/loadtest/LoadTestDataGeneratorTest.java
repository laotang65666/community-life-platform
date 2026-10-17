package com.laotang.core.loadtest;

import com.laotang.common.enums.RoleEnum;
import com.laotang.core.auth.jwt.JwtTokenProvider;
import com.laotang.core.user.entity.User;
import com.laotang.core.user.mapper.UserMapper;
import com.laotang.id.SnowflakeIdGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 压测数据生成器。
 *
 * <p>生成 1000 个测试用户和对应的 JWT,输出到 target/loadtest-users.csv。</p>
 *
 * <p>默认不运行,手动执行:</p>
 * <pre>
 * mvn test -Dtest=LoadTestDataGeneratorTest -Dloadtest.generate=true
 * </pre>
 */
@SpringBootTest
@EnabledIfSystemProperty(named = "loadtest.generate", matches = "true")
class LoadTestDataGeneratorTest {

    private static final int USER_COUNT = 1000;
    private static final String PHONE_PREFIX = "1390000";
    private static final Path OUTPUT = Path.of("target", "loadtest-users.csv");

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private SnowflakeIdGenerator snowflakeIdGenerator;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void generate() throws Exception {
        // 物理删除上一轮压测用户,避免手机号唯一索引冲突
        jdbcTemplate.update("DELETE FROM tb_user WHERE phone LIKE ?", PHONE_PREFIX + "%");

        Files.createDirectories(OUTPUT.getParent());
        try (BufferedWriter writer = Files.newBufferedWriter(OUTPUT, StandardCharsets.UTF_8)) {
            writer.write("userId,phone,token");
            writer.newLine();

            for (int i = 0; i < USER_COUNT; i++) {
                String phone = String.format("%s%04d", PHONE_PREFIX, i);
                long userId = snowflakeIdGenerator.nextId();

                User user = new User();
                user.setId(userId);
                user.setPhone(phone);
                user.setPassword("");
                user.setNickname("loadtest-" + i);
                user.setAvatar("");
                user.setIntro("");
                user.setRole("USER");
                user.setStatus("ACTIVE");
                userMapper.insert(user);

                String token = jwtTokenProvider.createAccessToken(userId, RoleEnum.USER);
                writer.write(userId + "," + phone + "," + token);
                writer.newLine();
            }
        }

        System.out.println("压测数据生成完成: " + OUTPUT.toAbsolutePath());
    }
}
