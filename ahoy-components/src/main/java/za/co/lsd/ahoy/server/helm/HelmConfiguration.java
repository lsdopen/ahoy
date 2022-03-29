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

package za.co.lsd.ahoy.server.helm;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;
import za.co.lsd.ahoy.server.helm.values.ApplicationConfigFileValues;
import za.co.lsd.ahoy.server.helm.values.ApplicationValues;
import za.co.lsd.ahoy.server.helm.values.Values;

@Configuration
public class HelmConfiguration {

	@Bean
	@Scope("prototype")
	public Yaml yaml() {
		DumperOptions options = new DumperOptions();
		options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
		options.setPrettyFlow(true);

		Representer representer = new Representer() {
			@Override
			protected NodeTuple representJavaBeanProperty(Object javaBean, Property property, Object propertyValue, Tag customTag) {
				// if value of property is null, ignore it.
				return propertyValue == null ?
					null :
					super.representJavaBeanProperty(javaBean, property, propertyValue, customTag);
			}
		};
		representer.addClassTag(Chart.class, Tag.MAP);
		representer.addClassTag(Values.class, Tag.MAP);
		representer.addClassTag(ApplicationValues.class, Tag.MAP);
		representer.addClassTag(ApplicationConfigFileValues.class, Tag.MAP);

		return new Yaml(representer, options);
	}
}
