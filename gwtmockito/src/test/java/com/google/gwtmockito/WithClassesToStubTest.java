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

import static org.junit.Assert.assertEquals;

import com.google.gwtmockito.WithClassesToStubTest.ClassToStub;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test checking the behavior of the {@link WithClassesToStub} annotation.
 */
@RunWith(GwtMockitoTestRunner.class)
@WithClassesToStub({ClassToStub.class})
public class WithClassesToStubTest {

  @Test
  public void shouldStubClassSpecifiedInAnnotation() {
    assertEquals("", new ClassToStub().doSomething());
  }

  static class ClassToStub {
    String doSomething() {
      throw new UnsupportedOperationException("this should be stubbed");
    }
  }
}
