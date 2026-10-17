package com.laotang.framework.ai.zhipu;

/**
 * 单条重排结果。
 *
 * @param index 候选文档在入参列表中的下标
 * @param score 相关性分数,越大越相关
 */
public record RerankResult(int index, double score) {
}
