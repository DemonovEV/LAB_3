package org.example.utils;

import lombok.Getter;
import org.example.utils.CacheObject.Clock;

/**
 * Класс описывает значение кэшируемой функции и время жизни этого кэша.
 * <p>
 * Используется как значение в ConcurrentHashMap.
 */

@Getter
public class Value {
    static private final long NO_NEED_TO_KILL = 0;

    private final Object value;
    private final long cacheTime;
    private long timeToKill;

    Value(Object value, long cacheTime, Clock clock) {
        this.cacheTime = cacheTime;
        if (cacheTime < 0) throw new IllegalArgumentException("Время жизни кэша не может быть отрицательным");
        updateTimeLife(clock);
        this.value = value;
    }

    synchronized void updateTimeLife(Clock clock) {
        if (this.cacheTime == NO_NEED_TO_KILL) this.timeToKill = NO_NEED_TO_KILL;
            // Немного тавтология получилась, но предпочитаю оставлять так
            // чтоб во-первых прослеживалась идея, во вторых так код не будет  деградировать при уточнении условия
        else this.timeToKill = clock.currentMillis() + cacheTime;//  расчитаем крайнее время жизни
    }

    public boolean timeExpired(Clock clock) {
        if (this.timeToKill == NO_NEED_TO_KILL) return false;
        return this.timeToKill <= clock.currentMillis();
    }

    @Override
    public String toString() {
        return "Value{" + "value=" + value +
                //   ", timeToKill=" + timeToKill +
                '}';
    }
}