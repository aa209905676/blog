## 博客介绍



<p align="center">
   基于Springboot + Vue3 开发的前后端分离博客
</p>


## 本地运行

1. MySQL版本为`8.0.27`，npm版本为`9.4.0`，node版本为`v16.18.0`
2. SQL 文件位于根目录下的`blog.sql`，将其中的数据导入到自己本地数据库中
3. ES 映射文件位于`deploy`文件夹下
4. 修改后端配置文件中的数据库等连接信息，项目中使用到的关于阿里云、腾讯云功能和第三方授权登录等需要自行开通
5. 一定要将前端`shoka-admin`和`shoka-blog`的 utils 下的 token.ts 中的`{ domain: domain }`给删除，然后再`npm install`、`npm run dev`
6. 项目启动后，使用`admin@qq.com`管理员账号登录后台，密码为`123456`

## 项目特点

- 前台界面参考 Hexo 的 Shoka 和 Butterfly 设计，页面美观，响应式布局
- 后台管理基于若依二次开发，含有侧边栏，历史标签，面包屑等
- 前后端分离，Docker Compose 一键部署
- 采用 RABC 权限模型，使用 Sa-Token 进行权限管理
- 支持动态权限修改、动态菜单和路由
- 说说、友链、相册、留言弹幕墙、音乐播放器、聊天室
- 支持代码高亮、图片预览、黑夜模式、点赞、取消点赞等功能
- 发布评论、回复评论、表情包
- 发送 HTML 邮件评论回复提醒，内容详细
- 接入第三方登录，减少注册成本
- 文章搜索支持关键字高亮分词
- 文章编辑使用 Markdown 编辑器
- 含有最新评论、文章目录、文章推荐和文章置顶功能
- 实现日志管理、定时任务管理、在线用户和下线用户
- 代码支持多种搜索模式（Elasticsearch 或 MYSQL），支持多种文件上传模式（OSS、COS、本地）
- 采用 Restful 风格的 API，注释完善，代码遵循阿里巴巴开发规范，有利于开发者学习

## 技术介绍

**前端：** Vue3 + Pinia + Vue Router + TypeScript + Axios + Element Plus + Naive UI + Echarts + Swiper

**后端：** SpringBoot + Mysql + Redis + Quartz + Thymeleaf + Nginx + Docker + Sa-Token + Swagger2 + MyBatisPlus +
ElasticSearch + RabbitMQ + Canal

**其他：** 接入 QQ、Gitee、Github 第三方登录

## 运行环境

**服务器：** 腾讯云 2 核 4G CentOS7.6

**对象存储：** 阿里云 OSS、腾讯云 COS

**最低配置：** 2 核 2G 服务器（关闭 ElasticSearch）

## 开发环境

|          开发工具           |          说明          |
|:-----------------------:|:--------------------:|
|          IDEA           |    Java 开发工具 IDE     |
|         VSCode          |     Vue 开发工具 IDE     |
|         Navicat         |     MySQL 远程连接工具     |
|  Redis Desktop Manager  |     Redis 远程连接工具     |
|         Xshell          |     Linux 远程连接工具     |
|          Xftp           |     Linux 文件上传工具     |

|   开发环境    |  版本  |
|:-------------:|:------:|
|    OpenJDK    |   11   |
|     MySQL     | 8.0.27 |
|     Redis     | 6.2.6  |
| Elasticsearch | 7.17.3 |
|   RabbitMQ    | 3.9.11 |

## 项目截图

![](https://static.ttkwsd.top/article/29ebbffccbe4367b8c113f9dee9184fc.png)
![](https://static.ttkwsd.top/article/14f21792d32ceac730dad62798ac353c.png)
![](https://static.ttkwsd.top/article/d4efdf65ea8a393693e317cdebdb6b43.png)

## 后续计划

- [ ] 整合 EasyExcel 导出 Excel
- [ ] 第三方登录使用 JustAuth
- [ ] 博客文章导入导出
- [ ] 移动端文章目录
- [x] 图片瀑布流布局
- [ ] B 站追番页
- [x] B 站图床
- [x] 聊天室

## 项目总结

整个项目花费了大量的心血，开发过程中参考了很多优秀的开源项目，在这里感谢大家的开源项目，收获了很多，希望我的项目能给你带来收获。

鸣谢项目：

- [ 风丶宇 ](https://github.com/X1192176811/blog)
- **[hexo-theme-shoka](https://github.com/amehime/hexo-theme-shoka)**
- [A Hexo Theme: Butterfly](https://github.com/jerryc127/hexo-theme-butterfly)
- [RuoYi-Vue](https://gitee.com/y_project/RuoYi-Vue)
- [vue3-element-admin](https://github.com/youlaitech/vue3-element-admin)
- [基于 Vue.js 的弹幕交互组件](https://github.com/hellodigua/vue-danmaku)
- ...
