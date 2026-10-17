package com.laotang.core.shop.init;

import com.laotang.core.shop.entity.ShopType;
import com.laotang.core.shop.mapper.ShopTypeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * 启动时补齐默认店铺分类。按名称判断,已存在的不重复插入,
 * 避免演示数据先写入一条分类后导致默认分类整体被跳过。
 */
@Component
@RequiredArgsConstructor
public class ShopTypeInitializer implements CommandLineRunner {

    private static final String[] DEFAULT_TYPES = {
            "美食", "休闲娱乐", "生活服务", "购物",
            "酒店", "丽人", "运动健身", "教育培训"
    };

    private final ShopTypeMapper shopTypeMapper;

    @Override
    public void run(String... args) {
        Set<String> existingNames = shopTypeMapper.selectList(null).stream()
                .map(ShopType::getName)
                .collect(Collectors.toSet());

        for (int i = 0; i < DEFAULT_TYPES.length; i++) {
            String name = DEFAULT_TYPES[i];
            if (existingNames.contains(name)) {
                continue;
            }
            ShopType type = new ShopType();
            type.setName(name);
            type.setIcon("");
            type.setSort(i);
            shopTypeMapper.insert(type);
        }
    }
}
