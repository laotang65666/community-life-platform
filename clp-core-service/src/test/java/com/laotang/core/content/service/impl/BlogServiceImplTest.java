package com.laotang.core.content.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.laotang.common.exception.BusinessException;
import com.laotang.common.exception.ErrorCode;
import com.laotang.core.content.config.FeedProperties;
import com.laotang.core.content.entity.Blog;
import com.laotang.core.content.entity.BlogFavorite;
import com.laotang.core.content.mapper.BlogFavoriteMapper;
import com.laotang.core.content.mapper.BlogMapper;
import com.laotang.core.user.entity.User;
import com.laotang.core.user.service.IFollowService;
import com.laotang.core.user.service.IUserService;
import com.laotang.parameter.content.request.CreateBlogRequest;
import com.laotang.parameter.content.response.BlogResponse;
import com.laotang.parameter.content.response.FeedResponse;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BlogServiceImplTest {

    @Mock
    private BlogMapper blogMapper;
    @Mock
    private BlogFavoriteMapper favoriteMapper;
    @Mock
    private IUserService userService;
    @Mock
    private IFollowService followService;
    @Mock
    private StringRedisTemplate redis;
    @Mock
    private ZSetOperations<String, String> zset;
    @org.mockito.Spy
    private FeedProperties feedProperties = new FeedProperties(1000);
    @InjectMocks
    private BlogServiceImpl blogService;

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Blog.class);
        TableInfoHelper.initTableInfo(assistant, BlogFavorite.class);
    }

    @BeforeEach
    void setUp() {
        lenient().when(redis.opsForZSet()).thenReturn(zset);
    }

    private Blog blog(String status) {
        Blog blog = new Blog();
        blog.setId(100L);
        blog.setUserId(1L);
        blog.setTitle("小区门口新开的面馆");
        blog.setContent("味道不错");
        blog.setImages(List.of());
        blog.setLikedCount(0);
        blog.setCommentCount(0);
        blog.setStatus(status);
        return blog;
    }

    private User author() {
        User user = new User();
        user.setId(1L);
        user.setNickname("作者");
        user.setAvatar("");
        return user;
    }

    @Test
    @DisplayName("发布笔记并写入粉丝 Feed")
    void create_fanOutToFollowers() {
        when(blogMapper.insert(any(Blog.class))).thenAnswer(inv -> {
            Blog blog = inv.getArgument(0);
            blog.setId(100L);
            return 1;
        });
        when(followService.getFollowerCount(1L)).thenReturn(2L);
        when(followService.findFollowerIds(1L)).thenReturn(List.of(2L, 3L));
        when(userService.getById(1L)).thenReturn(author());

        BlogResponse response = blogService.create(1L,
                new CreateBlogRequest("标题", "正文", List.of("a.png"), null));

        assertThat(response.id()).isEqualTo("100");
        verify(zset).add(eq("clp:outbox:user:1"), eq("100"), anyDouble());
        verify(zset).add(eq("clp:feed:user:2"), eq("100"), anyDouble());
        verify(zset).add(eq("clp:feed:user:3"), eq("100"), anyDouble());
    }

    @Test
    @DisplayName("大 V 发布只写发件箱,不推送粉丝")
    void create_bigV_skipsFanOut() {
        when(blogMapper.insert(any(Blog.class))).thenAnswer(inv -> {
            Blog blog = inv.getArgument(0);
            blog.setId(100L);
            return 1;
        });
        when(followService.getFollowerCount(1L)).thenReturn(2000L);
        when(userService.getById(1L)).thenReturn(author());

        blogService.create(1L, new CreateBlogRequest("标题", "正文", List.of(), null));

        verify(zset).add(eq("clp:outbox:user:1"), eq("100"), anyDouble());
        verify(followService, never()).findFollowerIds(anyLong());
    }

    @Test
    @DisplayName("笔记不存在 -> 8001")
    void getDetail_notFound() {
        when(blogMapper.selectById(99L)).thenReturn(null);

        BusinessException ex = catchThrowableOfType(
                () -> blogService.getDetail(99L, 1L), BusinessException.class);

        assertThat(ex).isNotNull();
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.BLOG_NOT_FOUND);
    }

    @Test
    @DisplayName("笔记被隐藏 -> 8002")
    void getDetail_hidden() {
        when(blogMapper.selectById(100L)).thenReturn(blog("HIDDEN"));

        BusinessException ex = catchThrowableOfType(
                () -> blogService.getDetail(100L, 1L), BusinessException.class);

        assertThat(ex).isNotNull();
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.BLOG_HIDDEN);
    }

    @Test
    @DisplayName("首次点赞增加计数,重复点赞不增加")
    void like_idempotentCount() {
        when(blogMapper.selectById(100L)).thenReturn(blog("PUBLISHED"));
        when(zset.add(anyString(), anyString(), anyDouble())).thenReturn(true);

        blogService.like(100L, 1L);

        verify(blogMapper).incrementLikedCount(100L);

        when(zset.add(anyString(), anyString(), anyDouble())).thenReturn(false);
        blogService.like(100L, 1L);

        verify(blogMapper).incrementLikedCount(100L);
    }

    @Test
    @DisplayName("非作者不能删除 -> 8003")
    void delete_notOwner() {
        when(blogMapper.selectById(100L)).thenReturn(blog("PUBLISHED"));

        BusinessException ex = catchThrowableOfType(
                () -> blogService.delete(100L, 2L), BusinessException.class);

        assertThat(ex).isNotNull();
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.CONTENT_NOT_OWNER);
    }

    @Test
    @DisplayName("关注流为空")
    void feed_empty() {
        when(zset.reverseRangeByScoreWithScores(anyString(), anyDouble(), anyDouble(),
                anyLong(), anyLong())).thenReturn(Set.of());
        when(followService.findFolloweeIds(1L)).thenReturn(List.of());

        FeedResponse response = blogService.feed(1L, null, 0, 10);

        assertThat(response.list()).isEmpty();
        assertThat(response.lastId()).isZero();
    }

    @Test
    @DisplayName("关注流从收件箱返回笔记")
    void feed_returnsBlogs() {
        Set<ZSetOperations.TypedTuple<String>> tuples =
                Set.of(new DefaultTypedTuple<>("100", 1000.0));
        when(zset.reverseRangeByScoreWithScores(eq("clp:feed:user:1"), anyDouble(), anyDouble(),
                anyLong(), anyLong())).thenReturn(tuples);
        when(followService.findFolloweeIds(1L)).thenReturn(List.of());
        when(blogMapper.selectBatchIds(List.of(100L))).thenReturn(List.of(blog("PUBLISHED")));
        when(userService.findByIds(any())).thenReturn(List.of(author()));

        FeedResponse response = blogService.feed(1L, null, 0, 10);

        assertThat(response.list()).hasSize(1);
        assertThat(response.list().getFirst().id()).isEqualTo("100");
        assertThat(response.lastId()).isEqualTo(1000L);
        assertThat(response.offset()).isEqualTo(1);
    }

    @Test
    @DisplayName("大 V 的笔记通过拉取发件箱进入 Feed")
    void feed_pullsBigVOutbox() {
        Set<ZSetOperations.TypedTuple<String>> tuples =
                Set.of(new DefaultTypedTuple<>("100", 1000.0));

        when(zset.reverseRangeByScoreWithScores(eq("clp:feed:user:1"), anyDouble(), anyDouble(),
                anyLong(), anyLong())).thenReturn(Set.of());
        when(followService.findFolloweeIds(1L)).thenReturn(List.of(2L));
        when(followService.getFollowerCount(2L)).thenReturn(2000L);
        when(zset.reverseRangeByScoreWithScores(eq("clp:outbox:user:2"), anyDouble(), anyDouble(),
                anyLong(), anyLong())).thenReturn(tuples);
        when(blogMapper.selectBatchIds(List.of(100L))).thenReturn(List.of(blog("PUBLISHED")));
        when(userService.findByIds(any())).thenReturn(List.of(author()));

        FeedResponse response = blogService.feed(1L, null, 0, 10);

        assertThat(response.list()).hasSize(1);
        assertThat(response.list().getFirst().id()).isEqualTo("100");
    }

    @Test
    @DisplayName("我的收藏返回笔记")
    void listFavorites() {
        BlogFavorite favorite = new BlogFavorite();
        favorite.setBlogId(100L);
        favorite.setUserId(1L);

        Page<BlogFavorite> page = new Page<>(1, 10);
        page.setRecords(List.of(favorite));
        page.setTotal(1);

        when(favoriteMapper.selectPage(any(), any())).thenReturn(page);
        when(blogMapper.selectBatchIds(List.of(100L))).thenReturn(List.of(blog("PUBLISHED")));
        when(userService.findByIds(any())).thenReturn(List.of(author()));

        var result = blogService.listFavorites(1L, 1, 10);

        assertThat(result.total()).isEqualTo(1);
        assertThat(result.list()).hasSize(1);
    }
}
