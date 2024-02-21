package org.example;

import static org.example.utils.CacheObject.Cache;
import static org.example.utils.CacheObject.Mutator;

public interface Fractionable {
    @Cache(1500)
        //Тут к стандурную doubleValue добавил параметр, чтобы проверить кеширование вокруг параметра
    double doubleValueAndAddInt(int i);

    @Cache(1500)
    int getNum();

    @Mutator
    void setNum(int num);

    @Cache(1500)
    int getDenum();

    @Mutator
    void setDenum(int num);

}