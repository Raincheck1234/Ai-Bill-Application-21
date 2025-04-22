package Utils;

import com.github.benmanes.caffeine.cache.*;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * 通用缓存工具类（支持泛型和异常传播）
 * @param <K> 键类型
 * @param <V> 值类型
 * @param <E> 异常类型（如 IOException）
 */
public class CacheUtil<K, V, E extends Exception> {
    private final LoadingCache<K, V> cache;

    public CacheUtil(Function<K, V> loader,
                     int maxSize,
                     long expireAfterWrite,
                     long refreshAfterWrite) {

        this.cache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(expireAfterWrite, TimeUnit.MINUTES)
                .refreshAfterWrite(refreshAfterWrite, TimeUnit.MINUTES)
                .build(loader::apply); // 关键修改：使用build(loader)创建LoadingCache
    }

    /**
     * 获取缓存值
     */
    public V get(K key) {
        return cache.get(key);
    }

    /**
     * 手动更新缓存
     */
    public void put(K key, V value) {
        cache.put(key, value);
    }

    /**
     * 手动移除缓存
     */
    public void invalidate(K key) {
        cache.invalidate(key);
    }
}


