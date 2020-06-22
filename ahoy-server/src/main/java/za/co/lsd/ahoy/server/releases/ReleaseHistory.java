package za.co.lsd.ahoy.server.releases;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;
import za.co.lsd.ahoy.server.environmentrelease.EnvironmentRelease;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReleaseHistory {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	private EnvironmentRelease environmentRelease;

	@ManyToOne
	private ReleaseVersion releaseVersion;

	@NotNull
	@Enumerated(EnumType.STRING)
	private ReleaseHistoryAction action;
	@NotNull
	@Enumerated(EnumType.STRING)
	private ReleaseHistoryStatus status;
	@NotNull
	private LocalDateTime time;
	@Lob
	@Type(type = "org.hibernate.type.TextType")
	private String description;

	@Override
	public String toString() {
		return "ReleaseHistory{" +
			"id=" + id +
			", environmentRelease=" + environmentRelease +
			", releaseVersion=" + releaseVersion +
			", action=" + action +
			", status=" + status +
			", time=" + time +
			", description='" + description + '\'' +
			'}';
	}
}
