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

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import java.util.Random;
import java.util.function.Supplier;

@Fork(1)
@Warmup(iterations = 5)
@Measurement(iterations = 3)
public class LazyBenchmark {

  static final int INNER_ROUNDS = 20000;
  static final int OUTER_ROUNDS = 2;
  static final Random RND = new Random();
  public static final Supplier<String> THROWING_SUPPLIER = () -> {
    throw new RuntimeException(Integer.toHexString(RND.nextInt()));
  };

  public static final Supplier<String> SUPPLIER = () -> Integer.toHexString(RND.nextInt());

  @Benchmark
  public void Lazy(Blackhole blackhole) {
    for (int i = 0; i < OUTER_ROUNDS; i++) {
      Lazy<String> lazy = Lazy.lazy(SUPPLIER);

      for (int j = 0; j < INNER_ROUNDS; j++) {
        blackhole.consume(lazy.get());
      }
    }
  }

  @Benchmark
  public void Eager(Blackhole blackhole) {
    for (int i = 0; i < OUTER_ROUNDS; i++) {
      String value = SUPPLIER.get();

      for (int j = 0; j < INNER_ROUNDS; j++) {
        blackhole.consume(value);
      }
    }
  }

  @Benchmark
  public void Lazy_throwing(Blackhole blackhole) {
    for (int i = 0; i < OUTER_ROUNDS; i++) {
      Lazy<String> lazy = Lazy.lazy(THROWING_SUPPLIER);

      for (int j = 0; j < INNER_ROUNDS; j++) {
        try {
          lazy.get();
        } catch (RuntimeException e) {
          blackhole.consume(e.getMessage());
        }
      }
    }
  }

  @Benchmark
  public void Eager_throwing(Blackhole blackhole) {
    for (int i = 0; i < OUTER_ROUNDS; i++) {
      try {
        THROWING_SUPPLIER.get();
      } catch (Exception e) {
        for (int j = 0; j < INNER_ROUNDS; j++) {
          try {
            throw e;
          } catch (RuntimeException ex) {
            blackhole.consume(ex.getMessage());
          }
        }
      }
    }
  }
}
