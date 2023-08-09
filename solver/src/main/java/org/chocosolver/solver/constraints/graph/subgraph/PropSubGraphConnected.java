
package org.chocosolver.solver.constraints.graph.subgraph;

import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.subgraph.SubGraphConnectedComponents;
import org.chocosolver.solver.variables.subgraph.SubGraphVar;
import org.chocosolver.util.ESat;


public class PropSubGraphConnected extends Propagator<BoolVar> {

    private final SubGraphVar g;

    public PropSubGraphConnected(SubGraphVar g) {
        super(g.getNodeVars(), PropagatorPriority.LINEAR, false);
        this.g = g;
    }

    @Override
    public void propagate(int evtmask) throws ContradictionException {
        g.updateConnectivity();
        // 0-node or 1-node graphs are accepted
        if (g.getNbPotentialNodes() <= 1) {
            setPassive();
            return;
        }

        // cannot filter if no mandatory node
        if (g.getNbMandatoryNodes() > 0) {
            SubGraphConnectedComponents ccUB = g.getConnectedComponentsUB();
            int oneLbNode = g.getMandatoryNode(0);
            int ccInUB = ccUB.getNodeCC(oneLbNode);
            for (int i = 0; i < g.getNbPotentialNodes(); i++) {
                int node = g.getPotentialNode(i);
                if (ccUB.getNodeCC(node) != ccInUB) {
                    g.removeNode(node, this);
                }
            }
            // 2 --- enforce articulation points and bridges that link two mandatory nodes
            for (int ap : ccUB.getArticulationPoints()) {
                g.enforceNode(ap, this);
            }
        }
    }

    @Override
    public ESat isEntailed() {
        g.updateConnectivity();
        // 0-node or 1-node graphs are accepted
        if (g.getNbPotentialNodes() <= 1) {
            return ESat.TRUE;
        }
        // cannot conclude if less than 2 mandatory nodes
        if (g.getNbMandatoryNodes() < 2) {
            return ESat.UNDEFINED;
        }
        // BFS from a mandatory node
        int node = g.getMandatoryNode(0);
        int cc = g.getConnectedComponentsUB().getNodeCC(node);
        for (int i = 1; i < g.getNbMandatoryNodes(); i++) {
            int v = g.getMandatoryNode(i);
            if (g.getConnectedComponentsUB().getNodeCC(v) != cc) {
                return ESat.FALSE;
            }
        }
        if (g.isInstantiated()) {
            return ESat.TRUE;
        }
        return ESat.UNDEFINED;
    }
}

