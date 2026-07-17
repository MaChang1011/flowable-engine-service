# 数据库凭证

## MySQL (flowable-mysql-test)

| 项目 | 值 |
|------|------|
| 主机 | 36.133.114.164 |
| 端口 | 3307 |
| 数据库 | flowable_db |
| root 用户 | root / `Flow@2026#Db!Str0ng` |
| 应用用户 | flowable / `Flow@2026#Fable!9` |

## 远程连接

```bash
mysql -h 36.133.114.164 -P 3307 -u flowable -p'Flow@2026#Fable!9' flowable_db
```

## 安全说明

- root 密码仅用于管理，应用使用 flowable 用户
- 生产部署前请更换密码
- 建议配置防火墙限制来源 IP
