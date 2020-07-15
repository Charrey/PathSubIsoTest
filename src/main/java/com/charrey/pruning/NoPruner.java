package com.charrey.pruning;

import com.charrey.settings.pruning.domainfilter.NoFiltering;

/**
 * Domain checker class that performs no pruning.
 */
@SuppressWarnings("RedundantThrows")
public class NoPruner extends Pruner {

    public NoPruner() {
        super(new NoFiltering(), null, null, null);
    }

    @Override
    public void afterReleaseVertex(int verticesPlaced, int released) {

    }

    @Override
    public void afterReleaseEdge(int verticesPlaced, int released) {

    }

    @Override
    public void beforeOccupyVertex(int verticesPlaced, int occupied, PartialMatching partialMatching) throws DomainCheckerException {

    }

    @Override
    public void afterOccupyEdge(int verticesPlaced, int occupied, PartialMatching partialMatching) throws DomainCheckerException {

    }

    @Override
    public void afterOccupyEdgeWithoutCheck(int verticesPlaced, int occupied) {

    }

    @Override
    public boolean isUnfruitfulCached(int verticesPlaced) {
        return false;
    }

    @Override
    public Pruner copy() {
        return this;
    }

    @Override
    public void checkPartial(PartialMatching partialMatching) throws DomainCheckerException {

    }

}