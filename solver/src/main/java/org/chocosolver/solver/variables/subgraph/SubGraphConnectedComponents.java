package org.chocosolver.solver.variables.subgraph;

import org.chocosolver.util.objects.setDataStructures.ISet;
import org.chocosolver.util.objects.setDataStructures.SetFactory;

public class SubGraphConnectedComponents {

    boolean lb;
    SubGraphVar g;
    int n;
    int[] attributeCell;
    int[] CCFirstNode;
    int[] CCNextNode;

    int[] nodeCC;
    int[] p;
    int[] fifo;
    int[] sizeCC;
    int[] attributeCC;

    boolean[] alsoInLB;
    boolean[] reachableFromLB;
    int[] depth;
    int[] low;

    int[] nodes;

    ISet articulationPoints;
    int nbNodes;

    int nbCC;
    int sizeMinCC;
    int sizeMaxCC;

    public SubGraphConnectedComponents(SubGraphVar g, boolean lb, int[] attributeCell) {
        this.lb = lb;
        this.g = g;
        this.n = g.n;
        this.attributeCell = attributeCell;
        p = new int[n];
        fifo = new int[n];
        CCFirstNode = new int[n];
        CCNextNode = new int[n];
        nodeCC = new int[n];
        sizeCC = new int[n];
        attributeCC = new int[n];
        nodes = new int[n];
        if (!lb) {
            reachableFromLB = new boolean[n];
            alsoInLB = new boolean[n];
            depth = new int[n];
            low = new int[n];
            articulationPoints = SetFactory.makeBipartiteSet(0);
        }
    }

    public void findAllCC() {
        initSearch();
        int cc = 0;
        for (int i = 0; i < nbNodes; i++) {
            int node = nodes[i];
            if (p[node] == -1) {
                findCCDFSRecursive(node, cc);
                if (sizeMinCC == 0 || sizeMinCC > sizeCC[cc]) {
                    sizeMinCC = sizeCC[cc];
                }
                if (sizeMaxCC < sizeCC[cc]) {
                    sizeMaxCC = sizeCC[cc];
                }
                cc++;
            }
        }
        nbCC = cc;
    }

    public void findAllCCNoInit() {
        int cc = 0;
        for (int i = 0; i < nbNodes; i++) {
            int node = nodes[i];
            if (p[node] == -1) {
                findCCDFSRecursive(node, cc);
                if (sizeMinCC == 0 || sizeMinCC > sizeCC[cc]) {
                    sizeMinCC = sizeCC[cc];
                }
                if (sizeMaxCC < sizeCC[cc]) {
                    sizeMaxCC = sizeCC[cc];
                }
                cc++;
            }
        }
        nbCC = cc;
    }

    void initSearchLBAndUB(SubGraphConnectedComponents ccUB) {
        sizeMinCC = 0;
        sizeMaxCC = 0;
        nbNodes = 0;
        ccUB.sizeMinCC = 0;
        ccUB.sizeMaxCC = 0;
        ccUB.nbNodes = 0;
        for (int i = 0; i < n; i++) {
            ccUB.alsoInLB[i] = false;
            if (g.getNodeVars()[i].isInstantiatedTo(1)) {
                p[i] = -1;
                ccUB.p[i] = -1;
                ccUB.alsoInLB[i] = true;
                nodes[nbNodes] = i;
                ccUB.nodes[ccUB.nbNodes] = i;
                nbNodes++;
                ccUB.nbNodes++;
            } else if (!g.getNodeVars()[i].isInstantiatedTo(0)){
                ccUB.p[i] = -1;
                ccUB.nodes[ccUB.nbNodes] = i;
                ccUB.nbNodes++;
            } else {
                p[i] = -2;
                ccUB.p[i] = -2;
            }
        }
    }

    private void initSearch() {
        sizeMinCC = 0;
        sizeMaxCC = 0;
        nbNodes = 0;
        for (int i = 0; i < n; i++) {
            if (!lb) {
                alsoInLB[i] = false;
            }
            if (g.getNodeVars()[i].isInstantiatedTo(1)) {
                p[i] = -1;
                if (!lb) {
                    alsoInLB[i] = true;
                }
                nodes[nbNodes] = i;
                nbNodes++;
            } else if (!lb && !g.getNodeVars()[i].isInstantiatedTo(0)){
                p[i] = -1;
                nodes[nbNodes] = i;
                nbNodes++;
            } else {
                p[i] = -2;
            }
        }
    }

    private void findCCBFS(int start, int cc) {
        int first = 0;
        int last = 0;
        int size = 1;
        int attribute = attributeCell[start];
        fifo[last++] = start;
        p[start] = start;
        add(start, cc);
        while (first < last) {
            int i = fifo[first++];
            for (int j : g.neighbors[i]) {
                if (p[j] == -1) {
                    p[j] = i;
                    add(j, cc);
                    size++;
                    attribute += attributeCell[j];
                    fifo[last++] = j;
                }
            }
        }
        attributeCC[cc] = attribute;
        sizeCC[cc] = size;
    }

    private void findCCDFS(int start, int cc) {
        int last = 0;
        int size = 1;
        int attribute = attributeCell[start];
        fifo[last++] = start;
        p[start] = start;
        add(start, cc);
        while (last > 0) {
            int i = fifo[--last];
            for (int j : g.neighbors[i]) {
                if (p[j] == -1) {
                    p[j] = i;
                    add(j, cc);
                    size++;
                    attribute += attributeCell[j];
                    fifo[last++] = j;
                }
            }
        }
        attributeCC[cc] = attribute;
        sizeCC[cc] = size;
    }

    private void findCCDFSRecursive(int start, int cc) {
        sizeCC[cc] = 1;
        attributeCC[cc] = attributeCell[start];
        p[start] = start;
        if (!lb) {
            reachableFromLB[cc] = alsoInLB[start];
            depth[start] = 0;
            low[start] = 0;
        }
        addFirst(start, cc);
        for (int j : g.neighbors[start]) {
            if (p[j] == -1) {
                doFindCCDFSRecursive(start, j, cc);
            }
            if (!lb) {
                low[start] = Math.min(low[start], low[j]);
                if (low[j] >= depth[start] && g.neighbors[start].length > 1) {
                    //articulationPoints.add(start);
                }
            }
        }
    }

    private void doFindCCDFSRecursive(int parent, int current, int cc) {
        p[current] = parent;
        add(current, cc);
        sizeCC[cc]++;
        attributeCC[cc] += attributeCell[current];
        if (!lb) {
            reachableFromLB[cc] |= alsoInLB[current];
            depth[current] = depth[parent] + 1;
            low[current] = depth[parent] + 1;
        }
        for (int j : g.neighbors[current]) {
            if (p[j] == -1) {
                doFindCCDFSRecursive(current, j, cc);
            }
            if (!lb) {
                low[current] = Math.min(low[current], low[j]);
                if (low[j] >= depth[current]) {
                    if (!alsoInLB[current] && alsoInLB[parent] && alsoInLB[j]) {
                        articulationPoints.add(current);
                    }
                }
            }
        }
    }

    private void addFirst(int node, int cc) {
        nodeCC[node] = cc;
        CCNextNode[node] = -1;
        CCFirstNode[cc] = node;
    }

    private void add(int node, int cc) {
        nodeCC[node] = cc;
        CCNextNode[node] = CCFirstNode[cc];
        CCFirstNode[cc] = node;
    }

    public int getNbCC() {
        return nbCC;
    }

    public boolean isReachableFromLB(int cc) {
        if (lb) {
            return true;
        }
        return reachableFromLB[cc];
    }

    public int getCCFirstNode(int cc) {
        return CCFirstNode[cc];
    }

    public int getCCNextNode(int node) {
        return CCNextNode[node];
    }

    public int getSizeCC(int cc) {
        return sizeCC[cc];
    }

    public int getNodeCC(int node) {
        return nodeCC[node];
    }

    public ISet getArticulationPoints() {
        return articulationPoints;
    }

    public boolean isAlsoInLB(int node) {
        if (lb) {
            return true;
        }
        return alsoInLB[node];
    }

}
