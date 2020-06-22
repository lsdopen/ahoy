package za.co.lsd.ahoy.server.releases;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ReleaseHistoryService {
	private final ReleaseHistoryRepository releaseHistoryRepository;

	public ReleaseHistoryService(ReleaseHistoryRepository releaseHistoryRepository) {
		this.releaseHistoryRepository = releaseHistoryRepository;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void save(ReleaseHistory history) {
		releaseHistoryRepository.save(history);
	}
}
