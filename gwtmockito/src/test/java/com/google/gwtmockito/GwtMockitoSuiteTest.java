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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * Test checking that GwtMockitoRunner works when used on a test inside a test suite.
 */
@RunWith(Suite.class)
@SuiteClasses({GwtMockitoSuiteTest.MyTest.class, GwtMockitoSuiteTest.MyOtherTest.class})
public class GwtMockitoSuiteTest {

  /** A test using GwtMockitoRunner */
  @RunWith(GwtMockitoTestRunner.class)
  public static class MyTest {
    private boolean ranBefore;

    @Before
    public void before() {
      ranBefore = true;
    }

    @Test
    public void testTautology() {
      Assert.assertTrue(ranBefore);
    }
  }

  /** A test using the default runner */
  @RunWith(JUnit4.class)
  public static class MyOtherTest {
    @Test
    public void testShouldHaveResetContextClassLoader() {
      String contextClassLoaderName = Thread.currentThread()
          .getContextClassLoader()
          .getClass()
          .getName();
      Assert.assertFalse("Unexpected context class loader: " + contextClassLoaderName,
          contextClassLoaderName.contains("gwt"));
    }
  }
}
