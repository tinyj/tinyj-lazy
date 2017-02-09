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

import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class LazyConcurrentTest {

  @Test
  public void concurrent_get_invokes_supplier_only_once() throws Exception {
    List<Worker> workers = Stream.generate(Worker::new).limit(7).collect(Collectors.toList());
    for (int i = 0; i < 200; i++) {
      Lazy<String> lazy = Lazy.lazy(new OnetimeThreadIdSupplier(i));
      workers.forEach(worker -> worker.reset(lazy));

      workers.forEach(Worker::go);
      List<String> results = workers.stream().map(Worker::result).collect(Collectors.toList());

      assertThat(results).containsOnly(results.get(0));
      assertThat(results.get(0)).startsWith(i + " - ");
    }
    workers.forEach(Worker::finish);
  }

  @Test(timeOut = 500)
  public void if_actualization_raises_an_exception_it_falls_through_in_the_originating_thread() throws Exception {
    Lazy<String> lazy = Lazy.lazy(() -> unsafeDelayedReturn(1000, "value"));
    Map<Thread, Throwable> uncaughtExceptions = new ConcurrentHashMap<>();
    Thread thread1 = new Thread(lazy::get);
    Thread thread2 = new Thread(lazy::get);
    thread1.setUncaughtExceptionHandler(uncaughtExceptions::put);
    thread2.setUncaughtExceptionHandler(uncaughtExceptions::put);

    thread1.start();
    Thread.sleep(50);

    thread2.start();
    Thread.sleep(50);

    thread1.interrupt();

    thread1.join();
    thread2.join();

    assertThat(uncaughtExceptions.get(thread1))
        .isInstanceOf(InterruptedException.class);
  }

  @Test(timeOut = 500)
  public void if_actualization_raises_an_exception_threads_other_than_the_originating_thread_receive_that_exception_wrapped_in_an_IllegalStateException() throws Exception {
    Lazy<String> lazy = Lazy.lazy(() -> unsafeDelayedReturn(1000, "value"));
    Map<Thread, Throwable> uncaughtExceptions = new ConcurrentHashMap<>();
    Thread thread1 = new Thread(lazy::get);
    Thread thread2 = new Thread(lazy::get);
    thread1.setUncaughtExceptionHandler(uncaughtExceptions::put);
    thread2.setUncaughtExceptionHandler(uncaughtExceptions::put);

    thread1.start();
    Thread.sleep(50);

    thread2.start();
    Thread.sleep(50);

    thread1.interrupt();

    thread1.join();
    thread2.join();

    assertThat(uncaughtExceptions.get(thread2))
        .isInstanceOf(IllegalStateException.class)
        .hasCauseInstanceOf(InterruptedException.class);
  }

  @Test(expectedExceptions = AssertionError.class)
  public void test_OnetimeThreadIdSupplier_throws_AssertionError_if_invoked_twice() throws Exception {
    OnetimeThreadIdSupplier supplier = new OnetimeThreadIdSupplier(1);
    supplier.get();

    supplier.get();

    // AssertionError expected
  }

  private static <R> R unsafeDelayedReturn(int delayInMillis, R value) {
    return LazyConcurrentTest.<R, RuntimeException>delayedReturn(delayInMillis, value);
  }

  private static <R, E extends Exception> R delayedReturn(int delayInMillis, R value) throws E {
    try {
      Thread.sleep(delayInMillis);
      return value;
    } catch (InterruptedException e) {
      throw (E) e;
    }
  }

  private static class OnetimeThreadIdSupplier implements Supplier<String> {

    final AtomicBoolean calledTwice = new AtomicBoolean(false);
    private final int run;

    public OnetimeThreadIdSupplier(int run) {
      this.run = run;
    }

    @Override
    public String get() {
      if (!calledTwice.compareAndSet(false, true)) {
        throw new AssertionError("may only run once");
      }
      Thread.yield(); // give other thread the chance to run into the check above
      return Integer.toString(run) + " - " + Long.toHexString(Thread.currentThread().getId());
    }
  }

  private static class Worker extends Thread {
    Lazy<String> lazy;
    volatile String result;
    volatile boolean running = true;
    final Semaphore runLock = new Semaphore(1);
    final AtomicBoolean paused = new AtomicBoolean(true);

    public Worker() {
      start();
    }

    public void reset(Lazy<String> lazy) {
      runLock.acquireUninterruptibly();
      result = null;
      this.lazy = lazy;
    }

    public void go() {
      paused.set(false);
      LockSupport.unpark(this);
    }

    @Override
    public void run() {
      while (running) {
        while (!paused.compareAndSet(false, true)) {
          LockSupport.park(this);
        }
        Thread.yield();
        result = lazy.get();
        runLock.release();
      }
    }

    public String result() {
      try {
        runLock.acquireUninterruptibly();
        return result;
      } finally {
        runLock.release();
      }
    }

    public void finish() {
      running = false;
    }
  }
}