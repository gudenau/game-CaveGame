package net.gudenau.cavegame.util;

import java.util.Random;
import java.util.random.RandomGenerator;

/**
 * A thread safe random wrapper.
 */
public class LockedRandom implements RandomGenerator {
    /**
     * The backing random instance.
     */
    private final Random random = new Random();

    /**
     * The exclusive lock to use.
     */
    private final ExclusiveLock lock = new ExclusiveLock();

    @Override
    public boolean nextBoolean() {
        return lock.lock(random::nextBoolean);
    }

    @Override
    public void nextBytes(byte[] bytes) {
        lock.lock(() -> random.nextBytes(bytes));
    }

    @Override
    public float nextFloat() {
        return lock.lock(() -> random.nextFloat());
    }

    @Override
    public double nextDouble() {
        return lock.lock(() -> random.nextDouble());
    }

    @Override
    public int nextInt() {
        return lock.lock(() -> random.nextInt());
    }

    @Override
    public int nextInt(int bound) {
        return lock.lock(() -> random.nextInt(bound));
    }

    @Override
    public double nextGaussian() {
        return lock.lock(() -> random.nextGaussian());
    }

    @Override
    public long nextLong() {
        return lock.lock(() -> random.nextLong());
    }

    @Override
    public boolean isDeprecated() {
        return random.isDeprecated();
    }

    @Override
    public float nextFloat(float bound) {
        return lock.lock(() -> random.nextFloat(bound));
    }

    @Override
    public float nextFloat(float origin, float bound) {
        return lock.lock(() -> random.nextFloat(origin, bound));
    }

    @Override
    public double nextDouble(double bound) {
        return lock.lock(() -> random.nextDouble(bound));
    }

    @Override
    public double nextDouble(double origin, double bound) {
        return lock.lock(() -> random.nextDouble(origin, bound));
    }

    @Override
    public int nextInt(int origin, int bound) {
        return lock.lock(() -> random.nextInt(origin, bound));
    }

    @Override
    public long nextLong(long bound) {
        return lock.lock(() -> random.nextLong(bound));
    }

    @Override
    public long nextLong(long origin, long bound) {
        return lock.lock(() -> random.nextLong(origin, bound));
    }

    @Override
    public double nextGaussian(double mean, double stddev) {
        return lock.lock(() -> random.nextGaussian(mean, stddev));
    }

    @Override
    public double nextExponential() {
        return lock.lock(this::nextExponential);
    }
}
