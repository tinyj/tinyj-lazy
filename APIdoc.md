## org.tinyj.lazy

### Lazy\<T>
_[(src)](src/main/java/org/tinyj/lazy/Lazy.java)_ |
T: the type of the wrapped object  
_implements_ (#Supplier)

Wrapper for a lazily actualized value. The value is actualized as soon as
 either (#get()), (#equals(Object)), (#hashCode()) or (#toString()) is
 called, or it is explicitly is actualized via (#actualize())

**`Lazy(Supplier<? extends T> supplier)`** _(constructor)_  


**`lazy(Supplier<? extends T> supplier)`**  
⇒ *`Lazy<T>`* _(`new Lazy<T>(supplier)`)_  
Convenience factory method, see (#Lazy()).

**`get()`**  
⇒ *`T`* _(the actualized object)_  
Actualize and return the value.

**`actualize()`**  
Actualize value, `supplier` is released for garbage collection.

 *Important:* After returning from this function the Lazy will always be
 in _actualized_ state. Raised exceptions are recorded and rethrown. In
 that case (#hasFailed()) will return `true` and further calls to (#get())
 will throw an (#IllegalStateException) with the raised exception as cause.

**`isActualized()`**  
⇒ *`boolean`* _(`true` if the value was already actualized; `false` otherwise)_  
Check if the value was already actualized.

**`hasFailed()`**  
⇒ *`boolean`* _(`true` if the actualization has failed, `false` otherwise,
 especially as long as the value was not actualized yet.)_  
Check if actualization has failed.

**`hashCode()`**  
⇒ *`int`* _(a hash code value for the wrapped object.)_  
Resolve the wrapped object and return its hash code value

**`equals(Object other)`**  
⇒ *`boolean`* _({@code true} if {@code other} is a {@link Lazy} wrapping an equal object;
 {@code false} otherwise.)_  
Compare the values of this and the {@link Lazy} {@code other},
 consequently both become actualized.

**`toString()`**  
⇒ *`String`* _(string representation of the wrapped object.)_  
Resolve the wrapped object and return its string representation

**`map(Function<? super T, ? extends U> mapping)`**  
⇒ *`Lazy<U>`*  
Returns a new [`Lazy`](#lazyt) mapping the actualized value. E.g. given

 ```
 Lazy a = lazy(() -> 1)
 Lazy b = a.map(x -> x + 1)
 ```

 - `b.get()` would actualize both `a` and `b`. The value of `b` would be `2`.
 - `a.get()` on the other hand would only actualize `a`.

 `mapping` is released for garbage collection as soon as the mapped value
 is actualized.

**`flatMap(Function<? super T, ? extends Lazy<? extends U>> mapping)`**  
⇒ *`Lazy<U>`*  
Returns a new [`Lazy`](#lazyt) mapping the actualized value of `this`. E.g. given

 ```
 Lazy a = lazy(() -> 1)
 Lazy b = a.map(x -> new Lazy(() -> x + 1))
 ```

 - `b.get()` would actualize both `a` and `b`. The value of `b` would be `2`.
 - `a.get()` on the other hand would only actualize `a`.

 `mapping` as well as the returned [`Lazy`](#lazyt) is released for garbage collection
 as soon as `b` is actualized.

**`error()`**  
⇒ *`Optional<Throwable>`*  
Get an exception caught during actualization. Return `Optional.empty()`
 before actualization or if actualization succeeded. Call `(#actualize())`
 to enforce actualization beforehand.

**`actualized()`**  
⇒ *`Optional<T>`*  
Get the actualize value. Calling before the value is actualized
 will return `Optional.empty()`.

 This will also return `Optional.empty()` if actualization failed or if
 the actualized value is `null`.

