package com.google.gwtmockito.subpackage;

public class ThreadLocalUsage {
    private static ThreadLocal<ThreadLocalUsage> MY_DUMMY_LOCAL = ThreadLocal.withInitial(() -> new ThreadLocalUsage());
}
