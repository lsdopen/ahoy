package za.co.lsd.ahoy.server.argocd;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.Type;
import org.springframework.util.StringUtils;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArgoSettings {
	@Id
	private Long id;
	private String argoServer;
	@Lob
	@Type(type = "org.hibernate.type.TextType")
	@ToString.Exclude
	private String argoToken;

	public boolean configured() {
		return !StringUtils.isEmpty(argoServer);
	}
}
