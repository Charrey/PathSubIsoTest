package com.charrey.pathiterators.controlpoint;

import com.charrey.graph.MyEdge;
import com.charrey.graph.MyGraph;
import com.charrey.graph.Path;
import com.charrey.matching.PartialMatchingProvider;
import com.charrey.occupation.AbstractOccupation;
import com.charrey.occupation.GlobalOccupation;
import com.charrey.occupation.OccupationTransaction;
import com.charrey.pathiterators.PathIterator;
import com.charrey.pruning.DomainCheckerException;
import com.charrey.settings.Settings;
import gnu.trove.TCollections;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.Graphs;
import org.jgrapht.alg.shortestpath.BFSShortestPath;
import org.jgrapht.graph.MaskSubgraph;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * A pathiterator that iterates paths between two vertices with a fixed number of intermediate vertices it has to visit.
 * Furthermore, it avoids paths that could be obtained with a lower number of intermediate vertices.
 */
class ControlPointIterator extends PathIterator {

    private final int controlPoints;
    @NotNull
    private final TIntSet localOccupation;
    private final int head;
    @NotNull
    private final MyGraph targetGraph;

    private final Supplier<Integer> verticesPlaced;
    private static final Logger LOG = Logger.getLogger("ControlPointIterator");
    private final GlobalOccupation globalOccupation;

    private boolean done = false;
    @NotNull
    private final ControlPointVertexSelector controlPointCandidates;

    @Nullable
    private ControlPointIterator child = null;
    private int chosenControlPoint = -1;
    @Nullable
    private Path chosenPath = null;
    //for filtering
    private int rightNeighbourOfRightNeighbour = -1;
    @Nullable
    private Path pathFromRightNeighbourToItsRightNeighbour = null;
    @Nullable
    private TIntSet previousLocalOccupation = null;
    private final Settings settings;

    /**
     * Instantiates a new ControlPointIterator.
     *
     * @param targetGraph            the target graph where homeomorphisms are found
     * @param tail                   the source vertex
     * @param head                   the target vertex
     * @param occupation             the occupationTransaction where intermediate nodes are registered
     * @param initialLocalOccupation a set used to keep track of vertices used in this very path
     * @param controlPoints          the number of control points that should be used
     * @param verticesPlaced         a supplier of the number of source graph vertices currently matched
     * @param settings               the settings of the homeomorphism search
     */
    ControlPointIterator(@NotNull MyGraph targetGraph,
                         int tail,
                         int head,
                         @NotNull GlobalOccupation globalOccupation,
                         @NotNull OccupationTransaction occupation,
                         @NotNull TIntSet initialLocalOccupation,
                         int controlPoints,
                         Supplier<Integer> verticesPlaced,
                         Settings settings,
                         PartialMatchingProvider provider) {
        super(tail, head, settings, globalOccupation, occupation, provider);
        this.targetGraph = targetGraph;
        this.controlPoints = controlPoints;
        this.globalOccupation = globalOccupation;
        this.localOccupation = initialLocalOccupation;
        this.head = head;
        this.controlPointCandidates = new ControlPointVertexSelector(targetGraph, occupation, TCollections.unmodifiableSet(initialLocalOccupation), tail, head, refuseLongerPaths, tail);
        this.verticesPlaced = verticesPlaced;
        this.settings = settings;
    }

    /**
     * Finds the shortest path between two vertices that avoids already placed vertices.
     *
     * @param targetGraph       the target graph
     * @param globalOccupation  the occupation where intermediate nodes are registered
     * @param localOccupation   the local occupation of vertices matched in this very path
     * @param from              the source vertex
     * @param to                the target vertex
     * @param refuseLongerPaths whether to refuse paths that use unnecessarily many resources
     * @param tail              the final goal vertex of this path (if this is a recursive call)
     * @return the path
     */
    @Nullable
    static Path filteredShortestPath(@NotNull MyGraph targetGraph, @NotNull AbstractOccupation globalOccupation, @NotNull TIntSet localOccupation, int from, int to, boolean refuseLongerPaths, int tail) {
        assert targetGraph.containsVertex(from);
        assert targetGraph.containsVertex(to);
        Graph<Integer, MyEdge> fakeGraph = new MaskSubgraph<>(targetGraph, x ->
                x != from &&
                        x != to &&
                        (localOccupation.contains(x) || globalOccupation.isOccupied(x) ||
                                (refuseLongerPaths && violatesLongerPaths(targetGraph, x, from, to, tail, localOccupation))), x -> false);
        GraphPath<Integer, MyEdge> algo = new BFSShortestPath<>(fakeGraph).getPath(from, to);
        return algo == null ? null : new Path(targetGraph, algo);
    }

    /**
     * Glues two paths together, where the end vertex of one path is the start vertex of another.
     *
     * @param graph the graph to which the vertices belong
     * @param left  the left path
     * @param right the right path
     * @return the glued path that starts with the left path's initial vertex and ends with the right path's final vertex.
     */
    @NotNull
    static Path merge(@NotNull MyGraph graph, @NotNull Path left, @NotNull Path right) {
        Path toReturn = new Path(graph, left.first());
        for (int i = 1; i < left.length() - 1; i++) {
            toReturn.append(left.get(i));
        }
        for (int i = 0; i < right.length(); i++) {
            toReturn.append(right.get(i));
        }
        return toReturn;
    }

    private static boolean violatesLongerPaths(Graph<Integer, MyEdge> targetGraph, int allowableIntermediateVertex, int from, int to, int goal, TIntSet localOccupation) {
        if (goal != from && (targetGraph.getEdge(goal, allowableIntermediateVertex) != null)) {
            return true;
        }
        List<Integer> intermediateVertexSuccessors = Graphs.successorListOf(targetGraph, allowableIntermediateVertex);

        final boolean[] toReturnTrue = {false};
        localOccupation.forEach(y -> {
            if (y != to && intermediateVertexSuccessors.contains(y)) {
                toReturnTrue[0] = true;
                return false;
            }
            return true;
        });
        return toReturnTrue[0];
    }

    private void setRightNeighbourOfRightNeighbour(int vertex, Path localOccupiedForThatPath, TIntSet previousLocalOccupation) {
        this.rightNeighbourOfRightNeighbour = vertex;
        this.pathFromRightNeighbourToItsRightNeighbour = localOccupiedForThatPath;
        this.previousLocalOccupation = previousLocalOccupation;
    }

    @Nullable
    private Path filteredShortestPath(int from, int to) {
        return filteredShortestPath(targetGraph, transaction, localOccupation, from, to, refuseLongerPaths, tail());
    }

    /**
     * Returns the path this iterator has currently picked between its control point and its target.
     *
     * @return the path between the picked control point and the target.
     */
    @Nullable
    Path getChosenPath() {
        return chosenPath;
    }

    @Override
    public String debugInfo() {
        throw new IllegalStateException("Call ManagedControlPointIterator insead");
    }

    private boolean isUnNecessarilyLong(@NotNull Path chosenPath) {
        List<Integer> fromCandidates = chosenPath.asList().subList(0, chosenPath.length() - 1);
        for (int candidate : fromCandidates) {
            List<Integer> successors = Graphs.successorListOf(targetGraph, candidate);
            for (int successor : successors) {
                boolean isHead = successor == chosenPath.last();
                boolean localOccupationContains = localOccupation.contains(successor);
                if (!isHead && localOccupationContains) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean rightShiftPossible(int left) {
        if (left != -1 && this.rightNeighbourOfRightNeighbour != -1) {
            int middle = this.head();
            assert pathFromRightNeighbourToItsRightNeighbour != null;
            assert previousLocalOccupation != null;
            Path middleToRight = pathFromRightNeighbourToItsRightNeighbour;
            for (int i = 0; i < middleToRight.intermediate().size(); i++) {
                int middleAlt = middleToRight.intermediate().get(i);
                Path middleAltToRight = new Path(targetGraph, middleToRight.asList().subList(i + 1, middleToRight.length()));

                TIntSet fictionalOccupation = new TIntHashSet(previousLocalOccupation);
                middleAltToRight.forEach(fictionalOccupation::add);
                Collection<Integer> temporarilyRemoveGlobal = middleToRight.asList().subList(0, middleToRight.length() - 1);
                temporarilyRemoveGlobal.forEach(x -> transaction.releaseRouting(verticesPlaced.get(), x));
                Path leftToMiddleAlt = filteredShortestPath(targetGraph, transaction, fictionalOccupation, left, middleAlt, refuseLongerPaths, tail());
                if (leftToMiddleAlt == null) {
                    temporarilyRemoveGlobal.forEach(x -> {
                        try {
                            transaction.occupyRoutingAndCheck(verticesPlaced.get(), x, getPartialMatching());
                        } catch (DomainCheckerException e) {
                            assert false;
                        }
                    });
                    return true;
                }
                Path leftToRightAlt = merge(targetGraph, leftToMiddleAlt, middleAltToRight);
                Path leftToMiddle = filteredShortestPath(left, middle);
                temporarilyRemoveGlobal.forEach(x -> {
                    try {
                        transaction.occupyRoutingAndCheck(verticesPlaced.get(), x, getPartialMatching());
                    } catch (DomainCheckerException e) {
                        assert false;
                    }
                });
                if (leftToMiddle == null) {
                    return true;
                }
                Path leftToRight = merge(targetGraph, leftToMiddle, middleToRight);
                if (leftToRight.equals(leftToRightAlt)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean makesNeighbourUseless(int left) {
        if (this.rightNeighbourOfRightNeighbour == -1 || left == -1) {
            return false;
        } else {
            assert pathFromRightNeighbourToItsRightNeighbour != null;
            int middle = this.head();
            int right = this.rightNeighbourOfRightNeighbour;
            this.pathFromRightNeighbourToItsRightNeighbour.forEach(localOccupation::remove);

            Collection<Integer> temporaryRemoveFromGlobal = pathFromRightNeighbourToItsRightNeighbour.asList().subList(0, pathFromRightNeighbourToItsRightNeighbour.length() - 1);
            temporaryRemoveFromGlobal.forEach(x -> transaction.releaseRouting(verticesPlaced.get(), x));
            Path skippedPath = filteredShortestPath(targetGraph, transaction, localOccupation, left, right, refuseLongerPaths, tail());
            this.pathFromRightNeighbourToItsRightNeighbour.forEach(localOccupation::add);
            temporaryRemoveFromGlobal.forEach(x -> {
                try {
                    transaction.occupyRoutingAndCheck(verticesPlaced.get(), x, getPartialMatching());
                } catch (DomainCheckerException e) {
                    e.printStackTrace();
                    assert false;
                }
            });
            if (skippedPath == null) {
                return true;
            } else {
                return skippedPath.contains(middle);
            }
        }
    }

    @Nullable
    @Override
    public Path getNext() {
        StringBuilder prefix = new StringBuilder();
        prefix.append("   ".repeat(Math.max(0, 10 - controlPoints)));
        while (!done) {
            if (controlPoints == 0) {
                LOG.finest(() -> prefix.toString() + "querying shortest path...");
                done = true;
                Path shortestPath = filteredShortestPath(tail(), head);
                assert shortestPath == null || new Path(shortestPath).intermediate().stream().noneMatch(localOccupation::contains);
                this.chosenPath = shortestPath == null ? null : new Path(shortestPath);
                if (refuseLongerPaths && this.chosenPath != null && isUnNecessarilyLong(this.chosenPath)) {
                    chosenPath = null;
                }
                if (chosenPath != null) {
                    try {
                        transaction.occupyRoutingAndCheck(verticesPlaced.get(), chosenPath, getPartialMatching());
                    } catch (DomainCheckerException e) {
                        chosenPath = null;
                    }
                }
                if (chosenPath == null) {
                    LOG.finest("...failed");
                } else {
                    LOG.finest(() -> "...succeeded: " + chosenPath);
                }
                return chosenPath;
            } else if (child == null) {
                if (chosenControlPoint != -1) {
                    this.transaction.releaseRouting(verticesPlaced.get(), chosenControlPoint);
                }
                boolean makesNeighbourUseless;
                boolean rightShiftPossible;
                do {
                    chosenControlPoint = controlPointCandidates.next();
                    LOG.finest(() -> prefix.toString() + "Chosen control point: " + chosenControlPoint);
                    makesNeighbourUseless = makesNeighbourUseless(chosenControlPoint);
                    rightShiftPossible = rightShiftPossible(chosenControlPoint);
                    if (makesNeighbourUseless) {
                        LOG.finest(() -> prefix.toString() + "It makes right neighbour " + head() + " useless: it is already on the shortest path between " + chosenControlPoint + " and " + rightNeighbourOfRightNeighbour);
                    }
                    if (!makesNeighbourUseless && rightShiftPossible) {
                        LOG.finest(() -> prefix.toString() + "Right shift possible.");
                    }
                } while (chosenControlPoint != -1 && (makesNeighbourUseless || rightShiftPossible));
                if (chosenControlPoint == -1) {
                    done = true;
                    return null;
                }
                try {
                    this.transaction.occupyRoutingAndCheck(verticesPlaced.get(), chosenControlPoint, getPartialMatching());
                } catch (DomainCheckerException e) {
                    LOG.finest(() -> prefix.toString() + "Domain check failed---------------------------------------------------------------------------");
                    chosenControlPoint = -1;
                    continue;
                }
                Path graphPath = filteredShortestPath(chosenControlPoint, head);
                if (graphPath != null) {
                    chosenPath = new Path(graphPath);
                    if (refuseLongerPaths && isUnNecessarilyLong(chosenPath)) {
                        continue;
                    }
                    try {
                        transaction.occupyRoutingAndCheck(verticesPlaced.get(), chosenPath, getPartialMatching());
                    } catch (DomainCheckerException e) {
                        continue;
                    }

                    TIntSet previousOccupation = new TIntHashSet(localOccupation);
                    chosenPath.forEach(localOccupation::add);
                    child = new ControlPointIterator(targetGraph, tail(), chosenControlPoint, globalOccupation, transaction, new TIntHashSet(localOccupation), controlPoints - 1, verticesPlaced, settings, partialMatchingProvider);
                    child.setRightNeighbourOfRightNeighbour(this.head(), chosenPath, previousOccupation);
                } else {
                    LOG.finest(() -> prefix.toString() + "Could not find route from control point.");
                }
            } else {
                TIntSet previousLocalOccupation = new TIntHashSet(localOccupation);
                Path childsPath = child.next();
                assert childsPath == null || childsPath.intermediate().stream().noneMatch(previousLocalOccupation::contains);
                assert chosenPath != null;
                if (childsPath != null) {
                    assert childsPath.first() == tail();
                    assert chosenPath.last() == head;
                    assert childsPath.last() == chosenPath.first();
                    Path toReturn = merge(targetGraph, childsPath, chosenPath);
                    LOG.finest(() -> prefix.toString() + "Merged paths into " + toReturn);
                    assert toReturn.first() == tail();
                    assert toReturn.last() == head;
                    return toReturn;
                } else {
                    child = null;
                    chosenPath.forEach(localOccupation::remove);
                    chosenPath.intermediate().forEach(x -> transaction.releaseRouting(verticesPlaced.get(), x));
                }
            }
        }
        if (controlPoints == 0 && chosenPath != null) {
            chosenPath.intermediate().forEach(x -> transaction.releaseRouting(verticesPlaced.get(), x));
            chosenPath = null;
        }
        return null;
    }

    @NotNull
    @Override
    public String toString() {
        return "(" + chosenControlPoint + ", " + child + ")";
    }

    /**
     * Lists all control points currently used from start of the path to end of the path.
     *
     * @return the list of control points
     */
    @NotNull
    List<Integer> controlPoints() {
        List<Integer> res = new LinkedList<>();
        if (child != null) {
            res.addAll(child.controlPoints());
        }
        if (chosenControlPoint != -1) {
            res.add(chosenControlPoint);
        }
        return res;
    }

    /**
     * Returns the recursive iterator that provides paths with one less control point.
     *
     * @return the recursive child
     */
    @Nullable
    public ControlPointIterator getChild() {
        return child;
    }

    /**
     * Returns the set of locally occupied vertices
     *
     * @return the local occupation
     */
    @NotNull
    TIntSet getLocalOccupation() {
        return TCollections.unmodifiableSet(localOccupation);
    }

    /**
     * Returns the path from start to finish of this control point iterator from its context. This includes the path
     * from controlpoint to the head vertex and the path yielded by the recursive child.
     *
     * @return the full path
     */
    Path finalPath() {
        return child == null ? this.chosenPath : child.finalPath();
    }
}
