package com.laotang.core.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.laotang.common.constant.RedisKey;
import com.laotang.common.exception.BusinessException;
import com.laotang.common.exception.ErrorCode;
import com.laotang.common.result.PageResult;
import com.laotang.core.content.config.FeedProperties;
import com.laotang.core.content.converter.BlogConverter;
import com.laotang.core.content.entity.Blog;
import com.laotang.core.content.entity.BlogFavorite;
import com.laotang.core.content.mapper.BlogFavoriteMapper;
import com.laotang.core.content.mapper.BlogMapper;
import com.laotang.core.content.service.IBlogService;
import com.laotang.core.user.converter.UserConverter;
import com.laotang.core.user.entity.User;
import com.laotang.core.user.service.IFollowService;
import com.laotang.core.user.service.IUserService;
import com.laotang.parameter.content.request.CreateBlogRequest;
import com.laotang.parameter.content.response.BlogBriefResponse;
import com.laotang.parameter.content.response.BlogResponse;
import com.laotang.parameter.content.response.FeedResponse;
import com.laotang.parameter.user.response.UserBriefResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 笔记服务实现。
 *
 * <p>Feed 采用推模式:发布笔记时写入所有粉丝的 Redis ZSet 收件箱。</p>
 */
@Service
@RequiredArgsConstructor
public class BlogServiceImpl implements IBlogService {

    private static final Logger log = LoggerFactory.getLogger(BlogServiceImpl.class);

    private static final String PUBLISHED = "PUBLISHED";
    private static final String HIDDEN = "HIDDEN";
    private static final String DELETED = "DELETED";

    private final BlogMapper blogMapper;
    private final BlogFavoriteMapper favoriteMapper;
    private final IUserService userService;
    private final IFollowService followService;
    private final StringRedisTemplate redis;
    private final FeedProperties feedProperties;

    @Override
    public BlogResponse create(Long userId, CreateBlogRequest request) {
        Blog blog = new Blog();
        blog.setUserId(userId);
        blog.setShopId(request.shopId());
        blog.setTitle(request.title());
        blog.setContent(request.content());
        blog.setImages(request.images() == null ? List.of() : request.images());
        blog.setLikedCount(0);
        blog.setCommentCount(0);
        blog.setStatus(PUBLISHED);
        blog.setCreatedAt(LocalDateTime.now());
        blogMapper.insert(blog);

        publishToFeed(userId, blog.getId());

        return BlogConverter.toResponse(blog, userService.getById(userId), false, false);
    }

    /**
     * 推拉结合:
     * <ul>
     *   <li>所有作者的笔记都写入自己的发件箱(大 V 的读者从这里拉)</li>
     *   <li>粉丝数低于阈值的普通作者,再推送到每个粉丝的收件箱</li>
     *   <li>大 V 不推送,避免写扩散风暴</li>
     * </ul>
     */
    private void publishToFeed(Long authorId, Long blogId) {
        try {
            long score = System.currentTimeMillis();
            String member = String.valueOf(blogId);

            // 1. 永远写作者发件箱
            redis.opsForZSet().add(RedisKey.userOutbox(authorId), member, score);

            // 2. 大 V 只写发件箱,由读者拉取
            long followerCount = followService.getFollowerCount(authorId);
            if (followerCount >= feedProperties.bigVThreshold()) {
                return;
            }

            // 3. 普通作者推送到粉丝收件箱
            List<Long> followers = followService.findFollowerIds(authorId);
            for (Long followerId : followers) {
                redis.opsForZSet().add(RedisKey.userFeed(followerId), member, score);
            }
        } catch (Exception e) {
            // Feed 是派生数据,写入失败不影响笔记发布,可后续重建
            log.error("写入 Feed 失败 authorId={} blogId={}", authorId, blogId, e);
        }
    }

    @Override
    public BlogResponse getDetail(Long blogId, Long currentUserId) {
        Blog blog = requirePublished(blogId);
        return BlogConverter.toResponse(
                blog,
                userService.getById(blog.getUserId()),
                isLiked(blogId, currentUserId),
                isFavorited(blogId, currentUserId));
    }

    @Override
    public void delete(Long blogId, Long userId) {
        Blog blog = blogMapper.selectById(blogId);
        if (blog == null) {
            throw new BusinessException(ErrorCode.BLOG_NOT_FOUND);
        }
        if (!blog.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.CONTENT_NOT_OWNER);
        }
        blogMapper.update(null, new LambdaUpdateWrapper<Blog>()
                .eq(Blog::getId, blogId)
                .set(Blog::getStatus, DELETED));
        blogMapper.deleteById(blogId);
    }

    @Override
    public PageResult<BlogBriefResponse> listHot(long page, long size) {
        Page<Blog> result = blogMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Blog>()
                        .eq(Blog::getStatus, PUBLISHED)
                        .orderByDesc(Blog::getLikedCount)
                        .orderByDesc(Blog::getCreatedAt));
        return toBriefPage(result);
    }

    @Override
    public PageResult<BlogBriefResponse> listByAuthor(Long authorId, long page, long size) {
        Page<Blog> result = blogMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Blog>()
                        .eq(Blog::getUserId, authorId)
                        .eq(Blog::getStatus, PUBLISHED)
                        .orderByDesc(Blog::getCreatedAt));
        return toBriefPage(result);
    }

    @Override
    public FeedResponse feed(Long userId, Long lastId, int offset, int size) {
        long max = (lastId == null || lastId <= 0) ? System.currentTimeMillis() : lastId;
        int fetchSize = Math.min(size + Math.max(offset, 0), 200);

        List<ZSetOperations.TypedTuple<String>> all = new ArrayList<>();

        // 1. 自己的收件箱:普通作者推送给我的笔记
        Set<ZSetOperations.TypedTuple<String>> inbox = redis.opsForZSet()
                .reverseRangeByScoreWithScores(RedisKey.userFeed(userId), 0, max, 0, fetchSize);
        if (inbox != null) {
            all.addAll(inbox);
        }

        // 2. 我关注的大 V 的发件箱
        int threshold = feedProperties.bigVThreshold();
        List<Long> followees = followService.findFolloweeIds(userId);
        for (Long followee : followees) {
            if (followService.getFollowerCount(followee) < threshold) {
                continue;
            }
            Set<ZSetOperations.TypedTuple<String>> outbox = redis.opsForZSet()
                    .reverseRangeByScoreWithScores(RedisKey.userOutbox(followee), 0, max, 0, fetchSize);
            if (outbox != null) {
                all.addAll(outbox);
            }
        }

        if (all.isEmpty()) {
            return new FeedResponse(List.of(), 0, 0);
        }

        // 3. 合并排序:score 倒序,同分按 blogId 倒序
        all.sort((a, b) -> {
            double sa = a.getScore() == null ? 0D : a.getScore();
            double sb = b.getScore() == null ? 0D : b.getScore();
            int cmp = Double.compare(sb, sa);
            return cmp != 0 ? cmp : b.getValue().compareTo(a.getValue());
        });

        // 4. 去重 + 应用全局 lastId/offset 游标
        List<ZSetOperations.TypedTuple<String>> page = new ArrayList<>(size);
        Set<String> seen = new HashSet<>();
        int skippedSameScore = 0;

        for (ZSetOperations.TypedTuple<String> tuple : all) {
            String value = tuple.getValue();
            if (value == null || !seen.add(value)) {
                continue;
            }
            long score = tuple.getScore() == null ? 0L : tuple.getScore().longValue();

            if (lastId != null && lastId > 0 && score == lastId && skippedSameScore < offset) {
                skippedSameScore++;
                continue;
            }

            page.add(tuple);
            if (page.size() >= size) {
                break;
            }
        }

        if (page.isEmpty()) {
            return new FeedResponse(List.of(), 0, 0);
        }

        List<Long> blogIds = page.stream()
                .map(tuple -> Long.valueOf(tuple.getValue()))
                .toList();

        Map<Long, Blog> blogMap = blogMapper.selectBatchIds(blogIds).stream()
                .filter(blog -> PUBLISHED.equals(blog.getStatus()))
                .collect(Collectors.toMap(Blog::getId, Function.identity()));

        Map<Long, User> authors = loadAuthors(
                blogMap.values().stream().map(Blog::getUserId).distinct().toList());

        List<BlogBriefResponse> list = blogIds.stream()
                .map(blogMap::get)
                .filter(Objects::nonNull)
                .map(blog -> BlogConverter.toBrief(blog, authors.get(blog.getUserId())))
                .toList();

        long newLastId = 0;
        int newOffset = 0;
        for (ZSetOperations.TypedTuple<String> tuple : page) {
            long score = tuple.getScore() == null ? 0L : tuple.getScore().longValue();
            if (score == newLastId) {
                newOffset++;
            } else {
                newLastId = score;
                newOffset = 1;
            }
        }

        return new FeedResponse(list, newLastId, newOffset);
    }

    @Override
    public void like(Long blogId, Long userId) {
        requirePublished(blogId);
        Boolean added = redis.opsForZSet().add(
                RedisKey.blogLike(blogId), String.valueOf(userId), System.currentTimeMillis());
        if (Boolean.TRUE.equals(added)) {
            blogMapper.incrementLikedCount(blogId);
        }
    }

    @Override
    public void unlike(Long blogId, Long userId) {
        Long removed = redis.opsForZSet().remove(RedisKey.blogLike(blogId), String.valueOf(userId));
        if (removed != null && removed > 0) {
            blogMapper.decrementLikedCount(blogId);
        }
    }

    @Override
    public PageResult<UserBriefResponse> listLikes(Long blogId, long page, long size) {
        requirePublished(blogId);

        String key = RedisKey.blogLike(blogId);
        long start = (page - 1) * size;
        long end = start + size - 1;
        Set<String> ids = redis.opsForZSet().reverseRange(key, start, end);
        Long card = redis.opsForZSet().zCard(key);
        long total = card == null ? 0L : card;

        if (ids == null || ids.isEmpty()) {
            return PageResult.of(List.of(), page, size, total);
        }

        List<Long> userIds = ids.stream().map(Long::valueOf).toList();
        Map<Long, User> users = loadAuthors(userIds);

        List<UserBriefResponse> list = userIds.stream()
                .map(users::get)
                .filter(Objects::nonNull)
                .map(UserConverter::toBrief)
                .toList();

        return PageResult.of(list, page, size, total);
    }

    @Override
    public void favorite(Long blogId, Long userId) {
        requirePublished(blogId);

        BlogFavorite favorite = new BlogFavorite();
        favorite.setBlogId(blogId);
        favorite.setUserId(userId);
        try {
            favoriteMapper.insert(favorite);
        } catch (DuplicateKeyException e) {
            // 幂等:重复收藏视为成功
        }
    }

    @Override
    public void unfavorite(Long blogId, Long userId) {
        favoriteMapper.delete(new LambdaQueryWrapper<BlogFavorite>()
                .eq(BlogFavorite::getBlogId, blogId)
                .eq(BlogFavorite::getUserId, userId));
    }

    @Override
    public PageResult<BlogBriefResponse> listFavorites(Long userId, long page, long size) {
        Page<BlogFavorite> result = favoriteMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<BlogFavorite>()
                        .eq(BlogFavorite::getUserId, userId)
                        .orderByDesc(BlogFavorite::getCreatedAt));

        List<Long> blogIds = result.getRecords().stream()
                .map(BlogFavorite::getBlogId)
                .toList();

        if (blogIds.isEmpty()) {
            return PageResult.of(List.of(), result.getCurrent(), result.getSize(), result.getTotal());
        }

        Map<Long, Blog> blogMap = blogMapper.selectBatchIds(blogIds).stream()
                .filter(blog -> PUBLISHED.equals(blog.getStatus()))
                .collect(Collectors.toMap(Blog::getId, Function.identity()));

        Map<Long, User> authors = loadAuthors(
                blogMap.values().stream().map(Blog::getUserId).distinct().toList());

        List<BlogBriefResponse> list = blogIds.stream()
                .map(blogMap::get)
                .filter(Objects::nonNull)
                .map(blog -> BlogConverter.toBrief(blog, authors.get(blog.getUserId())))
                .toList();

        return PageResult.of(list, result.getCurrent(), result.getSize(), result.getTotal());
    }

    @Override
    public PageResult<BlogBriefResponse> listForAdmin(String status, long page, long size) {
        LambdaQueryWrapper<Blog> wrapper = new LambdaQueryWrapper<Blog>()
                .orderByDesc(Blog::getCreatedAt);
        if (StringUtils.hasText(status)) {
            wrapper.eq(Blog::getStatus, status);
        }

        Page<Blog> result = blogMapper.selectPage(new Page<>(page, size), wrapper);
        return toBriefPage(result);
    }

    @Override
    public void hide(Long blogId) {
        if (blogMapper.selectById(blogId) == null) {
            throw new BusinessException(ErrorCode.BLOG_NOT_FOUND);
        }
        blogMapper.update(null, new LambdaUpdateWrapper<Blog>()
                .eq(Blog::getId, blogId)
                .set(Blog::getStatus, HIDDEN));
    }

    private Blog requirePublished(Long blogId) {
        Blog blog = blogMapper.selectById(blogId);
        if (blog == null) {
            throw new BusinessException(ErrorCode.BLOG_NOT_FOUND);
        }
        if (!PUBLISHED.equals(blog.getStatus())) {
            throw new BusinessException(ErrorCode.BLOG_HIDDEN);
        }
        return blog;
    }

    private boolean isLiked(Long blogId, Long userId) {
        if (userId == null) {
            return false;
        }
        return redis.opsForZSet().score(RedisKey.blogLike(blogId), String.valueOf(userId)) != null;
    }

    private boolean isFavorited(Long blogId, Long userId) {
        if (userId == null) {
            return false;
        }
        Long count = favoriteMapper.selectCount(new LambdaQueryWrapper<BlogFavorite>()
                .eq(BlogFavorite::getBlogId, blogId)
                .eq(BlogFavorite::getUserId, userId));
        return count != null && count > 0;
    }

    private Map<Long, User> loadAuthors(Collection<Long> authorIds) {
        if (authorIds == null || authorIds.isEmpty()) {
            return Map.of();
        }
        return userService.findByIds(authorIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    private PageResult<BlogBriefResponse> toBriefPage(Page<Blog> result) {
        List<Blog> blogs = result.getRecords();
        Map<Long, User> authors = loadAuthors(
                blogs.stream().map(Blog::getUserId).distinct().toList());

        List<BlogBriefResponse> list = blogs.stream()
                .map(blog -> BlogConverter.toBrief(blog, authors.get(blog.getUserId())))
                .toList();

        return PageResult.of(list, result.getCurrent(), result.getSize(), result.getTotal());
    }
}
