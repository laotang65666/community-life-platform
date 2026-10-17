package com.laotang.core.ai.controller;

import com.laotang.common.annotation.RequireRole;
import com.laotang.common.context.UserContext;
import com.laotang.common.enums.RoleEnum;
import com.laotang.common.result.PageResult;
import com.laotang.common.result.Result;
import com.laotang.common.util.PageUtils;
import com.laotang.core.ai.enums.AiScope;
import com.laotang.core.ai.service.IAiChatService;
import com.laotang.parameter.ai.request.CreateConversationRequest;
import com.laotang.parameter.ai.request.SendMessageRequest;
import com.laotang.parameter.ai.response.ChatReplyResponse;
import com.laotang.parameter.ai.response.ConversationResponse;
import com.laotang.parameter.ai.response.MessageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 客服接口。
 */
@Tag(name = "AI 客服")
@RestController
@RequestMapping("/api/v1/ai/conversations")
@RequiredArgsConstructor
@RequireRole(RoleEnum.USER)
public class AiChatController {

    private final IAiChatService aiChatService;

    @Operation(summary = "创建会话")
    @PostMapping
    public Result<ConversationResponse> create(@Valid @RequestBody CreateConversationRequest request) {
        return Result.ok(aiChatService.create(
                UserContext.getUserId(),
                UserContext.getRole(),
                AiScope.from(request.scope()),
                request.title()));
    }

    @Operation(summary = "我的会话列表")
    @GetMapping
    public Result<PageResult<ConversationResponse>> list(
            @RequestParam(value = "scope", required = false) String scope,
            @RequestParam(value = "page", defaultValue = "1") long page,
            @RequestParam(value = "size", defaultValue = "20") long size) {
        return Result.ok(aiChatService.listConversations(
                UserContext.getUserId(),
                StringUtils.hasText(scope) ? AiScope.from(scope) : null,
                PageUtils.normalizePage(page),
                PageUtils.normalizeSize(size)));
    }

    @Operation(summary = "会话消息列表")
    @GetMapping("/{conversationId}/messages")
    public Result<PageResult<MessageResponse>> messages(
            @PathVariable("conversationId") Long conversationId,
            @RequestParam(value = "page", defaultValue = "1") long page,
            @RequestParam(value = "size", defaultValue = "50") long size) {
        return Result.ok(aiChatService.listMessages(
                UserContext.getUserId(),
                conversationId,
                PageUtils.normalizePage(page),
                PageUtils.normalizeSize(size)));
    }

    @Operation(summary = "发送消息并获取回复")
    @PostMapping("/{conversationId}/messages")
    public Result<ChatReplyResponse> chat(
            @PathVariable("conversationId") Long conversationId,
            @Valid @RequestBody SendMessageRequest request) {
        return Result.ok(aiChatService.chat(
                UserContext.getUserId(), conversationId, request.content()));
    }

    @Operation(summary = "删除会话")
    @DeleteMapping("/{conversationId}")
    public Result<Void> delete(@PathVariable("conversationId") Long conversationId) {
        aiChatService.delete(UserContext.getUserId(), conversationId);
        return Result.ok();
    }
}
