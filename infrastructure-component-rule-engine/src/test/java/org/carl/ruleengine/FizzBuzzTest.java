package org.carl.ruleengine;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.carl.ruleengine.v1.FizzBuzz;
import org.junit.jupiter.api.Test;

public class FizzBuzzTest {

    @Test
    public void num_given_1() {
        // given
        int input = 1;
        // when
        String result = FizzBuzz.count(input);
        // then
        assertEquals(result, "1");
    }

    @Test
    public void fizz_given_3() {
        // given
        int input = 3;
        // when
        String result = FizzBuzz.count(input);
        // then
        assertEquals(result, "Fizz");
    }

    @Test
    public void buzz_given_5() {
        // given
        int input = 5;
        // when
        String result = FizzBuzz.count(input);
        // then
        assertEquals(result, "Buzz");
    }

    @Test
    public void fizz_buzz_given_15() {
        // given
        int input = 15;
        // when
        String result = FizzBuzz.count(input);
        // then
        assertEquals(result, "FizzBuzz");
    }
}
