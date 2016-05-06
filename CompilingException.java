import java.lang.Exception;

class CompilingException extends Exception {
	
	private ExceptionType type;
	
	public CompilingException(ExceptionType type, String msg) {
		super(msg);
		this.type = type;
	}

	public ExceptionType getType() {
		return type;
	}
}


