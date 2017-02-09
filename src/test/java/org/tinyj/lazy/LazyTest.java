/*
Copyright 2017 Eric Karge <e.karge@struction.de>

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package org.tinyj.lazy;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Objects;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.tinyj.lazy.Lazy.lazy;

public class LazyTest {

  @Test
  public void actualize_actualizes_the_value_by_invoking_the_supplier__get_returns_the_acutalized_value() throws Exception {
    @SuppressWarnings("unchecked")
    Supplier<String> supplier = mock(Supplier.class);
    given(supplier.get()).willReturn("a value");
    Lazy<String> lazy = Lazy.lazy(supplier);

    lazy.actualize();

    verify(supplier, times(1)).get();

    String actualValue = lazy.get();

    verifyNoMoreInteractions(supplier);
    assertThat(actualValue).isEqualTo("a value");
  }

  @Test
  public void actualize_does_nothing_after_first_invocation() throws Exception {
    Supplier<?> supplier = mock(Supplier.class);
    Lazy<?> lazy = Lazy.lazy(supplier);

    lazy.actualize();

    verify(supplier, times(1)).get();

    lazy.actualize();

    verifyNoMoreInteractions(supplier);
  }

  @Test
  public void actualize_does_nothing_after_first_invocation__even_if_first_invocation_raised_an_exception() throws Exception {
    Lazy<?> lazy = Lazy.lazy(() -> raise(new UnsupportedOperationException()));

    try {
      lazy.actualize();
      fail("Exception expected");
    } catch (UnsupportedOperationException ignored) {
    }

    lazy.actualize();

    // nothing happend
  }

  @Test
  public void on_first_get_value_is_actualized_by_invoking_the_supplier() throws Exception {
    Lazy<String> lazyString = lazy(() -> "a value");

    String string = lazyString.get();

    assertThat(string).isEqualTo("a value");
  }

  @Test(expectedExceptions = RuntimeException.class)
  public void get_throws_if_supplier_throws() throws Exception {
    Lazy<Object> lazy = lazy(() -> raise(new UnsupportedOperationException()));

    lazy.get();

    fail("RuntimeException expected");
  }

  @Test
  public void supplier_is_invoked_just_once() throws Exception {
    Supplier<? extends String> supplier = supplier("a", "b");
    Lazy<String> lazyString = lazy(supplier);

    String first = lazyString.get();
    String second = lazyString.get();

    assertThat(first).isEqualTo("a");
    assertThat(second).isEqualTo("a");
    verify(supplier, times(1)).get();
  }

  @Test
  public void supplier_is_invoked_just_once_even_when_throwing() throws Exception {
    @SuppressWarnings("unchecked")
    Supplier<? extends String> supplier = mock(Supplier.class);
    doThrow(new UnsupportedOperationException()).when(supplier).get();
    Lazy<String> lazyString = lazy(supplier);

    try {
      lazyString.get();
      fail("RuntimeException expected");
    } catch (RuntimeException ignored) {
    }
    try {
      lazyString.get();
      fail("RuntimeException expected");
    } catch (RuntimeException ignored) {
    }

    verify(supplier, times(1)).get();
  }

  @Test
  public void if_actualization_raises_an_exception_it_falls_through_on_the_first_get() throws Exception {
    final UnsupportedOperationException thrown = new UnsupportedOperationException();
    Lazy<?> lazy = Lazy.lazy(() -> raise(thrown));

    try {
      lazy.get();
      fail("Exception expected");
    } catch (UnsupportedOperationException caught) {
      assertThat(caught).isSameAs(thrown);
    }
  }

  @Test
  public void if_actualization_raises_an_exception_it_gets_wrapped_on_subsequent_get() throws Exception {
    final UnsupportedOperationException thrown = new UnsupportedOperationException();
    Lazy<?> lazy = Lazy.lazy(() -> raise(thrown));

    try {
      lazy.get();
      fail("Exception expected");
    } catch (Exception ignored) {
    }

    try {
      lazy.get();
      fail("Exception expected");
    } catch (IllegalStateException caught) {
      assertThat(caught.getCause()).isSameAs(thrown);
    }
  }

  @DataProvider
  public static Object[][] equals_data() {
    Supplier<? extends String> supplier = supplier("a", "b");
    return new Object[][] {
        {supplier("a"), supplier("a"), true},
        {supplier(null), supplier(null), true},
        {supplier("a"), supplier(new String(new char[] {'a'})), true},
        {supplier("a"), supplier("b"), false},
        {supplier("a"), supplier(null), false},
        {supplier, supplier, false}
    };
  }

  @Test(dataProvider = "equals_data")
  public void equals_compares_wrapped_values(Supplier<?> a, Supplier<?> b, boolean equals) throws Exception {
    assertThat(lazy(a).equals(lazy(b))).isEqualTo(equals);
  }

  @Test
  public void hashCode_returns_wrapped_values_hashCode() throws Exception {
    assertThat(lazy(() -> "a value").hashCode())
        .isEqualTo("a value".hashCode());
  }

  @Test
  public void toString_returns_wrapped_values_toString() throws Exception {
    assertThat(lazy(() -> 1024).toString())
        .isEqualTo("1024");
  }

  @SafeVarargs
  @SuppressWarnings({"unchecked", "ResultOfMethodCallIgnored"})
  private static <T> Supplier<? extends T> supplier(T first, T... values) {
    final Supplier<T> supplier = mock(Supplier.class);
    doReturn(first, (Object[]) values).when(supplier).get();
    doReturn(Objects.toString(first)).when(supplier).toString();
    return supplier;
  }

  static <T, E extends Throwable> T raise(E e) throws E {
    throw e;
  }
}