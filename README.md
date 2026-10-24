# 知行软考学习与知识管理系统 V1.0

一个面向信息系统项目管理师备考的全栈项目，前端使用 Vue 3，后端使用 Spring Boot，数据存储使用 MySQL。

## 技术栈

- 前端：Vue 3、Vite、Vue Router、Pinia、Element Plus、Axios、ECharts
- 后端：Java 8、Spring Boot 2.7.18、MyBatis、MySQL 8
- 构建：npm、Maven

## 项目结构

```text
ruankao-study-system/
├── backend/      Spring Boot REST API（MyBatis XML Mapper）
├── frontend/     Vue 3 单页应用
└── sql/          MySQL 初始化脚本
```

MyBatis 接口位于 `backend/src/main/java/com/sandy/ruankao/mapper`，对应 SQL 位于 `backend/src/main/resources/mapper`。

## 1. 初始化数据库

确保 MySQL 已启动，然后执行：

```bash
mysql -uroot -p < sql/init.sql
```

默认数据库名为 `ruankao_study`。后端默认读取以下环境变量：

```bash
export MYSQL_HOST=localhost
export MYSQL_PORT=3306
export MYSQL_DATABASE=ruankao_study
export MYSQL_USERNAME=root
export MYSQL_PASSWORD=你的密码
export APP_ADMIN_USERNAME=admin
export JWT_SECRET=至少32字节的生产密钥
# 以下软件身份信息会同步显示在登录页和“关于软件”中
export APP_SOFTWARE_NAME=知行软考学习与知识管理系统
export APP_SOFTWARE_VERSION=V1.0
export APP_SOFTWARE_DEVELOPER=邹军科
```

## 2. 启动后端

```bash
cd backend
mvn spring-boot:run
```

项目通过 `.mvn/maven.config` 自动加载 `settings.xml`，会绕开电脑上可能失效的 Maven 私服配置，直接使用 Maven Central。如果不是在 `backend` 目录执行 Maven，也可以显式运行 `mvn -s backend/settings.xml -f backend/pom.xml spring-boot:run`。

后端默认地址：`http://localhost:8080/api`

## 3. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端默认地址：`http://localhost:5173`

Vite 会把 `/api` 请求代理到 `http://localhost:8080`。

## 当前功能

- 用户注册、JWT 登录、修改密码与退出
- 管理员和普通用户权限、用户查询及账号启停
- 学习计划、章节、错题和仪表盘按账号隔离
- 登录/注册滑块验证和受保护路由
- BCrypt 密码加密与接口令牌鉴权
- 仪表盘和学习进度统计
- 学习计划创建、完成和删除
- 知识章节掌握度维护
- 错题录入、复习状态和删除
- 按章节随机选择题练习、统一交卷判分、成绩统计与错题自动收录
- 管理员题库维护和 Excel 模板批量导入
- 公开授权网页教程抓取、审核发布、小节学习与用户进度跟踪
- 挣值计算器
- 统计接口与图表

首次使用请注册与 `APP_ADMIN_USERNAME` 相同的用户名（默认 `admin`）作为管理员，其他账号自动成为普通用户。修改后端代码后需要重新启动 Spring Boot，启动时会自动升级旧数据库结构并将旧学习数据归属管理员。
