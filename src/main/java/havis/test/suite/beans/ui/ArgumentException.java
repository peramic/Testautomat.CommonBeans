package havis.test.suite.beans.ui;

public class ArgumentException extends Exception {
	private static final long serialVersionUID = 7504175433949158154L;

	public ArgumentException(String message) {
		super(message);
	}

	public ArgumentException(String message, Throwable cause) {
		super(message, cause);
	}

}
