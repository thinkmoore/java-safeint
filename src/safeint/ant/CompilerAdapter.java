package safeint.ant;

import java.io.File;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.compilers.DefaultCompilerAdapter;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.Path;

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
        safeintc.createArgument().setValue("-j");
        safeintc.createArgument().setValue("-Xmx4g");
        propertyArgument(safeintc, "safeint.instrumentShifts", "-instrumentShifts", false);
        
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
    
    private void propertyArgument(Commandline cmd, String property, String flag, boolean value, String defValue) {
		String propValue = project.getProperty(property);
		if (propValue != null) {
			cmd.createArgument().setValue(flag);
			if (value && !propValue.equals("")) {
				cmd.createArgument().setValue(propValue);
			} else if (value && defValue != null) {
				cmd.createArgument().setValue(defValue);
			}
		} else {
		    if (value && defValue != null) {
		        cmd.createArgument().setValue(flag);
		        cmd.createArgument().setValue(defValue);
		    }
		}
	}

	private void propertyArgument(Commandline cmd, String property, String flag, boolean value) {
		propertyArgument(cmd,property,flag,value,null);
	}
}
