package org.chocosolver.solver.variables.subgraph;

import org.chocosolver.solver.ICause;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.SetVar;
import org.chocosolver.solver.variables.UndirectedGraphVar;
import org.chocosolver.util.graphOperations.connectivity.ConnectivityFinder;
import org.chocosolver.util.graphOperations.connectivity.UGVarConnectivityHelper;
import org.chocosolver.util.objects.graphs.GraphFactory;
import org.chocosolver.util.objects.graphs.UndirectedGraph;
import org.chocosolver.util.objects.setDataStructures.ISet;
import org.chocosolver.util.objects.setDataStructures.SetType;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.BitSet;
import java.util.stream.IntStream;

public class SubGraphConnectedComponentsTest {

    @Test
    public void test() throws ContradictionException {
        int nbRow = 1, nbCol = 3;
        Model model = new Model();
        SetVar s1 = model.setVar(new int[] {}, IntStream.range(0, nbCol * nbRow).toArray());
        SetVar s2 = model.setVar(new int[] {}, IntStream.range(0, nbCol * nbRow).toArray());
        model.subsetEq(s1, s2).post();
        while (model.getSolver().solve()) {
            ISet vals1 = s1.getValue();
            ISet vals2 = s2.getValue();
            Model model2 = new Model();
            UndirectedGraph GUB2 = getGridGraph(nbCol, nbRow, model2);
            SubGraphVar sg = new SubGraphVar(model2, vals1, GUB2);
            ICause fakeCause = new ICause() {};
            for (int i : GUB2.getNodes()) {
                if (!vals2.contains(i)) {
                    sg.getNodeVars()[i].setToFalse(fakeCause);
                }
            }
            sg.updateConnectivity();
            // Check connected components
            UndirectedGraph GLB = sg.getLBAsGraph();
            UndirectedGraph GUB = sg.getUBAsGraph();
            ConnectivityFinder cfLB = new ConnectivityFinder(GLB);
            ConnectivityFinder cfUB = new ConnectivityFinder(GUB);
            cfLB.findAllCC();
            cfUB.findAllCC();
            Assert.assertEquals(cfLB.getNBCC(), sg.getConnectedComponentsLB().getNbCC());
            Assert.assertEquals(cfUB.getNBCC(), sg.getConnectedComponentsUB().getNbCC());
            // Check nbCC bounds
            UndirectedGraphVar g = model2.graphVar("test", GLB, GUB);
            NbCCBounds nbCCBounds = new NbCCBounds(g);
            Assert.assertEquals(nbCCBounds.minCC(), minCCSubGraph(sg));
            Assert.assertEquals(nbCCBounds.maxCC(), maxCCSubGraph(sg));
        }
    }

    private UndirectedGraph getGridGraph(int nbCol, int nbRow, Model model) {
        int n = nbCol * nbRow;
        int[] nodes = IntStream.range(0, n).toArray();
        UndirectedGraph g = GraphFactory.makeStoredUndirectedGraph(
                model,
                nbCol * nbRow,
                SetType.BIPARTITESET, SetType.BIPARTITESET,
                nodes, new int[][] {}
        );
        for (int i = 0; i < n; i++) {
            for (int j : getNeighbors(nbCol, nbRow, i)) {
                g.addEdge(i, j);
            }
        }
        return g;
    }

    private int[] getNeighbors(int nbCol, int nbRow, int i) {
        int nbCols = nbCol;
        int nbRows = nbRow;
        int left = i % nbCols != 0 ? i - 1 : -1;
        int right = (i + 1) % nbCols != 0 ? i + 1 : -1;
        int top = i >= nbCols ? i - nbCols : -1;
        int bottom = i < nbCols * (nbRows - 1) ? i + nbCols : -1;
        return IntStream.of(left, right, top, bottom).filter(x -> x >= 0).toArray();
    }

    private int minCCSubGraph(SubGraphVar g) {
        int min = 0;
        SubGraphConnectedComponents ccUB = g.getConnectedComponentsUB();
        for (int cc = 0; cc < ccUB.getNbCC(); cc++) {
            if (ccUB.isReachableFromLB(cc)) {
                min++;
            }
        }
        return min;
    }

    private int maxCCSubGraph(SubGraphVar g) {
        // Unsharp bound for this class of graphs
        int delta = g.getNbPotentialNodes() - g.getNbMandatoryNodes();
        return g.getConnectedComponentsLB().getNbCC() + delta;
    }

    // Methods copied from PropNbCC to ensure bounds correctness
    private class NbCCBounds {

        private final UndirectedGraphVar g;
        private final UGVarConnectivityHelper helper;
        private final BitSet visitedMin, visitedMax;
        private final int[] fifo, ccOf;

        public NbCCBounds(UndirectedGraphVar graph) {
            this.g = graph;
            this.helper = new UGVarConnectivityHelper(g);
            this.visitedMin = new BitSet(g.getNbMaxNodes());
            this.visitedMax = new BitSet(g.getNbMaxNodes());
            this.fifo = new int[g.getNbMaxNodes()];
            this.ccOf = new int[g.getNbMaxNodes()];
        }

        public int minCC() {
            int min = 0;
            visitedMin.clear();
            for (int i : g.getMandatoryNodes()) {
                if (!visitedMin.get(i)) {
                    helper.exploreFrom(i, visitedMin);
                    min++;
                }
            }
            return min;
        }

        public int maxCC() {
            int nbK = 0;
            visitedMax.clear();
            for(int i:g.getMandatoryNodes()) {
                if(!visitedMax.get(i)) {
                    exploreLBFrom(i, visitedMax);
                    nbK++;
                }
            }
            int delta = g.getPotentialNodes().size() - g.getMandatoryNodes().size();
            return nbK + delta;
        }

        public void exploreLBFrom(int root, BitSet visited) {
            int first = 0;
            int last = 0;
            int i = root;
            fifo[last++] = i;
            visited.set(i);
            ccOf[i] = root; // mark cc of explored node
            while (first < last) {
                i = fifo[first++];
                for (int j : g.getMandatoryNeighborsOf(i)) { // mandatory edges only
                    if (!visited.get(j)) {
                        visited.set(j);
                        ccOf[j] = root; // mark cc of explored node
                        fifo[last++] = j;
                    }
                }
            }
        }
    }
}
