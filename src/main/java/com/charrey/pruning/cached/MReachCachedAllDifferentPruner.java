package com.charrey.pruning.cached;

import com.charrey.algorithms.AllDifferent;
import com.charrey.graph.MyGraph;
import com.charrey.matching.PartialMatchingProvider;
import com.charrey.matching.VertexMatching;
import com.charrey.occupation.ReadOnlyOccupation;
import com.charrey.pruning.Pruner;
import com.charrey.settings.Settings;
import gnu.trove.set.TIntSet;

import java.util.LinkedList;
import java.util.List;

public class MReachCachedAllDifferentPruner extends MReachCachedPruner {

    AllDifferent allDifferent = new AllDifferent();


    public MReachCachedAllDifferentPruner(Settings settings,
                                          MyGraph sourceGraph,
                                          MyGraph targetGraph,
                                          ReadOnlyOccupation occupation,
                                          VertexMatching vertexMatching) {
        super(settings, sourceGraph, targetGraph, occupation, vertexMatching);
    }

    public MReachCachedAllDifferentPruner(MReachCachedAllDifferentPruner copyOf) {
        super(copyOf);
        allDifferent = copyOf.allDifferent;
    }

    @Override
    public boolean isUnfruitful(int verticesPlaced, PartialMatchingProvider partialMatchingProvider, int lastPlaced) {
        List<TIntSet> toCheck = new LinkedList<>();
        sourceGraph.vertexSet().forEach(x -> toCheck.add(getDomain(x)));
        return !allDifferent.get(toCheck);
    }

    @Override
    public Pruner copy() {
        return new MReachCachedAllDifferentPruner(this);
    }
}
