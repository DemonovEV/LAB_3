package org.example.utils.key;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Класс, описывающий ключ кэша.
 * Инкапсулирует в себе:
 * <p>
 * 1.1) Коллекции вызванных методов Mutable c их параметрами
 * <p>
 * 1.2) MAP с полями и значениями полей. Это новая вводная появилась после того как уже сделал задачу
 * <p>
 * 2) Информацию о вызванном кешируемом методе
 * <p>
 * Этот набор должен давать уникальный хэш.
 * <p>
 * Так же содержит в себе MAP кэшев со значениями и обеспечивает помпещение и извлечение значения из кеша.
 * Ко всем обрабатывает в отдельном потоке параметр "время жизни"
 */

public class CacheKey {

    private CallMethodWithArgs cacheMethodCall; // Часть ключа, формируемая вызовом функции cache с ее параметрами
    private Set<CallMethodWithArgs> mutatorMethodCalls; // часть ключа - набор вызовов методов Mutator с параметрами
    private Map<Field, Object> fields;

    public CacheKey() {
    }

    public CacheKey(CacheKey stateKey) {
        if (stateKey != null) {
            if (stateKey.mutatorMethodCalls != null)
                this.mutatorMethodCalls = Set.copyOf(stateKey.mutatorMethodCalls);// При создании ключа из другого ключа. Коллекцию mutatorMethodCalls копируем
            if (stateKey.fields != null) this.fields = Map.copyOf(stateKey.fields);
            this.cacheMethodCall = stateKey.cacheMethodCall;
        }
    }

    public void setCacheMethodCall(CallMethodWithArgs cacheKey) {
        cacheMethodCall = cacheKey;
    }

    public void addMutator(Method method, Object[] agrs) {
        if (mutatorMethodCalls == null) mutatorMethodCalls = new HashSet<>();
        mutatorMethodCalls.add(new CallMethodWithArgs(method, agrs));
    }

    public void addFieldValue(Field field, Object value) {
        if (fields == null) fields = new HashMap<>();
        fields.put(field, value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CacheKey cacheKey)) return false;

        if (!Objects.equals(cacheMethodCall, cacheKey.cacheMethodCall)) return false;
        if (!Objects.equals(mutatorMethodCalls, cacheKey.mutatorMethodCalls)) return false;
        return Objects.equals(fields, cacheKey.fields);
    }

    @Override
    public int hashCode() {
        int result = cacheMethodCall != null ? cacheMethodCall.hashCode() : 0;
        result = 31 * result + (mutatorMethodCalls != null ? mutatorMethodCalls.hashCode() : 0);
        result = 31 * result + (fields != null ? fields.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "CacheKey.hashCode()='" + this.hashCode() + "'{" + "cacheMethodCall=" + cacheMethodCall + ", mutatorMethodCalls=" + mutatorMethodCalls + ", fields=" + fields + '}';
    }

    public String oldString() {
        return super.toString();
    }


}
