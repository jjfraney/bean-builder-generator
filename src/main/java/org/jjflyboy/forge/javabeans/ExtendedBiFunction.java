package org.jjflyboy.forge.javabeans;

import java.util.function.BiFunction;
import java.util.function.Function;

@FunctionalInterface
public interface ExtendedBiFunction<T, U, R>
extends BiFunction<T, U, R> {

	default Function<U, R> curry1(T t) {
		return u -> apply(t, u);
	}

	default Function<T, R> curry2(U u) {
		return t -> apply(t, u);
	}
}