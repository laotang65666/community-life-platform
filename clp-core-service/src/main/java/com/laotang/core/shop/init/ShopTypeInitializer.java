package com.laotang.core.shop.init;

import com.laotang.core.shop.entity.ShopType;
import com.laotang.core.shop.mapper.ShopTypeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 首次启动时写入默认店铺分类。表非空则跳过。
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
        Long count = shopTypeMapper.selectCount(null);
        if (count != null && count > 0) {
            return;
        }

        for (int i = 0; i < DEFAULT_TYPES.length; i++) {
            ShopType type = new ShopType();
            type.setName(DEFAULT_TYPES[i]);
            type.setIcon("");
            type.setSort(i);
            shopTypeMapper.insert(type);
        }
    }
}
