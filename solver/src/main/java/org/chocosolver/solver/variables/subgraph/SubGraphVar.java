package org.chocosolver.solver.variables.subgraph;

import org.chocosolver.solver.ICause;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.util.objects.graphs.GraphFactory;
import org.chocosolver.util.objects.graphs.UndirectedGraph;
import org.chocosolver.util.objects.setDataStructures.ISet;
import org.chocosolver.util.objects.setDataStructures.SetType;

import java.util.stream.IntStream;

public class SubGraphVar implements ISubgraphVar {

    BoolVar[] nodes;
    UndirectedGraph graphUB;
    ISet nodesLB;
    int n;
    int[][] neighbors;
    Model model;
    SubGraphConnectedComponents ccLB, ccUB;

    public SubGraphVar(Model model, ISet nodesLB, UndirectedGraph graphUB) {
        this(model, nodesLB, graphUB, IntStream.range(0, graphUB.getNbMaxNodes()).map(i -> 1).toArray());
    }

    public SubGraphVar(Model model, ISet nodesLB, UndirectedGraph graphUB, int[] attributeNode) {
        //this.nbUpdates = 0;
        this.model = model;
        this.nodesLB = nodesLB;
        this.graphUB = graphUB;
        this.n = this.graphUB.getNbMaxNodes();
        this.nodes = new BoolVar[n];
        for (int i = 0; i < n; i++) {
            if (this.nodesLB.contains(i)) {
                nodes[i] = model.boolVar(true);
            } else if (this.graphUB.containsNode(i)) {
                nodes[i] = new NodeSubGraphVar("node_" + i, model, this, i);
            } else {
                nodes[i] = model.boolVar(false);
            }
        }
        neighbors = new int[n][];
        for (int i = 0; i < n; i++) {
            neighbors[i] = graphUB.getNeighborsOf(i).toArray();
        }
        this.ccLB = new SubGraphConnectedComponents(this, true, attributeNode);
        this.ccUB = new SubGraphConnectedComponents(this, false, attributeNode);
    }

    /**
     * Constructs a subgraph var as a node-induced subgraph var or another subgraph var.
     */
    public SubGraphVar(SubGraphVar subGraphVar, ISet nodeSet, boolean exclude) {
        this.model = subGraphVar.model;
        this.nodesLB = subGraphVar.nodesLB;
        this.graphUB = subGraphVar.graphUB; // Can be optimized: Only stored what is necessary and create an index map
        this.n = subGraphVar.n; // Can be optimized
        this.neighbors = subGraphVar.neighbors; // Can be optimized
        this.nodes = new BoolVar[n]; // Can be optimized
        for (int i = 0; i < n; i++) {
            boolean include = exclude ? !nodeSet.contains(i) : nodeSet.contains(i);
            if (include) {
                nodes[i] = subGraphVar.nodes[i];
                if (nodes[i] instanceof NodeSubGraphVar) {
                    ((NodeSubGraphVar) nodes[i]).addRef(this);
                }
            } else {
                nodes[i] = model.boolVar(false);
            }
        }
    }

    @Override
    public BoolVar[] getNodeVars() {
        return nodes;
    }

    public boolean removeNode(int i, ICause cause) throws ContradictionException {
        return nodes[i].setToFalse(cause);
    }

    public boolean enforceNode(int i, ICause cause) throws ContradictionException {
        return nodes[i].setToTrue(cause);
    }

    void notifyAdd(int index) {
        //ccLB.findAllCC();
        //ccUB.findAllCC();
        //needUpdate.set(true);
    }

    void notifyRemove(int index) {
        //ccUB.findAllCC();
        //ccLB.findAllCC();
        //needUpdate.set(true);
    }

    public void updateConnectivity() {
        ccLB.initSearchLBAndUB(ccUB);
        ccLB.findAllCCNoInit();
        ccUB.findAllCCNoInit();
    }

    public SubGraphConnectedComponents getConnectedComponentsLB() {
        return ccLB;
    }

    public SubGraphConnectedComponents getConnectedComponentsUB() {
        return ccUB;
    }

    public int getNbMandatoryNodes() {
        return ccLB.nbNodes;
    }

    public int getNbPotentialNodes() {
        return ccUB.nbNodes;
    }

    public int getMandatoryNode(int index) {
        if (index < getNbMandatoryNodes()) {
            return ccLB.nodes[index];
        }
        return -1;
    }

    public int getPotentialNode(int index) {
        if (index < getNbPotentialNodes()) {
            return ccUB.nodes[index];
        }
        return -1;
    }

    public UndirectedGraph getLBAsGraph() {
        UndirectedGraph g = GraphFactory.makeUndirectedGraph(n, SetType.BITSET, SetType.BIPARTITESET);
        for (int i = 0; i < n; i++) {
            if (nodes[i].isInstantiatedTo(1)) {
                g.addNode(i);
            }
        }
        for (int i : g.getNodes()) {
            for (int j : neighbors[i]) {
                if (g.getNodes().contains(j)) {
                    g.addEdge(i, j);
                }
            }
        }
        return g;
    }

    public UndirectedGraph getUBAsGraph() {
        UndirectedGraph g = GraphFactory.makeUndirectedGraph(n, SetType.BITSET, SetType.BIPARTITESET);
        for (int i = 0; i < n; i++) {
            if (!nodes[i].isInstantiatedTo(0)) {
                g.addNode(i);
            }
        }
        for (int i : g.getNodes()) {
            for (int j : neighbors[i]) {
                if (g.getNodes().contains(j)) {
                    g.addEdge(i, j);
                }
            }
        }
        return g;
    }

    public boolean isInstantiated() {
        for (BoolVar b : nodes) {
            if (!b.isInstantiated()) {
                return false;
            }
        }
        return true;
    }
}
