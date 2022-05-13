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

package za.co.lsd.ahoy.server.helm.values;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import za.co.lsd.ahoy.server.applications.ApplicationResources;

@Data
@AllArgsConstructor
public class ResourcesValues {
	public final ResourceValue limits;
	public final ResourceValue requests;

	public ResourcesValues() {
		limits = new ResourceValue();
		requests = new ResourceValue();
	}

	public void spec(ApplicationResources resources) {
		if (resources.getLimitCpu() != null)
			limits.setCpu(resources.getLimitCpu() + "m");
		if (resources.getLimitMemory() != null)
			limits.setMemory(resources.getLimitMemory() + resources.getLimitMemoryUnit().name());

		if (resources.getRequestCpu() != null)
			requests.setCpu(resources.getRequestCpu() + "m");
		if (resources.getRequestMemory() != null)
			requests.setMemory(resources.getRequestMemory() + resources.getRequestMemoryUnit().name());
	}

	public boolean hasValues() {
		return limits.getCpu() != null || limits.getMemory() != null ||
			requests.getCpu() != null || requests.getMemory() != null;
	}

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class ResourceValue {
		public String cpu;
		public String memory;
	}
}

