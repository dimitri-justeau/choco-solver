package org.chocosolver.solver.variables.subgraph;

import org.chocosolver.solver.ICause;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.impl.BoolVarImpl;

import java.util.ArrayList;

public class NodeSubGraphVar extends BoolVarImpl {

    private ArrayList<SubGraphVar> refs;
    private int index;

    public NodeSubGraphVar(String name, Model model, SubGraphVar g, int index) {
        super(name, model);
        this.refs = new ArrayList<>();
        this.refs.add(g);
        this.index = index;
    }

    @Override
    public boolean instantiateTo(int value, ICause cause) throws ContradictionException {
        boolean b = super.instantiateTo(value, cause);
        if (b) {
            if (value == kTRUE) {
                for (SubGraphVar g : refs) {
                    g.notifyAdd(index);
                }
            } else {
                for (SubGraphVar g : refs) {
                    g.notifyRemove(index);
                }
            }
        }
        return b;
    }

    void addRef(SubGraphVar g) {
        this.refs.add(g);
    }
}
