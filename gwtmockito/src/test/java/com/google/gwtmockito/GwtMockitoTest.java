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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.dev.util.collect.HashSet;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.IFrameElement;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.dom.client.Node;
import com.google.gwt.http.client.URL;
import com.google.gwt.i18n.client.BidiPolicy;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.Messages;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.DataResource;
import com.google.gwt.resources.client.ExternalTextResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.ResourceCallback;
import com.google.gwt.resources.client.ResourceException;
import com.google.gwt.resources.client.TextResource;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.safehtml.shared.SafeUri;
import com.google.gwt.safehtml.shared.UriUtils;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.CellList;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.cellview.client.SimplePager;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.InlineHTML;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.SimpleLayoutPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.ValueListBox;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.datepicker.client.DatePicker;
import com.google.gwtmockito.fakes.FakeProvider;

import org.hamcrest.BaseMatcher;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Description;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;

import java.util.Date;
import java.util.Set;

/**
 * Tests for {@link GwtMockito} when running with {@link GwtMockitoTestRunner}.
 */
@RunWith(GwtMockitoTestRunner.class)
public class GwtMockitoTest {

  @GwtMock SampleInterface mockedInterface;
  @Mock Element element;
  @Mock PackagePrivateClass packagePrivateClass;
  @Mock Object someMock;

  @Before
  public void setUp() {
    GWT.create(SomeUiBinder.class); // Make sure GWT.create in setUp works
  }

  @Test
  public void shouldReturnMocksFromGwtCreate() {
    SampleInterface createdInterface = GWT.create(SampleInterface.class);
    createdInterface.doSomething();
    verify(mockedInterface).doSomething();
  }

  @Test
  public void shouldCreateFakeUiBinders() {
    SampleWidget widget = new SampleWidget();
    widget.setText("text");
    verify(widget.label).setText("text");
  }

  /**
   * Since ensureDebugId is final, we need to explicitly ensure that
   * GWT.create(DebugIdImpl.class) return a mock instead of null.
   */
  @Test
  public void shouldNotBreakWhenCallingEnsureDebugId() {
    new SampleWidget().label.ensureDebugId("xxx");
  }

  /**
   * {@link BidiPolicy} relies on a GWT.create'd implementation class, so we
   * should return a mock to avoid surprising NPEs.
   */
  @Test
  public void shouldNotBreakWhenCallingIsBidiEnabled() {
    assertFalse(BidiPolicy.isBidiEnabled());
  }

  /**
   * {@link History} relies on a GWT.create'd implementation class, so we
   * should return a mock to avoid surprising NPEs.
   */
  @Test
  public void shouldNotBreakWhenCallingHistoryNewItem() {
    History.newItem("foo");
  }

  @Test
  public void shouldCreateFakeMessages() {
    SampleMessages messages = GWT.create(SampleMessages.class);

    assertEquals("noArgs", messages.noArgs());
    assertEquals("oneArg(somearg)", messages.oneArg("somearg"));
    assertEquals("twoArgs(onearg, twoarg)", messages.twoArgs("onearg", "twoarg"));
    assertEquals("safeHtml(arg)",
        messages.safeHtml(SafeHtmlUtils.fromTrustedString("arg")).asString());
    assertEquals("safeHtmlWithUri(argX, http://uriY)",
        messages.safeHtmlWithUri(SafeHtmlUtils.fromTrustedString("argX"),
            UriUtils.fromSafeConstant("http://uriY")).asString());
  }

  @Test
  public void shouldGenerateHashCodeForMessages() {
    SampleMessages messages = GWT.create(SampleMessages.class);
    SampleMessages2 messages2 = GWT.create(SampleMessages2.class);

    final Set<Messages> set = new HashSet<>();

    set.add(messages);
    set.add(messages2);
    assertEquals(2, set.size());

    set.add(messages);
    assertEquals(2, set.size());
  }

  @Test
  public void shouldCreateFakeCssResources() {
    SampleCss css = GWT.create(SampleCss.class);

    assertEquals("style1", css.style1());
    assertEquals("style2", css.style2());
  }

  @Test
  public void shouldNotBreakWhenInjectingCssResources() {
    SampleCss css = GWT.create(SampleCss.class);
    css.ensureInjected();
  }

  @Test
  public void shouldUseFakeMessagesInUiBinder() {
    GwtMockito.initMocks(this);
    SampleWidget widget = new SampleWidget();

    assertEquals("style1", widget.css.style1());

    GwtMockito.tearDown();
  }

  @Test
  public void shouldInitMockitoMocks() {
    assertNotNull(someMock);
  }

  @Test
  public void canUseProvidersForTypes() {
    GwtMockito.useProviderForType(AnotherInterface.class, new FakeProvider<AnotherInterface>() {
      @Override
      public AnotherInterface getFake(Class<?> type) {
        return new AnotherInterface() {
          @Override
          public String doSomethingElse() {
            return "some value";
          }
        };
      }
    });

    AnotherInterface result = GWT.create(AnotherInterface.class);

    assertEquals("some value", result.doSomethingElse());
  }

  @Test
  public void typeProvidersShouldWorkForSubtypes() {
    final Widget someWidget = mock(Widget.class);

    GwtMockito.useProviderForType(Widget.class, new FakeProvider<Widget>() {
      @Override
      public Widget getFake(Class<?> type) {
        assertTrue(type == Label.class);
        return someWidget;
      }
    });

    assertSame(someWidget, GWT.create(Label.class));
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldNotAllowProvidersForGwtMockedTypes() {
    GwtMockito.useProviderForType(SampleInterface.class, new FakeProvider<SampleInterface>() {
      @Override
      public SampleInterface getFake(Class<?> type) {
        return mock(SampleInterface.class);
      }
    });
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldNotAllowMultipleGwtMocksForSameType() {
    GwtMockito.initMocks(new Object() {
      @GwtMock SampleInterface mock1;
      @GwtMock SampleInterface mock2;
    });
  }

  @Test
  public void shouldMockGwtElements() {
    when(element.getClassName()).thenReturn("class"); // getClassName() is final native
    Assert.assertEquals("class", element.getClassName());
  }

  @Test
  public void shouldMockUiBinders() {
    class Owner {
      @UiField Element uiField;
    }
    Owner owner = new Owner();
    SomeUiBinder uiBinder = GWT.create(SomeUiBinder.class);
    uiBinder.createAndBindUi(owner);

    when(owner.uiField.getClassName()).thenReturn("class");
    Assert.assertEquals("class", owner.uiField.getClassName());
  }

  @Test
  public void shouldCallNativeMethodsWithoutFailures() throws Exception {
    class SomeComposite extends Composite {
      public SomeComposite() {
        // initWidget relies on native calls
        initWidget(mock(Widget.class));
      }
      private native boolean runNativeMethod() /*-{
        return true;
      }-*/;
    }

    // Note that the result will be false even though the native method should return true - we
    // can't run the actual javascript and have to return a default value
    assertFalse(new SomeComposite().runNativeMethod());
  }

  @Test
  public void shouldReturnMocksFromNativeMethods() throws Exception {
    class SomeClass {
      private native Element getDocument() /*-{
        return $doc;
      }-*/;
    }

    Element document = new SomeClass().getDocument();
    document.setClassName("clazz");
    verify(document).setClassName("clazz");
  }

  @Test
  public void shouldReturnDefaultValuesForPrimitiveWrappersFromNativeMethods() throws Exception {
    class SomeClass {
      private native Boolean getBoolean();
      private native Byte getByte();
      private native Character getCharacter();
      private native Double getDouble();
      private native Float getFloat();
      private native Integer getInteger();
      private native Long getLong();
      private native Short getShort();
    }

    assertEquals(false, new SomeClass().getBoolean());
    assertEquals(0, (char) new SomeClass().getCharacter());
    assertEquals(0, new SomeClass().getDouble(), 0.01);
    assertEquals(0, new SomeClass().getFloat(), 0.01);
    assertEquals(0, (int) new SomeClass().getInteger());
    assertEquals(0, (long) new SomeClass().getLong());
    assertEquals(0, (short) new SomeClass().getShort());
  }

  @Test
  public void shouldReturnFirstValueForEnumsFromNativeMethods() throws Exception {
    class SomeClass {
      private native SomeEnum getSomeEnum();
    }

    assertEquals(SomeEnum.ONE, new SomeClass().getSomeEnum());
  }

  @Test
  public void shouldReturnEmptyStringsFromNativeMethods() throws Exception {
    class SomeClass {
      private native String getString() /*-{
        return "foo";
      }-*/;
    }

    assertEquals("", new SomeClass().getString());
  }

  /**
   * This test exists to ensure that the GwtMockito classloader doesn't conflict with the built-in
   * classloader when loading mockito classes.
   */
  @Test
  public void shouldAllowArgumentMatchers() throws Exception {
    element.setClassName("classname");
    verify(element).setClassName(ArgumentMatchers.argThat(new ArgumentMatcher<String>() {
      @Override
      public boolean matches(String s) {
        return s.equals("classname");
      }
    }));
  }

  /**
   * This test would fail if the context classloader weren't set correctly, causing mocks to be
   * created by a different classloader so that they couldn't reference their non-public parent.
   */
  @Test
  public void shouldMockPackagePrivateClasses() throws Exception {
    when(packagePrivateClass.doStuff()).thenReturn("mocked");
    Assert.assertEquals("mocked", packagePrivateClass.doStuff());
  }

  @Test
  public void shouldMockClientBundles() throws Exception {
    SomeClientBundle clientBundle = GWT.create(SomeClientBundle.class);

    // CSS resources should be faked
    assertEquals("style1", clientBundle.css().style1());

    // Other internal resources should return their names from getSafeUri or getText
    assertEquals("data", clientBundle.data().getSafeUri().asString());
    assertEquals(0, clientBundle.image().getHeight());
    assertEquals(0, clientBundle.image().getLeft());
    assertEquals("image", clientBundle.image().getSafeUri().asString());
    assertEquals(0, clientBundle.image().getTop());
    assertEquals(0, clientBundle.image().getWidth());
    assertEquals(false, clientBundle.image().isAnimated());
    assertEquals("text", clientBundle.text().getText());

    // External resources should return their name in a callback
    final StringBuilder result = new StringBuilder();
    clientBundle.externalText().getText(new ResourceCallback<TextResource>() {
      @Override
      public void onSuccess(TextResource resource) {
        result.append(resource.getText());
      }
      @Override
      public void onError(ResourceException e) {
        throw new RuntimeException(e);
      }
    });
    assertEquals("externalText", result.toString());
  }

  @Test
  public void shouldMockCustomClientBundles() throws Exception {
    SvgClientBundle clientBundle = GWT.create(SvgClientBundle.class);
    assertTrue(clientBundle.icon().transform() instanceof DataResource);
  }

  /**
   * This would fail if we didn't stub the create methods from DOM. See
   * https://github.com/google/gwtmockito/issues/4.
   */
  @Test
  @SuppressWarnings("unused")
  public void shouldAllowCreatingLayoutPanels() {
    new SimpleLayoutPanel();
    // Expect no exceptions
  }

  @Test
  @SuppressWarnings("unused")
  public void testShouldAllowLayoutPanelSubclasses() {
    class MyPanel extends SimpleLayoutPanel {
      public MyPanel() {
        Label label = GWT.create(Label.class);
        add(label);
      }
    }
    new MyPanel();
  }

  @Test
  public void shouldMockResultsOfStaticDomCreateMethods() {
    com.google.gwt.user.client.Element div = DOM.createDiv();
    when(div.getClassName()).thenReturn("stubClass");
    assertEquals("stubClass", div.getClassName());
  }

  @Test
  @SuppressWarnings("unused")
  public void shouldAllowOnlyJavascriptCastsThatAreValidJavaCasts() {
    // Casts to ancestors should be legal
    JavaScriptObject o = Document.get().createDivElement().cast();
    Node n = Document.get().createDivElement().cast();
    DivElement d = Document.get().createDivElement().cast();

    // Casts to sibling elements shouldn't be legal (even though they are in javascript)
    try {
      IFrameElement i = Document.get().createDivElement().cast();
      fail("Exception not thrown");
    } catch (ClassCastException expected) {}
  }

  @Test
  public void canUseProvidersForDifferentTypes() {
    GwtMockito.useProviderForType(Button.class, new FakeProvider<Label>() {
      @Override
      public Label getFake(Class<?> type) {
        Label label = mock(Label.class);
        when(label.getText()).thenReturn("abc");
        return label;
      }
    });
    Label label = GWT.create(Button.class);
    assertEquals("abc", label.getText());
  }

  @Test
  public void testCanCreateUiBindersWithParameterizedTypes() throws Exception {
    ParameterizedTypeUiBinder binder = GWT.create(ParameterizedTypeUiBinder.class);
    assertNotNull(binder.createAndBindUi(this));
  }

  @Test
  public void shouldAlwaysUseMostSpecificProvider() {
    GwtMockito.useProviderForType(AnotherInterface.class, new FakeProvider<AnotherInterface>() {
      @Override
      public AnotherInterface getFake(Class<?> type) {
        AnotherInterface mock = (AnotherInterface) mock(type);
        when(mock.doSomethingElse()).thenReturn("anotherInterface");
        return mock;
      }
    });
    SubSubInterface i = GWT.create(SubSubInterface.class);
    assertEquals("anotherInterface", i.doSomethingElse());

    GwtMockito.useProviderForType(SubInterface.class, new FakeProvider<SubInterface>() {
      @Override
      public SubInterface getFake(Class<?> type) {
        SubInterface mock = (SubInterface) mock(type);
        when(mock.doSomethingElse()).thenReturn("subInterface");
        return mock;
      }
    });
    i = GWT.create(SubSubInterface.class);
    assertEquals("subInterface", i.doSomethingElse());

    GwtMockito.useProviderForType(SubSubInterface.class, new FakeProvider<SubSubInterface>() {
      @Override
      public SubSubInterface getFake(Class<?> type) {
        SubSubInterface mock = (SubSubInterface) mock(type);
        when(mock.doSomethingElse()).thenReturn("subSubInterface");
        return mock;
      }
    });
    i = GWT.create(SubSubInterface.class);
    assertEquals("subSubInterface", i.doSomethingElse());
  }

  @Test
  public void shouldFailForAmbiguousProviders() {
    GwtMockito.useProviderForType(AnotherInterface.class, new FakeProvider<AnotherInterface>() {
      @Override
      public AnotherInterface getFake(Class<?> type) {
        return mock(AnotherInterface.class);
      }
    });
    GwtMockito.useProviderForType(YetAnotherInterface.class,
        new FakeProvider<YetAnotherInterface>() {
      @Override
      public YetAnotherInterface getFake(Class<?> type) {
        return mock(YetAnotherInterface.class);
      }
    });

    try {
      GWT.create(AmbiguousInterface.class);
      fail("Exception not thrown");
    } catch (IllegalArgumentException e) {
      assertTrue(e.getMessage().contains("AmbiguousInterface"));
      assertTrue(e.getMessage().contains("AnotherInterface"));
      assertTrue(e.getMessage().contains("YetAnotherInterface"));
    }
  }

  @Test
  public void assertThatShouldWork() {
    // This fails when org.hamcrest isn't added to the package excludelist
    Assert.assertThat("123", CoreMatchers.containsString("12"));
  }

  @Test
  public void getFakeShouldReturnDefaultFakes() {
    SampleMessages messages = GwtMockito.getFake(SampleMessages.class);
    assertEquals("noArgs", messages.noArgs());
  }

  @Test
  public void getFakeShouldReturnRegisteredFakes() {
    GwtMockito.useProviderForType(
        AnotherInterface.class,
        new FakeProvider<AnotherInterface>() {
          @Override
          public AnotherInterface getFake(Class<?> type) {
            AnotherInterface mock = mock(AnotherInterface.class);
            when(mock.doSomethingElse()).thenReturn("string");
            return mock;
          }});

    AnotherInterface fake = GwtMockito.getFake(AnotherInterface.class);

    assertEquals("string", fake.doSomethingElse());
  }

  @Test
  public void getFakeShouldFailForUnregisteredFakes() {
    try {
      GwtMockito.getFake(SampleInterface.class);
      fail("Exception not thrown");
    } catch (IllegalArgumentException e) {
      assertTrue(e.getMessage().contains("SampleInterface"));
    }
  }

  @Test
  public void shouldBeAbleToInstantiateLabels() {
    assertNotNull(new Label());
    assertNotNull(new HTML());
    assertNotNull(new InlineLabel());
    assertNotNull(new InlineHTML());
  }

  @Test
  public void shouldBeAbleToInstantiateDatePicker() {
    assertNotNull(new DatePicker());
  }

  @Test
  public void shouldBeAbleToSetHtml() {
    new HTML().setHTML("html");
    new HTML().setHTML(SafeHtmlUtils.fromSafeConstant("html"));
  }

  @Test
  public void shouldBeAbleToSetTextInGrids() {
    Grid grid = new Grid();
    grid.setText(0, 0, "foo");
  }

  @Test
  public void shouldReturnRealElementTagNames() {
    assertEquals("div", Document.get().createDivElement().getTagName());
    assertEquals("span", Document.get().createSpanElement().getTagName());
    assertEquals("iframe", Document.get().createIFrameElement().getTagName());
  }

  @Test
  public void shouldReturnTextAsType() {
    assertEquals("text", InputElement.as(Document.get().createTextInputElement()).getType());
  }

  @Test
  public void shouldReturnFakeStringsFromURL() {
    assertEquals("encodeQueryStringImpl", URL.encodeQueryString(""));
    assertEquals("encodePathSegmentImpl", URL.encodePathSegment(""));
  }
  
  @Test
  public void shouldBeAbleToInstantiateCellLists() {
    assertNotNull(new CellList<String>(new AbstractCell<String>() {
      @Override
      public void render(Context context, String value, SafeHtmlBuilder sb) {}
    }));
  }

  @Test
  @SuppressWarnings("unused")
  public void shouldBeAbleToInstantiateRadioAndCheckBoxes() {
    new RadioButton("foo");
    new CheckBox();
  }

  @Test
  @SuppressWarnings("deprecation")
  public void shouldBeAbleToFormatDatesWithAmPm() {
    DateTimeFormat formatter = DateTimeFormat.getFormat("yyyy/MM/dd hh:mm:ss a");
    assertEquals("1992/11/09 12:34:56 PM", formatter.format(new Date(92, 10, 9, 12, 34, 56)));
  }

  @Test
  @SuppressWarnings("unused")
  public void shouldBeAbleToCreateImages() {
    new Image("url");
  }

  @Test
  @SuppressWarnings("unused")
  public void shouldBeAbleToCreateCellTables() {
    new CellTable<String>();
  }

  @Test
  @SuppressWarnings("unused")
  public void shouldBeAbleToCreateAnchors() {
    new Anchor();
  }

  @Test
  @SuppressWarnings("unused")
  public void shouldBeAbleToCreateDataGrids() {
    new DataGrid<String>();
  }

  @Test
  public void shouldBeAbleToAddListBoxItems() {
    new ListBox().addItem("item");
  }

  @Test
  public void shouldBeAbleToConfigureTextBoxes() {
    new TextBox().setMaxLength(123);
  }

  @Test
  public void shouldBeAbleToUseHistory() {
    History.newItem("token");
    assertEquals("token", History.getToken());
  }

  @Test
  public void shouldBeAbleToUseSimplePager() {
    new SimplePager().setDisplay(new DataGrid<String>());
  }

  @Test
  @SuppressWarnings("unused")
  public void shouldBeAbleToCreateValueListBox() {
    new ValueListBox<Object>();
  }

  static class PackagePrivateClass {
    String doStuff() {
      return "not mocked";
    }
  }

  interface SomeUiBinder extends UiBinder<Widget, Object> {}
  interface ParameterizedTypeUiBinder extends UiBinder<DataGrid<String>, Object> {}

  private interface SampleInterface {
    String doSomething();
  }

  private interface AnotherInterface {
    String doSomethingElse();
  }

  private interface YetAnotherInterface {
    String doSomethingElseAgain();
  }

  private interface SubInterface extends AnotherInterface {}

  private interface SubSubInterface extends SubInterface {}

  private interface AmbiguousInterface extends AnotherInterface, YetAnotherInterface {}

  interface SampleMessages extends Messages {
    String noArgs();
    String oneArg(String arg);
    String twoArgs(String arg1, String arg2);
    SafeHtml safeHtml(SafeHtml arg);
    SafeHtml safeHtmlWithUri(SafeHtml arg1, SafeUri arg2);
  }

  interface SampleMessages2 extends SampleMessages {

  }

  interface SampleCss extends CssResource {
    String style1();
    String style2();
  }

  interface SomeClientBundle extends ClientBundle {
    SampleCss css();
    DataResource data();
    ExternalTextResource externalText();
    ImageResource image();
    TextResource text();
  }

  interface SvgResource extends DataResource {
    SvgResource transform();
  }

  interface SvgClientBundle extends ClientBundle {
    SvgResource icon();
  }

  enum SomeEnum {
    ONE, TWO
  }

  private static class SampleWidget extends Composite {

    interface MyUiBinder extends UiBinder<Widget, SampleWidget> {}

    @UiField Label label;
    @UiField SampleCss css;

    SampleWidget() {
      MyUiBinder uiBinder = GWT.create(MyUiBinder.class);
      initWidget(uiBinder.createAndBindUi(this));
    }

    void setText(String text) {
      label.setText(text);
    }
  }
}
