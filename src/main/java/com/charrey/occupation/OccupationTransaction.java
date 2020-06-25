package com.charrey.occupation;

import com.charrey.graph.Path;
import com.charrey.runtimecheck.DomainChecker;
import com.charrey.runtimecheck.DomainCheckerException;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Class very similar to GlobalOccupation, but used exclusively for registration of intermediate vertices.
 * This is linked to a GlobalOccupation object that stores other occupations. All changes can be pushed to the
 * GlobalOccupation object or made undone in single method calls.
 */
public class OccupationTransaction extends AbstractOccupation {

    private final Set<Integer> routingOccupied;
    private final Set<Integer> vertexOccupied;
    private final DomainChecker domainChecker;
    private final GlobalOccupation parent;

    private final LinkedList<TransactionElement> waiting = new LinkedList<>();
    private boolean locked = false;

    /**
     * Instantiates a new OccupationTransaction and initializes it.
     *
     * @param routingOccupied initial vertices occupied for routing
     * @param vertexOccupied  initial vertices occupied for vertex-on-vertex
     * @param domainChecker   domain checker for pruning the search space
     * @param parent          GlobalOccupation to which changes may be committed
     */
    OccupationTransaction(Set<Integer> routingOccupied, Set<Integer> vertexOccupied, DomainChecker domainChecker, GlobalOccupation parent) {
        this.routingOccupied = routingOccupied;
        this.vertexOccupied = vertexOccupied;
        this.domainChecker = domainChecker;
        this.parent = parent;
    }

    /**
     * Occupies a vertex in the target graph and marks it as being used as 'intermediate' vertex.
     *
     * @param vertexPlacementSize the number of source graph vertices placed
     * @param vertex              the vertex being occupied for routing purposes
     * @throws DomainCheckerException thrown when this occupation would result in a dead end in the search.                                If this is thrown, this class remains unchanged.
     */
    public void occupyRoutingAndCheck(int vertexPlacementSize, int vertex) throws DomainCheckerException {
        assert !routingOccupied.contains(vertex);
        routingOccupied.add(vertex);
        String previous = null;
        try {
            previous = domainChecker.toString();
            domainChecker.afterOccupyEdge(vertexPlacementSize, vertex);
            waiting.add(new TransactionElement(vertexPlacementSize, vertex));
        } catch (DomainCheckerException e) {
            assertEquals(previous, domainChecker.toString());
            routingOccupied.remove(vertex);
            throw e;
        }
    }

    /**
     * Occupies all vertex along a target graph and marks them as being used as 'intermediate' vertices.
     *
     * @param vertexPlacementSize the number of source graph vertices placed
     * @param path                the path whose vertices to occupy
     * @throws DomainCheckerException thrown when this occupation would result in a dead end in the search.                                If this is thrown, this class remains unchanged.
     */
    public void occupyRoutingAndCheck(int vertexPlacementSize, @NotNull Path path) throws DomainCheckerException {
        for (int i = 0; i < path.intermediate().size(); i++) {
            try {
                occupyRoutingAndCheck(vertexPlacementSize, path.intermediate().get(i));
            } catch (DomainCheckerException e) {
                for (int j = i - 1; j >= 0; j--) {
                    releaseRouting(vertexPlacementSize, path.intermediate().get(j));
                }
                throw e;
            }
        }
    }

    /**
     * Unregister a vertex that was initially marked as used as intermediate vertex.
     *
     * @param vertexPlacementSize the number of source graph vertices placed
     * @param vertex              the vertex that is being unregistered
     * @throws IllegalArgumentException thrown when the vertex was not occupied for routing
     */
    public void releaseRouting(int vertexPlacementSize, int vertex) {
        if (!isOccupiedRouting(vertex)) {
            throw new IllegalArgumentException("Cannot release a vertex that was never occupied (for routing purposes): " + vertex);
        }
        assert isOccupiedRouting(vertex);
        routingOccupied.remove(vertex);
        domainChecker.afterReleaseEdge(vertexPlacementSize, vertex);
        waiting.remove(new TransactionElement(vertexPlacementSize, vertex));
    }

    private boolean isOccupiedRouting(int v) {
        return routingOccupied.contains(v);
    }

    private boolean isOccupiedVertex(int v) {
        return vertexOccupied.contains(v);
    }

    public boolean isOccupied(int vertex) {
        return isOccupiedRouting(vertex) || isOccupiedVertex(vertex);
    }


    /**
     * Remove the changes of this transaction from the globalOccupation. For example, if this transaction was used
     * to mark vertex 16 as occupied and commit() was called, the occupation would be visible everywhere in this
     * program. By calling uncommit(), the occupation becomes hidden again.
     */
    public void uncommit() {
        for (int i = waiting.size() - 1; i >= 0; i--) {
            TransactionElement transactionElement = waiting.get(i);
            parent.releaseRouting(transactionElement.verticesPlaced, transactionElement.added);
        }
        locked = false;
    }

    /**
     * Make the changes in this transaction visible to the rest of this program.
     */
    public void commit() {
        if (locked) {
            throw new IllegalStateException("You must uncommit before committing.");
        }
        for (TransactionElement transactionElement : waiting) {
            parent.occupyRoutingWithoutCheck(transactionElement.verticesPlaced, transactionElement.added);
        }
        locked = true;
    }

    private static class TransactionElement {
        private final int verticesPlaced;
        private final int added;
        private final long time;

        /**
         * Instantiates a new Transaction element.
         *
         * @param verticesPlaced the vertices placed
         * @param added          the added
         */
        TransactionElement(int verticesPlaced, int added) {
            this.verticesPlaced = verticesPlaced;
            this.added = added;
            this.time = System.currentTimeMillis();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TransactionElement that = (TransactionElement) o;
            return verticesPlaced == that.verticesPlaced &&
                    added == that.added;
        }

        @Override
        public int hashCode() {
            return Objects.hash(verticesPlaced, added);
        }
    }
}
