package safeint;

import polyglot.ext.jl5.JL5Scheduler;
import polyglot.frontend.CyclicDependencyException;
import polyglot.frontend.JLExtensionInfo;
import polyglot.frontend.Job;
import polyglot.frontend.goals.Goal;
import polyglot.util.InternalCompilerError;
import safeint.goals.InsertChecksGoal;
import safeint.goals.SimplifyExpressionsGoal;

public class SafeIntScheduler extends JL5Scheduler {

    public SafeIntScheduler(JLExtensionInfo extInfo) {
        super(extInfo);
    }
    
    public Goal InsertChecks(final Job job) {
        return InsertChecksGoal.create(this, job, extInfo.nodeFactory(), extInfo.typeSystem());
    }
    
    public Goal SimplifyExpressions(final Job job) {
        return SimplifyExpressionsGoal.create(this, job, extInfo.nodeFactory(), extInfo.typeSystem());
    }

    @Override
    public Goal Serialized(Job job) {
        Goal g = super.Serialized(job);
        try {
            g.addPrerequisiteGoal(InsertChecks(job), this);
        } catch (CyclicDependencyException e) {
            throw new InternalCompilerError(e);
        }
        return this.internGoal(g);
    }

}
