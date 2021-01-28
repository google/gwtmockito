## What is GwtMockito?

Testing GWT applications using `GWTTestCase` can be a pain - it's slower than
using pure Java tests, and you can't use reflection-based tools like mocking
frameworks. But if you've tried to test widgets using normal test cases, you've
probably run into this error:

    ERROR: GWT.create() is only usable in client code!  It cannot be called,
    for example, from server code. If you are running a unit test, check that 
    your test case extends GWTTestCase and that GWT.create() is not called
    from within an initializer or constructor.

GwtMockito solves this and other GWT-related testing problems by allowing you
to call GWT.create from JUnit tests, returning [Mockito][1] mocks.

## How do I use it?

Getting started with GwtMockito using Junit 4.5+ is easy. Just annotate your test
with `@RunWith(GwtMockitoTestRunner.class)`, then any calls to `GWT.create`
encountered will return Mockito mocks instead of throwing exceptions:

```java
@RunWith(GwtMockitoTestRunner.class)
public class MyTest {
  @Test
  public void shouldReturnMocksFromGwtCreate() {
    Label myLabel = GWT.create(Label.class);
    when(myLabel.getText()).thenReturn("some text");
    assertEquals("some text", myLabel.getText());
  }
}
```

GwtMockito also creates fake implementations of all UiBinders that automatically
populate `@UiField`s with Mockito mocks. Suppose you have a widget that looks
like this:

```java
public class MyWidget extends Composite {
  interface MyUiBinder extends UiBinder<Widget, MyWidget> {}
  private final MyUiBinder uiBinder = GWT.create(MyUiBinder.class);

  @UiField Label numberLabel;
  private final NumberFormatter formatter;

  public MyWidget(NumberFormatter formatter) {
    this.formatter = formatter;
    initWidget(uiBinder.createAndBindUi(this);
  }

  void setNumber(int number) {
    numberLabel.setText(formatter.format(number));
  }
}
```

When `createAndBindUi` is called, GwtMockito will automatically populate 
`numberLabel` with a mock object. Since `@UiField`s are package-visible, they 
can be read from your unit tests, which lets you test this widget as follows:

```java
@RunWith(GwtMockitoTestRunner.class)
public class MyWidgetTest {

  @Mock NumberFormatter formatter;
  private MyWidget widget;

  @Before
  public void setUp() {
    widget = new MyWidget(formatter);
  }

  @Test
  public void shouldFormatNumber() {
    when(formatter.format(5)).thenReturn("5.00");
    widget.setNumber(5);
    verify(widget.numberLabel).setText("5.00");
  }
}
```

Note that GwtMockito supports the `@Mock` annotation from Mockito, allowing 
standard Mockito mocks to be mixed with mocks created by GwtMockito.

That's all you need to know to get started - read on if you're interested in
hearing about some advanced features.

### Accessing the mock returned from GWT.create

Returning mocks from `GWT.create` isn't very useful if you don't have any way to
verify or set behaviors on them. You can do this by annotating a field in your 
test with `@GwtMock` - this will cause all calls to `GWT.create` for that type
to return a mock stored in that field, allowing you to reference it in your
test. So if you have a class that looks like

```java
public class MyClass {
  public MyClass() {
    SomeInterface myInterface = GWT.create(SomeInterface.class);
    myInterface.setSomething(true);
  }
}
```

then you can verify that it works correctly by writing a test that looks like
this:

```java
@RunWith(GwtMockitoTestRunner.class)
public class MyClassTest {
  @GwtMock SomeInterface mockInterface;

  @Test
  public void constructorShouldSetSomething() {
    new MyClass();
    verify(mockInterface).setSomething(true);
  }
}
```

### Returning fake objects

By default, GwtMockito will return fake implementations (which don't require you
to specify mock behavior) for any classes extending the following types:

  * UiBinder
  * ClientBundle
  * Messages
  * CssResource
  * SafeHtmlTemplates

You can add fakes for additional types by invoking 
`GwtMockito.useProviderForType(Class, FakeProvider)` in your `setUp` method. 
This will cause all calls to `GWT.create` for the given class or its subclasses
to invoke the given `FakeProvider` to determine what to return. See the
[javadoc reference][2] for more details.

### Mocking final classes and methods

Mockito does not normally allow final classes and methods to be mocked. This 
poses problems in GWT since [JavaScript overlay types][3] (which include 
`Element` and its subclasses) require all methods to be final. Fortunately,
GwtMockito does some classloader black magic to remove all final modifiers from
classes and interfaces, so the following test will pass:

```java
@RunWith(GwtMockitoTestRunner.class)
public class MyTest {
  @Mock Element element;

  @Test
  public void shouldReturnMocksFromGwtCreate() {
    when(element.getClassName()).thenReturn("mockClass");
    assertEquals("mockClass", myLabel.getClassName());
  }
}
```

As long as your test uses `GwtMockitoTestRunner`, it is possible to mock any
final methods.

### Dealing with native methods

Under normal circumstances, JUnit tests will fail with an `UnsatisfiedLinkError` when encountering a native JSNI method. GwtMockito works around this problem
using more classloader black magic to provide no-op implementations for all
native methods using the following rules:

  * `void` methods do nothing.
  * Methods returning primitive types return the default value for that type (0,
    false, etc.)
  * Methods returning `String`s return the empty string.
  * Methods returning other objects return a mock of that object configured with
    `RETURNS_MOCKS`.

These rules allow many tests to continue to work even when calling incindental
native methods. Note that this can be dangerous - a JSNI method that normally
always returns `true` would always return `false` when stubbed by GwtMockito.
As much as possible, you should isolate code that depends on JSNI into its own
class, [inject][4] that class into yours, and test the factored-out class using
`GWTTestCase`.

### Support for JUnit 3 and other tests that can't use custom runners

Though `GwtMockitoTestRunner` is the easiest way to use GwtMockito, it won't
work if you're using JUnit 3 or rely on another custom test runner. In these
situations, you can still get most of the benefit of GwtMockito by calling
`GwtMockito.initMocks` directly. A test written in this style looks like this:

```java
public class MyWidgetTest extends TestCase {

  private MyWidget widget;

  @Override
  public void setUp() {
    super.setUp();
    GwtMockito.initMocks(this);
    widget = new MyWidget() {
      protected void initWidget(Widget w) {
        // Disarm for testing
      }
    };
  }

  @Override
  public void tearDown() {
    super.tearDown();
    GwtMockito.tearDown();
  }

  public void testSomething() {
    // Test code goes here
  }
}
```

The test must explicitly call `initMocks` during its setup and `tearDown` when
it is being teared down, or else state can leak between tests. When 
instantiating a widget, the test must also subclass it and override initWidget
with a no-op implementation, or else it will fail when this method attempts to
call Javascript. Note that when testing in this way the features described in
"Mocking final classes and methods" and "Dealing with native methods" won't
work - there is no way to mock final methods or automatically replace native
methods without using `GwtMockitoTestRunner`.

## How do I install it?
If you're using Maven, you can add the following to your `<dependencies>`
section:

```xml
<dependency>
  <groupId>com.google.gwt.gwtmockito</groupId>
  <artifactId>gwtmockito</artifactId>
  <version>1.1.9</version>
  <scope>test</scope>
</dependency>
```

You can also download the [jar][5] directly or check out the source using git
from <https://github.com/google/gwtmockito.git>. In these cases you will have
to manually install the jars for [Mockito][6] and [Javassist][7].

## Where can I learn more?
  * For more details on the GwtMockito API, consult the [Javadoc][8]
  * For an example of using GwtMockito to test some example classes, see the
    [sample app][9].

## Version history

### 1.1.9
  * Support ResourcePrototype methods in fake CLientBundles. (Thanks to zbynek)
  * Add a `@WithExperimentalGarbageCollection` annotation. (Thanks to LudoP)
  * Updated javassist dependency. (Thanks to TimvdLippe)

### 1.1.8
  * Preliminary Java 9 support. (Thanks to benoitf)

### 1.1.7
  * Update GWT to version 2.8.0.
  * Update Javassist to version 3.22.
  * Stubbing for ValueListBox. (Thanks to jschmied)
  * Stubbing for URL encoding. (Thanks to jschmeid)
  * Generate hashCode and equals for Messages. (Thanks to zolv)

### 1.1.6
  * Improved support for running tests in IntelliJ.
  * Fix for stubbing DatePicker.
  * Better support for non-default classloaders. (Thanks to leanseefeld)
  * Depend on mockito-core instead of mockito-all. (Thanks to psiroky)

### 1.1.5
  * Support for JUnit 4.12. (Thanks to selesse)
  * Provide a better error message for `ClassCastException`s that we can't work around.
  * Support for JaCoCo. (Thanks to rsauciuc)
  * Include a fake implementation of `NumberConstants`.
  * Fixed support for `History` methods.
  * Fix for some `TextBox` methods.
  * Fix instantiation of checkboxes and radio buttons.

### 1.1.4
  * Many fixes for `ClassCastException`s when mocking various classes.
  * Support for Cobertura coverage tools. (Thanks to mvmn)
  * Try to intelligently return the right value for getTagName when possible.
  * Fixed a classloader delegation issue. (Thanks to paulduffin)
  * Add an annotation allowing the excludelist of classes that are always
    loaded via the standard classloader to be specified on a per-test bases.

### 1.1.3
  * Support for Hamcrest matchers.
  * Added a method to specify packages that should never be reloaded.
  * Added a getFake method to provide direct access to registered fakes.
  * Fixed assertion errors in classes extending LabelBase.
  * Added an annotation allowing stubbed classes to be specified on a
    per-test basis (suggested by danielkaneider)
  * Support for GWT 2.6.
  * Fix to allow FakeProviders to be specified for RPC interfaces.

### 1.1.2
  * Fix for UiBinders that generate parameterized widgets.
  * Fix to always use the most specific provider available when multiple
    providers could provide a type. (Thanks to reimai)
  * Compatability with EMMA code coverage tools.

### 1.1.1
  * Fix for a bug in `AsyncAnswers`. (Thanks to tinamou)
  * Mock `@GwtMock` fields in superclasses the same way that Mockito does.
    (Thanks to justinmk)
  * Fix for a conflict with PowerMock.

### 1.1.0
  * Support for GWT-RPC by returning mock async interfaces when GWT.creating
    the synchronous interface
  * Support for testing widgets that expand Panel classes by automatically
    stubbing Panel methods.
  * Ability to customize classes that are automatically stubbed in order to
    support widgets extending third-party base classes.
  * Ability to customize the classloader and classpath used by GwtMockito for
    better tool integration.
  * More flexible FakeProviders that allow the returned type to be unrelated
    to the input type. **Note that this can be a breaking change in some
    cases: `getFake` should now just take a `Class<?>` instsead of a 
    `Class<? extends T>`**. See [here][10] for an example.

### 1.0.0
  * Initial release

[1]: https://code.google.com/p/mockito/
[2]: https://static.javadoc.io/com.google.gwt.gwtmockito/gwtmockito/1.1.8/com/google/gwtmockito/GwtMockito.html#useProviderForType-java.lang.Class-com.google.gwtmockito.fakes.FakeProvider-
[3]: https://developers.google.com/web-toolkit/doc/latest/DevGuideCodingBasicsOverlay
[4]: https://en.wikipedia.org/wiki/Dependency_injection
[5]: https://search.maven.org/remotecontent?filepath=com/google/gwt/gwtmockito/gwtmockito/1.1.8/gwtmockito-1.1.8.jar
[6]: https://code.google.com/p/mockito/downloads/list
[7]: http://www.jboss.org/javassist/downloads
[8]: https://www.javadoc.io/doc/com.google.gwt.gwtmockito/gwtmockito/1.1.8
[9]: https://github.com/google/gwtmockito/tree/master/gwtmockito-sample/src
[10]: https://github.com/google/gwtmockito/commit/52b5ddfc08df1b630cd1f241d2afaa08fed82a77
