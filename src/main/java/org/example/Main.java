package org.example;

// Press Shift twice to open the Search Everywhere dialog and type `show whitespaces`,
// then press Enter. You can now see whitespace characters in your code.

import org.example.utils.CacheObject;

import java.util.Date;

import static org.example.utils.CacheObject.Cache;
import static org.example.utils.CacheObject.Mutator;


class Ancestor1 {
    public int i_public1 = 502;
    protected int i_private1 = 501;

}

class Ancestor2 extends Ancestor1 {
    private final int i_private2 = 503;
    public int i_public2 = 504;

}

class Fraction extends Ancestor2 implements Fractionable {
    public int num;
    private int demum;


    public Fraction(int num, int demum) {
        this.num = num;
        this.demum = demum;
    }

    @Override
    @Cache(1500)
    public double doubleValueAndAddInt(int i) {
        return i + (double) num / demum;
    }

    @Override
    @Cache(1500)
    public int getNum() {
        return num;
    }

    @Override
    @Mutator
    public void setNum(int num) {
        this.num = num;
    }

    @Override
    @Cache(1500)
    public int getDenum() {
        return demum;
    }

    @Override
    @Mutator
    public void setDenum(int demum) {
        this.demum = demum;
    }
}

public class Main {

    public static void main(String[] args) throws InterruptedException {

        CacheObject.log(new Date().toString());
        CacheObject.log("currentTimeMillis=" + System.currentTimeMillis());

        System.currentTimeMillis();

        Fraction fr = new Fraction(2, 3);

        Fractionable num = CacheObject.cache(fr, false, false);

        num.setNum(2);
        num.setDenum(3);
        CacheObject.log("-----------------Вызов первый раз - вычисляем и заполняем кэш");
        num.doubleValueAndAddInt(1);// sout сработал
        CacheObject.log("-----------------Вызов второй раз - берем из кеша");
        num.doubleValueAndAddInt(1);// sout молчит

        fr.i_public1 = 901;
        CacheObject.log("-----------------Вызов третий раз - поменяли поле - кеша нет");
        num.doubleValueAndAddInt(1);// sout молчит
        fr.i_public1 = 502;
        CacheObject.log("-----------------вызов 4 раз - вернули поле - кеша есть");
        num.doubleValueAndAddInt(1);// sout молчит

        CacheObject.log("-----------------нет кэша");
        num.getNum();
        CacheObject.log("-----------------есть кэш");
        num.getNum();
        CacheObject.log("-----------------нет кэша");
        num.getDenum();
        CacheObject.log("-----------------есть кэш");
        num.getDenum();

        num.setNum(2222);
        CacheObject.log("-----------------нет кэша");
        num.getNum();
        CacheObject.log("-----------------есть кэш");
        num.getNum();
        CacheObject.log("-----------------нет кэша");
        num.getDenum();
        CacheObject.log("-----------------есть кэш");
        num.getDenum();

        CacheObject.log("-----------------меняем статус");

        num.setNum(5);
        CacheObject.log("-----------------Вызов первый раз - вычисляем и заполняем кэш");
        num.doubleValueAndAddInt(1);// sout сработал
        CacheObject.log("-----------------Вызов второй раз - берем из кеша");
        num.doubleValueAndAddInt(1);// sout молчит

        CacheObject.log("-----------------нет кэша");
        num.getNum();
        CacheObject.log("-----------------есть кэша");
        num.getNum();
        CacheObject.log("-----------------нет кэша");
        num.getDenum();
        CacheObject.log("-----------------есть кэша");
        num.getDenum();

        CacheObject.log("-----------------меняем статус -  возвращаем в начало");
        num.setNum(2);

        CacheObject.log("-----------------Должен быть кэш");
        num.doubleValueAndAddInt(1);// sout молчит
        CacheObject.log("-----------------Должен быть кэш");
        num.doubleValueAndAddInt(1);// sout молчит

        CacheObject.log("-----------------Должен быть кэш");

        num.doubleValueAndAddInt(1);// sout сработал
        CacheObject.log("-----------------Должен быть кэш");
        num.doubleValueAndAddInt(1);// sout молчит

        CacheObject.log("-----------------Должен быть кэш");
        num.getNum();
        CacheObject.log("-----------------Должен быть кэш");
        num.getNum();
        CacheObject.log("-----------------Должен быть кэш");
        num.getDenum();
        CacheObject.log("-----------------Должен быть кэш");
        num.getDenum();

        num.setNum(5);
        CacheObject.log("-----------------Должен быть кэш");
        num.doubleValueAndAddInt(1);// sout сработал
        CacheObject.log("-----------------Должен быть кэш");
        num.doubleValueAndAddInt(1);// sout молчит

        CacheObject.log(new Date().toString());
        Thread.sleep(4000);
        CacheObject.log("-----------------Вызов третий раз - кэш был очищен. считаем заново");
        num.doubleValueAndAddInt(1);// sout молчит

        CacheObject.log("currentTimeMillis=" + System.currentTimeMillis());


    }
}