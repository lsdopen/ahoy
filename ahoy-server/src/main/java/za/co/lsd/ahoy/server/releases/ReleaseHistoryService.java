/*
 * Copyright  2020 LSD Information Technology (Pty) Ltd
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

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
