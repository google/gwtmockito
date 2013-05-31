/*
 * Copyright 2013 Google Inc.
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

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.CtPrimitiveType;
import javassist.Loader;
import javassist.NotFoundException;
import javassist.Translator;

import com.google.gwt.user.client.DOM;

import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.ParentRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;
import org.junit.runners.model.TestClass;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * A JUnit4 test runner that executes a test using GwtMockito. In addition to
 * the standard {@link BlockJUnit4ClassRunner} features, a test executed with
 * {@link GwtMockitoTestRunner} will behave as follows:
 *
 * <ul>
 * <li> Calls to GWT.create will return mock or fake objects instead of throwing
 *      an exception. If a field in the test is annotated with {@link GwtMock},
 *      that field will be returned. Otherwise, if a provider is registered via
 *      {@link GwtMockito#useProviderForType}, that provider will be used to
 *      create a fake. Otherwise, a new mock instance will be returned. See
 *      {@link GwtMockito} for more information and for the set of fake
 *      providers registered by default.
 * <li> Final modifiers on methods and classes will be removed. This allows
 *      Javascript overlay types like {@link com.google.gwt.dom.client.Element}
 *      to be mocked.
 * <li> Native methods will be given no-op implementations so that calls to them
 *      don't cause runtime failures. If the native method returns a value, it
 *      will return either a default primitive type or a new mock object.
 * </ul>
 *
 * @see GwtMockito
 * @author ekuefler@google.com (Erik Kuefler)
 */
public class GwtMockitoTestRunner extends BlockJUnit4ClassRunner {

  private static final ClassLoader GWT_MOCKITO_CLASS_LOADER = new GwtMockitoClassLoader();

  private Class<?> customLoadedGwtMockito;

  /**
   * Creates a test runner which allows final GWT classes to be mocked. Works by reloading the test
   * class using a custom classloader and substituting the reference.
   */
  public GwtMockitoTestRunner(Class<?> unitTestClass) throws InitializationError {
    super(unitTestClass);

    try {
      // Reload the test class with our own custom class loader that does things like remove
      // final modifiers, allowing GWT Elements to be mocked. Also load GwtMockito itself so we can
      // invoke initMocks on it later.
      Class<?> customLoadedTestClass = GWT_MOCKITO_CLASS_LOADER.loadClass(unitTestClass.getName());
      customLoadedGwtMockito = GWT_MOCKITO_CLASS_LOADER.loadClass(GwtMockito.class.getName());

      // Overwrite the private "fTestClass" field in ParentRunner (superclass of
      // BlockJUnit4ClassRunner). This refers to the test class being run, so replace it with our
      // custom-loaded class.
      Field testClassField = ParentRunner.class.getDeclaredField("fTestClass");
      testClassField.setAccessible(true);
      testClassField.set(this, new TestClass(customLoadedTestClass));
    } catch (Exception e) {
      throw new InitializationError(e);
    }
  }

  @Override
  public void run(RunNotifier notifier) {
    // When running the test, we want to be sure to use our custom classloader as the context
    // classloader. This is important because Mockito will create mocks using the context
    // classloader. Things that can go wrong if this isn't set include not being able to mock
    // package-private classes, since the mock implementation would be created by a different
    // classloader than the class being mocked.
    ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
    Thread.currentThread().setContextClassLoader(GWT_MOCKITO_CLASS_LOADER);
    super.run(notifier);
    Thread.currentThread().setContextClassLoader(originalClassLoader);
  }

  /**
   * Overridden to invoke GwtMockito.initMocks before starting each test.
   */
  @Override
  @SuppressWarnings("deprecation") // Currently the only way to support befores
  protected Statement withBefores(FrameworkMethod method, Object target,
      Statement statement) {
    try {
      // Invoke initMocks on the version of GwtMockito that was loaded via our custom classloader.
      // This is necessary to ensure that it uses the same set of classes as the unit test class,
      // which we loaded through the custom classloader above.
      customLoadedGwtMockito.getMethod("initMocks", Object.class).invoke(null, target);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return super.withBefores(method, target, statement);
  }

  /** Custom classloader that performs additional modifications to loaded classes. */
  private static class GwtMockitoClassLoader extends Loader implements Translator {

    GwtMockitoClassLoader() {
      try {
        addTranslator(ClassPool.getDefault(), this);
      } catch (NotFoundException e) {
        throw new AssertionError("Impossible since this.start does not throw");
      } catch (CannotCompileException e) {
        throw new AssertionError("Impossible since this.start does not throw");
      }
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
      // Always use the standard loader to load junit classes, otherwise the rest of junit
      // (specifically the ParentRunner) won't be able to recognize things like @Test and @Before
      // annotations in the relaoded class.
      if (name.startsWith("org.junit")) {
        return GwtMockitoTestRunner.class.getClassLoader().loadClass(name);
      }
      return super.loadClass(name);
    }

    @Override
    public void onLoad(ClassPool pool, String name)
        throws NotFoundException, CannotCompileException {
      CtClass clazz = pool.get(name);

      // Strip final modifiers from the class and all methods to allow them to be mocked
      clazz.setModifiers(clazz.getModifiers() & ~Modifier.FINAL);
      for (CtMethod method : clazz.getDeclaredMethods()) {
        method.setModifiers(method.getModifiers() & ~Modifier.FINAL);
      }

      // Create stub implementations for certain methods
      for (CtMethod method : clazz.getDeclaredMethods()) {
        if (shouldStubMethod(method)) {
          method.setModifiers(method.getModifiers() & ~Modifier.NATIVE);
          CtClass returnType = method.getReturnType();
          if (returnType != CtClass.voidType
              && !returnType.getName().equals("java.lang.String")
              && !(returnType instanceof CtPrimitiveType)) {
            // Return mocks for methods that don't return voids, primitives, or strings
            method.setBody(String.format(
                "return (%1$s) org.mockito.Mockito.mock("
                    + "%1$s.class, new com.google.gwtmockito.impl.ReturnsCustomMocks());",
                returnType.getName()));
          } else if (returnType.getName().equals("java.lang.String")) {
            // Return empty strings for string methods
            method.setBody("return \"\";");
          } else {
            // Return default values for void or primitive methods
            method.setBody(null);
          }
        }
      }
    }

    private boolean shouldStubMethod(CtMethod method) {
      return
          // Stub all native methods
          (method.getModifiers() & Modifier.NATIVE) != 0

          // Stub the create methods from DOM since they do non-typesafe element casts
          || (method.getDeclaringClass().getName().equals(DOM.class.getCanonicalName())
              && method.getName().startsWith("create"));
    }

    @Override
    public void start(ClassPool pool) {}
  }
}
