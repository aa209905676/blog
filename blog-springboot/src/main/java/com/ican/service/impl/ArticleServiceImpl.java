package com.ican.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.houbb.sensitive.word.core.SensitiveWordHelper;
import com.ican.entity.*;
import com.ican.handler.SensitiveWordException;
import com.ican.mapper.*;
import com.ican.model.dto.*;
import com.ican.model.vo.*;
import com.ican.service.ArticleService;
import com.ican.service.RedisService;
import com.ican.service.TagService;
import com.ican.strategy.context.SearchStrategyContext;
import com.ican.strategy.context.UploadStrategyContext;
import com.ican.utils.BeanCopyUtils;
import com.ican.utils.FileUtils;
import com.ican.utils.PageUtils;
import org.checkerframework.checker.units.qual.A;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static com.ican.constant.CommonConstant.FALSE;
import static com.ican.constant.RedisConstant.*;
import static com.ican.enums.ArticleStatusEnum.PUBLIC;
import static com.ican.enums.FilePathEnum.ARTICLE;

/**
 * 文章业务接口实现类
 *
 * @author ican
 * @date 2022/12/04 22:31
 **/
@Service
public class ArticleServiceImpl extends ServiceImpl<ArticleMapper, Article> implements ArticleService {

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private ArticleTagMapper articleTagMapper;

    @Autowired
    private TagMapper tagMapper;

    @Autowired
    private TagService tagService;

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private RedisService redisService;

    @Autowired
    private SearchStrategyContext searchStrategyContext;

    @Autowired
    private UploadStrategyContext uploadStrategyContext;

    @Autowired
    private BlogFileMapper blogFileMapper;



    @Override
    public PageResult<ArticleBackVO> listArticleBackVO(ConditionDTO condition) {
        // 查询文章数量
        Long count = articleMapper.countArticleBackVO(condition);
        if (count == 0) {
            return new PageResult<>();
        }
        // 查询文章后台信息
        List<ArticleBackVO> articleBackVOList = articleMapper.selectArticleBackVO(PageUtils.getLimit(), PageUtils.getSize(), condition);
        // 浏览量
        Map<Object, Double> viewCountMap = redisService.getZsetAllScore(ARTICLE_VIEW_COUNT);
        // 点赞量
        Map<String, Integer> likeCountMap = redisService.getHashAll(ARTICLE_LIKE_COUNT);
        // 封装文章后台信息
        articleBackVOList.forEach(item -> {
            Double viewCount = Optional.ofNullable(viewCountMap.get(item.getId())).orElse((double) 0);
            item.setViewCount(viewCount.intValue());
            Integer likeCount = likeCountMap.get(item.getId().toString());
            item.setLikeCount(Optional.ofNullable(likeCount).orElse(0));
        });
        return new PageResult<>(articleBackVOList, count);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void addArticle(ArticleDTO article) {
        List<String> sensitive = SensitiveWordHelper.findAll(article.toString());
        if (!sensitive.isEmpty()){
            throw new SensitiveWordException("包含违规敏感词：" + String.join(", ", sensitive));
        }
        // 保存文章分类
        Integer categoryId = saveArticleCategory(article);
        // 添加文章
        Article newArticle = BeanCopyUtils.copyBean(article, Article.class);
        if (StringUtils.isBlank(newArticle.getArticleCover())) {
            SiteConfig siteConfig = redisService.getObject(SITE_SETTING);
            newArticle.setArticleCover(siteConfig.getArticleCover());
        }
        newArticle.setCategoryId(categoryId);
        newArticle.setUserId(StpUtil.getLoginIdAsInt());
        baseMapper.insert(newArticle);
        // 保存文章标签
        saveArticleTag(article, newArticle.getId());
        redisService.deleteObject(ARTICLE_HOME_LIST);

    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteArticle(List<Integer> articleIdList) {
        // 删除文章标签
        articleTagMapper.delete(new LambdaQueryWrapper<ArticleTag>()
                .in(ArticleTag::getArticleId, articleIdList));
        // 删除文章
        articleMapper.deleteBatchIds(articleIdList);
        List<String> collect = articleIdList.stream()
                .map(Object::toString)
                .collect(Collectors.toList());
        redisService.deleteObject(collect);
    }

    @Override
    public void updateArticleDelete(DeleteDTO delete) {
        // 批量更新文章删除状态
        List<Article> articleList = delete.getIdList()
                .stream()
                .map(id -> Article.builder()
                        .id(id)
                        .isDelete(delete.getIsDelete())
                        .isTop(FALSE)
                        .isRecommend(FALSE)
                        .build())
                .collect(Collectors.toList());
        this.updateBatchById(articleList);
        delete.getIdList().forEach(id->{
            redisService.deleteHash(ARTICLE_VO,id.toString());
            redisService.deleteHash(ARTICLE_INFO,id.toString());
        });
        redisService.deleteObject(ARTICLE_HOME_LIST);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateArticle(ArticleDTO article) {
        // 保存文章分类
        Integer categoryId = saveArticleCategory(article);
        // 修改文章
        Article newArticle = BeanCopyUtils.copyBean(article, Article.class);
        newArticle.setCategoryId(categoryId);
        newArticle.setUserId(StpUtil.getLoginIdAsInt());
        baseMapper.updateById(newArticle);
        // 保存文章标签
        saveArticleTag(article, newArticle.getId());
        // 删除之前保存在redis的值
        redisService.deleteHash(ARTICLE_VO,article.getId());
    }

    @Override
    public ArticleInfoVO editArticle(Integer articleId) {
        ArticleInfoVO  articleInfoVO  = redisService.getHash(ARTICLE_INFO, articleId.toString());
        if(articleInfoVO == null && ObjectUtil.isEmpty(articleInfoVO)) {
            // 查询文章信息
            articleInfoVO = articleMapper.selectArticleInfoById(articleId);
            Assert.notNull(articleInfoVO, "没有该文章");
        }
        // 查询文章分类名称
        Category category = categoryMapper.selectOne(new LambdaQueryWrapper<Category>()
                .select(Category::getCategoryName)
                .eq(Category::getId, articleInfoVO.getCategoryId()));
        // 查询文章标签名称
        List<String> tagNameList = tagMapper.selectTagNameByArticleId(articleId);
        articleInfoVO.setCategoryName(category.getCategoryName());
        articleInfoVO.setTagNameList(tagNameList);
        redisService.setHash(ARTICLE_INFO, articleId.toString(),articleInfoVO);
        return articleInfoVO;
    }

    @Override
    public void updateArticleTop(TopDTO top) {
        // 修改文章置顶状态
        Article newArticle = Article.builder()
                .id(top.getId())
                .isTop(top.getIsTop())
                .build();
        articleMapper.updateById(newArticle);
    }

    @Override
    public void updateArticleRecommend(RecommendDTO recommend) {
        // 修改文章推荐状态
        Article newArticle = Article.builder()
                .id(recommend.getId())
                .isRecommend(recommend.getIsRecommend())
                .build();
        articleMapper.updateById(newArticle);
    }

    @Override
    public List<ArticleSearchVO> listArticlesBySearch(String keyword) {
        return searchStrategyContext.executeSearchStrategy(keyword);
    }

    @Override
    public PageResult<ArticleHomeVO> listArticleHomeVO() {
        List<ArticleHomeVO> articleHomeVOList = redisService.getList(ARTICLE_HOME_LIST);
        if (ObjectUtil.isNull(articleHomeVOList)) {
            return new PageResult<>(articleHomeVOList,(long)articleHomeVOList.size());
        }
        // 查询文章数量
        Long count = articleMapper.selectCount(new LambdaQueryWrapper<Article>()
                .eq(Article::getIsDelete, FALSE)
                .eq(Article::getStatus, PUBLIC.getStatus()));
        if (count == 0) {
            return new PageResult<>();
        }
        // 查询首页文章
        articleHomeVOList = articleMapper.selectArticleHomeList(PageUtils.getLimit(), PageUtils.getSize());
        redisService.setList(ARTICLE_HOME_LIST,articleHomeVOList);

        return new PageResult<>(articleHomeVOList, count);
    }


    @Override
    public ArticleVO getArticleHomeById(Integer articleId) {
        // 从Redis中获取文章数据
        ArticleVO article = redisService.getHash(ARTICLE_VO, articleId.toString());
        if (Objects.isNull(article)) {
            // 查询文章信息
            article = articleMapper.selectArticleHomeById(articleId);
            if (Objects.isNull(article)) {
                return null;
            }
        }
        // 将文章数据存储到Redis中
        redisService.setHash(ARTICLE_VO, articleId.toString(), article);
        // 浏览量+1
        redisService.incrZet(ARTICLE_VIEW_COUNT, articleId, 1D);
        // 查询上一篇文章
        ArticlePaginationVO lastArticle = articleMapper.selectLastArticle(articleId);
        // 查询下一篇文章
        ArticlePaginationVO nextArticle = articleMapper.selectNextArticle(articleId);
        article.setLastArticle(lastArticle);
        article.setNextArticle(nextArticle);
        // 查询浏览量
        Double viewCount = Optional.ofNullable(redisService.getZsetScore(ARTICLE_VIEW_COUNT, articleId))
                .orElse((double) 0);
        article.setViewCount(viewCount.intValue());
        // 查询点赞量
        Integer likeCount = redisService.getHash(ARTICLE_LIKE_COUNT, articleId.toString());
        article.setLikeCount(Optional.ofNullable(likeCount).orElse(0));
        return article;
    }

    @Override
    public PageResult<ArchiveVO> listArchiveVO() {
        // 查询文章数量
        Long count = articleMapper.selectCount(new LambdaQueryWrapper<Article>()
                .eq(Article::getIsDelete, FALSE)
                .eq(Article::getStatus, PUBLIC.getStatus()));
        if (count == 0) {
            return new PageResult<>();
        }
        List<ArchiveVO> archiveList = articleMapper.selectArchiveList(PageUtils.getLimit(), PageUtils.getSize());
        return new PageResult<>(archiveList, count);
    }

    @Override
    public List<ArticleRecommendVO> listArticleRecommendVO() {
        return articleMapper.selectArticleRecommend();
    }

    @Override
    public String saveArticleImages(MultipartFile file) {
        // 上传文件
        String url = uploadStrategyContext.executeUploadStrategy(file, ARTICLE.getPath());
        try {
            // 获取文件md5值
            String md5 = FileUtils.getMd5(file.getInputStream());
            // 获取文件扩展名
            String extName = FileUtils.getExtension(file);
            BlogFile existFile = blogFileMapper.selectOne(new LambdaQueryWrapper<BlogFile>()
                    .select(BlogFile::getId)
                    .eq(BlogFile::getFileName, md5)
                    .eq(BlogFile::getFilePath, ARTICLE.getFilePath()));
            if (Objects.isNull(existFile)) {
                // 保存文件信息
                BlogFile newFile = BlogFile.builder()
                        .fileUrl(url)
                        .fileName(md5)
                        .filePath(ARTICLE.getFilePath())
                        .extendName(extName)
                        .fileSize((int) file.getSize())
                        .isDir(FALSE)
                        .build();
                blogFileMapper.insert(newFile);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return url;
    }

    /**
     * 保存文章分类
     *
     * @param article 文章信息
     * @return 文章分类
     */
    private Integer saveArticleCategory(ArticleDTO article) {
        // 查询分类
        Category category = categoryMapper.selectOne(new LambdaQueryWrapper<Category>()
                .select(Category::getId)
                .eq(Category::getCategoryName, article.getCategoryName()));
        // 分类不存在
        if (Objects.isNull(category)) {
            category = Category.builder()
                    .categoryName(article.getCategoryName())
                    .build();
            // 保存分类
            categoryMapper.insert(category);

        }
        return category.getId();
    }

    /**
     * 保存文章标签
     *
     * @param article   文章信息
     * @param articleId 文章id
     */
    private void saveArticleTag(ArticleDTO article, Integer articleId) {
        // 删除文章标签
        articleTagMapper.delete(new LambdaQueryWrapper<ArticleTag>()
                .eq(ArticleTag::getArticleId, articleId));
        // 标签名列表
        List<String> tagNameList = article.getTagNameList();
        if (CollectionUtils.isNotEmpty(tagNameList)) {
            // 查询出已存在的标签
            List<Tag> existTagList = tagMapper.selectTagList(tagNameList);
            List<String> existTagNameList = existTagList.stream()
                    .map(Tag::getTagName)
                    .collect(Collectors.toList());
            List<Integer> existTagIdList = existTagList.stream()
                    .map(Tag::getId)
                    .collect(Collectors.toList());
            // 移除已存在的标签列表
            tagNameList.removeAll(existTagNameList);
            // 含有新标签
            if (CollectionUtils.isNotEmpty(tagNameList)) {
                // 新标签列表
                List<Tag> newTagList = tagNameList.stream()
                        .map(item -> Tag.builder()
                                .tagName(item)
                                .build())
                        .collect(Collectors.toList());
                // 批量保存新标签
                tagService.saveBatch(newTagList);
                // 获取新标签id列表
                List<Integer> newTagIdList = newTagList.stream()
                        .map(Tag::getId)
                        .collect(Collectors.toList());
                // 新标签id添加到id列表
                existTagIdList.addAll(newTagIdList);
            }
            // 将所有的标签绑定到文章标签关联表
            articleTagMapper.saveBatchArticleTag(articleId, existTagIdList);
        }
    }
}