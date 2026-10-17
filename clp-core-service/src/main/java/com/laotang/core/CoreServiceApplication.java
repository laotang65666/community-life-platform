package com.laotang.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 应用启动类。
 *
 * <p>扫描范围必须覆盖所有模块的根包 {@code com.laotang},
 * 否则 {@code clp-*} 框架模块中的 Bean 不会被注册。</p>
 *
 * <p>Mapper 不在这里用 {@code @MapperScan} 通配扫描:
 * MyBatis 的 {@code basePackages} 不支持 {@code **} 通配,
 * 统一在每个 Mapper 接口上标注 {@code @Mapper} 即可,
 * 自动配置会扫描启动类所在包及子包。</p>
 */
@SpringBootApplication(scanBasePackages = "com.laotang")
@ConfigurationPropertiesScan("com.laotang")
@EnableScheduling
public class CoreServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CoreServiceApplication.class, args);
    }
}
