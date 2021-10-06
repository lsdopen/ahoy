/*
 * Copyright  2021 LSD Information Technology (Pty) Ltd
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

package za.co.lsd.ahoy.server.environments;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import za.co.lsd.ahoy.server.AhoyServerApplication;

import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = AhoyServerApplication.class)
@ActiveProfiles(profiles = "test")
class EnvironmentServiceTest {
	@MockBean
	private EnvironmentRepository environmentRepository;
	@Autowired
	private EnvironmentService environmentService;

	@Test
	void updateOrderIndex() {
		// when
		environmentService.updateOrderIndex(1L, 500.0);

		// then
		verify(environmentRepository, times(1)).updateOrderIndex(eq(1L), eq(500.0));
	}
}
