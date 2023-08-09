/*
 * This file is part of choco-solver, http://choco-solver.org/
 *
 * Copyright (c) 2023, IMT Atlantique. All rights reserved.
 *
 * Licensed under the BSD 4-clause license.
 *
 * See LICENSE file in the project root for full license information.
 */
package org.chocosolver.solver.constraints.graph.subgraph;

import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.Variable;
import org.chocosolver.solver.variables.subgraph.SubGraphConnectedComponents;
import org.chocosolver.solver.variables.subgraph.SubGraphVar;
import org.chocosolver.util.ESat;
import org.chocosolver.util.tools.ArrayUtils;


/**
 */
public class PropSubGraphNbCC extends Propagator<Variable> {

    //***********************************************************************************
    // VARIABLES
    //***********************************************************************************

    private final SubGraphVar g;
    private final IntVar k;

    //***********************************************************************************
    // CONSTRUCTORS
    //***********************************************************************************

    public PropSubGraphNbCC(SubGraphVar graph, IntVar k) {
        super(ArrayUtils.concat(graph.getNodeVars(), k), PropagatorPriority.LINEAR, false);
        this.g = graph;
        this.k = k;
    }

    //***********************************************************************************
    // PROPAGATIONS
    //***********************************************************************************

    @Override
    public void propagate(int evtmask) throws ContradictionException {
        g.updateConnectivity();

        // trivial case
        k.updateBounds(0, g.getNbPotentialNodes(), this);
        if (k.getUB() == 0) {
            for (int i = 0; i < g.getNbPotentialNodes(); i++) {
                g.removeNode(g.getPotentialNode(i), this);
            }
            return;
        }

        // bound computation
        int min = minCC();
        int max = maxCC();
        k.updateLowerBound(min, this);
        k.updateUpperBound(max, this);

        // The number of CC cannot increase :
        // - remove unreachable nodes
        // - force articulation points and bridges
        SubGraphConnectedComponents ccUB = g.getConnectedComponentsUB();
        if(min != max) {
            if (k.getUB() == min) {
                // 1 --- remove unreachable nodes
                for (int i = 0; i < g.getNbPotentialNodes(); i++) {
                    int o = g.getPotentialNode(i);
                    if (!ccUB.isReachableFromLB(ccUB.getNodeCC(o))) {
                        g.removeNode(o, this);
                    }
                }
                // 2 --- enforce articulation points and bridges that link two mandatory nodes
                for(int ap : ccUB.getArticulationPoints()) {
                    g.enforceNode(ap, this);
                }
            }
            // a maximal number of CC is required
            else if (k.getLB() == max) {
                // Condition for pruning must be determined for this class of graphs
            }
        }
    }

    private int minCC() {
        int min = 0;
        SubGraphConnectedComponents ccUB = g.getConnectedComponentsUB();
        for (int cc = 0; cc < ccUB.getNbCC(); cc++) {
            if (ccUB.isReachableFromLB(cc)) {
                min++;
            }
        }
        return min;
    }

    private int maxCC() {
        // Unsharp bound for this class of graphs
        int delta = g.getNbPotentialNodes() - g.getNbMandatoryNodes();
        return g.getConnectedComponentsLB().getNbCC() + delta;
    }

    //***********************************************************************************
    // INFO
    //***********************************************************************************

    @Override
    public ESat isEntailed() {
        g.updateConnectivity();
        if (k.getUB() < minCC() || k.getLB() > maxCC()) {
            return ESat.FALSE;
        }
        if (isCompletelyInstantiated()) {
            return ESat.TRUE;
        }
        return ESat.UNDEFINED;
    }
}
