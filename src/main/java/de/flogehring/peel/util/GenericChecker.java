package de.flogehring.peel.util;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class GenericChecker {

    private GenericChecker() {
    }


    public static boolean isGeneric(Object o) {
        return isGeneric(o.getClass());
    }

    public static boolean isGeneric(Class<?> c) {
        boolean hasTypeParameters = hasTypeParameters(c);
        boolean hasGenericSuperclass = hasGenericSuperclass(c);
        // TODO
        //      boolean hasGenericSuperinterface = hasGenericSuperinterface(c);
        //      boolean isGeneric = hasTypeParameters || hasGenericSuperclass || hasGenericSuperinterface;
        return hasTypeParameters || hasGenericSuperclass;
    }

    public static boolean hasTypeParameters(Class<?> c) {
        return c.getTypeParameters().length > 0;
    }

    public static boolean hasGenericSuperclass(Class<?> c) {
        Class<?> testClass = c;

        while (testClass != null) {
            Type t = testClass.getGenericSuperclass();

            if (t instanceof ParameterizedType) {
                System.out.println(c.getName() + " hasGenericSuperclass: " + t.getClass().getName());
                return true;
            }

            testClass = testClass.getSuperclass();
        }

        return false;
    }

    public static boolean hasGenericSuperinterface(Class<?> c) {
        for (Type t : c.getGenericInterfaces()) {
            if (t instanceof ParameterizedType) {
                return true;
            }
        }
        return false;
    }
}
