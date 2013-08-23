package safeint;

import polyglot.lex.EscapedUnicodeReader;
import polyglot.lex.Lexer;
import polyglot.main.Options;
import safeint.parse.Lexer_c;
import safeint.parse.Grm;
import safeint.types.SafeIntTypeSystem_c;
import safeint.ast.*;

import polyglot.ast.*;
import polyglot.types.*;
import polyglot.util.*;
import polyglot.ext.jl5.JL5Options;
import polyglot.frontend.*;

import java.io.*;

/**
 * Extension information for java-safeint extension.
 */
public class ExtensionInfo extends polyglot.ext.jl5.ExtensionInfo {
    static {
        // force Topics to load
        @SuppressWarnings("unused")
        Topics t = new Topics();
    }

    @Override
    public String defaultFileExtension() {
        return "si";
    }

    @Override
    public String compilerName() {
        return "java-safeintc";
    }

    @Override
    public Parser parser(Reader reader, FileSource source, ErrorQueue eq) {
        reader = new EscapedUnicodeReader(reader);
        Lexer lexer = new Lexer_c(reader, source, eq);
        Grm grm = new Grm(lexer, ts, nf, eq);
        return new CupParser(grm, source, eq);
    }

    @Override
    protected NodeFactory createNodeFactory() {
        return new SafeIntNodeFactory_c();
    }

    @Override
    protected TypeSystem createTypeSystem() {
        return new SafeIntTypeSystem_c();
    }

    @Override
    public Scheduler createScheduler() {
        return new SafeIntScheduler(this);
    }
    
    @Override
    public Options createOptions() {
    	return new SafeIntOptions(this);
    }
}
