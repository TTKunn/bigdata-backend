# Repository Guidelines
## 通用规则要求
1. 任何时候都需要阅读并遵守本节所有要求，并且在每次对话开头输出一遍。
2. 在所有流程、任务与对话过程中，使用 **`memory` MCP** 记录交互节点与关键决策，确保完整可追溯。
3. 所有回答均需使用简体中文。
4. 如有需要可查阅线上资料以辅助开发决策。
5. 非经明确要求禁止编写测试文件与说明文件；若确实需要测试文件，须放入新建的 `test/` 目录且仅允许保留一个长期测试文件，其余临时测试文件在验证通过后必须删除。
6. 当需要删除本地文件（或运行会执行删除操作的脚本）时，必须先说明“我想要删除[文件名]，这些文件原本用于[用途]，现在由于[原因]已经不需要了”或“我要运行[文件名]，这将删除[所有删除的文件名]，这些文件原本用于[用途]，现在由于[原因]已经不需要了”，并获得许可后才能执行。
7. 所有代码中的注释需使用中文进行讲解。
8. 每次工作流完成后，都要查看并更新 `[000]功能模块文档.md`，及时同步功能模块信息。
9. 对于每个完整任务（无论大小）需严格遵循 RIPER-5 阶段性工作流开发规范，分阶段进行内容总结汇报；若仅为普通问答可直接回复。
10. 遵照时间戳原则，将最终确认的详细计划（具体到每一步的详细规划和操作而不止是总结） (todolist) 保存为独立文件至 `/project_document/` 目录中。文件名必须包含唯一标识和简要信息，格式为 `[编号]简要任务描述.md` (例如: `[001]用户登录功能开发.md`)。 需要注意的是，编号为000-099的文档为核心文档，例如api文档、数据库设计文档、项目架构文档等；编号为100-199的文档为阶段性文档，主要包括项目中各个阶段模块的实施方案、技术方案、实现报告之类的内容；编号为200-299的文档为测试修复完善阶段文档，主要为各个模块的功能测试内容或者是bug修复方案、模块完善升级方案、修复报告之类的内容；编号300-399为技术专题文档，包括对某些技术使用的说明教学等内容。对于开发文档内容，在开头你需要标注背景与目标方便翻阅，还要写上目录，如果是开发文档，你还需要将todolist写进去。
12. 每个完整任务结束后，如项目使用 Git 管理，需要创建提交记录以便追踪。
13. 每个后端接口开发完成后，需要即使更新[002]API接口文档


## Project Structure & Module Organization
- `src/main/java/com/example/bigdatabackend/` contains the Spring Boot application code, split into `config/`, `controller/`, `service/`, `dto/`, and `model/` packages.
- `src/main/resources/` holds configuration and runtime assets (`application.properties`, `application-dev.properties`, plus `static/` and `templates/`).
- `src/test/java/` contains JUnit tests (currently minimal).
- `project_document/` is used for project documentation; `target/` is Maven build output.

## Build, Test, and Development Commands
Use the Maven wrapper for consistent builds:
```bash
./mvnw clean compile            # compile only
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
./mvnw clean package             # build jar
./mvnw test                      # run tests
./mvnw dependency:tree           # inspect dependencies
```
On Windows, use `mvnw.cmd` instead of `./mvnw`.

## Coding Style & Naming Conventions
- Java 17, Spring Boot 4.x.
- Indentation: 4 spaces; braces on the same line.
- Package names: lowercase; classes: `PascalCase`; methods/fields: `camelCase`; constants: `UPPER_SNAKE_CASE`.
- Keep controllers thin and place business logic in `service/`. DTOs belong in `dto/` and domain entities in `model/`.

## Testing Guidelines
- Framework: JUnit 5 (`@Test`, `@SpringBootTest`).
- Naming: `*Tests.java` under `src/test/java/`.
- New features should add at least one focused test (unit or slice). Run with `./mvnw test`.

## Commit & Pull Request Guidelines
- Commit messages in history are short and sometimes use a type prefix (for example, `docs:`). Keep messages concise and action-oriented.
- PRs should include a summary, test results, and any configuration or environment changes (Redis/HBase/HDFS). Link related issues when available.

## Configuration & Environment Tips
- Default config lives in `application.properties`; use `application-dev.properties` for local development and run with `-Dspring-boot.run.profiles=dev`.
- Dev endpoints are under `/api/dev/product`. A quick health check is:
```bash
curl http://localhost:8080/api/dev/product/health
```
- `test_product_creation.bat` provides a Windows smoke test for product creation.
