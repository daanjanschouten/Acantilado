package com.acantilado.collection.utils;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

class HibernateUtilsTest {
  private final Function<Integer, Integer> produceDouble = input -> input * 2;
  private final Function<Integer, Integer> failingProduceDouble =
      (input) -> {
        if (input != 3) {
          return input * 2;
        }
        return null;
      };

  private final Callable<Set<Integer>> numbers =
      () -> {
        Set<Integer> numbers = new HashSet<>();
        for (int i = 0; i < 31; i++) {
          numbers.add(i);
        }
        return numbers;
      };

  private final ExecutorService executorService = Executors.newFixedThreadPool(5);

  //    @Test
  //    void executeUntilAllSuccessfulExecutes() throws Exception {
  //        HibernateUtils.executeUntilAllSuccessful(numbers.call(), produceDouble, Optional.of(10),
  // executorService);
  //    }
  //
  //    @Test
  //    void executeUntilAllSuccessfulGivesUp() {
  //        Assertions.assertThrows(RuntimeException.class, () ->
  // HibernateUtils.executeUntilAllSuccessful(numbers.call(), failingProduceDouble, Optional.of(10),
  // executorService));
  //    }
}
