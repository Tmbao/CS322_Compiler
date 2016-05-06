// This class is used to generate warning and error messages.
class Errors {
    static void fatal(int lineNum, int charNum, String msg) {
        System.out.println(lineNum + ":" + charNum + " **ERROR** " + msg);
        fatalError = true;
    }

    static void warn(int lineNum, int charNum, String msg) {
        System.out.println(lineNum + ":" + charNum + " **WARNING** " + msg);
    }
    
    static boolean fatalError = false;
    
    static void semanticError(int lineNum, int charNum, String msg) {
        System.out.println(lineNum + ":" + charNum + " **SEMANTIC ERROR** " + msg);
        semanticErrors++;
    }

    static void semanticWarn(int lineNum, int charNum, String msg) {
        System.out.println(lineNum + ":" + charNum + " **SEMANTIC WARNING** " + msg);
        semanticWarns++;
    }
    
    static int semanticErrors = 0;
    static int semanticWarns = 0;

	static void prompt(int lineNum, int charNum, CompilingException exception) {
		switch (exception.getType()) {
			case SEMANTIC_ERROR:
				semanticError(lineNum, charNum, exception.getMessage());
				break;
			case SEMANTIC_WARNING:
				semanticWarn(lineNum, charNum, exception.getMessage());
				break;
		}
	}
}
