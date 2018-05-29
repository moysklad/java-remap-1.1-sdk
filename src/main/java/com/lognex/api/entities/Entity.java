package com.lognex.api.entities;

import java.lang.reflect.Field;

/**
 * Класс сущности
 */
public abstract class Entity implements Cloneable {
    /**
     * Присваивает полям этой сущности значения полей другой сущности
     */
    public <T extends Entity> void set(T other) {
        if (other.getClass() != this.getClass()) {
            throw new RuntimeException("Невозможно установить поля объекта класса " + other.getClass().getSimpleName() + " объекту класса " + this.getClass().getSimpleName() + "!");
        }

        for (Field field : other.getClass().getFields()) {
            try {
                Object o = field.get(other);
                if (o instanceof Entity) {
                    o = clone((Entity) o);
                }
                field.set(this, o);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * Клонирует сущность, создавая новые объекты для полей со
     * значениями оригинальной сущности
     */
    public static <T extends Entity> T clone(T original) {
        try {
            T clone = (T) original.clone();
            clone.set(original);
            return clone;
        } catch (CloneNotSupportedException e) {
            /*
                Так как метод клонирования вызывается у объекта-наследника Entity,
                который в свою очередь является наследником Clonable, то данная
                ошибка вызываться не будет.
             */
            e.printStackTrace();
            return original;
        }
    }
}