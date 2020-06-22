package za.co.lsd.ahoy.server.clustermanager;

public class ClusterManagerException extends RuntimeException {

	public ClusterManagerException(String message) {
		super(message);
	}

	public ClusterManagerException(String message, Throwable cause) {
		super(message, cause);
	}
}
