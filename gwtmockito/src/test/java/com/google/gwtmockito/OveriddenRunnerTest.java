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

import com.google.gwtmockito.OveriddenRunnerTest.MyGwtMockitoTestRunner;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.model.InitializationError;

import java.util.Collection;

/**
 * Tests confirming expected behavior of user-defined classes extending {@link GwtMockitoTestRunner}
 */
@RunWith(MyGwtMockitoTestRunner.class)
public class OveriddenRunnerTest {

  @Test
  public void shouldAllowCustomStubClasses() {
    assertEquals("", new ClassToStub().getString());
  }

  public static class MyGwtMockitoTestRunner extends GwtMockitoTestRunner {
    public MyGwtMockitoTestRunner(Class<?> unitTestClass) throws InitializationError {
      super(unitTestClass);
    }

    @Override
    protected Collection<Class<?>> getClassesToStub() {
      Collection<Class<?>> classes = super.getClassesToStub();
      classes.add(ClassToStub.class);
      return classes;
    }
  }

  public static class ClassToStub {
    public String getString() {
      return "this should be stubbed out";
    }
  }
}
