package org.example.utils.key;

import lombok.AllArgsConstructor;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;

/**
 * Этот класс задает образ вызова метода с его аргументами.
 * Используется как ключ для понятия "вызов функции".
 * <p>
 * Состава ключа:
 * <p>
 * 1) Ссылка на метод
 * <p>
 * 2) Аргументы вызова метода. Порядок имеет значения в hashCode.
 * <p>
 * Массив Аргументы не копируется в класс CacheKey, достаточно ссылки на массив.
 * <p>
 * От класса требуется реализация hashCode и equals.
 * т.к. Класс далее используется для описания ключа кэша в CacheKey
 */

@AllArgsConstructor
public class CallMethodWithArgs {

    private Method method;
    private Object[] arguments;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CallMethodWithArgs that)) return false;

        if (!Objects.equals(method, that.method)) return false;
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        return Arrays.equals(arguments, that.arguments);
    }

    @Override
    public int hashCode() {
        int result = method != null ? method.hashCode() : 0;
        result = 31 * result + Arrays.hashCode(arguments);
        return result;
    }

    @Override
    public String toString() {
        return "MethodCall.hashCode()='" + this.hashCode() + "'{" + "method=" + method + ", arguments=" + Arrays.toString(arguments) + '}';
    }
}
