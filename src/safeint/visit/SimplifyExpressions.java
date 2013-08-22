package safeint.visit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import polyglot.ast.ArrayAccess;
import polyglot.ast.ArrayInit;
import polyglot.ast.Assign;
import polyglot.ast.Binary;
import polyglot.ast.Block;
import polyglot.ast.Branch;
import polyglot.ast.Call;
import polyglot.ast.Case;
import polyglot.ast.Cast;
import polyglot.ast.CodeDecl;
import polyglot.ast.Conditional;
import polyglot.ast.Do;
import polyglot.ast.Eval;
import polyglot.ast.Expr;
import polyglot.ast.Field;
import polyglot.ast.FieldDecl;
import polyglot.ast.FloatLit;
import polyglot.ast.For;
import polyglot.ast.Id;
import polyglot.ast.Instanceof;
import polyglot.ast.IntLit;
import polyglot.ast.Labeled;
import polyglot.ast.Lit;
import polyglot.ast.Local;
import polyglot.ast.LocalAssign;
import polyglot.ast.LocalDecl;
import polyglot.ast.New;
import polyglot.ast.NewArray;
import polyglot.ast.Node;
import polyglot.ast.NodeFactory;
import polyglot.ast.Receiver;
import polyglot.ast.Special;
import polyglot.ast.Stmt;
import polyglot.ast.Switch;
import polyglot.ast.SwitchBlock;
import polyglot.ast.TypeNode;
import polyglot.ast.Unary;
import polyglot.types.ArrayType;
import polyglot.types.FieldInstance;
import polyglot.types.Flags;
import polyglot.types.InitializerInstance;
import polyglot.types.MethodInstance;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;
import polyglot.util.UniqueID;
import polyglot.visit.DeepCopy;
import polyglot.visit.HaltingVisitor;
import polyglot.visit.NodeVisitor;

/**
 * Simplify some expressions for the later analyses. Actually, this is a kitchen-sink
 * clean up pass...
 */
public class SimplifyExpressions extends HaltingVisitor {
    NodeFactory nf;
    TypeSystem ts;

    public SimplifyExpressions(NodeFactory nf, TypeSystem ts) {
        super();
        this.nf = nf;
        this.ts = ts;
    }

    /** track how many variables we have created in this CodeDecl
     * 
     */
    private LinkedList<Integer> varCount = new LinkedList<Integer>();

    @Override
    public NodeVisitor enter(Node n) {
        if (n instanceof CodeDecl) {
            varCount.addLast(0);
        }
        return this;
    }

    @Override
    public Node leave(Node parent, Node old, Node n, NodeVisitor v) {
        if (n instanceof CodeDecl) {
            varCount.removeLast();
        }
        if (n instanceof Call) {
            // just make sure that all classes are loaded appropriately.
            Call c = (Call) n;
            c.target().type().toReference().members();
        }
        if (n instanceof Field) {
            // just make sure that all classes are loaded appropriately.
            Field f = (Field) n;
            f.target().type().toReference().members();
        }
        if (n instanceof New) {
            // just make sure that all classes are loaded appropriately.
            New ne = (New) n;
            ne.type().toReference().members();
        }
        if (n instanceof Assign) {
            return simplifyAssignment((Assign) n);
        }
        if (n instanceof For) {
            String loopLabel = null;
            if (parent instanceof Labeled) {
                loopLabel = ((Labeled) parent).label();
            }
            return simplifyFor((For) n, loopLabel);
        }
        if (n instanceof Labeled) {
            Labeled l = (Labeled) old;
            if (l.statement() instanceof For) {
                // already taken care of this label
                return ((Labeled) n).statement();
            }
        }
        if (n instanceof Switch) {
            return simplifySwitch((Switch) n);
        }

        if (n instanceof Do) {
            return simplifyDo((Do) n);
        }

        if (n instanceof NewArray) {
            NewArray na = (NewArray) n;
            if (na.init() != null) {
                // fix the type of init
                return na.init(fixInitTypes(na.init(), na.type().toArray()));
            }
        }
        if (n instanceof LocalDecl) {
            LocalDecl ld = (LocalDecl) n;
            if (ld.init() != null && ld.init() instanceof ArrayInit) {
                // fix the type of init
                return ld.init(fixInitTypes((ArrayInit) ld.init(),
                                            ld.localInstance().type().toArray()));
            }
        }
        if (n instanceof FieldDecl) {
            return simplifyFieldDecl((FieldDecl) n);
        }
        if (n instanceof Unary) {
            if (parent instanceof Eval) {
                // don't refactor the unary here, do it from the eval...
                return n;
            }
            return simplifyUnary((Unary) n, false);
        }
        if (n instanceof Eval && ((Eval) n).expr() instanceof Unary) {
            Eval e = (Eval) n;
            Node ret = simplifyUnary((Unary) e.expr(), true);
            if (ret instanceof Expr) {
                return e.expr((Expr) ret);
            }
            return ret;
        }
        if (n instanceof Binary) {
            return simplifyBinary((Binary) n);
        }

        return super.leave(old, n, v);
    }

    private Id freshName(String desc) {
        int count = varCount.removeLast();
        varCount.addLast(count + 1);
        if (count == 0) {
            return nf.Id(Position.COMPILER_GENERATED, "simplExpr$" + desc);
        }
        return nf.Id(Position.COMPILER_GENERATED, "simplExpr$" + desc + "$"
                + count);
    }

    private Node simplifySwitch(Switch n) {
        // convert "switch (e) { case A: sa; case B: sb; case C: caseD: sc; default: sd }" to
        // "while (true) { boolean fallthroughA=false, fallthroughB=false, fallthroughC=false;
        //                 int x = e; 
        //                 if (fallthroughA || x == A) { fallthruB = true; sa; }
        //                 if (fallthroughB || x == B) { fallthruC = true; sb; }
        //                 if (fallthroughC || x == C || x == D) { sc; }
        //                 sd;
        //                 if (true) break;
        //               }"
        boolean defaultReachable = true;

        Position p = n.position();
        Local switchE = nf.Local(p, freshName("switch"));
        //Local switchE = nf.Local(p, "_accrue$switch");
        switchE = (Local) switchE.type(n.expr().type());
        switchE =
                switchE.localInstance(ts.localInstance(p,
                                                       Flags.FINAL,
                                                       switchE.type(),
                                                       switchE.name()));

        // get locals for the fallthrough vars
        List<Local> fallThruVars = new ArrayList<Local>();
        for (int i = 0; i < n.elements().size(); i++) {
            if (n.elements().get(i) instanceof SwitchBlock) {
                Local ft = nf.Local(p, freshName("ft"));
                //Local ft = nf.Local(p, "_accrue$ft"+i);
                ft = (Local) ft.type(ts.Boolean());
                ft =
                        ft.localInstance(ts.localInstance(p,
                                                          Flags.NONE,
                                                          ts.Boolean(),
                                                          ft.name()));
                fallThruVars.add(ft);
            }
        }

        List<Stmt> stmts = new ArrayList<Stmt>();
        // add declarations for the fallthrough vars
        for (Local ft : fallThruVars) {
            LocalDecl ld =
                    nf.LocalDecl(p,
                                 Flags.NONE,
                                 nf.CanonicalTypeNode(p, ft.type()),
                                 nf.Id(p, ft.name()),
                                 nf.BooleanLit(p, false).type(ts.Boolean()));
            ld = ld.localInstance(ft.localInstance());
            stmts.add(ld);
        }
        // add declaration for the switchE
        stmts.add(nf.LocalDecl(p,
                               Flags.FINAL,
                               nf.CanonicalTypeNode(p, switchE.type()),
                               nf.Id(p, switchE.name()),
                               n.expr()).localInstance(switchE.localInstance()));

        // now go through each element appropriately
        int elementIndex = 0;
        int fallThruCount = 0;
        List<Stmt> decls = new ArrayList<Stmt>();
        while (elementIndex < n.elements().size()) {
            Expr cond = null;
            while (elementIndex < n.elements().size()
                    && n.elements().get(elementIndex) instanceof Case) {
                Case c = (Case) n.elements().get(elementIndex++);

                if (c.isDefault()) {
                    cond = null;
                }
                else {
                    Binary b =
                            nf.Binary(p,
                                      deepCopyLocal(switchE),
                                      Binary.EQ,
                                      c.expr());
                    b = (Binary) b.type(ts.Boolean());
                    b = simplifyBinary(b);
                    if (cond == null) {
                        cond = b;
                    }
                    else {
                        cond = nf.Binary(p, cond, Binary.COND_OR, b);
                        cond = cond.type(ts.Boolean());
                        cond = simplifyBinary((Binary) cond);
                    }
                }
            }
            if (elementIndex < n.elements().size()) {
                SwitchBlock sb = (SwitchBlock) n.elements().get(elementIndex++);
                if (cond == null) {
                    // This is the default!
                    // If the SwitchBlock is reachable, it means that the default
                    // does not always break or return, so we should add a break
                    // after it.
                    defaultReachable = sb.reachable();
                    stmts.add(nf.Block(p, liftDecls(sb.statements(), decls)));
                }
                else {
                    Local fallThruVar =
                            deepCopyLocal(fallThruVars.get(fallThruCount));
                    Local nextFallThruVar = null;
                    if (fallThruCount + 1 < fallThruVars.size()) {
                        nextFallThruVar =
                                deepCopyLocal(fallThruVars.get(fallThruCount + 1));
                    }
                    fallThruCount++;

                    List<Stmt> innerStmts =
                            new ArrayList<Stmt>(sb.statements().size() + 1);
                    if (nextFallThruVar != null) {
                        LocalAssign la =
                                nf.LocalAssign(p,
                                               nextFallThruVar,
                                               Assign.ASSIGN,
                                               nf.BooleanLit(p, true)
                                                 .type(ts.Boolean()));
                        la = (LocalAssign) la.type(ts.Boolean());

                        innerStmts.add(nf.Eval(p, la));
                    }
                    innerStmts.addAll(liftDecls(sb.statements(), decls));
                    Block b = nf.Block(p, innerStmts);

                    cond = nf.Binary(p, fallThruVar, Binary.COND_OR, cond);
                    cond = cond.type(ts.Boolean());
                    stmts.add(nf.If(p, cond, b));
                }
            }
        }
        // Add a break in case the default didn't have one (i.e. it the default block was reachable)
        if (defaultReachable) {
            stmts.add(nf.Break(p));
        }
        stmts.addAll(0, decls);
        // while (true) stmts
        return nf.While(p,
                        nf.BooleanLit(p, true).type(ts.Boolean()),
                        nf.Block(p, stmts));
    }

    private List<Stmt> liftDecls(List<Stmt> statements, List<Stmt> decls) {
        List<Stmt> stmts = new ArrayList<Stmt>(statements.size());
        for (Stmt s : statements) {
            if (s instanceof LocalDecl) {
                LocalDecl ld = (LocalDecl) s;
                if (ld.init() != null) {
                    Local l =
                            nf.Local(s.position(),
                                     nf.Id(s.position(), ld.id().id()));
                    l = l.localInstance(ld.localInstance());
                    l = (Local) l.type(ld.type().type());
                    LocalAssign la =
                            nf.LocalAssign(s.position(),
                                           l,
                                           Assign.ASSIGN,
                                           ld.init());
                    la = (LocalAssign) la.type(l.type());
                    stmts.add(nf.Eval(s.position(), la));
                    ld = ld.init(null);
                }
                decls.add(ld);
            }
            else {
                stmts.add(s);
            }
        }
        return stmts;
    }

    private Local deepCopyLocal(Local l) {
        return (Local) l.visit(new DeepCopy());
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

    private Node simplifyUnary(Unary u, boolean canReturnStmt) {
        /*
         * Change x++ to (x += 1)-1
         * Change x-- to (x -= 1)+1
         * Change ++x to (x += 1)
         * Change --x to (x -= 1)
         */
        if (Unary.PRE_DEC.equals(u.operator())
                || Unary.PRE_INC.equals(u.operator())
                || (canReturnStmt && (Unary.POST_DEC.equals(u.operator()) || Unary.POST_INC.equals(u.operator())))) {
            if (!isTargetPure(u.expr())) {
                if (!canReturnStmt) {
                    throw new InternalCompilerError("Don't support " + u + " "
                            + u.expr().getClass());
                }
                else {
                    return flattenImpureUnary(u);
                }
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

    protected Node flattenImpureUnary(Unary u) {
        List<Stmt> stmts = new ArrayList<Stmt>();
        Expr e = u.expr();

        if (e instanceof Field) {
            Field f = (Field) e;
            // translate "t.f++" to "C tmp = t; tmp.f++;"
            // note that t must be an expression for the unary to be impure
            String tmpLocal = UniqueID.newID("tmpUnary");
            LocalDecl localDecl =
                    nf.LocalDecl(u.position(),
                                 Flags.NONE,
                                 nf.CanonicalTypeNode(u.position(), f.target()
                                                                     .type()),
                                 nf.Id(u.position(), tmpLocal),
                                 (Expr) f.target());
            localDecl =
                    localDecl.localInstance(ts.localInstance(u.position(),
                                                             Flags.NONE,
                                                             f.target().type(),
                                                             tmpLocal));
            stmts.add(localDecl);

            Local local = nf.Local(u.position(), nf.Id(u.position(), tmpLocal));
            local = local.localInstance(localDecl.localInstance());
            local = (Local) local.type(local.localInstance().type());
            // change the unary from t.f++ to tmp.f++.
            u = u.expr(f.target(local));

            // Simplify the unary, telling it that it can return a stmt (even though it won't)
            stmts.add(nf.Eval(u.position(), (Expr) simplifyUnary(u, true)));

        }
        else if (e instanceof ArrayAccess) {
            ArrayAccess aa = (ArrayAccess) e;
            // translate "t1[t2]++" to "C tmp1 = t1; int tmp2=t2; tmp1[tmp2]++;"
            String tmpBaseLocal = UniqueID.newID("tmpUnary");
            LocalDecl localDeclBase =
                    nf.LocalDecl(u.position(),
                                 Flags.NONE,
                                 nf.CanonicalTypeNode(u.position(), aa.array()
                                                                      .type()),
                                 nf.Id(u.position(), tmpBaseLocal),
                                 aa.array());
            localDeclBase =
                    localDeclBase.localInstance(ts.localInstance(u.position(),
                                                                 Flags.NONE,
                                                                 aa.array()
                                                                   .type(),
                                                                 tmpBaseLocal));
            stmts.add(localDeclBase);

            String tmpIndexLocal = UniqueID.newID("tmpUnary");
            LocalDecl localDeclIndex =
                    nf.LocalDecl(u.position(),
                                 Flags.NONE,
                                 nf.CanonicalTypeNode(u.position(), aa.index()
                                                                      .type()),
                                 nf.Id(u.position(), tmpIndexLocal),
                                 aa.index());
            localDeclIndex =
                    localDeclIndex.localInstance(ts.localInstance(u.position(),
                                                                  Flags.NONE,
                                                                  aa.index()
                                                                    .type(),
                                                                  tmpIndexLocal));
            stmts.add(localDeclIndex);

            Local localBase =
                    nf.Local(u.position(), nf.Id(u.position(), tmpBaseLocal));
            localBase = localBase.localInstance(localDeclBase.localInstance());
            localBase =
                    (Local) localBase.type(localBase.localInstance().type());
            Local localIndex =
                    nf.Local(u.position(), nf.Id(u.position(), tmpIndexLocal));
            localIndex =
                    localIndex.localInstance(localDeclIndex.localInstance());
            localIndex =
                    (Local) localIndex.type(localIndex.localInstance().type());

            // change the unary from t1[t2]++ to tmp1[tmp2]++.
            u = u.expr(aa.array(localBase).index(localIndex));

            // Simplify the unary, telling it that it can return a stmt (even though it won't)
            stmts.add(nf.Eval(u.position(), (Expr) simplifyUnary(u, true)));

        }
        else {
            throw new InternalCompilerError("Cannot handle unary " + u,
                                            u.position());
        }

        return nf.Block(u.position(), stmts);
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

    private boolean isTargetPure(Receiver target) {
        if (target instanceof Expr) {
            return isTargetPure((Expr) target);
        }
        else {
            return true;
        }
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

    private Node simplifyFieldDecl(FieldDecl fd) {
        FieldInstance fi = fd.fieldInstance();
        if (fd.init() == null && !fd.fieldInstance().flags().isFinal()) {
            // add a default initializer
            Expr def = uninitializedValue(fi.type(), fd.position());
            fd = fd.init(def);
            Flags iflags = fi.flags().isStatic() ? Flags.STATIC : Flags.NONE;
            TypeSystem ts = fi.typeSystem();
            InitializerInstance ii =
                    ts.initializerInstance(fd.position(), fi.container()
                                                            .toClass(), iflags);
            fd = fd.initializerInstance(ii);
        }
        if (fd.init() != null && fd.init() instanceof ArrayInit) {
            // fix the type of init
            return fd.init(fixInitTypes((ArrayInit) fd.init(), fi.type()
                                                                 .toArray()));
        }
        return fd;
    }

    private Expr uninitializedValue(Type type, Position pos) {
        Lit lit;
        if (type.isReference()) {
            lit = (Lit) nf.NullLit(pos).type(type.typeSystem().Null());
        }
        else if (type.isBoolean()) {
            lit = (Lit) nf.BooleanLit(pos, false).type(type);
        }
        else if (type.isInt() || type.isShort() || type.isChar()
                || type.isByte()) {
            lit = (Lit) nf.IntLit(pos, IntLit.INT, 0).type(type);
        }
        else if (type.isLong()) {
            lit = (Lit) nf.IntLit(pos, IntLit.LONG, 0).type(type);
        }
        else if (type.isFloat()) {
            lit = (Lit) nf.FloatLit(pos, FloatLit.FLOAT, 0.0).type(type);
        }
        else if (type.isDouble()) {
            lit = (Lit) nf.FloatLit(pos, FloatLit.DOUBLE, 0.0).type(type);
        }
        else throw new InternalCompilerError("Don't know default value for type "
                + type);

        return lit;
    }

    private Node simplifyFor(For n, String loopLabel) {
        // translate "for (inits; test; incr) body" to
        //   "{inits; while(test) {body; incr} }"
        List<Stmt> stmts = new ArrayList<Stmt>();
        stmts.addAll(n.inits());

        List<Stmt> bodyStmts = new ArrayList<Stmt>();
        bodyStmts.add(n.body());
        List<Stmt> iters = new ArrayList<Stmt>(n.iters());
        // Add the increment at the end if it is reachable
        if (n.body().reachable()) {
            bodyStmts.addAll(n.iters());
        }

        Expr cond = n.cond();
        if (cond == null) {
            cond = nf.BooleanLit(n.position(), true).type(ts.Boolean());
        }
        Stmt w =
                nf.While(n.position(), cond, nf.Block(n.position(), bodyStmts));
        if (loopLabel != null) {
            stmts.add(nf.Labeled(w.position(),
                                 nf.Id(w.position(), loopLabel),
                                 w));
        }
        else {
            stmts.add(w);
        }

        Block newFor = nf.Block(n.position(), stmts);
        newFor =
                (Block) newFor.visit(new IncrementInserter(nf.Block(n.position(),
                                                                    iters),
                                                           loopLabel));
        return newFor;
    }

    private static class IncrementInserter extends NodeVisitor {
        Block iterBlock;
        String label;

        public IncrementInserter(Block update, String label) {
            super();
            this.label = label;
            this.iterBlock = update;
        }

        @Override
        public Node leave(Node old, Node n, NodeVisitor v) {
            if (n instanceof Branch) {
                Branch b = (Branch) n;
                if ((b.label() != null && b.label().equals(label))
                        || (b.labelNode() != null && b.labelNode()
                                                      .id()
                                                      .equals(label))) {
                    // Need to add the increment before this continue
                    Block newBlock = (Block) iterBlock.visit(new DeepCopy());
                    List<Stmt> newStatements =
                            new ArrayList<Stmt>(newBlock.statements());
                    newStatements.add(b);
                    return newBlock.statements(newStatements);
                }
            }
            return super.leave(old, n, v);
        }
    }

    private Node simplifyDo(Do n) {
        // Actually, we're not simplifying do-while loops. If the body
        // has breaks or continues, then the suggested translation below
        // will fail.
        return n;
        // translate "do body while c" to "{ body; while (c) do body} "
        /*        List stmts = new ArrayList();
                stmts.add(n.body().visit(new DeepCopy()));
                
                While w = nf.While(n.position(), 
                          n.cond(), 
                          n.body());
                stmts.add(w);
                
                return nf.Block(n.position(), stmts);
        */
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

//        if (ass.right() instanceof ArrayInit) {
//            // fix the type of init
//            return ass.right(fixInitTypes((ArrayInit)ass.right(), ass.left().type().toArray()));
//        }

        return ass;
    }

//    /**
//     * Translate a while loop while (e) { S } to boolean x = e; while (x) {S;x=e}
//     */
//    protected Stmt simplifyWhile(While n) {
//      Expr cond = n.cond();
//      if (cond instanceof Local || cond instanceof Lit) {
//          // it's already a local or a literal, so nothing to do.
//      }
//      Position pos = n.position();
//      LocalInstance li = ts.localInstance(pos, Flags.NONE, ts.Boolean(), UniqueID.newID("whileCond"));
//      LocalDecl ld = nf.LocalDecl(pos, 
//                                    Flags.NONE,
//                                    nf.CanonicalTypeNode(pos, ts.Boolean()), 
//                                    li.name(),
//                                    cond);
//      ld = ld.localInstance(li);
//      
//      Local l = nf.Local(pos, li.name());
//      l = l.localInstance(li);
//      l = (Local) l.type(ts.Boolean());
//      n = n.cond(l);
//      
//      List<Stmt> stmts = new ArrayList<Stmt>();
//      if (n.body() instanceof Block) {
//          stmts.addAll(((Block)n.body()).statements());
//      }
//      else {
//          stmts.add(n.body());
//      }
//      LocalAssign la = nf.LocalAssign(pos, 
//                                      (Local)l.visit(new DeepCopy()), 
//                                      Assign.ASSIGN, 
//                                      (Expr)cond.visit(new DeepCopy()));
//      la = (LocalAssign) la.type(ts.Boolean());
//      stmts.add(nf.Eval(pos, la));
//      n = n.body(nf.Block(pos, stmts));
//      
//      return nf.Block(pos, ld, n);
//    }
}
