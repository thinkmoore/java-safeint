package safeint;

import polyglot.lex.Lexer;
import safeint.parse.Lexer_c;
import safeint.parse.Grm;
import safeint.ast.*;
import safeint.types.*;

import polyglot.ast.*;
import polyglot.types.*;
import polyglot.util.*;
import polyglot.visit.*;
import polyglot.frontend.*;
import polyglot.main.*;

import java.util.*;
import java.io.*;

/**
 * Extension information for java-safeint extension.
 */
public class ExtensionInfo extends polyglot.frontend.JLExtensionInfo {
    static {
        // force Topics to load
        Topics t = new Topics();
    }

    public String defaultFileExtension() {
        return "si";
    }

    public String compilerName() {
        return "java-safeintc";
    }

    public Parser parser(Reader reader, FileSource source, ErrorQueue eq) {
        Lexer lexer = new Lexer_c(reader, source, eq);
        Grm grm = new Grm(lexer, ts, nf, eq);
        return new CupParser(grm, source, eq);
    }

    protected NodeFactory createNodeFactory() {
        return new SafeIntNodeFactory_c();
    }

    protected TypeSystem createTypeSystem() {
        return new SafeIntTypeSystem_c();
    }

}
