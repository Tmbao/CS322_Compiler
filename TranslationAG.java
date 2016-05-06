// **********************************************************************
// TranslationAG
// **********************************************************************
class TranslationAG {

	public TranslationAG() {
		trueLabel = "";
		falseLabel = "";
		nextLabel = "";
		code = new StringBuilder();
		addr = "";
	}

	public String getTrueLabel() {
		return trueLabel;
	}

	public void setTrueLabel(String trueLabel) {
		this.trueLabel = trueLabel;
	}

	public String getFalseLabel() {
		return falseLabel;
	}

	public void setFalseLabel(String falseLabel) {
		this.falseLabel = falseLabel;
	}

	public String getNextLabel() {
		return nextLabel;
	}

	public void setNextLabel(String nextLabel) {
		this.nextLabel = nextLabel;
	}

	public StringBuilder getCode() {
		return code;
	}

	public void setCode(StringBuilder code) {
		this.code = code;
	}

	public boolean appendCode(StringBuilder code) {
		if (code.length() > 0) {
			this.code.append(code);
			return true;
		}
		return false;
	}

	public boolean appendCode(String code) {
		if (code.length() > 0) {
			this.code.append(code);
			this.code.append("\n");
			return true;
		}
		return false;
	}

	public String getAddress() {
		return addr;
	}

	public void setAddress(String addr) {
		this.addr = addr;
	}	

	private String trueLabel;
	private String falseLabel;
	private String nextLabel;
	private StringBuilder code;
	private String addr;
}
