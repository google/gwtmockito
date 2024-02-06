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
import javassist.CtConstructor;
import javassist.CtMethod;
import javassist.Loader;
import javassist.LoaderClassPath;
import javassist.NotFoundException;
import javassist.Translator;
import javassist.bytecode.AccessFlag;

import com.google.gwt.user.cellview.client.CellList;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.CellPanel;
import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DeckLayoutPanel;
import com.google.gwt.user.client.ui.DeckPanel;
import com.google.gwt.user.client.ui.DecoratorPanel;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.HTMLTable;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.RenderablePanel;
import com.google.gwt.user.client.ui.ResizeLayoutPanel;
import com.google.gwt.user.client.ui.SimpleLayoutPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.SplitLayoutPanel;
import com.google.gwt.user.client.ui.StackPanel;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.ValueListBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwtmockito.impl.StubGenerator;

import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.ParentRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;
import org.junit.runners.model.TestClass;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

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
 * <li> The methods of many common widget base classes, such as {@link Widget},
 *      {@link Composite}, and most subclasses of {@link Panel}, will have their
 *      methods replaced with no-ops to make it easier to test widgets extending
 *      them. This behavior can be customized by overriding
 *      {@link GwtMockitoTestRunner#getClassesToStub}.
 * </ul>
 *
 * @see GwtMockito
 * @author ekuefler@google.com (Erik Kuefler)
 */
public class GwtMockitoTestRunner extends BlockJUnit4ClassRunner {

  private final Class<?> unitTestClass;
  private final ClassLoader gwtMockitoClassLoader;
  private final Class<?> customLoadedGwtMockito;

  /**
   * Creates a test runner which allows final GWT classes to be mocked. Works by reloading the test
   * class using a custom classloader and substituting the reference.
   */
  public GwtMockitoTestRunner(Class<?> unitTestClass) throws InitializationError {
    super(unitTestClass);
    this.unitTestClass = unitTestClass;

    // Build a fresh class pool with the system path and any user-specified paths and use it to
    // create the custom classloader
    ClassPool classPool = new ClassPool();
    classPool.appendClassPath(new LoaderClassPath(getParentClassloader()));
    for (String path : getAdditionalClasspaths()) {
      try {
        classPool.appendClassPath(path);
      } catch (NotFoundException e) {
        throw new IllegalStateException("Cannot find classpath entry: " + path, e);
      }
    }
    gwtMockitoClassLoader = new GwtMockitoClassLoader(getParentClassloader(), classPool);

    // Use this custom classloader as the context classloader during the rest of the initialization
    // process so that classes loaded via the context classloader will be compatible with the ones
    // used during test.
    ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
    Thread.currentThread().setContextClassLoader(gwtMockitoClassLoader);

    try {
      // Reload the test class with our own custom class loader that does things like remove
      // final modifiers, allowing GWT Elements to be mocked. Also load GwtMockito itself so we can
      // invoke initMocks on it later.
      Class<?> customLoadedTestClass = gwtMockitoClassLoader.loadClass(unitTestClass.getName());
      customLoadedGwtMockito = gwtMockitoClassLoader.loadClass(GwtMockito.class.getName());

      // Overwrite the private "fTestClass" field in ParentRunner (superclass of
      // BlockJUnit4ClassRunner). This refers to the test class being run, so replace it with our
      // custom-loaded class. As of JUnit 4.12, "fTestClass" was renamed to "testClass".
      Field testClassField;
      try {
        testClassField = ParentRunner.class.getDeclaredField("fTestClass");
      } catch (NoSuchFieldException e) {
        testClassField = ParentRunner.class.getDeclaredField("testClass");
      }
      testClassField.setAccessible(true);
      testClassField.set(this, new TestClass(customLoadedTestClass));
    } catch (Exception e) {
      throw new InitializationError(e);
    } finally {
      Thread.currentThread().setContextClassLoader(originalClassLoader);
    }
  }

  /**
   * Returns a collection of classes whose non-abstract methods should always be replaced with
   * no-ops. By default, this list includes {@link Composite}, {@link DOM} {@link UIObject},
   * {@link Widget}, {@link Image}, and most subclasses of {@link Panel}. It will also include any
   * classes specified via the {@link WithClassesToStub} annotation on the test class. This makes
   * it much safer to test code that uses or extends these types.
   * <p>
   * This list can be customized via {@link WithClassesToStub} or by defining a new test runner
   * extending {@link GwtMockitoTestRunner} and overriding this method. This allows users to
   * explicitly stub out particular classes that are causing problems in tests. If you override this
   * method, you will probably want to retain the classes that are stubbed here by doing something
   * like this:
   *
   * <pre>
   * &#064;Override
   * protected Collection&lt;Class&lt;?&gt;&gt; getClassesToStub() {
   *   Collection&lt;Class&lt;?&gt;&gt; classes = super.getClassesToStub();
   *   classes.add(MyBaseWidget.class);
   *   return classes;
   * }
   * </pre>
   *
   * @return a collection of classes whose methods should be stubbed with no-ops while running tests
   */
  protected Collection<Class<?>> getClassesToStub() {
    Collection<Class<?>> classes = new LinkedList<Class<?>>();
    classes.add(Composite.class);
    classes.add(DOM.class);
    classes.add(UIObject.class);
    classes.add(Widget.class);

    classes.add(DataGrid.class);
    classes.add(HTMLTable.class);
    classes.add(Image.class);

    classes.add(AbsolutePanel.class);
    classes.add(CellList.class);
    classes.add(CellPanel.class);
    classes.add(CellTable.class);
    classes.add(ComplexPanel.class);
    classes.add(DeckLayoutPanel.class);
    classes.add(DeckPanel.class);
    classes.add(DecoratorPanel.class);
    classes.add(DockLayoutPanel.class);
    classes.add(DockPanel.class);
    classes.add(FlowPanel.class);
    classes.add(FocusPanel.class);
    classes.add(HorizontalPanel.class);
    classes.add(HTMLPanel.class);
    classes.add(LayoutPanel.class);
    classes.add(Panel.class);
    classes.add(PopupPanel.class);
    classes.add(RenderablePanel.class);
    classes.add(ResizeLayoutPanel.class);
    classes.add(SimpleLayoutPanel.class);
    classes.add(SimplePanel.class);
    classes.add(SplitLayoutPanel.class);
    classes.add(StackPanel.class);
    classes.add(VerticalPanel.class);
    classes.add(ValueListBox.class);

    WithClassesToStub annotation = unitTestClass.getAnnotation(WithClassesToStub.class);
    if (annotation != null) {
      classes.addAll(Arrays.asList(annotation.value()));
    }

    return classes;
  }

  /**
   * Returns a list of package names that should always be loaded via the standard system
   * classloader instead of through GwtMockito's custom classloader. Any subpackages of these
   * packages will also be loaded with the standard loader. If you're getting
   * "loader constraint violation" errors, try using
   * {@link WithPackagesToLoadViaStandardClassLoader} or defining a new runner class that overrides
   * this method and adds the package the error is complaining about. If you override this method,
   * you will probably want to retain the packages that are blacklisted by default by doing
   * something like this:
   *
   * <pre>
   * &#064;Override
   * protected Collection&lt;String;&gt; getPackagesToLoadViaStandardClassloader() {
   *   Collection&lt;String&gt; packages = super.getPackagesToLoadViaStandardClassloader();
   *   packages.add("my.package");
   *   return packages;
   * }
   * </pre>
   *
   * @return a collection of strings such that any class whose fully-qualified name starts with a
   *         string in the collection will always be loaded via the system classloader
   */
  protected Collection<String> getPackagesToLoadViaStandardClassloader() {
    Collection<String> packages = new LinkedList<String>();
    packages.add("com.vladium"); // To support EMMA code coverage tools
    packages.add("jdk.internal.reflect"); // Java9 loading mechanism
    packages.add("net.bytebuddy"); // To support Mockito 2
    packages.add("net.sf.cglib"); // To support Mockito 1
    packages.add("net.sourceforge.cobertura"); // To support Cobertura code coverage tools
    packages.add("org.jacoco"); // To support JaCoCo code coverage tools
    packages.add("org.hamcrest"); // Since this package is referenced directly from org.junit
    packages.add("org.junit"); // Make sure the ParentRunner can recognize annotations like @Test
//    packages.add("org.mockito.cglib"); // To support Mockito 1

    WithPackagesToLoadViaStandardClassLoader annotation = unitTestClass.getAnnotation(WithPackagesToLoadViaStandardClassLoader.class);
    if (annotation != null) {
      packages.addAll(Arrays.asList(annotation.value()));
    }

    return packages;
  }

  /**
   * Returns the classloader to use as the parent of GwtMockito's classloader. By default this is
   * the current thread's context classloader. This can be customized by defining a custom test
   * runner extending {@link GwtMockitoTestRunner} and overriding this method.
   *
   * @return classloader to use for delegation when loading classes via GwtMockito
   */
  protected ClassLoader getParentClassloader() {
    return Thread.currentThread().getContextClassLoader();
  }

  /**
   * Returns a list of additional sources from which the classloader should read while running
   * tests. By default this list is empty; custom sources can be specified by defining a custom test
   * runner extending {@link GwtMockitoTestRunner} and overriding this method.
   * <p>
   * The entries in this list must be paths referencing a directory, jar, or zip file. The entries
   * must not end with a "/". If an entry ends with "/*", then all jars matching the path name are
   * included.
   *
   * @return a list of strings to be appended to the classpath used when running tests
   * @see ClassPool#appendClassPath(String)
   */
  protected List<String> getAdditionalClasspaths() {
    return new LinkedList<String>();
  }

  /**
   * Runs the tests in this runner, ensuring that the custom GwtMockito classloader is installed as
   * the context classloader.
   */
  @Override
  public void run(final RunNotifier notifier) {
    // When running the test, we want to be sure to use our custom classloader as the context
    // classloader. This is important because Mockito will create mocks using the context
    // classloader. Things that can go wrong if this isn't set include not being able to mock
    // package-private classes, since the mock implementation would be created by a different
    // classloader than the class being mocked.
    ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
    Thread.currentThread().setContextClassLoader(gwtMockitoClassLoader);
    RunNotifier wrapperNotifier = new RunNotifier();
    wrapperNotifier.addListener(new RunListener() {
      @Override
      public void testAssumptionFailure(Failure failure) {
        notifier.fireTestAssumptionFailed(failure);
      }
      @Override
      public void testFailure(Failure failure) throws Exception {
        if (failure.getException() instanceof ClassCastException
            && failure.getMessage().contains("EnhancerByMockitoWithCGLIB")) {
          notifier.fireTestFailure(new Failure(
              failure.getDescription(),
              new FailedCastException(
                  "The test failed with a ClassCastException. This often indicates that you are "
                      + "using a library that invokes JavaScriptObject.cast() to convert an object "
                      + "from one type to another in a way that can't be mimicked in pure Java. "
                      + "There are a few ways to deal with this:\n"
                      + "  1) Inject the class that calls cast() into the class being tested so "
                      + "that you can replace it with a mock.\n"
                      + "  2) Annotate your test class with @WithClassesToStub and pass in the "
                      + "class that is causing problems.\n"
                      + "  3) If the class is part of GWT and not a third-party library, try "
                      + "reporting it on the issue tracker at "
                      + "https://github.com/google/gwtmockito/issues and it might be possible to "
                      + " insert a workaround.\n",
                  failure.getException())));
        } else {
          notifier.fireTestFailure(failure);
        }
      }

      @Override
      public void testFinished(Description description) throws Exception {
        notifier.fireTestFinished(description);
      }

      @Override
      public void testIgnored(Description description) throws Exception {
        notifier.fireTestIgnored(description);
      }

      @Override
      public void testRunFinished(Result result) throws Exception {
        notifier.fireTestRunFinished(result);
      }

      @Override
      public void testRunStarted(Description description) throws Exception {
        notifier.fireTestRunStarted(description);
      }

      @Override
      public void testStarted(Description description) throws Exception {
        notifier.fireTestStarted(description);
      }
    });
    try {
      super.run(wrapperNotifier);
    } finally {
      Thread.currentThread().setContextClassLoader(originalClassLoader);
      if (unitTestClass.isAnnotationPresent(WithExperimentalGarbageCollection.class)) {
        ThreadLocalCleaner.cleanUpThreadLocalValues(gwtMockitoClassLoader);
      }
    }
  }

  /**
   * Overridden to invoke GwtMockito.initMocks before starting each test.
   */
  @Override
  @SuppressWarnings("deprecation") // Currently the only way to support befores
  protected final Statement withBefores(FrameworkMethod method, Object target,
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
  private final class GwtMockitoClassLoader extends Loader implements Translator {

    GwtMockitoClassLoader(ClassLoader classLoader, ClassPool classPool) {
      super(classLoader == null ? GwtMockitoTestRunner.class.getClassLoader() : classLoader, classPool);
      try {
        addTranslator(classPool, this);
        setDomain(getClass().getProtectionDomain());
      } catch (NotFoundException e) {
        throw new AssertionError("Impossible since this.start does not throw");
      } catch (CannotCompileException e) {
        throw new AssertionError("Impossible since this.start does not throw");
      }
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
      // If the class is in a blacklisted package, load it with the parent or default classloader.
      for (String blacklistedPackage : getPackagesToLoadViaStandardClassloader()) {
        if (name.startsWith(blacklistedPackage)) {
          return null;
        }
      }

      // Otherwise load it with our custom classloader.
      return super.findClass(name);
    }

    @Override
    public void onLoad(ClassPool pool, String name)
        throws NotFoundException, CannotCompileException {
      CtClass clazz = pool.get(name);

      // Strip final accessFlag from the class and all methods to allow them to be mocked
      clazz.getClassFile().setAccessFlags(clazz.getClassFile().getAccessFlags() & ~AccessFlag.FINAL);
      for (CtMethod method : clazz.getDeclaredMethods()) {
        method.setModifiers(method.getModifiers() & ~Modifier.FINAL);
      }

      // Create stub implementations for certain methods
      for (CtMethod method : clazz.getDeclaredMethods()) {
        if (StubGenerator.shouldStub(method, getClassesToStub())) {
          method.setModifiers(method.getModifiers() & ~Modifier.NATIVE);
          CtClass returnType = method.getReturnType();
          // TODO(ekuefler): Handle primitives, voids, and enums in StubGenerator
          if (returnType.isPrimitive() || returnType.getName().equals("void")) {
            method.setBody(null);
          } else if (returnType.isEnum()) {
            method.setBody(String.format("return %s.values()[0];", returnType.getName()));
          } else {
            method.setBody(String.format(
                "return (%s) com.google.gwtmockito.impl.StubGenerator.invoke("
                    + "Class.forName(\"%s\"), \"%s\", \"%s\");",
                method.getReturnType().getName(),
                method.getReturnType().getName(),
                clazz.getName(),
                method.getName()));
          }
        }
      }

      // Also stub certain constructors
      for (Class<?> classToStub : getClassesToStub()) {
        if (classToStub.getName().equals(clazz.getName())) {
          for (CtConstructor constructor : clazz.getConstructors()) {
            String parameters = makeNullParameters(
                clazz.getSuperclass().getConstructors()[0].getParameterTypes());
            constructor.setBody("super(" + parameters + ");");
          }
        }
      }
    }

    private String makeNullParameters(CtClass[] paramClasses) {
      if (paramClasses.length == 0) {
        return "";
      }
      StringBuilder params = new StringBuilder();
      for (CtClass paramClass : paramClasses) {
        params.append(",");
        String className = paramClass.getName();
        if (className.equals("boolean")) {
          params.append("false");
        } else if (className.equals("byte")) {
          params.append("(byte) 0");
        } else if (className.equals("char")) {
          params.append("(char) 0");
        } else if (className.equals("double")) {
          params.append("(double) 0");
        } else if (className.equals("int")) {
          params.append("(int) 0");
        } else if (className.equals("float")) {
          params.append("(float) 0");
        } else if (className.equals("long")) {
          params.append("(long) 0");
        } else if (className.equals("short")) {
          params.append("(short) 0");
        } else {
          params.append(newMockForClassSnippet(paramClass));
        }
      }
      return params.substring(1).toString();
    }

    private String newMockForClassSnippet(CtClass paramClass) {
      return String.format(
          "(%1$s) org.mockito.Mockito.mock("
              + "%1$s.class, new com.google.gwtmockito.impl.ReturnsCustomMocks())",
          paramClass.getName());
    }

    @Override
    public void start(ClassPool pool) {}
  }

  private static class FailedCastException extends Exception {
    FailedCastException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
