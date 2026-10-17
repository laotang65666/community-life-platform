package com.laotang.core.ai.gateway;

/**
 * 模型调用网关。
 *
 * <p>把 Spring AI 的细节挡在业务层之外:业务层只关心"给角色、历史、问题和工具,
 * 拿回文本与 token 数",方便单测和后续替换模型。</p>
 */
public interface AiChatGateway {

    ChatResult chat(AiChatCommand command);
}
