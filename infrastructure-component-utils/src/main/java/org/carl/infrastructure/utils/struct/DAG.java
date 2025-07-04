package org.carl.infrastructure.utils.struct;

import java.util.*;

/**
 * {<a href="https://github.com/michelleclar/Relax/blob/master/core/base/structs.py">...</a>}
 *
 * <p>code transform by GitHub copilot
 */
public class DAG {
    private Map<String, Set<String>> graph;

    public DAG() {
        resetGraph();
    }

    public void addNode(String nodeName) {
        if (graph.containsKey(nodeName)) {
            throw new IllegalArgumentException("Node " + nodeName + " already exists");
        }
        graph.put(nodeName, new HashSet<>());
    }

    public void addNodeIfNotExists(String nodeName) {
        graph.putIfAbsent(nodeName, new HashSet<>());
    }

    public void deleteNode(String nodeName) {
        if (!graph.containsKey(nodeName)) {
            throw new IllegalArgumentException("Node " + nodeName + " does not exist");
        }
        graph.remove(nodeName);
        for (Set<String> edges : graph.values()) {
            edges.remove(nodeName);
        }
    }

    public void deleteNodeIfExists(String nodeName) {
        graph.remove(nodeName);
        for (Set<String> edges : graph.values()) {
            edges.remove(nodeName);
        }
    }

    public void addEdge(String indNode, String depNode) {
        if (!graph.containsKey(indNode) || !graph.containsKey(depNode)) {
            throw new IllegalArgumentException("One or more nodes do not exist in the graph");
        }
        // Validate graph for cycles before adding
        Map<String, Set<String>> testGraph = deepCopyGraph();
        testGraph.get(indNode).add(depNode);
        if (validate(testGraph)) {
            graph.get(indNode).add(depNode);
        } else {
            throw new IllegalStateException("Adding this edge would create a cycle");
        }
    }

    public void deleteEdge(String indNode, String depNode) {
        if (!graph.containsKey(indNode) || !graph.get(indNode).contains(depNode)) {
            throw new IllegalArgumentException("This edge does not exist in the graph");
        }
        graph.get(indNode).remove(depNode);
    }

    public List<String> predecessors(String node) {
        List<String> predecessors = new ArrayList<>();
        for (Map.Entry<String, Set<String>> entry : graph.entrySet()) {
            if (entry.getValue().contains(node)) {
                predecessors.add(entry.getKey());
            }
        }
        return predecessors;
    }

    public List<String> downstream(String node) {
        if (!graph.containsKey(node)) {
            throw new IllegalArgumentException("Node " + node + " is not in the graph");
        }
        return new ArrayList<>(graph.get(node));
    }

    public List<String> allDownstreams(String node) {
        if (!graph.containsKey(node)) {
            throw new IllegalArgumentException("Node " + node + " is not in the graph");
        }
        Set<String> nodesSeen = new HashSet<>();
        List<String> nodes = new ArrayList<>();
        nodes.add(node);

        int i = 0;
        while (i < nodes.size()) {
            String currentNode = nodes.get(i);
            for (String downstreamNode : graph.get(currentNode)) {
                if (!nodesSeen.contains(downstreamNode)) {
                    nodesSeen.add(downstreamNode);
                    nodes.add(downstreamNode);
                }
            }
            i++;
        }
        return nodes;
    }

    public List<String> allLeaves() {
        List<String> leaves = new ArrayList<>();
        for (Map.Entry<String, Set<String>> entry : graph.entrySet()) {
            if (entry.getValue().isEmpty()) {
                leaves.add(entry.getKey());
            }
        }
        return leaves;
    }

    public void fromMap(Map<String, List<String>> graphMap) {
        resetGraph();
        for (String node : graphMap.keySet()) {
            addNode(node);
        }
        for (Map.Entry<String, List<String>> entry : graphMap.entrySet()) {
            for (String depNode : entry.getValue()) {
                addEdge(entry.getKey(), depNode);
            }
        }
    }

    public void resetGraph() {
        graph = new LinkedHashMap<>();
    }

    public List<String> independentNodes() {
        Set<String> dependentNodes = new HashSet<>();
        for (Set<String> dependents : graph.values()) {
            dependentNodes.addAll(dependents);
        }
        List<String> independentNodes = new ArrayList<>();
        for (String node : graph.keySet()) {
            if (!dependentNodes.contains(node)) {
                independentNodes.add(node);
            }
        }
        return independentNodes;
    }

    public boolean validate(Map<String, Set<String>> graphToValidate) {
        return !independentNodes().isEmpty() && topologicalSort(graphToValidate) != null;
    }

    public List<String> topologicalSort() {
        return topologicalSort(graph);
    }

    private List<String> topologicalSort(Map<String, Set<String>> graphToSort) {
        Map<String, Integer> inDegree = new HashMap<>();
        for (String node : graphToSort.keySet()) {
            inDegree.put(node, 0);
        }
        for (Set<String> edges : graphToSort.values()) {
            for (String node : edges) {
                inDegree.put(node, inDegree.get(node) + 1);
            }
        }

        Queue<String> ready = new LinkedList<>();
        for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                ready.add(entry.getKey());
            }
        }

        List<String> result = new ArrayList<>();
        while (!ready.isEmpty()) {
            String node = ready.poll();
            result.add(node);
            for (String neighbor : graphToSort.get(node)) {
                inDegree.put(neighbor, inDegree.get(neighbor) - 1);
                if (inDegree.get(neighbor) == 0) {
                    ready.add(neighbor);
                }
            }
        }

        if (result.size() == graphToSort.size()) {
            return result;
        } else {
            throw new IllegalStateException("Graph is not acyclic");
        }
    }

    public int size() {
        return graph.size();
    }

    @Override
    public String toString() {
        return "DAG(nodes=" + graph.keySet() + ")";
    }

    private Map<String, Set<String>> deepCopyGraph() {
        Map<String, Set<String>> copy = new LinkedHashMap<>();
        for (Map.Entry<String, Set<String>> entry : graph.entrySet()) {
            copy.put(entry.getKey(), new HashSet<>(entry.getValue()));
        }
        return copy;
    }
}
