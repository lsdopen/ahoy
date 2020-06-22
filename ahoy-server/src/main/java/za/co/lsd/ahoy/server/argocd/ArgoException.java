package za.co.lsd.ahoy.server.argocd;

public class ArgoException extends RuntimeException {

	public ArgoException(String message) {
		super(message);
	}

	public ArgoException(String message, Throwable cause) {
		super(message, cause);
	}
}
