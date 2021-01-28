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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation allowing the test to configure the set of classes that will be stubbed completely
 * when the test is executed. If a class is stubbed, all of its non-abstract methods (including
 * native and final methods) will be replaced with a no-op implementation that returns a fake value
 * if needed. This stubbing takes place at the classloader level, so all instances of the class
 * created during the execution of the test will be stubbed. This can be a useful way to work around
 * dependencies that do things that aren't safe in unit tests, such as relying on the DOM API.
 * <p>
 * An example usage follows:
 *
 * <pre>
 * &#064;RunWith(GwtMockitoTestRunner.class)
 * &#064;WithClassesToStub({ClassToStub.class})
 * public class MyTest {
 *
 *   &#064;Test
 *   public void shouldStubClass() {
 *     // The stubbed class will return an empty string instead of throwing an exception
 *     assertEquals(&quot;&quot;, new ClassToStub().doSomething());
 *   }
 *
 *   static class ClassToStub {
 *     String doSomething() {
 *       throw new UnsupportedOperationException(&quot;this shouldn't be thrown&quot;);
 *     }
 *   }
 * }
 * </pre>
 * <p>
 * Note that if you have a set of classes that need to be stubbed in every class, you might be
 * better off implementing your own subclass of {@link GwtMockitoTestRunner} that overrides
 * {@link GwtMockitoTestRunner#getClassesToStub} so that you don't have to duplicate the annotation
 * in every test.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface WithClassesToStub {
  /**
   * The set of classes to be stubbed during the execution of all test methods in the annotated test
   * class.
   */
  Class<?>[] value();
}
