/*
 * Copyright  2022 LSD Information Technology (Pty) Ltd
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

package za.co.lsd.ahoy.server.releases.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import za.co.lsd.ahoy.server.AhoyServerApplication;
import za.co.lsd.ahoy.server.argocd.model.ResourceTree;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = AhoyServerApplication.class)
@ActiveProfiles(profiles = {"test", "keycloak"})
class ResourceTreeConverterTest {
	@Autowired
	private ObjectMapper objectMapper;
	@Autowired
	private ResourceTreeConverter resourceTreeConverter;

	@Test
	void convert() throws IOException {
		// given
		ResourceTree resourceTree;
		try (InputStream resourceTreeJson = this.getClass().getResourceAsStream("/za/co/lsd/ahoy/server/releases/resources/resource-tree.json")) {
			resourceTree = objectMapper.readValue(resourceTreeJson, ResourceTree.class);
		}

		// when
		ResourceNode rootResourceNode = resourceTreeConverter.convert(resourceTree);

		// then
		assertNotNull(rootResourceNode, "We should have a root node");
		try (InputStream expectedNodeTreeJson = this.getClass().getResourceAsStream("/za/co/lsd/ahoy/server/releases/resources/expected-resource-node-tree.json")) {
			ResourceNode expectedRootResourceNode = objectMapper.readValue(expectedNodeTreeJson, ResourceNode.class);
			assertEquals(expectedRootResourceNode, rootResourceNode, "Convert failed; did not get expected tree");
		}
	}

	@Test
	void convertEmpty() throws IOException {
		// given
		ResourceTree resourceTree;
		try (InputStream resourceTreeJson = this.getClass().getResourceAsStream("/za/co/lsd/ahoy/server/releases/resources/resource-tree-empty.json")) {
			resourceTree = objectMapper.readValue(resourceTreeJson, ResourceTree.class);
		}

		// when
		ResourceNode rootResourceNode = resourceTreeConverter.convert(resourceTree);

		// then
		assertNotNull(rootResourceNode, "We should have a root node");
		try (InputStream expectedNodeTreeJson = this.getClass().getResourceAsStream("/za/co/lsd/ahoy/server/releases/resources/expected-resource-node-tree-empty.json")) {
			ResourceNode expectedRootResourceNode = objectMapper.readValue(expectedNodeTreeJson, ResourceNode.class);
			assertEquals(expectedRootResourceNode, rootResourceNode, "Convert failed; did not get expected tree");
		}
	}
}
