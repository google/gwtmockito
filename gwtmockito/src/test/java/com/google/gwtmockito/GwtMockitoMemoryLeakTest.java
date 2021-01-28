package com.google.gwtmockito;

import com.google.gwtmockito.subpackage.ThreadLocalUsage;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.JUnit4;
import org.junit.runners.model.InitializationError;

import java.lang.ref.WeakReference;

import static junit.framework.TestCase.assertNull;

public class GwtMockitoMemoryLeakTest {

    @Test
    public void shouldGarbageCollectGwtClassLoaderWhenThreadLocalIsUsed() throws InitializationError {
        GwtMockitoTestRunner runner = new GwtMockitoTestRunner(TestWithThreadLocal.class);
        runner.run(new RunNotifier());
        WeakReference<GwtMockitoTestRunner> wkRunner = new WeakReference<>(runner);
        // remove the reference to the runner to allow the garbage collector to collect it.
        runner = null;
        // Call the Garbage Collector to (try to) force the collection of the runner.
        // This test is not perfect and may depend of the JVM Garbage Collector implementation as
        // as return from "System.gc()" simply garantee that a "best effort to reclaim space from all discarded objects"
        // has been done.
        System.gc();
        // Check if the garbage colllector has collected the runner instance through the WeakReference
        assertNull("Expected to garbage collect the GwtMockitoTestRunner", wkRunner.get());
    }

    @RunWith(JUnit4.class)
    public static class FakeTestClass {

        @Test
        public void fake() {
        }
    }

    @RunWith(JUnit4.class)
    @WithExperimentalGarbageCollection
    public static class TestWithThreadLocal {
        @Test
        public void fake() {
            ThreadLocalUsage threadLocalUsage = new ThreadLocalUsage();
        }
    }
}
