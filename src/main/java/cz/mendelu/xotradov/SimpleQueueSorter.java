package cz.mendelu.xotradov;

import hudson.model.Queue;
import hudson.model.queue.QueueSorter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.PriorityQueue;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.logging.Logger;
import jenkins.model.Jenkins;

/**
 * The new sorter of Simple Queue plugin. Preserves the order of default jenkins sorter, when no desires have been made.
 */
@SuppressWarnings("unused")
public class SimpleQueueSorter extends QueueSorter {
    private static Logger logger = Logger.getLogger(SimpleQueueSorter.class.getName());
    private final QueueSorter originalQueueSorter;
    private final SimpleQueueComparator simpleQueueComparator;

    public SimpleQueueSorter(QueueSorter originalQueueSorter) {
        this.originalQueueSorter = originalQueueSorter;
        this.simpleQueueComparator = SimpleQueueComparator.getInstance();
    }

    @Override
    public void sortBuildableItems(List<Queue.BuildableItem> list) {
        List<Queue.BuildableItem> automatic = new ArrayList<>(list);
        if (this.originalQueueSorter != null) {
            this.originalQueueSorter.sortBuildableItems(automatic);
        }
        // Desires form a partial order, not a transitive Comparator. Snapshot
        // the desires before choosing the earliest unblocked automatic item.
        int size = automatic.size();
        Map<Long, Integer> positions = new HashMap<>();
        List<List<Integer>> successors = new ArrayList<>(size);
        int[] predecessors = new int[size];
        for (int i = 0; i < size; i++) {
            positions.put(automatic.get(i).getId(), i);
            successors.add(new ArrayList<>());
        }
        Map<Long, Set<Long>> desires = simpleQueueComparator.snapshotDesires();
        for (Map.Entry<Long, Set<Long>> desire : desires.entrySet()) {
            Integer before = positions.get(desire.getKey());
            if (before == null) {
                continue;
            }
            for (Long id : desire.getValue()) {
                Integer after = positions.get(id);
                if (after != null && !after.equals(before)) {
                    successors.get(before).add(after);
                    predecessors[after]++;
                }
            }
        }
        PriorityQueue<Integer> ready = new PriorityQueue<>();
        for (int i = 0; i < size; i++) {
            if (predecessors[i] == 0) {
                ready.add(i);
            }
        }
        List<Queue.BuildableItem> ordered = new ArrayList<>(size);
        boolean[] emitted = new boolean[size];
        while (ordered.size() < size) {
            if (ready.isEmpty()) {
                logger.warning("Cyclic Simple Queue desires; breaking at automatic-first cycle member");
                ready.add(earliestCycleMember(successors, emitted));
            }
            int next = ready.remove();
            emitted[next] = true;
            ordered.add(automatic.get(next));
            for (int after : successors.get(next)) {
                if (--predecessors[after] == 0 && !emitted[after]) {
                    ready.add(after);
                }
            }
        }
        // Do not mutate Jenkins' live list until the full order is constructed.
        for (int i = 0; i < size; i++) {
            list.set(i, ordered.get(i));
        }
    }

    private int earliestCycleMember(List<List<Integer>> successors, boolean[] emitted) {
        // This fallback is only used for cyclic desires. A downstream item may
        // have higher automatic priority, but forcing it would break another edge.
        for (int candidate = 0; candidate < emitted.length; candidate++) {
            if (emitted[candidate]) {
                continue;
            }
            boolean[] visited = new boolean[emitted.length];
            ArrayDeque<Integer> pending = new ArrayDeque<>();
            visited[candidate] = true;
            pending.add(candidate);
            while (!pending.isEmpty()) {
                for (int after : successors.get(pending.remove())) {
                    if (emitted[after]) {
                        continue;
                    }
                    if (after == candidate) {
                        return candidate;
                    }
                    if (!visited[after]) {
                        visited[after] = true;
                        pending.add(after);
                    }
                }
            }
        }
        throw new IllegalStateException("No cycle in blocked Simple Queue desires");
    }

    public SimpleQueueComparator getSimpleQueueComparator() {
        return simpleQueueComparator;
    }

    void reset() {
        simpleQueueComparator.resetDesires();
        sortBuildableItems(Jenkins.get().getQueue().getBuildableItems());
        Queue.getInstance().maintain();
    }
}
