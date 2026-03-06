# MusicTools Git 操作指南

> 仓库地址：https://github.com/ThinkOS1023/MusicTools.git
> 主分支：`master`

---

## 一、基础配置

```bash
# 查看当前配置
git config --list

# 查看远程仓库
git remote -v
```

---

## 二、拉取（Pull）

```bash
# 拉取远程 master 并合并到当前分支
git pull origin master

# 拉取所有远程分支（不合并）
git fetch origin

# 拉取后用 rebase 代替 merge（保持线性历史）
git pull --rebase origin master
```

---

## 三、提交（Commit）

```bash
# 查看工作区状态
git status

# 查看具体改动
git diff

# 暂存指定文件（推荐，避免误提交 .env 等敏感文件）
git add app/src/main/java/com/example/SomeFile.kt

# 暂存所有改动（谨慎使用）
git add -A

# 提交（附上协作者标注）
git commit -m "$(cat <<'EOF'
feat: 描述本次改动的内容

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>
EOF
)"

# 查看提交历史
git log --oneline -10
```

---

## 四、推送（Push）

```bash
# 推送到远程 master
git push origin master

# 首次推送并设置上游跟踪
git push -u origin master

# 推送新分支
git push -u origin feature/your-branch-name
```

> **警告：** 不要对 `master` 执行 `git push --force`，会覆盖远程历史。

---

## 五、回退（Rollback）

### 5.1 撤销工作区未暂存的修改

```bash
# 撤销单个文件的改动
git restore path/to/file.kt

# 撤销所有未暂存改动（不可逆）
git restore .
```

### 5.2 撤销已暂存的文件（保留修改）

```bash
git restore --staged path/to/file.kt
```

### 5.3 回退到上一次提交（保留改动在工作区）

```bash
git reset HEAD~1
```

### 5.4 回退到指定提交（保留改动在工作区）

```bash
# 先查看提交 hash
git log --oneline -10

# 回退（soft：改动保留在暂存区）
git reset --soft <commit-hash>

# 回退（mixed：改动保留在工作区，默认）
git reset --mixed <commit-hash>
```

### 5.5 硬回退（丢弃所有改动，危险！）

```bash
# 回退到指定提交，工作区改动全部丢失
git reset --hard <commit-hash>

# 回退到远程最新状态
git reset --hard origin/master
```

### 5.6 用 revert 安全撤销已推送的提交（推荐）

```bash
# 生成一个新的"撤销提交"，不改写历史
git revert <commit-hash>

# 撤销最近一次提交
git revert HEAD
```

---

## 六、分支操作

```bash
# 查看所有分支
git branch -a

# 创建并切换到新分支
git checkout -b feature/new-feature

# 切换分支
git checkout master

# 删除本地分支
git branch -d feature/done-branch

# 删除远程分支
git push origin --delete feature/done-branch
```

---

## 七、本项目常用提交流程

```bash
# 1. 拉取最新代码
git pull origin master

# 2. 暂存改动的文件
git add app/src/main/java/...

# 3. 提交
git commit -m "feat/fix/refactor: 描述改动"

# 4. 推送
git push origin master
```

---

## 八、常用 Commit 类型前缀

| 前缀 | 含义 |
|------|------|
| `feat` | 新功能 |
| `fix` | Bug 修复 |
| `refactor` | 代码重构（不改功能） |
| `docs` | 文档更新 |
| `chore` | 构建/依赖/配置调整 |
| `test` | 测试相关 |
| `style` | 代码格式（不影响逻辑） |

---

## 九、紧急情况速查

| 场景 | 命令 |
|------|------|
| 刚提交发现写错了提交信息 | `git commit --amend -m "新消息"` |
| 刚推送发现有 bug，需要撤回 | `git revert HEAD` 然后 `git push` |
| 误删文件想恢复 | `git restore path/to/file` |
| 查看某次提交改了什么 | `git show <commit-hash>` |
| 查看某文件的改动历史 | `git log --follow -p path/to/file` |
