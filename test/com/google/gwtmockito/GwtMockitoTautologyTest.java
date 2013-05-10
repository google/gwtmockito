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

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * This test exists to ensure that it's possible to load multiple tests using
 * {@link GwtMockitoTestRunner} in the same suite without them conflicting.
 */
@RunWith(GwtMockitoTestRunner.class)
public class GwtMockitoTautologyTest {
  @Test
  public void testTautology() {}
}
