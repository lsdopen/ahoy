package za.co.lsd.ahoy.server.argocd.model;

import lombok.Data;

import java.util.List;

@Data
public class ArgoRepositories {
	private List<ArgoRepository> items;
}
