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

import com.google.gwt.user.client.rpc.AsyncCallback;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * A collection of answers useful for testing asynchronous GWT applications.
 *
 * @see Answer
 * @author ekuefler@google.com (Erik Kuefler)
 */
public class AsyncAnswers {

  /**
   * Invokes {@link AsyncCallback#onSuccess} on the first argument of type {@link AsyncCallback}
   * passed to the method. The method must take an {@link AsyncCallback} parameter of the
   * appropriate type. Used like this:
   *
   * <pre>
   *   doAnswer(returnSuccess("some data")).when(myService).getData(any(AsyncCallback.class));
   * </pre>
   *
   * @param result argument to pass to onSuccess
   * @return an answer that invokes onSuccess with the given argument
   */
  public static <T> Answer<Void> returnSuccess(final T result) {
    return new Answer<Void>() {
      @Override
      @SuppressWarnings("unchecked")
      public Void answer(InvocationOnMock invocation) {
        for (Object arg : invocation.getArguments()) {
          if (arg instanceof AsyncCallback<?>) {
            ((AsyncCallback<T>) arg).onSuccess(result);
            return null;
          }
        }
        throw new IllegalStateException(
            "returnSuccess can only be used for methods that take an AsyncCallback as a parameter");
      }
    };
  }

  /**
   * Invokes {@link AsyncCallback#onFailure} on the first argument of type {@link AsyncCallback}
   * passed to the method. The method must take an {@link AsyncCallback} parameter of the
   * appropriate type. Used like this:
   *
   * <pre>
   *   doAnswer(returnFailure(new IOException())).when(myService).getData(any(AsyncCallback.class));
   * </pre>
   *
   * @param result argument to pass to onFailure
   * @return an answer that invokes onFailure with the given argument
   */
  public static Answer<Void> returnFailure(final Throwable result) {
    return new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) {
        for (Object arg : invocation.getArguments()) {
          if (arg instanceof AsyncCallback<?>) {
            ((AsyncCallback<?>) arg).onFailure(result);
            return null;
          }
        }
        throw new IllegalStateException(
            "returnFailure can only be used for methods that take an AsyncCallback as a parameter");
      }
    };
  }
}
