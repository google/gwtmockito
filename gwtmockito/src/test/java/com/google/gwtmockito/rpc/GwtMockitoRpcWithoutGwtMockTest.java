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
package com.google.gwtmockito.rpc;

import static com.google.gwtmockito.AsyncAnswers.returnSuccess;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.google.gwt.core.shared.GWT;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HasText;
import com.google.gwt.user.client.ui.Label;
import com.google.gwtmockito.GwtMockito;
import com.google.gwtmockito.GwtMockitoTestRunner;
import com.google.gwtmockito.fakes.FakeProvider;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests that GwtMockito works when mocking gwt-rpc RemoteServices without a corresponding
 * GwtMock-annoted field.
 */
@RunWith(GwtMockitoTestRunner.class)
public class GwtMockitoRpcWithoutGwtMockTest {

  private static class MyWidget {
    @UiField HasText message = GWT.create(Label.class);
    private final TestRemoteServiceAsync service = GWT.create(TestRemoteService.class);

    MyWidget() {
      service.doRpcWithoutArgs(new AsyncCallback<String>() {
        @Override
        public void onSuccess(String result) {
          message.setText(result);
        }

        @Override
        public void onFailure(Throwable caught) {
          message.setText(caught.getMessage());
        }
      });
    }
  }

  @Test
  public void shouldNeverCallBackIfNoFake() throws Exception {
    MyWidget widget = new MyWidget();

    verify(widget.message, never()).setText(anyString());
  }

  @Test
  public void shouldUseFakeIfProvided() {
    GwtMockito.useProviderForType(
        TestRemoteService.class,
        new FakeProvider<TestRemoteServiceAsync>() {
          @Override
          public TestRemoteServiceAsync getFake(Class<?> type) {
            TestRemoteServiceAsync mock = mock(TestRemoteServiceAsync.class);
            doAnswer(returnSuccess("faked")).when(mock).doRpcWithoutArgs(anyAsyncCallback());
            return mock;
          }
        });
    MyWidget widget = new MyWidget();

    verify(widget.message).setText("faked");
  }

  @SuppressWarnings("unchecked")
  private AsyncCallback<String> anyAsyncCallback() {
    return any(AsyncCallback.class);
  }
}
