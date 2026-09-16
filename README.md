# 养老院 · 房间床位与护理管理系统

养老院的日常台账：**房间与床位**、**老人档案**、**护理班次**、**药品代管与发放**。

## 技术栈

Spring Boot 3.3（Java 17）+ MySQL 8.0 + Redis 7 + Vue 3 + Element Plus + Vite + nginx，全栈 `docker compose` 一键启动。

## 启动

```bash
./start.sh              # 等价于 docker compose up -d --build
```

| 入口 | 地址 |
| --- | --- |
| 前端页面 | http://127.0.0.1:8207/ |
| 后端接口 | http://127.0.0.1:8307/api/ |
| MySQL | 127.0.0.1:3507（库 `nursing_home`） |
| Redis | 127.0.0.1:6507 |

## 停止

```bash
docker compose down       # 保留数据卷
docker compose down -v    # 连数据卷一起删，下次启动重新灌种子数据
```

容器名统一是 `claude-qd-007-{mysql,redis,backend,frontend}`。

## 业务模块

### 1. 房间与床位台账（`room` / `bed`）

房间编号 `R-xxx` 唯一，房型分单人间 / 双人间 / 多人间，带「可住人数」，状态 `在用 / 停用 / 维修`。
床位编号 `BD-xxxx` 唯一，归属到某个房间，朝向靠窗 / 靠门 / 中间，状态 `空闲 / 占用 / 停用`。

- 房间里还住着老人时**不许停用或维修**，也不许把可住人数改到比在住人数小；当天还有没交接完的护理班次时也停不了。
- 床位上有在住的老人时，**不许停用、也不许挪到别的房间**；停用或维修中的房间加不了床位。

- 页面：房间与床位（`/rooms`）
- 接口：`GET/POST /api/rooms`、`PUT /api/rooms/{id}`、`GET/POST /api/beds`、`PUT /api/beds/{id}`

### 2. 老人档案与入住（`resident`）

档案号 `LA-xxxx` 唯一，记姓名、性别、年龄、护理等级（自理 / 半自理 / 不能自理）、家属电话。
状态 `在住 / 已退住 / 请假外出`。

办入住时校验：**必须指定床位**、床位得是空闲的、床得属于选的那间房、房间得在用、
**房间在住人数不能超过可住人数**、同一张床不能住两个人；人多时自动把床置为占用。

- 转床：把老人挪到另一张床，旧床自动释放回空闲。
- 办退住：床位释放回空闲，写退住日期；**已经退住的老人改不回在住**。
- 请假外出：床位仍然占着（人还在院里）。

- 页面：老人档案（`/residents`）
- 接口：`GET/POST /api/residents`、`PUT /api/residents/{id}`

### 3. 护理班次（`care_shift`）

班次号 `HS-xxxx` 自动生成。一条班次 = 某间房 + 某天 + 一个班次（早 / 中 / 夜）+ 一位护理员 + 一段时段。
排班时校验：房间必须在用、结束时间晚于开始时间、**同一间房同日同时段不重叠**、
**同一位护理员同日同时段也不能同时守两间房**。

状态机 `待接班 → 值班中 → 已交班`，前两态可以取消；**交班必须写清注意事项**，接班的才知道老人情况。

- 页面：护理班次（`/shifts`）
- 接口：`GET/POST /api/shifts`、`POST /api/shifts/{id}/advance?action=&handoverNote=`

### 4. 药品代管与发放（`medicine` / `medicine_issue`）

药品编号 `MD-xxxx` 唯一，单位片 / 支 / 袋 / 毫升，类别处方药 / 非处方，带库存与预警线，状态 `在用 / 停用`。
发放流水记「哪位老人、哪种药、几剂、早中晚」，类型 `发放 / 退回`。

发放时校验：老人不能是已退住的、药品得在用、**库存不够拦下**、
**同一位老人同一天同一种药的同一剂次不能重复发放**；退回时**不能退超过净发出去的量**（发放减退回）。
发放扣库存、退回加库存。

- 页面：药品与发放（`/medicines`）
- 接口：`GET/POST /api/medicines`、`PUT /api/medicines/{id}`、`GET/POST /api/medicine-issues`

## 目录

```
backend/src/main/java/com/nursing/home/
├── config/       CORS 配置
├── controller/   REST 入口
├── dto/          BizException + 统一错误响应
├── entity/       6 张业务表
├── repository/   Spring Data JPA
└── service/      业务规则（编号唯一、床位占用与容量、班次时段、发药与库存）
backend/src/main/resources/schema.sql   建表 + 种子数据（挂进 MySQL initdb）
frontend/src/views/                     4 个业务页面
```
