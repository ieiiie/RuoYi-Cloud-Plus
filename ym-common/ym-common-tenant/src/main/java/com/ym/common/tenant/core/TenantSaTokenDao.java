package com.ym.common.tenant.core;

import com.ym.common.core.constant.GlobalConstants;
import com.ym.common.redis.utils.RedisUtils;
import com.ym.common.satoken.core.dao.PlusSaTokenDao;

import java.time.Duration;
import java.util.List;

/**
 * Sa-Token 认证数据持久层，令认证会话保持在全局 Redis 命名空间。
 *
 * @author Lion Li
 */
public class TenantSaTokenDao extends PlusSaTokenDao {

    @Override
    public String get(String key) {
        return super.get(GlobalConstants.GLOBAL_REDIS_KEY + key);
    }

    @Override
    public void set(String key, String value, long timeout) {
        super.set(GlobalConstants.GLOBAL_REDIS_KEY + key, value, timeout);
    }

    @Override
    public void update(String key, String value) {
        // 更新认证值时保留 Redis 原始 TTL，不能将取整后的秒数重新设为有效期。
        super.update(GlobalConstants.GLOBAL_REDIS_KEY + key, value);
    }

    @Override
    public void delete(String key) {
        super.delete(GlobalConstants.GLOBAL_REDIS_KEY + key);
    }

    @Override
    public long getTimeout(String key) {
        return super.getTimeout(GlobalConstants.GLOBAL_REDIS_KEY + key);
    }

    @Override
    public void updateTimeout(String key, long timeout) {
        if (timeout == NEVER_EXPIRE) {
            long expire = getTimeout(key);
            if (expire != NEVER_EXPIRE) {
                set(key, get(key), timeout);
            }
            return;
        }
        RedisUtils.expire(GlobalConstants.GLOBAL_REDIS_KEY + key, Duration.ofSeconds(timeout));
    }

    @Override
    public Object getObject(String key) {
        return super.getObject(GlobalConstants.GLOBAL_REDIS_KEY + key);
    }

    @Override
    public <T> T getObject(String key, Class<T> classType) {
        return super.getObject(GlobalConstants.GLOBAL_REDIS_KEY + key, classType);
    }

    @Override
    public void setObject(String key, Object object, long timeout) {
        super.setObject(GlobalConstants.GLOBAL_REDIS_KEY + key, object, timeout);
    }

    @Override
    public void updateObject(String key, Object object) {
        super.updateObject(GlobalConstants.GLOBAL_REDIS_KEY + key, object);
    }

    @Override
    public void deleteObject(String key) {
        super.deleteObject(GlobalConstants.GLOBAL_REDIS_KEY + key);
    }

    @Override
    public long getObjectTimeout(String key) {
        return super.getObjectTimeout(GlobalConstants.GLOBAL_REDIS_KEY + key);
    }

    @Override
    public void updateObjectTimeout(String key, long timeout) {
        if (timeout == NEVER_EXPIRE) {
            long expire = getObjectTimeout(key);
            if (expire != NEVER_EXPIRE) {
                setObject(key, getObject(key), timeout);
            }
            return;
        }
        RedisUtils.expire(GlobalConstants.GLOBAL_REDIS_KEY + key, Duration.ofSeconds(timeout));
    }

    @Override
    public List<String> searchData(String prefix, String keyword, int start, int size, boolean sortType) {
        return super.searchData(GlobalConstants.GLOBAL_REDIS_KEY + prefix, keyword, start, size, sortType);
    }

}
