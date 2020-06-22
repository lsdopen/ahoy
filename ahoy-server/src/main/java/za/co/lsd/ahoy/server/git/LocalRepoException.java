package za.co.lsd.ahoy.server.git;

public class LocalRepoException extends RuntimeException {
	public LocalRepoException(String message) {
		super(message);
	}

	public LocalRepoException(String message, Throwable cause) {
		super(message, cause);
	}
}
