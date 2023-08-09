package org.chocosolver.solver.variables.subgraph;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.constraints.graph.subgraph.PropSubGraphConnected;
import org.chocosolver.solver.constraints.graph.subgraph.PropSubGraphNbCC;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.UndirectedGraphVar;
import org.chocosolver.util.objects.graphs.GraphFactory;
import org.chocosolver.util.objects.graphs.UndirectedGraph;
import org.chocosolver.util.objects.setDataStructures.ISet;
import org.chocosolver.util.objects.setDataStructures.SetFactory;
import org.chocosolver.util.objects.setDataStructures.SetType;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.stream.IntStream;

public class SubGraphVarTest {

    @Test
    public void testSubGraphVarConnected() {
        int nbRow = 4, nbCol = 4;
        // Old model
        Model model = new Model();
        UndirectedGraph GLB = GraphFactory.makeStoredUndirectedGraph(model, nbRow * nbCol, SetType.BITSET, SetType.BIPARTITESET);
        UndirectedGraph GUB = getGridGraph(nbCol, nbRow, model);
        UndirectedGraphVar g = model.nodeInducedGraphVar("G", GLB, GUB);
        model.connected(g).post();
        while (model.getSolver().solve()) {
        }
        // New model
        Model model2 = new Model();
        UndirectedGraph GUB2 = getGridGraph(nbCol, nbRow, model2);
        SubGraphVar sg = new SubGraphVar(model2, SetFactory.makeConstantSet(new int[] {}), GUB2);
        PropSubGraphConnected sgc = new PropSubGraphConnected(sg);
        model2.post(new Constraint("connected", sgc));
        while (model2.getSolver().solve()) {
        }
        Assert.assertEquals(model.getSolver().getSolutionCount(), model2.getSolver().getSolutionCount());
    }

    @Test
    public void testSubGraphVarNbCC() {
        int nbRow = 4, nbCol = 4;
        // Old model
        Model model = new Model();
        UndirectedGraph GLB = GraphFactory.makeStoredUndirectedGraph(model, nbRow * nbCol, SetType.BITSET, SetType.BIPARTITESET);
        UndirectedGraph GUB = getGridGraph(nbCol, nbRow, model);
        UndirectedGraphVar g = model.nodeInducedGraphVar("G", GLB, GUB);
        IntVar nbCC1 = model.intVar(0, 3);
        model.nbConnectedComponents(g, nbCC1).post();
        model.getSolver().setSearch(Search.graphVarSearch(g));
        while (model.getSolver().solve()) {
        }
        // New model
        Model model2 = new Model();
        UndirectedGraph GUB2 = getGridGraph(nbCol, nbRow, model2);
        SubGraphVar sg = new SubGraphVar(model2, SetFactory.makeConstantSet(new int[] {}), GUB2);
        IntVar nbCC2 = model2.intVar(0, 3);
        PropSubGraphNbCC sgc = new PropSubGraphNbCC(sg, nbCC2);
        model2.post(new Constraint("NbCC", sgc));
        model2.getSolver().setSearch(Search.minDomLBSearch(sg.getNodeVars()));
        while (model2.getSolver().solve()) {
        }
        Assert.assertEquals(model.getSolver().getSolutionCount(), model2.getSolver().getSolutionCount());
    }

    @Test
    public void testSubGraphOfSubGraphInclude() {
        int nbRow = 4, nbCol = 4;
        ISet nodeSet = SetFactory.makeConstantSet(new int[] {0, 1, 2, 3, 4, 5});
        // Old model
        Model model = new Model();
        UndirectedGraph GLB = GraphFactory.makeStoredUndirectedGraph(model, nbRow * nbCol, SetType.BITSET, SetType.BIPARTITESET);
        UndirectedGraph GUB = getGridGraph(nbCol, nbRow, model);
        UndirectedGraphVar g = model.nodeInducedGraphVar("G", GLB, GUB);
        UndirectedGraphVar subG = model.nodeInducedSubgraphView(g, nodeSet, false);
        model.getSolver().setSearch(Search.graphVarSearch(g));
        while (model.getSolver().solve()) {
        }
        // New model
        Model model2 = new Model();
        UndirectedGraph GUB2 = getGridGraph(nbCol, nbRow, model2);
        SubGraphVar sg = new SubGraphVar(model2, SetFactory.makeConstantSet(new int[] {}), GUB2);
        SubGraphVar subSG = new SubGraphVar(sg, nodeSet, false);
        model2.getSolver().setSearch(Search.minDomLBSearch(sg.getNodeVars()));
        while (model2.getSolver().solve()) {
            // Check that the subgraph is correct.
            UndirectedGraph g1 = sg.getLBAsGraph();
            UndirectedGraph g2 = subSG.getLBAsGraph();
            ISet s = SetFactory.makeBipartiteSet(0);
            for (int i : g1.getNodes()) {
                if (nodeSet.contains(i)) {
                    s.add(i);
                    Assert.assertTrue(g2.getNodes().contains(i));
                }
            }
            Assert.assertEquals(s.size(), g2.getNodes().size());
        }
        Assert.assertEquals(model.getSolver().getSolutionCount(), model2.getSolver().getSolutionCount());
    }

    @Test
    public void testSubGraphOfSubGraphExclude() {
        int nbRow = 4, nbCol = 4;
        ISet nodeSet = SetFactory.makeConstantSet(new int[] {0, 1, 2, 3, 4, 5});
        // Old model
        Model model = new Model();
        UndirectedGraph GLB = GraphFactory.makeStoredUndirectedGraph(model, nbRow * nbCol, SetType.BITSET, SetType.BIPARTITESET);
        UndirectedGraph GUB = getGridGraph(nbCol, nbRow, model);
        UndirectedGraphVar g = model.nodeInducedGraphVar("G", GLB, GUB);
        UndirectedGraphVar subG = model.nodeInducedSubgraphView(g, nodeSet, true);
        model.getSolver().setSearch(Search.graphVarSearch(g));
        while (model.getSolver().solve()) {
        }
        // New model
        Model model2 = new Model();
        UndirectedGraph GUB2 = getGridGraph(nbCol, nbRow, model2);
        SubGraphVar sg = new SubGraphVar(model2, SetFactory.makeConstantSet(new int[] {}), GUB2);
        SubGraphVar subSG = new SubGraphVar(sg, nodeSet, true);
        model2.getSolver().setSearch(Search.minDomLBSearch(sg.getNodeVars()));
        while (model2.getSolver().solve()) {
            // Check that the subgraph is correct.
            UndirectedGraph g1 = sg.getLBAsGraph();
            UndirectedGraph g2 = subSG.getLBAsGraph();
            ISet s = SetFactory.makeBipartiteSet(0);
            for (int i : g1.getNodes()) {
                if (!nodeSet.contains(i)) {
                    s.add(i);
                    Assert.assertTrue(g2.getNodes().contains(i));
                }
            }
            Assert.assertEquals(s.size(), g2.getNodes().size());
        }
        Assert.assertEquals(model.getSolver().getSolutionCount(), model2.getSolver().getSolutionCount());
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
}
