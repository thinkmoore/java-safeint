package safeint;

import polyglot.lex.Lexer;
import safeint.parse.Lexer_c;
import safeint.parse.Grm;
import safeint.types.SafeIntTypeSystem_c;
import safeint.ast.*;

import polyglot.ast.*;
import polyglot.types.*;
import polyglot.util.*;
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

}
