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

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import za.co.lsd.ahoy.server.argocd.model.ResourceTree;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Converts an ArgoCD {@link ResourceTree} into a tree structure of {@link ResourceNode}.
 */
@Component
@Slf4j
public class ResourceTreeConverter {
	public static final String ROOT_NODE_NAME = "root";

	/**
	 * Simple routine that indexes all nodes in the {@link ResourceTree} and links them to their parent {@link ResourceNode}'s.
	 *
	 * @param resourceTree the ArgoCd resource tree
	 * @return a single root node with the tree structure of {@link ResourceNode}'s
	 */
	public ResourceNode convert(ResourceTree resourceTree) {
		final Map<String, ResourceNode> index = new HashMap<>();
		final ResourceNode rootResourceNode = new ResourceNode(ROOT_NODE_NAME);

		resourceTree.getNodes().forEach((treeNode) -> index.put(treeNode.getUid(), nodeFromResourceTreeNode(treeNode)));
		index.forEach((uid, resourceNode) -> {
			ResourceNode parent = resourceNode.hasParentUid() ? index.get(resourceNode.getParentUid()) : rootResourceNode;
			parent.addChild(resourceNode);
		});

		return rootResourceNode;
	}

	private ResourceNode nodeFromResourceTreeNode(ResourceTree.Node treeNode) {
		List<ResourceTree.ParentRef> parentRefs = treeNode.getParentRefs();
		String parentUid = null;
		if (parentRefs != null && !parentRefs.isEmpty()) {
			if (parentRefs.size() > 1)
				log.warn("More the one parent ref..");

			parentUid = parentRefs.get(0).getUid();
		}
		return new ResourceNode(treeNode.getName(), treeNode.getKind(), treeNode.getNamespace(), treeNode.getUid(), treeNode.getVersion(), parentUid);
	}
}
