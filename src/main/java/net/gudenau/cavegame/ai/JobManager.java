package net.gudenau.cavegame.ai;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.gudenau.cavegame.actor.LivingActor;
import net.gudenau.cavegame.util.Registries;
import net.gudenau.cavegame.util.SharedLock;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class JobManager {
    @NotNull
    private final SharedLock lock = new SharedLock();
    @NotNull
    private final List<JobType<?>> priorities = new ArrayList<>();
    @NotNull
    private final Map<JobType<?>, Set<Job>> jobs = new Object2ObjectOpenHashMap<>();

    public JobManager() {
        priorities.add(JobTypes.RESOURCE);
        priorities.add(JobTypes.MINING);
    }

    public void enqueueJob(@NotNull Job job) {
        Objects.requireNonNull(job, "job can't be null");

        lock.write(() -> {
            jobs.computeIfAbsent(JobType.from(job), (key) -> new HashSet<>()).add(job);
        });
    }

    public boolean hasJobs() {
        return jobs.values().stream().noneMatch(Set::isEmpty);
    }

    private record JobCost(Job job, long cost){
        private boolean valid() {
            return cost >= 0;
        }
    }

    @NotNull
    public Optional<Job> findJob(LivingActor actor) {
        var state = new Object() {
            @NotNull Set<Job> jobs = Set.of();
            @Nullable JobType<?> type = null;
        };

        lock.read(() -> {
            for(var type : priorities) {
                var value = this.jobs.getOrDefault(type, Set.of());
                if(!value.isEmpty()) {
                    state.jobs = Set.copyOf(value);
                    state.type = type;
                    return;
                }
            }
        });
        if(state.jobs.isEmpty()) {
            return Optional.empty();
        }

        var proposedJob = state.jobs.stream()
            .map((job) -> new JobCost(job, job.estimateCost(actor)))
            .filter(JobCost::valid)
            .min(Comparator.comparingLong(JobCost::cost))
            .map(JobCost::job)
            .orElse(null);
        if(proposedJob == null) {
            return Optional.empty();
        }

        return lock.write(() -> {
            if(jobs.getOrDefault(state.type, Set.of()).remove(proposedJob)) {
                return Optional.of(proposedJob);
            } else {
                return Optional.empty();
            }
        });
    }
}
