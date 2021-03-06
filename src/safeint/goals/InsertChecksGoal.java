package safeint.goals;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import polyglot.ast.NodeFactory;
import polyglot.ext.jl5.JL5Scheduler;
import polyglot.frontend.Job;
import polyglot.frontend.Scheduler;
import polyglot.frontend.goals.Goal;
import polyglot.frontend.goals.VisitorGoal;
import polyglot.types.TypeSystem;
import safeint.SafeIntScheduler;
import safeint.visit.IntegerChecks;

public class InsertChecksGoal extends VisitorGoal {

    public static Goal create(Scheduler scheduler, Job job, NodeFactory nf,
                              TypeSystem ts) {
        return scheduler.internGoal(new InsertChecksGoal(job, nf, ts));
    }

    protected InsertChecksGoal(Job job, NodeFactory nf, TypeSystem ts) {
        super(job, new IntegerChecks(job, ts, nf));
    }

    @Override
    public Collection<Goal> prerequisiteGoals(Scheduler scheduler) {
        SafeIntScheduler sischeduler = (SafeIntScheduler)scheduler;
        List<Goal> l = new ArrayList<Goal>();
        l.add(sischeduler.SimplifyExpressions(job));
        l.add(sischeduler.TypeChecked(job));
        return l;
    }

}
