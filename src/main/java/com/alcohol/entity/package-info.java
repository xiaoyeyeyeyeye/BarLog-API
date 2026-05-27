/**
 * 数据库表实体，与 {@code scripts/schema-full.sql} 中表名一一对应。
 * <p>JSON 数组字段（如 moodTags）在库中为 TEXT，读写时经 {@link com.alcohol.util.JsonUtil} 转换。</p>
 */
package com.alcohol.entity;
