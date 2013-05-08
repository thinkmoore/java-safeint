package safeint.visit;

import polyglot.ast.Binary;
import polyglot.ast.Expr;
import polyglot.ast.NodeFactory;
import polyglot.frontend.Job;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.visit.AscriptionVisitor;

public class IntegerChecks extends AscriptionVisitor {

    public IntegerChecks(Job job, TypeSystem ts, NodeFactory nf) {
        super(job, ts, nf);
    }

    @Override
    public Expr ascribe(Expr e, Type toType) throws SemanticException {
        if (e instanceof Binary && toType.isNumeric()) {
            System.out.println("Visiting integer expression: " + e);
        }
        return e;
    }

}
