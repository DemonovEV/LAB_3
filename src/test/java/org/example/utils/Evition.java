package org.example.utils;

import lombok.Setter;
import org.example.utils.CacheObject.Cache;
import org.example.utils.CacheObject.Clock;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

import static junit.framework.Assert.assertNotSame;
import static junit.framework.Assert.assertSame;

interface TestInterface {
    @Cache(1000)
    default Object Method1() {
        return new Object();
    }

    @Cache(1000)
    default Object Method2(Object arg1) {
        return new Object();
    }

}

class TestBlockingClock implements Clock {
    // Вариант с реализацией clock на основе блокирующи очередней  получился сложным
    // т.к. метод currentMillis вызыает не только  очищающий Thread
    // но так же и вызов проксирующи методов для того чтобы обновить кеш.
    // пришлось  становить clock только на момент старта thread. Чтобы убедится тчо она точно стартовала
    private final BlockingQueue<Long> time;
    private final BlockingQueue<Boolean> ack;

    public TestBlockingClock() {
        ack = new LinkedBlockingDeque<>(100);
        time = new LinkedBlockingDeque<>(100);
    }

    @Override
    public long currentMillis() {
        try {
            var t = time.take();
            ack.put(true);
            return t;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void awaitConsume(long t) {
        try {
            time.put(t);
            ack.take();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void NowaitConsume(long t) {
        try {
            time.put(t);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}

class TestClass implements TestInterface {
    @Setter
    int someValue = 0;

}

public class Evition {
    @Test
    void evictionCache() {
        // Ряд тестов для проерки кеширования

        TestClass ci = new TestClass();

        TestInterface proxy = CacheObject.cache(ci, false, false);

        var m1_v1 = proxy.Method1();
        var m2_v1 = proxy.Method2(500);

        var m1_v2 = proxy.Method1();
        var m2_v2 = proxy.Method2(500);

        assertSame(m1_v1, m1_v2); // Проверим что прокси вернула то что было в кеше

        var m2_v3 = proxy.Method2(1500);// Вызываем Method2 с другим аргументом.
        var m1_v3 = proxy.Method1(); // Повторный вызов Method1 - должен взять из кэша

        assertNotSame(m2_v2, m2_v3); // Прокси должна вернуть новый объект. не то  же что раньше
        assertSame(m1_v2, m1_v3); // Убедимся что повторный вызов Method1 вернул  предыдущий кэш

        ci.setSomeValue(600); // Меняем некую переменную состояния.  // кэш должен это почувствовать

        var m1_v4 = proxy.Method1();
        var m2_v4 = proxy.Method2(1);
        // кеш лолжен обновиться новыми состояниями проксируемого объекта
        assertNotSame(m2_v3, m2_v4);
        assertNotSame(m1_v3, m1_v4);

    }

    @Test
    void evictionCacheClear() {
        Clock clock = () -> 1000L; // Начинаем с простого clock наполнять кэш

        TestClass ci = new TestClass();
        // прокси создадим без очерди которая очищает.
        TestInterface proxy = CacheObject.cache(ci, false, false, clock, false);

        // Наполняем кэш
        var m1_v1 = proxy.Method1();
        var m1_v2 = proxy.Method1();

        assertSame(m1_v1, m1_v2);


        Proxy p = (Proxy) proxy;
        CacheObject.cach_InvocationHandler handler = (CacheObject.cach_InvocationHandler) Proxy.getInvocationHandler(p);

        // меняем часы на более слодные, которые обеспечат нашу уверенность в запуске джоба для очисики

        var clock2 = new TestBlockingClock();
        handler.setClock(clock2); // Зададим сложный clock c целью убедится быть уверенным что поток очистки будет работать.

        handler.StartCacheClearTread(); // Запускаем поток очистки
        clock2.awaitConsume(2200L);// Джоб по очистке точно запустит тут
        // т.к. мы получим ответ по вычитке из блокируещей очереди и основной поток продолжит работать.
        handler.setClock(() -> 2300L); // Отключаем тот сложный таймер т.к. за него зацепляются методы. Они же тоже тянут currentMillis
        clock2.NowaitConsume(2300L); // Ещё  для сложного clock даем данные, чтобы джоб проснулся если успел запрость по clock2

        var m1_v3 = proxy.Method1(); // Повторно вызываем кэш
        // т.к. время жизни кеша настпуило. Джоб должен его почистить.
        assertNotSame(m1_v1, m1_v3);


    }


}


