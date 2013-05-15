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

Getting started with GwtMockito using Junit 4 is easy. Just annotate your test
with `@RunWith(GwtMockitoTestRunner.class)`, then any calls to `GWT.create`
encountered will return Mockito mocks instead of throwing exceptions:

    @RunWith(GwtMockitoTestRunner.class)
    public class MyTest {
      @Test
      public void shouldReturnMocksFromGwtCreate() {
        Label myLabel = GWT.create(Label.class);
        when(myLabel.getText()).thenReturn("some text");
        assertEquals("some text", myLabel.getText());
      }
    }

GwtMockito also creates fake implementations of all UiBinders that automatically
populate `@UiField`s with Mockito mocks. Suppose you have a widget that looks
like this:

    class MyWidget extends Composite {
      interface MyUiBinder extends UiBinder<Widget, MyWidget> {}
      private final MyUiBinder uiBinder = GWT.create(MyUiBinder.class);

      @UiField Label numberLabel;
      private final NumberFormatter formatter;

      MyWidget(NumberFormatter formatter) {
        this.formatter = formatter;
        initWidget(uiBinder.createAndBindUi(this);
      }

      void setNumber(int number) {
        numberLabel.setText(formatter.format(number));
      }
    }

When `createAndBindUi` is called, GwtMockito will automatically populate 
`numberLabel` with a mock object. Since `@UiField`s are package-visible, they 
can be read from your unit tests, which lets you test this widget as follows:

    @RunWith(GwtMockitoTestRunner.class)
    public class MyWidgetTest extends TestCase {

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

    class MyClass {
      MyClass() {
        SomeInterface myInterface = GWT.create(SomeInterface.class);
        myInterface.setSomething(true);
      }
    }

then you can verify that it works correctly by writing a test that looks like
this:

    @RunWith(GwtMockitoTestRunner.class)
    public class MyClassTest {
      @GwtMock SomeInterface mockInterface;

      @Test
      public void constructorShouldSetSomething() {
        new MyClass();
        verify(mockInterface).setSomething(true);
      }
    }

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

    @RunWith(GwtMockitoTestRunner.class)
    public class MyTest {
      @Mock Element element;

      @Test
      public void shouldReturnMocksFromGwtCreate() {
        when(element.getClassName()).thenReturn("mockClass");
        assertEquals("mockClass", myLabel.getClassName());
      }
    }

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

The test must explicitly call `initMocks` during its setup and `tearDown` when
it is being teared down, or else state can leak between tests. When 
instantiating a widget, the test must also subclass it and override initWidget
with a no-op implementation, or else it will fail when this method attempts to
call Javascript. Note that when testing in this way the features described in
"Mocking final classes and methods" and "Dealing with native methods" won't
work - there is no way to mock final methods or automatically replace native
methods without using `GwtMockitoTestRunner`.

## How do I install it?
A Maven repository will be available soon. In the meantime, do the following:

  1. Install the jars for the latest versions of [Mockito][5] and [Javassist][6]
     in your classpath.
  2. Download the GwtMockito jar from [here][7] or check it out directly using 
     git from <https://github.com/google/gwtmockito.git>.
  3. Start writing new tests or annotate existing ones with
     `@RunWith(GwtMockitoTestRunner.class)`.

## Where can I learn more?
  * For more details on the GwtMockito API, consult the [Javadoc][8]
  * For an example of using GwtMockito to test some example classes, see the
    [sample app][9].

[1]: https://code.google.com/p/mockito/
[2]: http://google.github.io/gwtmockito/javadoc/com/google/gwtmockito/GwtMockito.html#useProviderForType(java.lang.Class,%20com.google.gwtmockito.fakes.FakeProvider)
[3]: https://developers.google.com/web-toolkit/doc/latest/DevGuideCodingBasicsOverlay
[4]: http://en.wikipedia.org/wiki/Dependency_injection
[5]: https://code.google.com/p/mockito/downloads/list
[6]: http://www.jboss.org/javassist/downloads
[7]: TODO
[8]: http://google.github.io/gwtmockito/javadoc/
[9]: TODO
