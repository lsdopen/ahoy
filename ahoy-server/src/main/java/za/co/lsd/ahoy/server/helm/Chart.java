package za.co.lsd.ahoy.server.helm;

import lombok.Builder;
import lombok.ToString;

@Builder
@ToString
public class Chart {
	public final String apiVersion = "v2";
	public final String name;
	public final String version;
}
