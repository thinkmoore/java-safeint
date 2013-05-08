package safeint.visit;

import polyglot.ast.Binary;
import polyglot.ast.Call;
import polyglot.ast.Cast;
import polyglot.ast.Expr;
import polyglot.ast.Id;
import polyglot.ast.Node;
import polyglot.ast.NodeFactory;
import polyglot.ast.Receiver;
import polyglot.ast.TypeNode;
import polyglot.ast.Unary;
import polyglot.frontend.Job;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;
import polyglot.visit.AscriptionVisitor;

public class IntegerChecks extends AscriptionVisitor {

    public IntegerChecks(Job job, TypeSystem ts, NodeFactory nf) {
        super(job, ts, nf);
    }
    
    @Override
    public Expr ascribe(Expr e, Type toType) throws SemanticException {
        if (!e.type().isNumeric()) {
            return e;
        }
        
        Position pos = e.position();
        Receiver marker = markerClass(pos);
        Expr result = e;
        
        if (e instanceof Binary) {
            Binary b = (Binary)e;
            result = this.nodeFactory().Call(pos, marker, binaryMethod(b,pos), b.left(), b.right());
        } else if (e instanceof Unary) {
            Unary u = (Unary)e;
            result = this.nodeFactory().Call(pos, marker, unaryMethod(u,pos), u.expr());
        } else if (e instanceof Cast) {
            Cast c = (Cast)e;
            result = this.nodeFactory().Call(pos, marker, castMethod((Cast)e,pos), c.expr());
        }
        
        result.type(toType);
        return result;
    }
    
    protected Id binaryMethod(Binary b, Position p) {
        Binary.Operator op = b.operator();
        if (op == Binary.ADD) return this.nodeFactory().Id(p, "add");
        if (op == Binary.SUB) return this.nodeFactory().Id(p, "sub");
        if (op == Binary.MUL) return this.nodeFactory().Id(p, "mul");
        if (op == Binary.DIV) return this.nodeFactory().Id(p, "div");
        if (op == Binary.MOD) return this.nodeFactory().Id(p, "rem");
        if (op == Binary.SHL) return this.nodeFactory().Id(p, "shl");
        if (op == Binary.SHR) return this.nodeFactory().Id(p, "shr");
        if (op == Binary.USHR) return this.nodeFactory().Id(p, "ushr");
        return null;
    }
    
    protected Id unaryMethod(Unary u, Position p) {
        Unary.Operator op = u.operator();
        if (op == Unary.NEG) return this.nodeFactory().Id(p, "neg");
        if (op == Unary.POST_INC) return this.nodeFactory().Id(p, "postinc");
        if (op == Unary.POST_DEC) return this.nodeFactory().Id(p, "postdec");
        if (op == Unary.PRE_INC) return this.nodeFactory().Id(p, "preinc");
        if (op == Unary.PRE_DEC) return this.nodeFactory().Id(p, "predec");
        return null;
    }
    
    protected Id castMethod(Cast c, Position p) {
        return this.nodeFactory().Id(p, c.castType().name() + "Cast");
    }
    
    protected Receiver markerClass(Position p) {
        TypeNode clazz = this.nodeFactory().TypeNodeFromQualifiedName(p,"safeint.runtime.IntegerChecks");
        try {
            return clazz.type(ts.typeForName("safeint.runtime.IntegerChecks"));
        } catch (SemanticException e) {
            throw new InternalCompilerError(e);
        }
    }
}
