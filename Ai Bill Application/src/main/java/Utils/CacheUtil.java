package Utils;

import com.github.benmanes.caffeine.cache.*;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * Generic Cache Utility Class (Supports generics and exception propagation through the loader)
 * @param <K> Key type
 * @param <V> Value type
 * @param <E> Exception type (e.g., IOException, which the loader function might throw)
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
     * Gets the cached value.
     * If the key is not in the cache, the loader function will be called to compute it.
     * @param key The key whose associated value is to be returned.
     * @return The value associated with the key.
     */
    public V get(K key) {
        return cache.get(key);
    }

    /**
     * Manually updates the cache with a new value for a given key.
     * @param key The key with which the specified value is to be associated.
     * @param value The value to be associated with the specified key.
     */
    public void put(K key, V value) {
        cache.put(key, value);
    }

    /**
     * Manually removes the entry for a key from the cache.
     * @param key The key whose mapping is to be removed from the cache.
     */
    public void invalidate(K key) {
        cache.invalidate(key);
    }
}


