import java.io.*;
import java_cup.runtime.*;

// **********************************************************************
// Main program to test the C-- parser.
//
// The program opens the input file (C-- source file), creates a scanner 
// and a parser, and calls the parser and the checker.
// **********************************************************************
public class Checker {

    public static void main(String[] args)
            throws IOException // may be thrown by the scanner
    {
        String inName = "";
		String ouName = "";

        // check for command-line args
        if (args.length == 2) {
            inName = args[0];
			ouName = args[1];
        } else {
            System.err.println("usage: Checker <input file> <output file>");
            System.exit(-1);
        }

        // open input file
        FileReader inFile = null;
        try {
            inFile = new FileReader(inName);
        } catch (FileNotFoundException ex) {
            System.err.println("File " + inName + " not found.");
            System.exit(-1);
        }

        parser P = new parser(new Yylex(inFile));

        Program program = null;

        try {
            program = (Program) P.parse().value; // do the parse
        } catch (Exception ex) {
            System.err.println("Exception occured during parse: " + ex);
            System.exit(-1);
        }

        if (Errors.fatalError) {
            System.err.println("Confused by earlier errors: aborting");
            System.exit(0);
        }

        // Semantic checking
        program.check();
        
        System.out.println("Semantic Error(s): " + Errors.semanticErrors
                + ". Semantic Warning(s): " + Errors.semanticWarns + ".");

		if (Errors.semanticErrors > 0) {
			System.err.println("Compile error(s): aborting");
            System.exit(0);
		}

		// Translating
		TranslationAG translationAG = program.translate();
		
		PrintWriter writer = new PrintWriter(ouName, "UTF-8");
		writer.print(translationAG.getCode().toString());
		writer.close();
    }
}
