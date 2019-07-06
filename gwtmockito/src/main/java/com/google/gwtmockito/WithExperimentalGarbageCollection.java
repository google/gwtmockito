package com.google.gwtmockito;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation allowing the test to enable a clean up in the ThreadLocalMap at the end of the test
 * in order to avoid a memory leak.
 *
 * <p> Without this annotation, if a test uses a ThreadLocal object load through GwtMockitoClassLoader
 * and stored in the test runner thread context, the garbage collection of the GwtMockitoClassLoader
 * will be prevented.
 *
 * <p> Note: the memory leak issue and this experimental garbage collection fix is dependent of the JVM
 * (observed with the OpenJDK). It will raise an AssertionError on uncompatible JVM.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface WithExperimentalGarbageCollection {

}
