package org.example.utils;

import lombok.Setter;
import org.example.utils.key.CacheKey;
import org.example.utils.key.CallMethodWithArgs;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CacheObject {
    private static final Logger logger;

    static {
        logger = Logger.getLogger(CacheObject.class.getName());
        logger.setLevel(Level.ALL);
        //   logger = null;
    }

    static public <T> T cache(T oriObject, boolean useMutableForCache, boolean useCacheForPrivateFields, Clock clock, boolean autoStartCacheClearTread) {
        var cachInvocationHandler = new cach_InvocationHandler(oriObject, useMutableForCache, useCacheForPrivateFields, clock, autoStartCacheClearTread);

        return (T) Proxy.newProxyInstance(//
                oriObject.getClass().getClassLoader(), //
                oriObject.getClass().getInterfaces(), //
                cachInvocationHandler);

    }

    static public <T> T cache(T oriObject, boolean useMutableForCache, boolean useCacheForPrivateFields, Clock clock) {
        var cachInvocationHandler = new cach_InvocationHandler(oriObject, useMutableForCache, useCacheForPrivateFields, clock, true);

        return (T) Proxy.newProxyInstance(//
                oriObject.getClass().getClassLoader(), //
                oriObject.getClass().getInterfaces(), //
                cachInvocationHandler);

    }

    static public <T> T cache(T oriObject, boolean useMutableForCache, boolean useCacheForPrivateFields) {
        return cache(oriObject,//
                useMutableForCache,//
                useCacheForPrivateFields,//
                System::currentTimeMillis,//
                true);
    }

    public static void log(String str) {
        if (logger != null) logger.info(str);
        else System.out.println(str);
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Cache {
        long value() default 0;
    }

    interface Clock {
        long currentMillis();
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Mutator {
    }

    static class cach_InvocationHandler<T> implements InvocationHandler, Runnable {

        private final long sleepTime = 10;
        T hadledObject;
        boolean autoStartCacheClearTread;
        boolean useMutableForCache;
        boolean useCacheForPrivateFields;
        // Получилось 3 измерения
        // первое измерение - текущее Состояние заданное Mutable
        // Второе измерение - Функция для которой кешируется вместе с её параметрамиж
        // Третье измерение - закешированное значение функции+время
        CacheKey currentCacheKey;
        @Setter
        Clock clock;
        Map<CacheKey, Value> cacheMap; // Это сам кеш.


        public cach_InvocationHandler(T hadledObject, boolean useMutableForCache, boolean useCacheForPrivateFields, Clock clock, boolean autoStartCacheClearTread) {
            this.hadledObject = hadledObject;
            this.useMutableForCache = useMutableForCache;
            this.autoStartCacheClearTread = autoStartCacheClearTread;
            this.currentCacheKey = new CacheKey();// Это переменная описывает текущее состояние кэша.
            this.useCacheForPrivateFields = useCacheForPrivateFields;
            this.clock = clock;
        }


        @Override
        public void run() {

            var alive = true;
            while (alive) {

                cacheMap.forEach((stateKey, value) -> {

                    if (value.timeExpired(clock)) {
                        CacheObject.log("Удаление кеша " + stateKey);
                        cacheMap.remove(stateKey);
                    }
                });

                try {
                    if (sleepTime > 0) Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    alive = false;
                }
            }
        }

        public void StartCacheClearTread() {
            var th = new Thread(this);
            th.setDaemon(true);
            th.start();
        }


        public Value getCacheValue(CacheKey key) {
            if (cacheMap == null) return null;
            return cacheMap.get(key);


        }

        public void putCacheValue(CacheKey key, Value value) {

            if (cacheMap == null) {
                cacheMap = new ConcurrentHashMap<>();//ConcurrentHashMap<>();IdentityHashMap
                if (autoStartCacheClearTread) StartCacheClearTread();
            }

            var keyForCache = new CacheKey(key);

            CacheObject.log("Сохраняем кэш для " + key);

            cacheMap.put(keyForCache, value);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            synchronized (proxy) {

                // Тут в прокси method - это ссылка не метод интерфйса, а не проксируемого класса.
                // Поэтому надо найти ссылку на перекрытый метод объекта для которого применена proxy.
                Method methForCheckAnnotation = hadledObject.getClass().getMethod(method.getName(), method.getParameterTypes());

                var cacheAnnotation = methForCheckAnnotation.getAnnotation(Cache.class);
                updateCacheFromObjectFields(); // Сначала была мысль для методов без Cache собирать значения полей. потом подуиал, что перед вызовом метода с Cache вызову

                if (cacheAnnotation != null) {

                    log("methForCheckAnnotation.getAnnotation(Cache.class)=" + methForCheckAnnotation.getAnnotation(Cache.class).value());
                    var ThisMethodCallKey = new CallMethodWithArgs(method, args); /// Это ключ для текущей кешируемой функции и для ее текущих параметров

                    currentCacheKey.setCacheMethodCall(ThisMethodCallKey); // Обновим текущее состояние инофрмцией о функции и ее параметраъ, которые собиремся кешировать

                    var cacheForCurrentState = getCacheValue(currentCacheKey);// Найдем MAP-кэш для текущего состояния и функции

                    if (!Objects.isNull(cacheForCurrentState)) // кэш нашли - ищем значит дальше для текущего ключа CacheKeyThisMethodCall
                    {
                        log("Есть кеш для текущего состояния  (" + currentCacheKey + "). ");
                        log("Значение " + cacheForCurrentState);

                        cacheForCurrentState.updateTimeLife(clock);
                        return cacheForCurrentState.getValue();
                    }

                    log("НЕТ кеша для текущего состояния  " + currentCacheKey);
                    // log("Вызываем функцию " + ThisMethodCallKey);
                    // Сюда пришли так как не нашли кеша.
                    // Значит вызываем и после кешируем.
                    var result = method.invoke(hadledObject, args);
                    putCacheValue(currentCacheKey, new Value(result, cacheAnnotation.value(), clock));// кладем в кэш состояние

                    return result;
                } else {
                    if (methForCheckAnnotation.getAnnotation(Mutator.class) != null && useMutableForCache)
                        currentCacheKey.addMutator(method, args); // корректируем информацию о состоянии //currentCash.clear();
                    // log("Вызываем функцию MUTABLE " + method.getName() + " состояние объекта " + currentCacheKey);
                    return method.invoke(hadledObject, args);
                }
                //   throw new RuntimeException("Непонятно как обрабатывать метод " + method.getName());
            }
        }

        private void updateCacheFromObjectFields() throws IllegalAccessException {
            var class_loop = hadledObject.getClass();
            while (class_loop != null) {
                for (Field field : class_loop.getDeclaredFields()) {
                    int modifiers;
                    modifiers = field.getModifiers();
                    if (!useCacheForPrivateFields) {
                        if (Modifier.isPrivate(modifiers) || Modifier.isProtected(modifiers)) {
                            continue;
                        }
                    }
                    field.setAccessible(true);
                    currentCacheKey.addFieldValue(field, field.get(hadledObject));
                }
                class_loop = class_loop.getSuperclass();
            }
        }
    }
}