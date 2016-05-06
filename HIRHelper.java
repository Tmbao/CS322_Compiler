class HIRHelper {

	private static int temporaryCounter = 0; // Temporary variables

	public static int newTemporary() {
		return temporaryCounter++;
	}

	public static int countTemporary() {
		return temporaryCounter + 1;
	}

	private static int localCounter = 0; // Local variables

	public static int newLocal() {
		return localCounter++;
	}

	public static int countLocal() {
		return localCounter + 1;
	}

	private static int globalCounter = 0; // Global variables
	
	public static int newGlobal() {
		return globalCounter++;
	}

	public static int countGlobal() {
		return globalCounter + 1;
	}

	private static int constCounter = 0; // Constants
	
	public static int newConst() {
		return constCounter++;
	}

	public static int countConst() {
		return constCounter + 1;
	}

	public static int paramCounter = 0; // Parameters

	public static int newParam() {
		return paramCounter++;
	}

	public static int countParam() {
		return paramCounter + 1;
	}

	public static int newVariable(Scope scope) {
		switch (scope) {
			case GLOBAL:
				return newGlobal();
			case CONST:
				return newConst();
			case PARAM:
				return newParam();
			case LOCAL:
				return newLocal();
			case TEMPORARY:
				return newTemporary();
		}
		return -1;
	}

	public static void reset() {
		localCounter = 0;
		temporaryCounter = 0;
		paramCounter = 0;
	}

	private static int labelCounter = 0;
	
	public static String newLabel() {
		String ret = String.format("~%d", labelCounter);
		labelCounter++;
		return ret;
	}

	public static String getAddress(int addr, Scope scope) {
		switch (scope) {
			case GLOBAL:
				return String.format("$%d", addr);
			case CONST:
				return String.format("?%d", addr);
			case PARAM:
				return String.format("%%%d", addr);
			case LOCAL:
				return String.format("@%d", addr);
			case TEMPORARY:
				return String.format("&%d", addr);
		}
		return "";
	}

	public static String assign(String res, String op) {
		return String.format("move %s, %s", res, op);
	}

	public static String assignArray(String arr, String id, String val) {
		return String.format("arrs %s, %s, %s", arr, id, val);
	}

	public static String getArray(String res, String arr, String id) {
		return String.format("arrg %s, %s, %s", res, arr, id);
	}

	public static String add(String res, String op1, String op2) {
		return String.format("add %s, %s, %s", res, op1, op2);
	}

	public static String subtract(String res, String op1, String op2) {
		return String.format("sub %s, %s, %s", res, op1, op2);
	}

	public static String multiply(String res, String op1, String op2) {
		return String.format("mult %s, %s, %s", res, op1, op2);
	}

	public static String divide(String res, String op1, String op2) {
		return String.format("div %s, %s, %s", res, op1, op2);
	}

	public static String modulo(String res, String op1, String op2) {
		return String.format("mod %s, %s, %s", res, op1, op2);
	}

	public static String and(String res, String op1, String op2) {
		return String.format("and %s, %s, %s", res, op1, op2);
	}

	public static String or(String res, String op1, String op2) {
		return String.format("or %s, %s, %s", res, op1, op2);
	}

	public static String greater(String res, String op1, String op2) {
		return String.format("gt %s, %s, %s", res, op1, op2);
	}

	public static String greaterOrEqual(String res, String op1, String op2) {
		return String.format("gte %s, %s, %s", res, op1, op2);
	}

	public static String less(String res, String op1, String op2) {
		return String.format("lt %s, %s, %s", res, op1, op2);
	}

	public static String lessOrEqual(String res, String op1, String op2) {
		return String.format("lte %s, %s, %s", res, op1, op2);
	}

	public static String equal(String res, String op1, String op2) {
		return String.format("eq %s, %s, %s", res, op1, op2);
	}

	public static String notEqual(String res, String op1, String op2) {
		return String.format("neq %s, %s, %s", res, op1, op2);
	}

	public static String complement(String res, String op) {
		return String.format("comp %s, %s", res, op);
	}

	public static String not(String res, String op) {
		return String.format("not %s, %s", res, op);
	}

	public static String jump(String lbl) {
		return String.format("jump %s", lbl);
	}

	public static String jumpLess(String op1, String op2, String lbl) {
		return String.format("jlt %s, %s, %s", op1, op2, lbl);
	}

	public static String jumpLessOrEqual(String op1, String op2, String lbl) {
		return String.format("jlte %s, %s, %s", op1, op2, lbl);
	}

	public static String jumpEqual(String op1, String op2, String lbl) {
		return String.format("jeq %s, %s, %s", op1, op2, lbl);
	}

	public static String jumpNotEqual(String op1, String op2, String lbl) {
		return String.format("jneq %s, %s, %s", op1, op2, lbl);
	}

	public static String setArgument(int id, String value) {
		return String.format("arg %s, %d", value, id);
	}

	public static String callProc(String res, String fn, int argc) {
		return String.format("callf %s, %s, %d", res, fn, argc);
	}

	public static String callProc(String fn, int argc) {
		return String.format("call %s, %d", fn, argc);
	}

	public static String beginFunction(String fn, int varc, int tempc) {
		return String.format("func %s\nfunci %d, %d", fn, varc, tempc);
	}

	public static String endFunction(String fn) {
		return String.format("efunc %s", fn);
	}

	public static String returnFn(String fn) {
		return String.format("ret %s", fn);
	}

	public static String returnFn(String fn, String val) {
		return String.format("retf %s, %s", fn, val);
	}

	public static String addConst(String val) {
		return String.format("str %s", val);
	}

	public static String setEntry(String mainFn, int glbc) {
		return String.format("entry %s, %d", mainFn, glbc);
	}

	public static String readValue(String val) {
		return String.format("read %s", val);
	}

	public static String writeValue(String val) {
		return String.format("write %s", val);
	}
}
