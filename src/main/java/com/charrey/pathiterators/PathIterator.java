package com.charrey.pathiterators;

import com.charrey.algorithms.UtilityData;
import com.charrey.graph.MyGraph;
import com.charrey.graph.Path;
import com.charrey.matching.PartialMatchingProvider;
import com.charrey.occupation.GlobalOccupation;
import com.charrey.occupation.OccupationTransaction;
import com.charrey.pathiterators.controlpoint.ManagedControlPointIterator;
import com.charrey.pathiterators.dfs.DFSPathIterator;
import com.charrey.pathiterators.kpath.KPathPathIterator;
import com.charrey.pruning.PartialMatching;
import com.charrey.settings.Settings;
import com.charrey.settings.iterator.ControlPointIteratorStrategy;
import gnu.trove.set.hash.TIntHashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * The type Path iterator.
 */
public abstract class PathIterator {


    private final int maxPaths = Integer.MAX_VALUE;
    private final GlobalOccupation globalOccupation;


    private final int head;
    private final int tail;
    /**
     * The Refuse longer paths.
     */
    protected final boolean refuseLongerPaths;
    private int counter = 0;
    protected PartialMatchingProvider partialMatchingProvider;
    protected OccupationTransaction transaction;


    /**
     * Instantiates a new Path iterator.
     *
     * @param tail the tail
     * @param head the head
     */
    protected PathIterator(int tail, int head, Settings settings, GlobalOccupation globalOccupation, OccupationTransaction transaction, PartialMatchingProvider partialMatchingProvider) {
        this.tail = tail;
        this.head = head;
        this.refuseLongerPaths = settings.getRefuseLongerPaths();
        this.partialMatchingProvider = partialMatchingProvider;
        this.globalOccupation = globalOccupation;
        this.transaction = transaction;
    }


    /**
     * Get path iterator.
     *
     * @param targetGraph       the target graph
     * @param data              the data
     * @param tail              the tail
     * @param head              the head
     * @param occupation        the occupation
     * @param placementSize     the placement size
     * @return the path iterator
     */
    @NotNull
    public static PathIterator get(@NotNull MyGraph targetGraph,
                                   @NotNull UtilityData data,
                                   int tail,
                                   int head,
                                   @NotNull GlobalOccupation occupation,
                                   Supplier<Integer> placementSize,
                                   Settings settings,
                                   PartialMatchingProvider provider) {
        if (targetGraph.getEdge(tail, head) != null) {
            return new SingletonPathIterator(targetGraph, settings, tail, head, provider);
        }
        switch (settings.getPathIteration().iterationStrategy) {
            case DFS_ARBITRARY:
            case DFS_GREEDY:
                int[][] targetNeighbours = data.getTargetNeighbours(settings.getPathIteration().iterationStrategy)[head];
                return new DFSPathIterator(targetGraph, settings, tail, head, occupation, placementSize, provider, targetNeighbours);
            case CONTROL_POINT:
                return new ManagedControlPointIterator(targetGraph, settings, tail, head, occupation, placementSize, provider, ((ControlPointIteratorStrategy) settings.getPathIteration()).getMaxControlpoints());
            case KPATH:
                return new KPathPathIterator(targetGraph, settings, tail, head, occupation, placementSize, provider);
            default:
                throw new UnsupportedOperationException();
        }
    }

    protected PartialMatching getPartialMatching() {
        PartialMatching fromParent = partialMatchingProvider.getPartialMatching();
        return new PartialMatching(fromParent.getVertexMapping(), fromParent.getEdgeMapping(), new TIntHashSet());
    }


    @Nullable
    public abstract Path getNext();

    @Nullable
    public Path next() {
        if (counter == maxPaths) {
            return null;
        }
        Path toReturn;
        if (globalOccupation != null) {
            globalOccupation.claimActiveOccupation(transaction);
            toReturn = getNext();
            globalOccupation.unclaimActiveOccupation();
        } else {
            toReturn = getNext();
        }
        counter++;
        return toReturn;
    }

    /**
     * Tail int.
     *
     * @return the int
     */
    public int tail() {
        return tail;
    }

    /**
     * Head int.
     *
     * @return the int
     */
    public int head() {
        return head;
    }

    /**
     * Debug info string.
     *
     * @return the string
     */
    public abstract String debugInfo();
}
