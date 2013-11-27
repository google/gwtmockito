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
import static org.mockito.Mockito.verify;

import com.google.gwt.core.shared.GWT;
import com.google.gwt.i18n.client.Messages;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwtmockito.fakes.FakeProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for {@link GwtMockito} when not running with
 * {@link GwtMockitoTestRunner}, as would be the case for JUnit3 or in tests
 * that must use a different runner.
 */
@RunWith(JUnit4.class)
public class GwtMockitoRunnerlessTest {

  @GwtMock SampleInterface mockedInterface;
  private static final SampleMessages MESSAGES = GwtMockito.getFake(SampleMessages.class);

  @Before
  public void setUp() {
    GwtMockito.initMocks(this);
  }

  @After
  public void tearDown() {
    GwtMockito.tearDown();
  }

  @Test
  public void shouldReturnMocksFromGwtCreate() {
    SampleInterface createdInterface = GWT.create(SampleInterface.class);
    createdInterface.doSomething();
    verify(mockedInterface).doSomething();
  }

  @Test
  public void shouldCreateFakeUiBinders() {
    SampleWidget widget = new SampleWidget() {
      @Override
      protected void initWidget(Widget widget) { /* disarm */ }
    };
    widget.setText("text");
    verify(widget.label).setText("text");
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

  @Test(expected = UnsupportedOperationException.class)
  public void shouldRestoreGwtCreateAfterTearDown() {
    GwtMockito.tearDown();
    GWT.create(SampleInterface.class);
  }

  @Test
  public void shouldHandleStaticallyInitializedFakes() {
    assertEquals("message", MESSAGES.message());
  }

  private interface SampleInterface {
    String doSomething();
  }

  private interface AnotherInterface {
    String doSomethingElse();
  }

  private interface SampleMessages extends Messages {
    String message();
  }

  private static class SampleWidget extends Composite {

    interface MyUiBinder extends UiBinder<Widget, SampleWidget> {}

    @UiField Label label;

    SampleWidget() {
      MyUiBinder uiBinder = GWT.create(MyUiBinder.class);
      initWidget(uiBinder.createAndBindUi(this));
    }

    void setText(String text) {
      label.setText(text);
    }
  }
}
