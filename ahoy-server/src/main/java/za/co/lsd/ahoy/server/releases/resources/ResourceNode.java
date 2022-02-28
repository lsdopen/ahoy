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

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Data
public class ResourceNode {
	private final String name;
	private final String kind;
	private final String namespace;
	private final String uid;
	private final String parentUid;
	private final List<ResourceNode> children = new ArrayList<>();

	@JsonIgnore
	@EqualsAndHashCode.Exclude
	@ToString.Exclude
	private ResourceNode parent;

	public ResourceNode(String name) {
		this.name = name;
		this.kind = null;
		this.namespace = null;
		this.uid = null;
		this.parentUid = null;
	}

	public ResourceNode(String name, String kind, String namespace, String uid, String parentUid) {
		this.name = Objects.requireNonNull(name);
		this.kind = kind;
		this.namespace = namespace;
		this.uid = uid;
		this.parentUid = parentUid;
	}

	public boolean hasParentUid() {
		return this.parentUid != null;
	}

	public void addChild(ResourceNode resourceNode) {
		children.add(Objects.requireNonNull(resourceNode));
		resourceNode.setParent(this);
	}

	public boolean isRoot() {
		return (this.parent == null);
	}

	public boolean isLeaf() {
		return this.children.isEmpty();
	}

	private void setParent(ResourceNode parent) {
		this.parent = Objects.requireNonNull(parent);
	}
}
