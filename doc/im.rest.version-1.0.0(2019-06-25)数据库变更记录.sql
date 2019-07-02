#创建数据库(im)
CREATE DATABASE IF NOT EXISTS im;

#切换数据库
USE im;

#im群组
CREATE TABLE im_group
(
    id                   INTEGER PRIMARY KEY AUTO_INCREMENT
        COMMENT '群组id,主键',
    name                 VARCHAR(64) NOT NULL
        COMMENT '群组名称',
    destination          VARCHAR(64) COMMENT '目的地',
    latitude             DECIMAL(18, 6) COMMENT '目的地纬度(gps)',
    longitude            DECIMAL(18, 6) COMMENT '目的地经度(gps)',
    password             VARCHAR(64) NOT NULL
        COMMENT '口令',
    password_expire_time DATETIME    NOT NULL
        COMMENT '口令过期时间',
    max_member           TINYINT     NOT NULL DEFAULT 10
        COMMENT '最大成员数量,默认10',
    member_count         TINYINT     NOT NULL DEFAULT 1
        COMMENT '群成员数',
    creator_id           INTEGER     NOT NULL
        COMMENT '群主id',
    create_time          DATETIME    NOT NULL DEFAULT NOW()
        COMMENT '创建时间',
    update_time          DATETIME    NOT NULL DEFAULT NOW()
        COMMENT '更新时间'
);

#im群成员
CREATE TABLE im_group_member
(
    id          INTEGER PRIMARY KEY AUTO_INCREMENT
        COMMENT '主键',
    uid         INTEGER     NOT NULL
        COMMENT '用户id',
    group_id    INTEGER     NOT NULL
        COMMENT '群组id',
    nickname    VARCHAR(64) NOT NULL
        COMMENT '昵称',
    avatar      VARCHAR(255) COMMENT '头像',
    is_owner    TINYINT     NOT NULL DEFAULT 0
        COMMENT '是否群主(0:成员;1:群主),默认值为0',
    create_time DATETIME    NOT NULL DEFAULT NOW()
        COMMENT '创建时间',
    update_time DATETIME    NOT NULL DEFAULT NOW()
        COMMENT '更新时间'
);