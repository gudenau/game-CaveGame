package net.gudenau.cavegame.util.functional;

@FunctionalInterface
public interface TriFunction<A, B, C, Q> {
    Q apply(A a, B b, C c);
}
