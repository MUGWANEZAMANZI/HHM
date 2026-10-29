package com.hhm.util;

import java.util.HashMap;
import java.util.List;



public class  AppCache {

    public static final HashMap<Class<?>, List<?>> cache = new HashMap<>();


    public static <T> void put(Class<T> type, List<T> data)
    {
        cache.put(type, data);
    }

    /// @param type
    /// @param <T>
    /// @return Object of type T from cache, or null if not found
    public static <T> List<T> getAll(Class<T> type)
    {
        return (List<T>) cache.get(type);
    }

    public static <T> void invalidate(Class<T> type) {
        cache.remove(type);
    }
}
