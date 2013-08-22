package safeint.ant;

import java.io.File;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.compilers.DefaultCompilerAdapter;
import org.apache.tools.ant.types.Commandline;

import polyglot.util.InternalCompilerError;

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
            	String encoding = args[++i];
                if (!(encoding.equalsIgnoreCase("UTF-8") || encoding.equalsIgnoreCase("ISO-8859-1"))) {
                    throw new InternalCompilerError("Unsupported encoding " + args[i]);
                }
                continue;
            }
            if (args[i].equals("-O")) {
                continue;
            }
            if (args[i].startsWith("-X")) {
            	continue;
            }
            safeintc.createArgument().setValue(args[i]);
        }
        safeintc.createArgument().setValue("-morepermissiveinference");
        safeintc.createArgument().setValue("-morepermissivecasts");
        safeintc.createArgument().setValue("-post");
        safeintc.createArgument().setValue(post.toString());
        safeintc.createArgument().setValue("-j");
        safeintc.createArgument().setValue("-Xmx4g");
        
        // Add files to be compiled
        for (File file : compileList) {
        	String arg = file.getAbsolutePath();
        	if (!arg.endsWith("package-info.java")) {
        		safeintc.createArgument().setValue(arg);
        	}
        }
        
        attributes.log("Invoking safeintc with: " + safeintc.toString(), Project.MSG_INFO);
        return executeExternalCompile(safeintc.getCommandline(), safeintc.size(), true) == 0;
    }
}
