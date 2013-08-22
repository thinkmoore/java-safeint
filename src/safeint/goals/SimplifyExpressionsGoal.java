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
import safeint.visit.SimplifyExpressions;

public class SimplifyExpressionsGoal extends VisitorGoal {

    public static Goal create(Scheduler scheduler, Job job, NodeFactory nf,
                              TypeSystem ts) {
        return scheduler.internGoal(new SimplifyExpressionsGoal(job, nf, ts));
    }

    protected SimplifyExpressionsGoal(Job job, NodeFactory nf, TypeSystem ts) {
        super(job, new SimplifyExpressions(nf, ts));
    }

    @Override
    public Collection<Goal> prerequisiteGoals(Scheduler scheduler) {
        JL5Scheduler jl5scheduler = (JL5Scheduler)scheduler;
        List<Goal> l = new ArrayList<Goal>();
        l.add(scheduler.ReachabilityChecked(job));
        l.add(scheduler.ConstantsChecked(job));
        l.add(scheduler.ExceptionsChecked(job));
        l.add(scheduler.ExitPathsChecked(job));
        l.add(scheduler.InitializationsChecked(job));
        l.add(scheduler.ConstructorCallsChecked(job));
        l.add(scheduler.ForwardReferencesChecked(job));
        return l;
    }

}
