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

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Wrapper for a lazily actualized value. The value is actualized as soon as
 * either (#get()), (#equals(Object)), (#hashCode()) or (#toString()) is
 * called, or it is explicitly is actualized via (#actualize())
 *
 * @param <T> the type of the wrapped object
 */
public final class Lazy<T> implements Supplier<T> {

  volatile Supplier<? extends T> supplier;
  Throwable error;
  T value;

  /**
   * Convenience factory method, see (#Lazy()).
   *
   * @param supplier to resolve the object, the supplier is released for
   * garbage collection as soon as the value is actualized
   * @param <T> the type of wrapped object
   * @return `new Lazy<T>(supplier)`
   */
  public static <T> Lazy<T> lazy(Supplier<? extends T> supplier) {
    return new Lazy<>(supplier);
  }

  /**
   * @param supplier to resolve the object, the supplier is released for
   * garbage collection as soon as the value is actualized
   */
  public Lazy(Supplier<? extends T> supplier) {
    Objects.requireNonNull(supplier);
    this.supplier = supplier;
  }

  /**
   * Actualize and return the value.
   *
   * @return the actualized object
   */
  @Override
  public final T get() {
    if (!isActualized()) {
      actualize();
    }
    if (hasFailed()) {
      throw new IllegalStateException("Actualization failed.", error);
    }
    return value;
  }

  /**
   * Actualize value, `supplier` is released for garbage collection.
   *
   * *Important:* After returning from this function the Lazy will always be
   * in _actualized_ state. Raised exceptions are recorded and rethrown. In
   * that case (#hasFailed()) will return `true` and further calls to (#get())
   * will throw an (#IllegalStateException) with the raised exception as cause.
   */
  public synchronized void actualize() {
    if (supplier != null) {
      try {
        value = supplier.get();
      } catch (Throwable e) {
        error = e;
        throw e;
      } finally {
        supplier = null;
      }
    }
  }

  /**
   * Check if the value was already actualized.
   *
   * @return `true` if the value was already actualized; `false` otherwise
   */
  public final boolean isActualized() {
    return supplier == null;
  }

  /**
   * Check if actualization has failed.
   *
   * @return `true` if the actualization has failed, `false` otherwise,
   * especially as long as the value was not actualized yet.
   */
  public final boolean hasFailed() {
    return error != null;
  }

  /**
   * Resolve the wrapped object and return its hash code value
   *
   * @return a hash code value for the wrapped object.
   */
  @Override
  public int hashCode() {
    return Objects.hashCode(get());
  }

  /**
   * Compare the values of this and the {@link Lazy} {@code other},
   * consequently both become actualized.
   *
   * @param other the reference object with which to compare.
   * @return {@code true} if {@code other} is a {@link Lazy} wrapping an equal object;
   * {@code false} otherwise.
   */
  @Override
  public boolean equals(Object other) {
    return this == other
           || other != null && other instanceof Lazy<?>
              && Objects.equals(get(), ((Lazy<?>) other).get());
  }

  /**
   * Resolve the wrapped object and return its string representation
   *
   * @return string representation of the wrapped object.
   */
  @Override
  public String toString() {
    return Objects.toString(get());
  }

  /**
   * Returns a new (#Lazy) mapping the actualized value. E.g. given
   *
   * ```
   * Lazy a = lazy(() -> 1)
   * Lazy b = a.map(x -> x + 1)
   * ```
   *
   * - `b.get()` would actualize both `a` and `b`. The value of `b` would be `2`.
   * - `a.get()` on the other hand would only actualize `a`.
   *
   * `mapping` is released for garbage collection as soon as the mapped value
   * is actualized.
   */
  public <U> Lazy<U> map(Function<? super T, ? extends U> mapping) {
    return new Lazy<>(() -> mapping.apply(get()));
  }

  /**
   * Returns a new (#Lazy) mapping the actualized value of `this`. E.g. given
   *
   * ```
   * Lazy a = lazy(() -> 1)
   * Lazy b = a.map(x -> new Lazy(() -> x + 1))
   * ```
   *
   * - `b.get()` would actualize both `a` and `b`. The value of `b` would be `2`.
   * - `a.get()` on the other hand would only actualize `a`.
   *
   * `mapping` as well as the returned (#Lazy) is released for garbage collection
   * as soon as `b` is actualized.
   */
  public <U> Lazy<U> flatMap(Function<? super T, ? extends Lazy<? extends U>> mapping) {
    return new Lazy<>(() -> mapping.apply(get()).get());
  }

  /**
   * Get an exception caught during actualization. Return `Optional.empty()`
   * before actualization or if actualization succeeded. Call `(#actualize())`
   * to enforce actualization beforehand.
   */
  public Optional<Throwable> error() {
    return Optional.ofNullable(error);
  }

  /**
   * Get the actualize value. Calling before the value is actualized
   * will return `Optional.empty()`.
   *
   * This will also return `Optional.empty()` if actualization failed or if
   * the actualized value is `null`.
   */
  public Optional<T> actualized() {
    return Optional.ofNullable(value);
  }

  @SuppressWarnings({"unchecked", "ConstantConditions"})
  private static <R, E extends Exception> R throwUnchecked(Exception e) throws E {
    throw (E) e;
  }
}
