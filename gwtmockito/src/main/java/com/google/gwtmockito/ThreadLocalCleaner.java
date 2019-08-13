package com.google.gwtmockito;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;

/**
 * Utility classes to modify the private ThreadLocalMap attribute from the java.lang.Thread
 */
final class ThreadLocalCleaner {

    private ThreadLocalCleaner() {
        // Nothing
    }

    /**
     * Remove ThreadLocalMap entries of objects loaded through the given classLoader as they prevent garbage collection
     * of this classLoader while the thread that holds the ThreadLocalMap is alive
     */
    public static void cleanUpThreadLocalValues(ClassLoader classLoader) {
        try {
            Object threadLocalMap = getPrivateAttribute(Thread.class, "threadLocals", Thread.currentThread());
            WeakReference[] table = (WeakReference[]) getPrivateAttribute("table", threadLocalMap);
            int length = table.length;
            for (int i = 0; i < length; i++) {
                WeakReference mapEntry = table[i];
                if (mapEntry != null && mapEntry.get() != null) {
                    ClassLoader mapEntryKeyClassLoader = mapEntry.get().getClass().getClassLoader();
                    Field mapEntryValueField = getPrivateAttributeAccessibleField(mapEntry.getClass(), "value");
                    Object value = mapEntryValueField.get(mapEntry);
                    if (value != null) {
                        ClassLoader mapEntryValueClassLoader = value.getClass().getClassLoader();
                        if (mapEntryKeyClassLoader == classLoader || mapEntryValueClassLoader == classLoader) {
                            mapEntry.clear();
                            mapEntryValueField.set(mapEntry, null);
                            // The ThreadLocalMap is able to expunge the remaining stale entries, no need to remove it from the map
                        }
                    }
                }
            }
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new AssertionError("Unable to access expected class fields for cleaning ThreadLocal values", e);
        }
    }

    private static Object getPrivateAttribute(String attributeName, Object holder) throws NoSuchFieldException, IllegalAccessException {
        return getPrivateAttribute(holder.getClass(), attributeName, holder);
    }

    private static Object getPrivateAttribute(Class attributeClass, String attributeName, Object holder) throws NoSuchFieldException, IllegalAccessException {
        Field field = getPrivateAttributeAccessibleField(attributeClass, attributeName);
        return field.get(holder);
    }

    private static Field getPrivateAttributeAccessibleField(Class attributeClass, String attributeName) throws NoSuchFieldException {
        Field field = attributeClass.getDeclaredField(attributeName);
        field.setAccessible(true);
        return field;
    }

}
