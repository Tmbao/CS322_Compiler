import java.io.*;
import java.util.*;

// **********************************************************************
// Ast class (base class for all other kinds of nodes)
// **********************************************************************
abstract class Ast {

	public Ast() {
		scope = Scope.LOCAL;
	}

	public void setFalse(String label) {
		falseLabel = label;
	}

	public String getFalse() {
		return falseLabel;
	}

	public void setTrue(String label) {
		trueLabel = label;
	}

	public String getTrue() {
		return trueLabel;
	}

	public void setNext(String label) {
		nextLabel = label;
	}

	public String getNext() {
		return nextLabel;
	}

	public void setScope(Scope scope) {
		this.scope = scope;
	}

	public Scope getScope() {
		return scope;
	}

	protected String falseLabel;
	protected String trueLabel;
	protected String nextLabel;
	protected Scope scope;
}

class Program extends Ast {

    public Program(DeclList declList) {
        this.declList = declList;
    }
    
    // Semantic checking
    public void check() {
        declList.check(new SymbolTable());
    }

	// Translating
	public TranslationAG translate() {
		declList.setScope(Scope.GLOBAL);
		declList.setNext(HIRHelper.newLabel());
		
		consts = new LinkedList<String>();
		TranslationAG dag = declList.translate(new SymbolTable());
		
		TranslationAG ret = new TranslationAG();
		for (String item : consts)
			ret.appendCode(HIRHelper.addConst(item));
		ret.appendCode(HIRHelper.setEntry("main_", HIRHelper.countGlobal()));
		ret.appendCode(dag.getCode());

		return ret;
	}

	public static void addConst(String value) {
		consts.add(value);
	}

	private static LinkedList<String> consts;

    private DeclList declList;
}

// **********************************************************************
// Decls
// **********************************************************************
class DeclList extends Ast {

    public DeclList(LinkedList<Decl> decls) {
        this.decls = decls;
    }

    // Semantic checking
    public void check(SymbolTable symbolTable) {
        for (Decl item : decls) 
            item.check(symbolTable);
    }

	public TranslationAG translate(SymbolTable symbolTable) {
		TranslationAG ret = new TranslationAG();
		for (Decl item : decls) {
			TranslationAG iag = item.translate(symbolTable);
			ret.appendCode(iag.getCode());
		}
		return ret;
	}

	public void setScope(Scope scope) {
		for (Decl item : decls) 
			item.setScope(scope);
	}

    // linked list of kids (Decls)
    protected LinkedList<Decl> decls;
}

abstract class Decl extends Ast {
    // Semantic checking
    public abstract void check(SymbolTable symbolTable);

	public abstract TranslationAG translate(SymbolTable symbolTable);
}

class VarDecl extends Decl {

    public VarDecl(Type type, Id name) {
        this.type = type;
        this.name = name;
    }

    // Semantic checking
    public void check(SymbolTable symbolTable) {
        if (type.getName() == Type.voidTypeName)
            Errors.prompt(name.getLine(), name.getChar(), 
                    new CompilingException(ExceptionType.SEMANTIC_WARNING, 
                        "Variable " + name.getName() + " cannot be of void type"));
        try {
            symbolTable.addEntry(name.getName(), new VariableType(type));
        } catch (CompilingException exception) {
            Errors.prompt(name.getLine(), name.getChar(), exception);
        }
    }

	public TranslationAG translate(SymbolTable symbolTable) {
		VariableType varType;
		try {
			varType = new VariableType(type, HIRHelper.newVariable(getScope()), getScope());
			symbolTable.addEntry(name.getName(), varType);
		} catch (CompilingException exception) {
		}
		return new TranslationAG();
	}

    private Type type;
    private Id name;
}

class FnDecl extends Decl {

    public FnDecl(Type type, Id name, FormalsList formalList, FnBody body) {
        this.type = type;
        this.name = name;
        this.formalList = formalList;
        this.body = body;
        this.body.setFunction(this);
    }

    // Semantic checking
    public void check(SymbolTable symbolTable) {
        SymbolTable fnSymbolTable = new SymbolTable();
        fnSymbolTable.setParent(symbolTable);
        formalList.check(fnSymbolTable);
        try {
            symbolTable.addEntry(name.getName(), new FunctionType(type, formalList.getTypes(), false));
        } catch (CompilingException exception) {
            Errors.prompt(name.getLine(), name.getChar(), exception);
        }
        body.check(fnSymbolTable);
    }

	public TranslationAG translate(SymbolTable symbolTable) {
		formalList.setScope(Scope.PARAM);
		HIRHelper.reset();

		try {
            symbolTable.addEntry(name.getName(), new FunctionType(type, formalList.getTypes(), false));
        } catch (CompilingException exception) {
        }

		SymbolTable fnSymbolTable = new SymbolTable();
		fnSymbolTable.setParent(symbolTable);
		TranslationAG fag = formalList.translate(fnSymbolTable);
		TranslationAG bag = body.translate(fnSymbolTable);
		
		TranslationAG ret = new TranslationAG();
		ret.appendCode(HIRHelper.beginFunction(name.getName() + "_" + formalList.getLabel(), HIRHelper.countLocal(), HIRHelper.countTemporary()));
		ret.appendCode(fag.getCode());
		ret.appendCode(bag.getCode());
		ret.appendCode(HIRHelper.endFunction(name.getName() + "_" + formalList.getLabel()));
		return ret;
	}

	public String getLabel() {
		return name.getName() + "_" + formalList.getLabel();
	}

    public Id getId() {
        return name;
    }

    public Type getType() {
        return type;
    }

    private Type type;
    private Id name;
    private FormalsList formalList;
    private FnBody body;
}

class FnPreDecl extends Decl {

    public FnPreDecl(Type type, Id name, FormalsList formalList) {
        this.type = type;
        this.name = name;
        this.formalList = formalList;
    }

    // Semantic checking
    public void check(SymbolTable symbolTable) {
        formalList.check(new SymbolTable());
        try {
            symbolTable.addEntry(name.getName(), new FunctionType(type, formalList.getTypes(), true));
        } catch (CompilingException exception) {
            Errors.prompt(name.getLine(), name.getChar(), exception);
        }
    }

	public TranslationAG translate(SymbolTable symbolTable) {
		return new TranslationAG();
	}

    private Type type;
    private Id name;
    private FormalsList formalList;
}

class FormalsList extends Ast {

    public FormalsList(LinkedList<FormalDecl> formals) {
        this.formals = formals;
    }

    // Semantic checking
    public void check(SymbolTable symbolTable) {
        for (FormalDecl item : formals) 
            item.check(symbolTable);
    }

	public TranslationAG translate(SymbolTable symbolTable) {
		TranslationAG ret = new TranslationAG();
		for (FormalDecl item : formals) {
			TranslationAG temp = item.translate(symbolTable);
			ret.appendCode(temp.getCode());
		}
		return ret;
	}

	public void setScope(Scope scope) {
		for (FormalDecl item : formals)
			item.setScope(scope);
	}

	public int size() {
		return formals.size();
	}

	public String getLabel() {
		String ret = "";
		for (FormalDecl item : formals)
			ret += "_" + item.getType().getName();
		return ret;
	}

    public LinkedList<Type>getTypes() {
        LinkedList<Type> types = new LinkedList<Type>();
        for (FormalDecl item : formals)
            types.add(item.getType());
        return types;
    }

    // linked list of kids (FormalDecls)
    private LinkedList<FormalDecl> formals;
}

class FormalDecl extends Decl {

    public FormalDecl(Type type, Id name) {
        this.type = type;
        this.name = name;
    }

    // Semantic checking
    public void check(SymbolTable symbolTable) {
        if (type.getName() == Type.voidTypeName && type.getNumPointers() == 0) 
            Errors.prompt(name.getLine(), name.getChar(), 
                    new CompilingException(ExceptionType.SEMANTIC_ERROR,
                        "Variable " + name.getName() + " cannot be of void type"));
        try {
            symbolTable.addEntry(name.getName(), new VariableType(type));
        } catch (CompilingException exception) {
            Errors.prompt(name.getLine(), name.getChar(), exception);
        }
    }

	public TranslationAG translate(SymbolTable symbolTable) {
		VariableType varType;
		try {
			varType = new VariableType(type, HIRHelper.newVariable(Scope.PARAM), Scope.PARAM);
			symbolTable.addEntry(name.getName(), varType);
		} catch (CompilingException exception) {
		}
		return new TranslationAG();
	}

    public Type getType() {
        return type;
    }

    private Type type;
    private Id name;
}

class FnBody extends Ast {

    public FnBody(DeclList declList, StmtList stmtList) {
        this.declList = declList;
        this.stmtList = stmtList;
    }

    // Semantic checking
    public void check(SymbolTable symbolTable) {
        declList.check(symbolTable);
        stmtList.check(symbolTable);
    }

	public TranslationAG translate(SymbolTable symbolTable) {
		TranslationAG dag = declList.translate(symbolTable);
		TranslationAG sag = stmtList.translate(symbolTable);

		TranslationAG ret = new TranslationAG();
		ret.appendCode(dag.getCode());
		ret.appendCode(sag.getCode());
		return ret;
	}

    public void setFunction(FnDecl fn) {
        this.stmtList.setFunction(fn);
    }

    private DeclList declList;
    private StmtList stmtList;
}

class StmtList extends Ast {

    public StmtList(LinkedList<Stmt> stmts) {
        this.stmts = stmts;
    } 

    // Semantic checking 
    public void check(SymbolTable symbolTable) {
        for (Stmt item : stmts) 
            item.check(symbolTable);
    }

	public TranslationAG translate(SymbolTable symbolTable) {
		TranslationAG ret = new TranslationAG();
		for (Stmt item : stmts) {
			item.setNext(HIRHelper.newLabel());
			TranslationAG iag = item.translate(symbolTable);
			if (ret.appendCode(iag.getCode()))
				ret.appendCode(item.getNext() + ":");
		}
		return ret;
	}

    public void setFunction(FnDecl fn) {
        for (Stmt item : stmts) 
            item.setFunction(fn);
    }

    // linked list of kids (Stmts)
    private LinkedList<Stmt> stmts;
}

// **********************************************************************
// Types
// **********************************************************************
class Type extends Ast {
    
    private Type() {}
    
    public static Type CreateSimpleType(String name)
    {
        Type t = new Type();
        t.name = name;
        t.size = -1;
        t.numPointers = 0;
        
        return t;
    }
    
    public static Type CreateArrayType(String name, int size) {
        Type t = new Type();
        t.name = name;
        t.size = size;
        t.numPointers = 0;
        
        return t;
    }

    public static Type CreatePointerType(String name, int numPointers) {
        Type t = new Type();
        t.name = name;
        t.size = -1;
        t.numPointers = numPointers;
        
        return t;
    }

    public static Type CreateArrayPointerType(String name, int size, int numPointers) {
        Type t = new Type();
        t.name = name;
        t.size = size;
        t.numPointers = numPointers;
        
        return t;
    }
    
    public String getName() {
        return name;
    }

    public int getNumPointers() {
        return numPointers;
    }
 
    private String name;
    private int size;  // use if this is an array type
    private int numPointers;
    
    public static final String voidTypeName = "void";
    public static final String boolTypeName = "bool";
    public static final String intTypeName = "int";
    public static final String stringTypeName = "string";
	public static final String errorTypeName = "error";

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Type) 
            return this.name == ((Type)obj).name
                //&& this.size == (Type)obj.size 
                && this.numPointers == ((Type)obj).numPointers;
        else
            return false;
    }

	@Override 
	public int hashCode() {
		return name.hashCode();
	}
}

// **********************************************************************
// Stmts
// **********************************************************************
abstract class Stmt extends Ast {
    // Semantic checking
    public abstract void check(SymbolTable symbolTable);

	public abstract TranslationAG translate(SymbolTable symbolTable);

    public void setFunction(FnDecl fn) {
        this.fn = fn;
    }

    protected FnDecl fn;
}

class AssignStmt extends Stmt {

    public AssignStmt(Exp lhs, Exp exp) {
        this.lhs = lhs;
        this.exp = exp;
    }

    // Semantic checking
    public void check(SymbolTable symbolTable) {
        Type typ1 = lhs.getType(symbolTable);
        Type typ2 = exp.getType(symbolTable);

        // Check if exp and lhs are of the same type
        if (!typ1.equals(typ2)) 
            Errors.prompt(lhs.getLine(), lhs.getChar(), 
                    new CompilingException(ExceptionType.SEMANTIC_ERROR,
                        "Illegal assignment (Both lhs and expression must be of the same type)"));

    }

	// Translating
	public TranslationAG translate(SymbolTable symbolTable) {
		TranslationAG ag = new TranslationAG();
		lhs.setResult();
		TranslationAG lag = lhs.translate(symbolTable);
		TranslationAG rag = exp.translate(symbolTable);
		ag.appendCode(lag.getCode());
		ag.appendCode(rag.getCode());
		ag.setAddress(rag.getAddress());
	
		if (lhs instanceof ArrayExp) 
			ag.appendCode(HIRHelper.assignArray(lag.getAddress(), ((ArrayExp)lhs).getExpAddress(), rag.getAddress()));
		else
			ag.appendCode(HIRHelper.assign(lag.getAddress(), rag.getAddress()));
		return ag;
	}

    private Exp lhs;
    private Exp exp;
}

class IfStmt extends Stmt {

    public IfStmt(Exp exp, DeclList declList, StmtList stmtList) {
        this.exp = exp;
        this.declList = declList;
        this.stmtList = stmtList;
    }

    // Semantic checking
    public void check(SymbolTable symbolTable) {
        if (exp.getType(symbolTable).getName() != Type.boolTypeName)
			Errors.prompt(exp.getLine(), exp.getChar(),
					new CompilingException(ExceptionType.SEMANTIC_ERROR,
						"Condition expression must be of bool type"));
        SymbolTable ifSymbolTable = new SymbolTable();
        ifSymbolTable.setParent(symbolTable);
        declList.check(ifSymbolTable);
        stmtList.setFunction(fn);
        stmtList.check(ifSymbolTable);
    }

	// Translating
	public TranslationAG translate(SymbolTable symbolTable) {
		exp.setTrue(HIRHelper.newLabel());
		exp.setFalse(getNext());
		stmtList.setNext(getNext());

		TranslationAG eag = exp.translate(symbolTable);
		SymbolTable ifSymbolTable = new SymbolTable();
		ifSymbolTable.setParent(symbolTable);
		TranslationAG dag = declList.translate(ifSymbolTable);
		TranslationAG sag = stmtList.translate(ifSymbolTable);

		TranslationAG ret = new TranslationAG();
		ret.appendCode(eag.getCode());
		ret.appendCode(exp.getTrue() + ":");
		ret.appendCode(dag.getCode());
		ret.appendCode(sag.getCode());
		return ret;
	}
    
    private Exp exp;
    private DeclList declList;
    private StmtList stmtList;
}

class IfElseStmt extends Stmt {

    public IfElseStmt(Exp exp, DeclList declList1, StmtList stmtList1, 
            DeclList declList2, StmtList stmtList2) {
        this.exp = exp;
        this.declList1 = declList1;
        this.stmtList1 = stmtList1;
        this.declList2 = declList2;
        this.stmtList2 = stmtList2;
    }

    // Semantic checking
    public void check(SymbolTable symbolTable) {
		if (exp.getType(symbolTable).getName() != Type.boolTypeName)
			Errors.prompt(exp.getLine(), exp.getChar(),
					new CompilingException(ExceptionType.SEMANTIC_ERROR,
						"Condition expression must be of bool type"));
        SymbolTable ifSymbolTable = new SymbolTable();
        ifSymbolTable.setParent(symbolTable);
        declList1.check(ifSymbolTable);
        stmtList1.setFunction(fn);
        stmtList1.check(ifSymbolTable);
        SymbolTable elseSymbolTable = new SymbolTable();
        elseSymbolTable.setParent(symbolTable);
        declList2.check(elseSymbolTable);
        stmtList2.setFunction(fn);
        stmtList2.check(elseSymbolTable);
    }

	public TranslationAG translate(SymbolTable symbolTable) {
		exp.setTrue(HIRHelper.newLabel());
		exp.setFalse(HIRHelper.newLabel());
		stmtList1.setNext(getNext());
		stmtList2.setNext(getNext());
		stmtList1.setFunction(fn);
		stmtList2.setFunction(fn);

		TranslationAG eag = exp.translate(symbolTable);
		SymbolTable ifSymbolTable = new SymbolTable();
        ifSymbolTable.setParent(symbolTable);
		TranslationAG d1ag = declList1.translate(ifSymbolTable);
        TranslationAG s1ag = stmtList1.translate(ifSymbolTable);
        SymbolTable elseSymbolTable = new SymbolTable();
        elseSymbolTable.setParent(symbolTable);
        TranslationAG d2ag = declList2.translate(elseSymbolTable);
        TranslationAG s2ag = stmtList2.translate(elseSymbolTable);
		
		TranslationAG ret = new TranslationAG();
		ret.appendCode(eag.getCode());
		ret.appendCode(exp.getTrue() + ":");
		ret.appendCode(d1ag.getCode());
		ret.appendCode(s1ag.getCode());
		ret.appendCode(HIRHelper.jump(getNext()));
		ret.appendCode(exp.getFalse() + ":");
		ret.appendCode(d2ag.getCode());
		ret.appendCode(s2ag.getCode());

		return ret;
	}

    private Exp exp;
    private DeclList declList1;
    private DeclList declList2;
    private StmtList stmtList1;
    private StmtList stmtList2;
}

class WhileStmt extends Stmt {

    public WhileStmt(Exp exp, DeclList declList, StmtList stmtList) {
        this.exp = exp;
        this.declList = declList;
        this.stmtList = stmtList;
    }

    // Semantic checking
    public void check(SymbolTable symbolTable) {
		if (exp.getType(symbolTable).getName() != Type.boolTypeName)
			Errors.prompt(exp.getLine(), exp.getChar(),
					new CompilingException(ExceptionType.SEMANTIC_ERROR,
						"Condition expression must be of bool type"));
        SymbolTable whlSymbolTable = new SymbolTable();
        whlSymbolTable.setParent(symbolTable);
        declList.check(whlSymbolTable);
        stmtList.setFunction(fn);
        stmtList.check(whlSymbolTable);
    }

	public TranslationAG translate(SymbolTable symbolTable) {
		stmtList.setNext(HIRHelper.newLabel());
		stmtList.setFunction(fn);
		exp.setTrue(HIRHelper.newLabel());
		exp.setFalse(getNext());

		SymbolTable whlSymbolTable = new SymbolTable();
        whlSymbolTable.setParent(symbolTable);
		TranslationAG eag = exp.translate(symbolTable);
        TranslationAG dag = declList.translate(whlSymbolTable);
        TranslationAG sag = stmtList.translate(whlSymbolTable);

		TranslationAG ret = new TranslationAG();
		ret.appendCode(stmtList.getNext() + ":");
		ret.appendCode(eag.getCode());
		ret.appendCode(exp.getTrue() + ":");
		ret.appendCode(dag.getCode());
		ret.appendCode(sag.getCode());
		ret.appendCode(HIRHelper.jump(stmtList.getNext()));
		return ret;
	}

    private Exp exp;
    private DeclList declList;
    private StmtList stmtList;
}

class ForStmt extends Stmt {

    public ForStmt(Stmt init, Exp cond, Stmt incr, 
            DeclList declList, StmtList stmtList) {
        this.init = init;
        this.cond = cond;
        this.incr = incr;
        this.declList = declList;
        this.stmtList = stmtList;
    }

    // Semantic checking
    public void check(SymbolTable symbolTable) {
        init.check(symbolTable);
		if (cond.getType(symbolTable).getName() != Type.boolTypeName)
			Errors.prompt(cond.getLine(), cond.getChar(),
					new CompilingException(ExceptionType.SEMANTIC_ERROR,
						"Condition expression must be of bool type"));
        incr.check(symbolTable);

        SymbolTable forSymbolTable = new SymbolTable();
        forSymbolTable.setParent(symbolTable);
        declList.check(forSymbolTable);
        stmtList.setFunction(fn);
        stmtList.check(forSymbolTable);
    }

	public TranslationAG translate(SymbolTable symbolTable) {
		stmtList.setNext(HIRHelper.newLabel());
		stmtList.setFunction(fn);
		cond.setTrue(HIRHelper.newLabel());
		cond.setFalse(getNext());

		TranslationAG iag = init.translate(symbolTable);
		TranslationAG cag = cond.translate(symbolTable);
		TranslationAG mag = incr.translate(symbolTable);
		SymbolTable forSymbolTable = new SymbolTable();
		forSymbolTable.setParent(symbolTable);
		TranslationAG dag = declList.translate(forSymbolTable);
		TranslationAG sag = stmtList.translate(forSymbolTable);

		TranslationAG ret = new TranslationAG();
		ret.appendCode(iag.getCode());
		ret.appendCode(stmtList.getNext() + ":");
		ret.appendCode(cag.getCode());
		ret.appendCode(cond.getTrue() + ":");
		ret.appendCode(dag.getCode());
		ret.appendCode(sag.getCode());
		ret.appendCode(mag.getCode());
		ret.appendCode(HIRHelper.jump(stmtList.getNext()));

		return ret;
	}

    private Stmt init;
    private Exp cond;
    private Stmt incr;
    private DeclList declList;
    private StmtList stmtList;
}

class CallStmt extends Stmt {

    public CallStmt(CallExp callExp) {
        this.callExp = callExp;
    }

    // Semantic checking
    public void check(SymbolTable symbolTable) {
        callExp.getType(symbolTable);
    }

	public TranslationAG translate(SymbolTable symbolTable) {
		return callExp.translate(symbolTable);
	}

    private CallExp callExp;
}

class ReturnStmt extends Stmt {

    public ReturnStmt(Exp exp) {
        this.exp = exp;
    }

    // Semantic checking
    public void check(SymbolTable symbolTable) {
        if (exp == null) {
            if (fn.getType().getName() != Type.voidTypeName)
                Errors.prompt(fn.getId().getLine(), fn.getId().getChar(), 
                        new CompilingException(ExceptionType.SEMANTIC_ERROR,
                            "Illegal return statement"));
        } else {
            if (!fn.getType().equals(exp.getType(symbolTable)))
                Errors.prompt(exp.getLine(), fn.getId().getChar(), 
                        new CompilingException(ExceptionType.SEMANTIC_ERROR,
                            "Illegal return statement"));
        }
    }

	public TranslationAG translate(SymbolTable symbolTable) {
		TranslationAG ret = new TranslationAG();
		if (exp == null) {
			ret.appendCode(HIRHelper.returnFn(fn.getLabel()));
		} else {
			TranslationAG eag = exp.translate(symbolTable);
			ret.appendCode(eag.getCode());
			ret.appendCode(HIRHelper.returnFn(fn.getLabel(), eag.getAddress()));
		}
		return ret;
	}

    private Exp exp; // null for empty return
}

// **********************************************************************
// Exps
// **********************************************************************
abstract class Exp extends Ast {

	public Exp() {
		isResult = false;
	}

    public abstract int getLine();
    public abstract int getChar();
    public abstract Type getType(SymbolTable symbolTable);
	public abstract TranslationAG translate(SymbolTable symbolTable);

	public void setResult() {
		isResult = true;
	}

	protected boolean isResult;
}

abstract class BasicExp extends Exp
{
    private int lineNum;
    private int charNum;
    
    public BasicExp(int lineNum, int charNum) {
        this.lineNum = lineNum;
        this.charNum = charNum;
    }

	public TranslationAG translate(SymbolTable symbolTable) {
		return new TranslationAG();
	}
    
    public int getLine() {
        return lineNum;
    }
    
	public int getChar() {
        return charNum;
    }
}

class IntLit extends BasicExp {

    public IntLit(int lineNum, int charNum, int intVal) {
        super(lineNum, charNum);
        this.intVal = intVal;
    }

    public Type getType(SymbolTable symbolTable) {
        return Type.CreateSimpleType(Type.intTypeName);
    }

	public TranslationAG translate(SymbolTable symbolTable) {
		TranslationAG ret = new TranslationAG();
		ret.setAddress(Integer.toString(intVal));
		return ret;
	}
 
    private int intVal;
}

class StringLit extends BasicExp {

    public StringLit(int lineNum, int charNum, String strVal) {
        super(lineNum, charNum);
        this.strVal = strVal;
    }

    public Type getType(SymbolTable symbolTable) {
        return Type.CreateSimpleType(Type.stringTypeName);
    }

	public TranslationAG translate(SymbolTable symbolTable) {
		Program.addConst(strVal);
		TranslationAG ret = new TranslationAG();
		ret.setAddress(HIRHelper.getAddress(HIRHelper.newConst(), Scope.CONST));
		return ret;
	}

    public String str() {
        return strVal;
    }
    
    private String strVal;
}

class Id extends BasicExp {

    public Id(int lineNum, int charNum, String strVal) {
        super(lineNum, charNum);
        this.strVal = strVal;
    }

    public Type getType(SymbolTable symbolTable) {
		while (symbolTable != null) {
	        try {
		        return symbolTable.getVariableType(strVal).getType();
			} catch (CompilingException exception) {
		    }
			symbolTable = symbolTable.getParent();
		}
		Errors.prompt(getLine(), getChar(), 
				new CompilingException(
				    ExceptionType.SEMANTIC_ERROR,
					"Variable " + strVal + " has not been declared"));
	    return Type.CreateSimpleType(Type.errorTypeName);
    }

	public TranslationAG translate(SymbolTable symbolTable) {
		TranslationAG ret = new TranslationAG();
		VariableType varType;
		while (true) {
			try {
				varType = symbolTable.getVariableType(strVal);
				if (varType.getType().getName() != Type.errorTypeName) 
					break;
			} catch (CompilingException exception) {
			}
			symbolTable = symbolTable.getParent();
		}
		ret.setAddress(HIRHelper.getAddress(varType.getAddress(), varType.getScope()));
		return ret;
	}

    public String getName() {
        return strVal;
    }

    private String strVal;
}

class ArrayExp extends Exp {

    public ArrayExp(Exp lhs, Exp exp) {
        this.lhs = lhs;
        this.exp = exp;
    }

    public Type getType(SymbolTable symbolTable) {
        Type ret = lhs.getType(symbolTable);
        Type eType = exp.getType(symbolTable);
        if (eType.getName() != Type.intTypeName)
            Errors.prompt(getLine(), getChar(), 
                    new CompilingException(ExceptionType.SEMANTIC_ERROR,
                        "Index operand must be of int type"));
        return ret;
    }

	public TranslationAG translate(SymbolTable symbolTable) {
		TranslationAG ret = new TranslationAG();

		TranslationAG lag = lhs.translate(symbolTable);
		TranslationAG eag = exp.translate(symbolTable);
		ret.appendCode(lag.getCode());
		ret.appendCode(eag.getCode());
		lhsAddr = lag.getAddress();
		expAddr = eag.getAddress();

		if (!isResult) {
			ret.setAddress(HIRHelper.getAddress(HIRHelper.newVariable(Scope.TEMPORARY), Scope.TEMPORARY));
			ret.appendCode(HIRHelper.getArray(ret.getAddress(), lhsAddr, expAddr));
		}

		return ret;
	}

    public int getLine() {
        return lhs.getLine();
    }

    public int getChar() {
        return lhs.getChar();
    }

	public String getLhsAddress() {
		return lhsAddr;
	}

	public String getExpAddress() {
		return expAddr;
	}

    private Exp lhs;
    private Exp exp;
	private String lhsAddr;
	private String expAddr;
}

class CallExp extends Exp {

    public CallExp(Id name, ActualList actualList) {
        this.name = name;
        this.actualList = actualList;
    }

    public CallExp(Id name) {
        this.name = name;
        this.actualList = new ActualList(new LinkedList<Exp>());
    }

	private boolean isSystemCall() {
		return name.getName().equals("scanf") || name.getName().equals("printf");
	}

	private TranslationAG translateSystemCall(SymbolTable symbolTable) {
		TranslationAG ret = new TranslationAG();
		TranslationAG aag = actualList.translateSystemCall(symbolTable);
		ret.appendCode(aag.getCode());
		if (name.getName().equals("scanf"))
			ret.appendCode(HIRHelper.readValue(aag.getAddress()));
		else 
			ret.appendCode(HIRHelper.writeValue(aag.getAddress()));
		return ret;
	}

	private Type getTypeSystemCall(SymbolTable symbolTable) {
		if (actualList.size() > 1) 
			Errors.prompt(getLine(), getChar(), 
					new CompilingException(ExceptionType.SEMANTIC_ERROR, "Invalid parameters"));
		return Type.CreateSimpleType(Type.voidTypeName);
	}

    public Type getType(SymbolTable symbolTable) {
		// Handle system call
		if (isSystemCall()) 
			return getTypeSystemCall(symbolTable);

        Type ret = Type.CreateSimpleType(Type.errorTypeName);
        LinkedList<Type> params = actualList.getType(symbolTable);
        try {
            ret = symbolTable.getGlobalScope().getFunctionType(name.getName(), params).getType();
        } catch (CompilingException exception) {
            Errors.prompt(getLine(), getChar(), exception);
        }
        
        return ret;
    }

	public TranslationAG translate(SymbolTable symbolTable) {
		// Handle system call
		if (isSystemCall()) 
			return translateSystemCall(symbolTable);

		LinkedList<Type> params = actualList.getType(symbolTable);
		String formalsLabel = "";
		for (Type param : params)
			formalsLabel += "_" + param.getName();

		TranslationAG aag = actualList.translate(symbolTable);

		TranslationAG ret = new TranslationAG();
		ret.appendCode(aag.getCode());
		try {
			if (symbolTable.getGlobalScope().getFunctionType(name.getName(), params).getType().getName() == Type.voidTypeName) {
				ret.appendCode(HIRHelper.callProc(name.getName() + "_" + formalsLabel, actualList.size()));
			} else {
				ret.setAddress(HIRHelper.getAddress(HIRHelper.newVariable(Scope.TEMPORARY), Scope.TEMPORARY));
				ret.appendCode(HIRHelper.callProc(ret.getAddress(), name.getName() + "_" + formalsLabel, actualList.size()));
			}
		} catch (CompilingException exception) {
		}
		return ret;
	}

    public int getLine() {
        return name.getLine();
    }

    public int getChar() {
        return name.getChar();
    }

    private Id name;
    private ActualList actualList;
}

class ActualList extends Ast {

    public ActualList(LinkedList<Exp> exps) {
        this.exps = exps;
    }

    public LinkedList<Type> getType(SymbolTable symbolTable) {
        LinkedList<Type> type = new LinkedList<Type>();
        for (Exp item : exps)
            type.add(item.getType(symbolTable));
        return type;
    }

	public TranslationAG translate(SymbolTable symbolTable) {
		TranslationAG ret = new TranslationAG();
		for (int idx = 0; idx < exps.size(); ++idx) {
			TranslationAG temp = exps.get(idx).translate(symbolTable);
			ret.appendCode(temp.getCode());
			ret.appendCode(HIRHelper.setArgument(idx, temp.getAddress()));
			ret.setAddress(temp.getAddress());
		}
		return ret;
	}
	
	public TranslationAG translateSystemCall(SymbolTable symbolTable) {
		TranslationAG ret = new TranslationAG();
		for (int idx = 0; idx < exps.size(); ++idx) {
			TranslationAG temp = exps.get(idx).translate(symbolTable);
			ret.appendCode(temp.getCode());
			ret.setAddress(temp.getAddress());
		}
		return ret;
	}

	public int size() {
		return exps.size();
	}

    // linked list of kids (Exps)
    private LinkedList<Exp> exps;
}

abstract class UnaryExp extends Exp {

    public UnaryExp(Exp exp) {
        this.exp = exp;
    }

    public int getLine() {
        return exp.getLine();
    }

    public int getChar() {
        return exp.getChar();
    }

    protected Exp exp;
}

abstract class BinaryExp extends Exp {

    public BinaryExp(Exp exp1, Exp exp2) {
        this.exp1 = exp1;
        this.exp2 = exp2;
    }

    public int getLine() {
        return exp1.getLine();
    }

    public int getChar() {
        return exp1.getChar();
    }

    protected Exp exp1;
    protected Exp exp2;
}


// **********************************************************************
// UnaryExps
// **********************************************************************
class UnaryMinusExp extends UnaryExp {

    public UnaryMinusExp(Exp exp) {
        super(exp);
    }

    public Type getType(SymbolTable symbolTable) {
        Type type = exp.getType(symbolTable);
        if (type.getName() != Type.intTypeName) 
            Errors.prompt(getLine(), getChar(),
                    new CompilingException(ExceptionType.SEMANTIC_ERROR,
                        "Expression must be of int type"));
        return Type.CreateSimpleType(Type.intTypeName);
    }

	public TranslationAG translate(SymbolTable symbolTable) {
		TranslationAG eag = exp.translate(symbolTable);
		
		TranslationAG ret = new TranslationAG();
		ret.appendCode(eag.getCode());
		ret.setAddress(HIRHelper.getAddress(HIRHelper.newVariable(Scope.TEMPORARY), Scope.TEMPORARY));
		ret.appendCode(HIRHelper.subtract(ret.getAddress(), "0", eag.getAddress()));
		return ret;
	}
}

class NotExp extends UnaryExp {

    public NotExp(Exp exp) {
        super(exp);
    }

    public Type getType(SymbolTable symbolTable) {
        Type type = exp.getType(symbolTable);
        if (type.getName() != Type.boolTypeName) 
            Errors.prompt(getLine(), getChar(),
                    new CompilingException(ExceptionType.SEMANTIC_ERROR,
                        "Expression must be of bool type"));
        return Type.CreateSimpleType(Type.boolTypeName);
    }

	public TranslationAG translate(SymbolTable symbolTable) {
		exp.setTrue(getFalse());
		exp.setFalse(getTrue());
		TranslationAG eag = exp.translate(symbolTable);
		TranslationAG ret = new TranslationAG();
		ret.appendCode(eag.getCode());
		return ret;
	}
}

class AddrOfExp extends UnaryExp {

    public AddrOfExp(Exp exp) {
        super(exp);
    }

    public Type getType(SymbolTable symbolTable) {
        Type type = exp.getType(symbolTable);
        if (exp.getClass().getSimpleName() != "Id")
            Errors.prompt(getLine(), getChar(),
                    new CompilingException(ExceptionType.SEMANTIC_ERROR,
                        "Expression must be an identifier"));
        return type;
    }

	public TranslationAG translate(SymbolTable symbolTable) {
		TranslationAG ret = new TranslationAG();
		TranslationAG eag = exp.translate(symbolTable);
		ret.appendCode(eag.getCode());

		// AddrOf has not been supported yet
		return ret;
	}
}

class DeRefExp extends UnaryExp {

    public DeRefExp(Exp exp) {
        super(exp);
    }
    
    public Type getType(SymbolTable symbolTable) {
        Type type = exp.getType(symbolTable);
        if (exp.getClass().getSimpleName() != "Id")
            Errors.prompt(getLine(), getChar(),
                    new CompilingException(ExceptionType.SEMANTIC_ERROR,
                        "Expression must be an identifier"));
        return type;
    }
	
	public TranslationAG translate(SymbolTable symbolTable) {
		TranslationAG ret = new TranslationAG();
		TranslationAG eag = exp.translate(symbolTable);
		ret.appendCode(eag.getCode());

		// DeRefExp has not been supported yet
		return ret;
	}
}

// **********************************************************************
// BinaryExps
// **********************************************************************
class PlusExp extends BinaryExp {

    public PlusExp(Exp exp1, Exp exp2) {
        super(exp1, exp2);
    }
    
    public Type getType(SymbolTable symbolTable) {
        Type t1 = exp1.getType(symbolTable);
        Type t2 = exp2.getType(symbolTable);
        if (t1 != null && t2 != null && t1.getName() == Type.intTypeName && t2.getName() == Type.intTypeName) 
            return Type.CreateSimpleType(Type.intTypeName);
        Errors.prompt(getLine(), getChar(),
                new CompilingException(ExceptionType.SEMANTIC_ERROR,
                    "Illegal expression (All operands must be of int type)"));
        return Type.CreateSimpleType(Type.intTypeName);
    }

	public TranslationAG translate(SymbolTable symbolTable) {
		TranslationAG eag1 = exp1.translate(symbolTable);
		TranslationAG eag2 = exp2.translate(symbolTable);
		
		TranslationAG ret = new TranslationAG();
		ret.appendCode(eag1.getCode());
		ret.appendCode(eag2.getCode());
		ret.setAddress(HIRHelper.getAddress(HIRHelper.newVariable(Scope.TEMPORARY), Scope.TEMPORARY));
		ret.appendCode(HIRHelper.add(ret.getAddress(), eag1.getAddress(), eag2.getAddress()));
		return ret;
	}
}

class MinusExp extends BinaryExp {

    public MinusExp(Exp exp1, Exp exp2) {
        super(exp1, exp2);
    }

    public Type getType(SymbolTable symbolTable) {
        Type t1 = exp1.getType(symbolTable);
        Type t2 = exp2.getType(symbolTable);
        if (t1 != null && t2 != null && t1.getName() == Type.intTypeName && t2.getName() == Type.intTypeName) 
            return Type.CreateSimpleType(Type.intTypeName);
        Errors.prompt(getLine(), getChar(),
                new CompilingException(ExceptionType.SEMANTIC_ERROR,
                    "Illegal expression (All operands must be of int type)"));
        return Type.CreateSimpleType(Type.intTypeName);
    }

	public TranslationAG translate(SymbolTable symbolTable) {
		TranslationAG eag1 = exp1.translate(symbolTable);
		TranslationAG eag2 = exp2.translate(symbolTable);
		
		TranslationAG ret = new TranslationAG();
		ret.appendCode(eag1.getCode());
		ret.appendCode(eag2.getCode());
		ret.setAddress(HIRHelper.getAddress(HIRHelper.newVariable(Scope.TEMPORARY), Scope.TEMPORARY));
		ret.appendCode(HIRHelper.subtract(ret.getAddress(), eag1.getAddress(), eag2.getAddress()));
		return ret;
	}

}

class TimesExp extends BinaryExp {

    public TimesExp(Exp exp1, Exp exp2) {
        super(exp1, exp2);
    }

    public Type getType(SymbolTable symbolTable) {
        Type t1 = exp1.getType(symbolTable);
        Type t2 = exp2.getType(symbolTable);
        if (t1 != null && t2 != null && t1.getName() == Type.intTypeName && t2.getName() == Type.intTypeName) 
            return Type.CreateSimpleType(Type.intTypeName);
        Errors.prompt(getLine(), getChar(),
                new CompilingException(ExceptionType.SEMANTIC_ERROR,
                    "Illegal expression (All operands must be of int type)"));
        return Type.CreateSimpleType(Type.intTypeName);
    }

	public TranslationAG translate(SymbolTable symbolTable) {
		TranslationAG eag1 = exp1.translate(symbolTable);
		TranslationAG eag2 = exp2.translate(symbolTable);
		
		TranslationAG ret = new TranslationAG();
		ret.appendCode(eag1.getCode());
		ret.appendCode(eag2.getCode());
		ret.setAddress(HIRHelper.getAddress(HIRHelper.newVariable(Scope.TEMPORARY), Scope.TEMPORARY));
		ret.appendCode(HIRHelper.multiply(ret.getAddress(), eag1.getAddress(), eag2.getAddress()));
		return ret;
	}

}

class DivideExp extends BinaryExp {

    public DivideExp(Exp exp1, Exp exp2) {
        super(exp1, exp2);
    }

    public Type getType(SymbolTable symbolTable) {
        Type t1 = exp1.getType(symbolTable);
        Type t2 = exp2.getType(symbolTable);
        if (t1 != null && t2 != null && t1.getName() == Type.intTypeName && t2.getName() == Type.intTypeName) 
            return Type.CreateSimpleType(Type.intTypeName);
        Errors.prompt(getLine(), getChar(),
                new CompilingException(ExceptionType.SEMANTIC_ERROR,
                    "Illegal expression (All operands must be of int type)"));
        return Type.CreateSimpleType(Type.intTypeName);
    }

	public TranslationAG translate(SymbolTable symbolTable) {
		TranslationAG eag1 = exp1.translate(symbolTable);
		TranslationAG eag2 = exp2.translate(symbolTable);
		
		TranslationAG ret = new TranslationAG();
		ret.appendCode(eag1.getCode());
		ret.appendCode(eag2.getCode());
		ret.setAddress(HIRHelper.getAddress(HIRHelper.newVariable(Scope.TEMPORARY), Scope.TEMPORARY));
		ret.appendCode(HIRHelper.divide(ret.getAddress(), eag1.getAddress(), eag2.getAddress()));
		return ret;
	}

}

class ModuloExp extends BinaryExp {

    public ModuloExp(Exp exp1, Exp exp2) {
        super(exp1, exp2);
    }

    public Type getType(SymbolTable symbolTable) {
        Type t1 = exp1.getType(symbolTable);
        Type t2 = exp2.getType(symbolTable);
        if (t1 != null && t2 != null && t1.getName() == Type.intTypeName && t2.getName() == Type.intTypeName) 
            return Type.CreateSimpleType(Type.intTypeName);
        Errors.prompt(getLine(), getChar(),
                new CompilingException(ExceptionType.SEMANTIC_ERROR,
                    "Illegal expression (All operands must be of int type)"));

        return Type.CreateSimpleType(Type.intTypeName);
    }

	public TranslationAG translate(SymbolTable symbolTable) {
		TranslationAG eag1 = exp1.translate(symbolTable);
		TranslationAG eag2 = exp2.translate(symbolTable);
		
		TranslationAG ret = new TranslationAG();
		ret.appendCode(eag1.getCode());
		ret.appendCode(eag2.getCode());
		ret.setAddress(HIRHelper.getAddress(HIRHelper.newVariable(Scope.TEMPORARY), Scope.TEMPORARY));
		ret.appendCode(HIRHelper.modulo(ret.getAddress(), eag1.getAddress(), eag2.getAddress()));
		return ret;
	}

}

class AndExp extends BinaryExp {

    public AndExp(Exp exp1, Exp exp2) {
        super(exp1, exp2);
    }

    public Type getType(SymbolTable symbolTable) {
        Type t1 = exp1.getType(symbolTable);
        Type t2 = exp2.getType(symbolTable);
        if (t1 != null && t2 != null && t1.getName() == Type.boolTypeName && t2.getName() == Type.boolTypeName) 
            return Type.CreateSimpleType(Type.boolTypeName);
        Errors.prompt(getLine(), getChar(),
                new CompilingException(ExceptionType.SEMANTIC_ERROR,
                    "Illegal expression (All operands must be of bool type)"));
        return Type.CreateSimpleType(Type.boolTypeName);
    }

	public TranslationAG translate(SymbolTable symbolTable) {
		exp1.setTrue(HIRHelper.newLabel());
		exp1.setFalse(getFalse());
		exp2.setTrue(getTrue());
		exp2.setFalse(getFalse());

		TranslationAG eag1 = exp1.translate(symbolTable);
		TranslationAG eag2 = exp2.translate(symbolTable);
		
		TranslationAG ret = new TranslationAG();
		ret.appendCode(eag1.getCode());
		ret.appendCode(exp1.getTrue() + ":");
		ret.appendCode(eag2.getCode());
		return ret;
	}

}

class OrExp extends BinaryExp {

    public OrExp(Exp exp1, Exp exp2) {
        super(exp1, exp2);
    }

    public Type getType(SymbolTable symbolTable) {
        Type t1 = exp1.getType(symbolTable);
        Type t2 = exp2.getType(symbolTable);
        if (t1 != null && t2 != null && t1.getName() == Type.boolTypeName && t2.getName() == Type.boolTypeName) 
            return Type.CreateSimpleType(Type.boolTypeName);
        Errors.prompt(getLine(), getChar(),
                new CompilingException(ExceptionType.SEMANTIC_ERROR,
                    "Illegal expression (All operands must be of bool type)"));
        return Type.CreateSimpleType(Type.boolTypeName);
    }

	public TranslationAG translate(SymbolTable symbolTable) {
		exp1.setTrue(getTrue());
		exp1.setFalse(HIRHelper.newLabel());
		exp2.setTrue(getTrue());
		exp2.setFalse(getFalse());

		TranslationAG eag1 = exp1.translate(symbolTable);
		TranslationAG eag2 = exp2.translate(symbolTable);
		
		TranslationAG ret = new TranslationAG();
		ret.appendCode(eag1.getCode());
		ret.appendCode(exp1.getFalse() + ":");
		ret.appendCode(eag2.getCode());
		return ret;
	}

}

class EqualsExp extends BinaryExp {

    public EqualsExp(Exp exp1, Exp exp2) {
        super(exp1, exp2);
    }

    public Type getType(SymbolTable symbolTable) {
        Type t1 = exp1.getType(symbolTable);
        Type t2 = exp2.getType(symbolTable);
        if (t1 != null && t2 != null && t1.getName() == Type.intTypeName && t2.getName() == Type.intTypeName) 
                return Type.CreateSimpleType(Type.boolTypeName);
        Errors.prompt(getLine(), getChar(),
                new CompilingException(ExceptionType.SEMANTIC_ERROR,
                    "Illegal expression (All operands must be of int type)"));
        return Type.CreateSimpleType(Type.boolTypeName);
    }

	public TranslationAG translate(SymbolTable symbolTable) {
		TranslationAG eag1 = exp1.translate(symbolTable);
		TranslationAG eag2 = exp2.translate(symbolTable);
		
		TranslationAG ret = new TranslationAG();
		ret.appendCode(eag1.getCode());
		ret.appendCode(eag2.getCode());
		ret.appendCode(HIRHelper.jumpEqual(eag1.getAddress(), eag2.getAddress(), getTrue()));
		ret.appendCode(HIRHelper.jump(getFalse()));
		return ret;
	}

}

class NotEqualsExp extends BinaryExp {

    public NotEqualsExp(Exp exp1, Exp exp2) {
        super(exp1, exp2);
    }

    public Type getType(SymbolTable symbolTable) {
        Type t1 = exp1.getType(symbolTable);
        Type t2 = exp2.getType(symbolTable);
        if (t1 != null && t2 != null && t1.getName() == Type.intTypeName && t2.getName() == Type.intTypeName) 
                return Type.CreateSimpleType(Type.boolTypeName);
        Errors.prompt(getLine(), getChar(),
                new CompilingException(ExceptionType.SEMANTIC_ERROR,
                    "Illegal expression (All operands must be of int type)"));
        return Type.CreateSimpleType(Type.boolTypeName);
    }

	public TranslationAG translate(SymbolTable symbolTable) {
		TranslationAG eag1 = exp1.translate(symbolTable);
		TranslationAG eag2 = exp2.translate(symbolTable);
		
		TranslationAG ret = new TranslationAG();
		ret.appendCode(eag1.getCode());
		ret.appendCode(eag2.getCode());
		ret.appendCode(HIRHelper.jumpNotEqual(eag1.getAddress(), eag2.getAddress(), getTrue()));
		ret.appendCode(HIRHelper.jump(getFalse()));
		return ret;
	}

}

class LessExp extends BinaryExp {

    public LessExp(Exp exp1, Exp exp2) {
        super(exp1, exp2);
    }
    
    public Type getType(SymbolTable symbolTable) {
        Type t1 = exp1.getType(symbolTable);
        Type t2 = exp2.getType(symbolTable);
        if (t1 != null && t2 != null && t1.getName() == Type.intTypeName && t2.getName() == Type.intTypeName) 
            return Type.CreateSimpleType(Type.boolTypeName);
        Errors.prompt(getLine(), getChar(),
                new CompilingException(ExceptionType.SEMANTIC_ERROR,
                    "Illegal expression (All operands must be of int type)"));
        return Type.CreateSimpleType(Type.boolTypeName);
    }

	public TranslationAG translate(SymbolTable symbolTable) {
		TranslationAG eag1 = exp1.translate(symbolTable);
		TranslationAG eag2 = exp2.translate(symbolTable);
		
		TranslationAG ret = new TranslationAG();
		ret.appendCode(eag1.getCode());
		ret.appendCode(eag2.getCode());
		ret.appendCode(HIRHelper.jumpLess(eag1.getAddress(), eag2.getAddress(), getTrue()));
		ret.appendCode(HIRHelper.jump(getFalse()));
		return ret;
	}

}

class GreaterExp extends BinaryExp {

    public GreaterExp(Exp exp1, Exp exp2) {
        super(exp1, exp2);
    }

    public Type getType(SymbolTable symbolTable) {
        Type t1 = exp1.getType(symbolTable);
        Type t2 = exp2.getType(symbolTable);
        if (t1 != null && t2 != null && t1.getName() == Type.intTypeName && t2.getName() == Type.intTypeName) 
            return Type.CreateSimpleType(Type.boolTypeName);
        Errors.prompt(getLine(), getChar(),
                new CompilingException(ExceptionType.SEMANTIC_ERROR,
                    "Illegal expression (All operands must be of int type)"));
        return Type.CreateSimpleType(Type.boolTypeName);
    }

	public TranslationAG translate(SymbolTable symbolTable) {
		TranslationAG eag1 = exp1.translate(symbolTable);
		TranslationAG eag2 = exp2.translate(symbolTable);
		
		TranslationAG ret = new TranslationAG();
		ret.appendCode(eag1.getCode());
		ret.appendCode(eag2.getCode());
		ret.appendCode(HIRHelper.jumpLess(eag2.getAddress(), eag1.getAddress(), getTrue()));
		ret.appendCode(HIRHelper.jump(getFalse()));
		return ret;
	}

}

class LessEqExp extends BinaryExp {

    public LessEqExp(Exp exp1, Exp exp2) {
        super(exp1, exp2);
    }

    public Type getType(SymbolTable symbolTable) {
        Type t1 = exp1.getType(symbolTable);
        Type t2 = exp2.getType(symbolTable);
        if (t1 != null && t2 != null && t1.getName() == Type.intTypeName && t2.getName() == Type.intTypeName) 
            return Type.CreateSimpleType(Type.boolTypeName);
        Errors.prompt(getLine(), getChar(),
                new CompilingException(ExceptionType.SEMANTIC_ERROR,
                    "Illegal expression (All operands must be of int type)"));
        return Type.CreateSimpleType(Type.boolTypeName);
    }

	public TranslationAG translate(SymbolTable symbolTable) {
		TranslationAG eag1 = exp1.translate(symbolTable);
		TranslationAG eag2 = exp2.translate(symbolTable);
		
		TranslationAG ret = new TranslationAG();
		ret.appendCode(eag1.getCode());
		ret.appendCode(eag2.getCode());
		ret.appendCode(HIRHelper.jumpLessOrEqual(eag1.getAddress(), eag2.getAddress(), getTrue()));
		ret.appendCode(HIRHelper.jump(getFalse()));
		return ret;
	}

}

class GreaterEqExp extends BinaryExp {

    public GreaterEqExp(Exp exp1, Exp exp2) {
        super(exp1, exp2);
    }

    public Type getType(SymbolTable symbolTable) {
        Type t1 = exp1.getType(symbolTable);
        Type t2 = exp2.getType(symbolTable);
        if (t1 != null && t2 != null && t1.getName() == Type.intTypeName && t2.getName() == Type.intTypeName) 
                return Type.CreateSimpleType(Type.boolTypeName);
        Errors.prompt(getLine(), getChar(),
                new CompilingException(ExceptionType.SEMANTIC_ERROR,
                    "Illegal expression (All operands must be of int type)"));
        return Type.CreateSimpleType(Type.boolTypeName);
    }

	public TranslationAG translate(SymbolTable symbolTable) {
		TranslationAG eag1 = exp1.translate(symbolTable);
		TranslationAG eag2 = exp2.translate(symbolTable);
		
		TranslationAG ret = new TranslationAG();
		ret.appendCode(eag1.getCode());
		ret.appendCode(eag2.getCode());
		ret.appendCode(HIRHelper.jumpLessOrEqual(eag2.getAddress(), eag1.getAddress(), getTrue()));
		ret.appendCode(HIRHelper.jump(getFalse()));
		return ret;
	}

}

// **********************************************************************
// Symbol Table
// **********************************************************************
class SymbolTable {
    private SymbolTable root;
    private SymbolTable parent;
    private TreeMap<String, List<ObjectType>> table;

    public SymbolTable() {
        parent = null;
        root = this;
        table = new TreeMap<String, List<ObjectType>>();
    }

    public int size() {
        return table.size();
    }

    public void setParent(SymbolTable parent) {
        this.parent = parent;
        this.root = parent.root;
    }

    public SymbolTable getParent() {
        return parent;
    }

    public void addEntry(String name, ObjectType type) throws CompilingException {
        if (type instanceof FunctionType) {
            boolean legal = true;
            List<ObjectType> list;
            if (table.containsKey(name)) {
                list = table.get(name);
                for (ObjectType item : list) 
                    if (item instanceof VariableType || // This entry is variable 
                        !(!((FunctionType)type).getParams().equals(((FunctionType)item).getParams()) || // This entry is different from the existing entries
                          (!((FunctionType)type).isPreDecl() && ((FunctionType)item).isPreDecl()))) // This entry is not a pre-decl function but the existing one is
                        throw new CompilingException(
                                ExceptionType.SEMANTIC_ERROR,
                                "Function " + name + " has already been declared");    
            } else
                list = new LinkedList<ObjectType>();
            list.add(type);
            table.put(name, list);
        } else {
            if (table.containsKey(name)) // If there exists any entry with the same name, throw an error
                throw new CompilingException(
                        ExceptionType.SEMANTIC_ERROR,
                        "Variable " + name + " has already been declared");
            LinkedList<ObjectType> list = new LinkedList<ObjectType>();
            list.add(type);
            table.put(name, list);    
        }
    }

    public VariableType getVariableType(String name) throws CompilingException {
        List<ObjectType> list = table.get(name);
        if (list != null)
            for (ObjectType item : list)
                if (item instanceof VariableType)
                    return (VariableType)item;
        throw new CompilingException(
                ExceptionType.SEMANTIC_ERROR,
                "Variable " + name + " has not been declared");
    }

    public FunctionType getFunctionType(String name, List<Type> params) throws CompilingException {
        List<ObjectType> list = table.get(name);
        if (list != null) 
            for (ObjectType item : list) 
                if (item instanceof FunctionType && ((FunctionType)item).getParams().equals(params))
                    return (FunctionType)item;
        throw new CompilingException(
                ExceptionType.SEMANTIC_ERROR,
                "Function " + name + " has not been declared");
    }

    public SymbolTable getGlobalScope() {
        return root;
    }
}

class ObjectType {
    private Type type;

    public ObjectType(Type type) {
        this.type = type;
    }

    public Type getType() {
        return type;
    }
}

class VariableType extends ObjectType {
    public VariableType(Type type) {
        super(type);
    }

	public VariableType(Type type, int addr, Scope scope) {
		super(type);
		this.addr = addr;
		this.scope = scope;
	}

	public void setAddress(int addr) {
		this.addr = addr;
	}

	public int getAddress() {
		return addr;
	}

	public Scope getScope() {
		return scope;
	}

	private int addr;
	private Scope scope;
}

class FunctionType extends ObjectType {
    private LinkedList<Type> params;
    private boolean preDecl;

    public FunctionType(Type type, LinkedList<Type> params, boolean preDecl) {
        super(type);
        this.params = params;
        this.preDecl = preDecl;
    }

    public boolean isPreDecl() {
        return preDecl;
    }

    public LinkedList<Type> getParams() {
        return params;
    }
}


