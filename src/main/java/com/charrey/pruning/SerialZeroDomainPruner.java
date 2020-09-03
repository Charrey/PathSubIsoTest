package com.charrey.pruning;

import com.charrey.graph.MyGraph;
import com.charrey.matching.PartialMatchingProvider;
import com.charrey.matching.VertexMatching;
import com.charrey.occupation.GlobalOccupation;
import com.charrey.settings.Settings;

import java.util.Iterator;

public class SerialZeroDomainPruner extends DefaultSerialPruner {

    private final VertexMatching vertexMatching;

    public SerialZeroDomainPruner(Settings settings, MyGraph sourceGraph, MyGraph targetGraph, GlobalOccupation occupation, VertexMatching vertexMatching) {
        super(settings, sourceGraph, targetGraph, occupation);
        this.vertexMatching = vertexMatching;
    }

    @Override
    public Pruner copy() {
        return new SerialZeroDomainPruner(settings, sourceGraph, targetGraph, occupation, vertexMatching);
    }

    @Override
    public void close() {
        //nothing to close
    }

    @Override
    public void checkPartial(PartialMatchingProvider partialMatching) throws DomainCheckerException {
        for (int i = 0; i < this.sourceGraph.vertexSet().size(); i++) {
            int finalI = i;
            Iterator<Integer> customIterator = targetGraph.vertexSet().stream().filter(x ->
                    settings.getFiltering().filter(sourceGraph, targetGraph, finalI, x, occupation, vertexMatching)).iterator();
            if (!customIterator.hasNext()) {
                throw new DomainCheckerException("Vertex exists with empty domain: " + i);
            }
        }
    }
}
