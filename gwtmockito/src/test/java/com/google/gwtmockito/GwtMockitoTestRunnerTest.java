/*
 * Copyright 2014 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwtmockito;

import com.google.gwtmockito.subpackage.LoadedFromStandardClassLoader;

import java.lang.ref.WeakReference;
import java.util.Collection;

import com.google.gwtmockito.subpackage.ThreadLocalUsage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.JUnit4;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.TestClass;

import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link GwtMockitoTestRunner}.
 */
@RunWith(JUnit4.class)
public class GwtMockitoTestRunnerTest {

  @Test
  public void shouldLoadClassFromStandardClassLoaderEvenWhenRequestedByChildClassLoader()
      throws InitializationError, ClassNotFoundException {

    // Runner should reload the test class in its own ClassLoader.
    GwtMockitoTestRunner runner = new GwtMockitoTestRunner(DummyTestClass.class) {

      @Override
      protected Collection<String> getPackagesToLoadViaStandardClassloader() {
        Collection<String> packages = super.getPackagesToLoadViaStandardClassloader();
        packages.add("com.google.gwtmockito.subpackage");
        return packages;
      }
    };

    // Assert that test class is loaded from a different class loader.
    TestClass testClass = runner.getTestClass();
    Class<?> javaClass = testClass.getJavaClass();
    assertNotEquals(DummyTestClass.class, javaClass);

    // Create a child ClassLoader of the one used by Runner.
    ClassLoader gwtMockitoClassLoader = javaClass.getClassLoader();
    ClassLoader childClassLoader = new ClassLoader(gwtMockitoClassLoader) {};

    // Attempt to load the class from the child ClassLoader, should delegate up to the one used to
    // load this class.
    Class<?> loadedClass =
        childClassLoader.loadClass(LoadedFromStandardClassLoader.class.getName());

    assertEquals("Expected to load class from the standard ClassLoader, i.e. the one that "
        + "loaded this test class, but loaded it from the GwtMockitoTestRunner's ClassLoader "
        + "instead. That means there is a problem with the delegation of class loading.",
        getClass().getClassLoader(), loadedClass.getClassLoader());
  }

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
  public static class DummyTestClass {

    @Test
    public void dummy() {
    }
  }

  @RunWith(JUnit4.class)
  public static class TestWithThreadLocal {
    @Test
    public void dummy() {
      ThreadLocalUsage threadLocalUsage = new ThreadLocalUsage();
    }
  }

}
