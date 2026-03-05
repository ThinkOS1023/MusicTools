# 实现计划：音乐播放器与本地导入功能

## 概述

基于Kotlin和Jetpack Compose构建Material Design 3风格的Android音乐播放器，采用MVVM架构，全动态UI（无XML），支持本地音乐导入和播放控制。

## 任务

- [-] 1. 初始化项目结构和依赖
  - 创建Android项目，配置Gradle依赖（Jetpack Compose, Material3, Room, Coroutines）
  - 初始化git仓库
  - 配置主题系统（深色/浅色模式）
  - 设置项目包结构（ui, viewmodel, domain, data层）
  - _设计参考: 系统架构_

- [ ] 2. 实现数据模型和数据库
  - [~] 2.1 创建核心数据模型
    - 实现MusicItem, PlaybackState, PlaybackProgress, AudioFile数据类
    - 添加数据验证逻辑
    - _设计参考: 数据模型 1-6_
  
  - [~] 2.2 实现Room数据库
    - 创建MusicDatabase和DAO接口
    - 定义Entity和查询方法
    - _设计参考: 组件5 MusicRepository_
  
  - [ ]* 2.3 编写数据模型单元测试
    - 测试数据验证规则
    - 测试边界条件

- [ ] 3. 实现MediaPlayer服务层
  - [~] 3.1 创建MediaPlayerService
    - 实现Android Service生命周期
    - 封装MediaPlayer操作（prepare, start, pause, stop, seekTo）
    - 添加播放完成和错误监听器
    - _设计参考: 组件7 MediaPlayerService_
  
  - [~] 3.2 实现前台服务通知
    - 创建播放控制通知
    - 添加通知按钮（播放/暂停/上一首/下一首）
    - _设计参考: 组件7职责_
  
  - [ ]* 3.3 编写MediaPlayerService单元测试
    - 测试播放状态转换
    - 测试错误处理

- [~] 4. 检查点 - 确保所有测试通过
  - 确保所有测试通过，如有问题请询问用户

- [ ] 5. 实现领域层逻辑
  - [~] 5.1 实现PlaybackController
    - 实现播放控制逻辑（play, pause, resume, stop, seekTo）
    - 实现播放队列管理（playNext, playPrevious）
    - 实现重复和随机播放模式
    - 添加播放进度更新协程
    - _设计参考: 组件4 PlaybackController, 算法: 主播放工作流, 播放队列管理_
  
  - [~] 5.2 实现MusicRepository
    - 实现数据访问接口（CRUD操作）
    - 实现搜索和排序功能
    - 协调Room数据库和MediaStore
    - _设计参考: 组件5 MusicRepository_
  
  - [~] 5.3 实现FileScanner
    - 实现目录扫描功能
    - 实现MediaStore扫描
    - 实现音频元数据提取
    - 添加扫描进度反馈
    - _设计参考: 组件6 FileScanner, 算法: 文件扫描_
  
  - [ ]* 5.4 编写领域层单元测试
    - 测试PlaybackController状态转换
    - 测试播放队列逻辑
    - 测试FileScanner扫描结果

- [ ] 6. 实现ViewModel层
  - [~] 6.1 实现MusicPlayerViewModel
    - 管理播放器UI状态
    - 处理用户播放控制操作
    - 管理播放进度和时间显示
    - _设计参考: 组件1 MusicPlayerViewModel_
  
  - [~] 6.2 实现MusicLibraryViewModel
    - 管理音乐库列表展示
    - 实现排序和搜索功能
    - 处理音乐项选择和删除
    - _设计参考: 组件2 MusicLibraryViewModel_
  
  - [~] 6.3 实现ImportViewModel
    - 管理文件扫描流程
    - 处理文件选择和导入
    - 显示导入进度
    - _设计参考: 组件3 ImportViewModel_
  
  - [ ]* 6.4 编写ViewModel单元测试
    - 测试状态流更新
    - 测试用户操作处理

- [~] 7. 检查点 - 确保所有测试通过
  - 确保所有测试通过，如有问题请询问用户

- [ ] 8. 实现UI层（Jetpack Compose）
  - [~] 8.1 创建主题和通用组件
    - 实现Material3主题配置
    - 实现深色/浅色模式切换
    - 创建通用UI组件（按钮、卡片等）
    - _设计参考: 系统架构 ThemeManager_
  
  - [~] 8.2 实现MusicPlayerScreen
    - 创建播放器主界面
    - 实现播放控制按钮
    - 实现进度条和时间显示
    - 显示当前播放音乐信息和封面
    - _设计参考: 示例1 基本播放流程_
  
  - [~] 8.3 实现MusicListScreen
    - 创建音乐库列表界面
    - 实现搜索和排序UI
    - 实现列表项点击播放
    - 添加删除功能
    - _设计参考: 组件2职责_
  
  - [~] 8.4 实现ImportScreen
    - 创建导入界面
    - 实现目录选择（Storage Access Framework）
    - 显示扫描进度
    - 实现文件列表和选择UI
    - 添加导入按钮
    - _设计参考: 示例2 导入本地音乐_
  
  - [~] 8.5 实现MainActivity和导航
    - 创建MainActivity（无XML）
    - 实现底部导航栏
    - 配置屏幕间导航
    - _设计参考: 系统架构 MainActivity_

- [ ] 9. 实现权限管理
  - [~] 9.1 添加运行时权限请求
    - 实现READ_EXTERNAL_STORAGE权限请求
    - 实现READ_MEDIA_AUDIO权限请求（Android 13+）
    - 添加权限说明对话框
    - _设计参考: 算法: 文件扫描前置条件_
  
  - [~] 9.2 配置AndroidManifest权限声明
    - 添加必要的权限声明
    - 配置Service和前台服务权限

- [ ] 10. 集成和连接所有组件
  - [~] 10.1 连接ViewModel和UI
    - 在Composable中注入ViewModel
    - 连接状态流和UI更新
    - _设计参考: 示例1, 示例2_
  
  - [~] 10.2 连接Service和ViewModel
    - 实现Service绑定逻辑
    - 连接PlaybackController和MediaPlayerService
    - _设计参考: 主要工作流程序列图_
  
  - [~] 10.3 实现完整播放流程
    - 测试从选择音乐到播放的完整流程
    - 测试播放控制（播放/暂停/上一首/下一首）
    - 测试进度更新和拖动
    - _设计参考: 主要工作流程_
  
  - [~] 10.4 实现完整导入流程
    - 测试从扫描到导入的完整流程
    - 测试权限请求流程
    - 测试元数据提取和显示
  
  - [ ]* 10.5 编写集成测试
    - 测试端到端播放流程
    - 测试端到端导入流程

- [~] 11. 最终检查点 - 确保所有测试通过
  - 确保所有测试通过，如有问题请询问用户

## 注意事项

- 标记`*`的任务为可选任务，可跳过以加快MVP开发
- 每个任务都引用了设计文档中的具体组件或算法
- 检查点确保增量验证
- 所有代码使用Kotlin编写，UI全部使用Jetpack Compose动态创建（无XML）
- 遵循Material Design 3设计规范
- 确保高度解耦，每个组件职责单一
