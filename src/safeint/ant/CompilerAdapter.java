package safeint.ant;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.compilers.DefaultCompilerAdapter;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.Path;

import polyglot.main.Options;
import polyglot.util.InternalCompilerError;

import safeint.ExtensionInfo;
import safeint.SafeIntScheduler;

public class CompilerAdapter extends DefaultCompilerAdapter {
    
    @Override
    public boolean execute() throws BuildException {
        attributes.log("Using safeint compiler", Project.MSG_INFO);
       
        Commandline safeintc = new Commandline();
        safeintc.setExecutable("safeintc");
        Commandline post = new Commandline();
        post.setExecutable("javac");
        setupModernJavacCommandlineSwitches(post);
        String[] args = post.getArguments();
        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("-g")) {
                continue;
            }
            if (args[i].equals("-target")) {
                i++; continue;
            }
            if (args[i].equals("-source")) {
                i++; continue;
            }
            if (args[i].equals("-deprecation")) {
                continue;
            }
            if (args[i].equals("-nowarn")) {
                continue;
            }
            if (args[i].equals("-encoding")) {
                if (!args[++i].equalsIgnoreCase("UTF-8")) {
                    throw new InternalCompilerError("Unsupported encoding " + args[i]);
                }
                continue;
            }
            if (args[i].equals("-O")) {
                continue;
            }
            safeintc.createArgument().setValue(args[i]);
        }
        safeintc.createArgument().setValue("-post");
        safeintc.createArgument().setValue(post.toString());
        safeintc.createArgument().setValue("-j");
        safeintc.createArgument().setValue("-Xmx4g");
        logAndAddFilesToCompile(safeintc);
        
        return executeExternalCompile(safeintc.getCommandline(), safeintc.size(), true) == 0;
    }

}
