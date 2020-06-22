package za.co.lsd.ahoy.server.environments;

public class EnvironmentException extends RuntimeException {

	public EnvironmentException(String message) {
		super(message);
	}

	public EnvironmentException(String message, Throwable cause) {
		super(message, cause);
	}
}
