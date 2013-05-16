package safeint.visit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import polyglot.ast.ArrayAccess;
import polyglot.ast.ArrayInit;
import polyglot.ast.Assign;
import polyglot.ast.Binary;
import polyglot.ast.Call;
import polyglot.ast.Cast;
import polyglot.ast.Conditional;
import polyglot.ast.Eval;
import polyglot.ast.Expr;
import polyglot.ast.Field;
import polyglot.ast.Instanceof;
import polyglot.ast.IntLit;
import polyglot.ast.Lit;
import polyglot.ast.Local;
import polyglot.ast.New;
import polyglot.ast.NewArray;
import polyglot.ast.Node;
import polyglot.ast.NodeFactory;
import polyglot.ast.Receiver;
import polyglot.ast.Special;
import polyglot.ast.TypeNode;
import polyglot.ast.Unary;
import polyglot.types.ArrayType;
import polyglot.types.MethodInstance;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.util.InternalCompilerError;
import polyglot.visit.DeepCopy;
import polyglot.visit.NodeVisitor;

public class SimplifyExpressions extends NodeVisitor {

    private NodeFactory nf;
    private TypeSystem ts;

    public SimplifyExpressions(NodeFactory nf, TypeSystem ts) {
        super();
        this.nf = nf;
        this.ts = ts;
    }
    
    @Override
    public Node leave(Node parent, Node old, Node n, NodeVisitor v) {
        if (n instanceof Unary) {
            return simplifyUnary((Unary) n, parent instanceof Eval);
        }
        return n;
    }
    
    /**
     * Can expr modify memory?
     */
    private boolean isTargetPure(Expr target) {
        if (target instanceof Assign) return false;
        if (target instanceof New) return false;
        if (target instanceof NewArray) return false;
        if (target instanceof Call) return false; // possibility for special casing calls to pure functions

        if (target instanceof Special) return true;
        if (target instanceof Local) return true;
        if (target instanceof Lit) return true;
        if (target instanceof Binary) {
            Binary b = (Binary) target;
            return isTargetPure(b.left()) && isTargetPure(b.right());
        }
        if (target instanceof Unary) {
            Unary u = (Unary) target;
            return u.operator() != Unary.POST_DEC
                    && u.operator() != Unary.POST_INC
                    && u.operator() != Unary.PRE_DEC
                    && u.operator() != Unary.PRE_INC && isTargetPure(u.expr());
        }
        if (target instanceof Field)
            return isTargetPure(((Field) target).target());
        if (target instanceof Cast)
            return isTargetPure(((Cast) target).expr());
        if (target instanceof Instanceof)
            return isTargetPure(((Instanceof) target).expr());
        if (target instanceof Conditional) {
            Conditional c = (Conditional) target;
            return isTargetPure(c.cond()) && isTargetPure(c.consequent())
                    && isTargetPure(c.alternative());
        }
        if (target instanceof ArrayAccess) {
            ArrayAccess aa = (ArrayAccess) target;
            return isTargetPure(aa.array()) && isTargetPure(aa.index());
        }
        return false;
    }
    
    private boolean isTargetPure(Receiver target) {
        if (target instanceof Expr) {
            return isTargetPure((Expr) target);
        }
        else {
            return true;
        }
    }
    
    private ArrayInit fixInitTypes(ArrayInit init, ArrayType type) {
        init = (ArrayInit) init.type(type);
        if (type.base().isArray()) {
            List<Expr> newElements =
                    new ArrayList<Expr>(init.elements().size());
            for (Expr e : init.elements()) {
                if (e instanceof ArrayInit) {
                    newElements.add(fixInitTypes((ArrayInit) e, type.base()
                                                                    .toArray()));
                }
                else {
                    newElements.add(e);
                }

            }
            init = init.elements(newElements);
        }
        return init;
    }
    
    private Node simplifyUnary(Unary u, boolean discardValue) {
        /*
         * Change x++ to (x += 1)-1
         * Change x-- to (x -= 1)+1
         * Change ++x to (x += 1)
         * Change --x to (x -= 1)
         */
        if (Unary.PRE_DEC.equals(u.operator())
                || Unary.PRE_INC.equals(u.operator())
                || (discardValue && (Unary.POST_DEC.equals(u.operator()) || Unary.POST_INC.equals(u.operator())))) {
            if (!isTargetPure(u.expr())) {
                throw new InternalCompilerError("Don't support " + u + " "
                        + u.expr().getClass());
            }
            Expr left = u.expr();
            boolean add =
                    Unary.PRE_INC.equals(u.operator())
                            || Unary.POST_INC.equals(u.operator());
            Assign.Operator newOp = add ? Assign.ADD_ASSIGN : Assign.SUB_ASSIGN;
            Assign ass =
                    nf.Assign(u.position(),
                              left,
                              newOp,
                              nf.IntLit(u.position(), IntLit.INT, 1)
                                .type(ts.Int()));
            ass = (Assign) ass.type(left.type());
            return simplifyAssignment(ass);
        }
        else if (Unary.POST_DEC.equals(u.operator())
                || Unary.POST_INC.equals(u.operator())) {
            if (!isTargetPure(u.expr())) {
                throw new InternalCompilerError("Don't support " + u + " "
                        + u.expr().getClass());
            }
            Expr left = u.expr();
            Assign.Operator newOp =
                    Unary.POST_INC.equals(u.operator()) ? Assign.ADD_ASSIGN
                            : Assign.SUB_ASSIGN;
            Assign ass =
                    nf.Assign(u.position(),
                              left,
                              newOp,
                              nf.IntLit(u.position(), IntLit.INT, 1)
                                .type(ts.Int()));
            Expr sa = simplifyAssignment((Assign) ass.type(left.type()));

            Binary b =
                    nf.Binary(u.position(),
                              sa,
                              Unary.POST_INC.equals(u.operator()) ? Binary.SUB
                                      : Binary.ADD,
                              nf.IntLit(u.position(), IntLit.INT, 1)
                                .type(ts.Int()));
            b = (Binary) b.type(left.type().isLong() ? ts.Long() : ts.Int());
            b = simplifyBinary(b);
            if (!left.type().equals(b.type())) {
                Cast c =
                        nf.Cast(ass.position(),
                                nf.CanonicalTypeNode(ass.position(), ass.left()
                                                                        .type()),
                                b);
                c = (Cast) c.type(ass.left().type());
                return c;
            }
            return b;

        }
        return u;
    }
    
    protected Expr simplifyAssignment(Assign ass) {
        if (!ass.operator().equals(Assign.ASSIGN)) {
            // translate "a op= b" to "a = (A)(a op b)", where A is the type of a.
            // We make an exception is A = String, and translate it to "a = a op b".
            Binary.Operator op = ass.operator().binaryOperator();
            Expr right = ass.right();
            TypeSystem ts = ass.left().type().typeSystem();
            Binary b =
                    nf.Binary(ass.position(),
                              (Expr) ass.left().visit(new DeepCopy()),
                              op,
                              right);
            // TODO: Check this type assignment and make more robust
            // SM: I think this is correct. Changed from what is in the else
            // branch to match Java numeric promotion rules
            if (ass.left().type().isNumeric() && ass.right().type().isNumeric()) {
                try {
                    b =
                            (Binary) b.type(ts.promote(b.left().type(),
                                                       b.right().type()));
                }
                catch (SemanticException e) {
                    throw new InternalCompilerError(e);
                }
            }
            else {
                b = (Binary) b.type(ass.left().type());
            }
            b = simplifyBinary(b);
            Expr rightExpr = b;
            if (!ass.left().type().equals(ass.right().type())
                    || ass.left().type().isIntOrLess()) {
                // the right hand type and the left hand type are not the same.
                // We normally want to add a cast, except in the case where
                // there is an implicit conversion to a string.
                if (!ass.left().type().equals(ts.String())) {
                    Cast c =
                            nf.Cast(ass.position(),
                                    nf.CanonicalTypeNode(ass.position(),
                                                         ass.left().type()),
                                    b);
                    c = (Cast) c.type(ass.left().type());
                    rightExpr = c;
                }
            }

            return ass.operator(Assign.ASSIGN).right(rightExpr);
        }

        return ass;
    }
    
    private Binary simplifyBinary(Binary b) {
        if (ts.String().equals(b.type())) {
            // we have a binary whose type is string.
            // Insert explicit coercions to string if needed.
            Expr left = b.left();
            Expr right = b.right();
            if (!ts.String().equals(left.type())) {
                left = coerceToString(left);
            }
            if (!ts.String().equals(right.type())) {
                right = coerceToString(right);
            }
            return b.left(left).right(right);
        }
        return b;
    }
    
    private Expr coerceToString(Expr e) {
        Type et = e.type();
        if (ts.String().equals(et)) {
            return e;
        }
        if (et.isReference() || et.isNull()) {
            // use String.valueOf(Object) for all reference types, e.g., arrays.
            et = ts.Object();
        }
        if (et.isByte() || et.isShort()) {
            // there is no String.valueOf(byte) or String.valueOf(short) so promote to int instead
            et = ts.Int();
        }
        TypeNode tn = nf.CanonicalTypeNode(e.position(), ts.String());
        Call c = nf.Call(e.position(), tn, nf.Id(e.position(), "valueOf"), e);
        c = (Call) c.type(ts.String());

        List<? extends MethodInstance> mis =
                ts.String().methods("valueOf", Collections.singletonList(et));
        if (mis == null || mis.isEmpty()) {
            // Hmmm, couldn't find the right method.
            throw new InternalCompilerError("Couldn't find method String.valueOf("
                    + et + ")");
        }
        c = c.methodInstance(mis.get(0));
        return c;
    }
}
