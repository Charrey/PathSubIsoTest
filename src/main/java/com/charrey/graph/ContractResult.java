package com.charrey.graph;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class ContractResult {
    private final MyGraph graph;
    private final Map<Integer, Integer> oldToNew;
    private final Map<Chain, LinkedList<Integer>> origins;


    public ContractResult(MyGraph contracted,
                          Map<Integer, Integer> newToOld,
                          Map<Chain, LinkedList<Integer>> origins) {
        this.graph = contracted;
        this.oldToNew = new HashMap<>();
        newToOld.forEach((key, value) -> oldToNew.put(value, key));
        this.origins = origins;
    }

    public MyGraph getGraph() {
        return graph;
    }

    public Map<Integer, Integer> getOldToNew() {
        return oldToNew;
    }

    public Map<Chain, LinkedList<Integer>> getOrigins() {
        return origins;
    }
}