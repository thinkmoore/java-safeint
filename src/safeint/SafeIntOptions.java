package safeint;

import java.util.Set;

import polyglot.ext.jl5.JL5Options;
import polyglot.frontend.ExtensionInfo;
import polyglot.main.OptFlag;
import polyglot.main.UsageError;
import polyglot.main.OptFlag.Arg;
import polyglot.main.OptFlag.Kind;

public class SafeIntOptions extends JL5Options {
	public boolean instrumentShifts = false;

	public SafeIntOptions(ExtensionInfo extension) {
		super(extension);
	}
	
	@Override
    protected void populateFlags(Set<OptFlag<?>> flags) {
        super.populateFlags(flags);
        flags.add(new OptFlag.Switch(Kind.MAIN, new String[] { "-instrumentShifts", "--instrumentShifts" }, "Check shifts for overflow", false));
	}

	@Override
    protected void handleArg(Arg<?> arg) throws UsageError {
        if (arg.flag().ids().contains("-instrumentShifts")) {
            this.instrumentShifts = (Boolean) arg.value();
        }
        else super.handleArg(arg);
    }
	
}
