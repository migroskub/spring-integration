/*
 * Copyright 2014-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.config;

import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.MethodMetadata;
import org.springframework.integration.channel.interceptor.GlobalChannelInterceptorWrapper;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.util.CollectionUtils;

/**
 * The {@link IntegrationConfigurationInitializer} to populate {@link GlobalChannelInterceptorWrapper}
 * for {@link org.springframework.messaging.support.ChannelInterceptor}s marked with
 * {@link GlobalChannelInterceptor} annotation.
 * <p>
 * {@link org.springframework.context.annotation.Bean} methods are also processed.
 *
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 4.0
 */
public class GlobalChannelInterceptorInitializer implements IntegrationConfigurationInitializer {

	private ConfigurableListableBeanFactory beanFactory;

	@Override
	public void initialize(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
		BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;
		for (String beanName : registry.getBeanDefinitionNames()) {
			BeanDefinition beanDefinition = registry.getBeanDefinition(beanName);
			if (beanDefinition instanceof AnnotatedBeanDefinition) {
				AnnotationMetadata metadata = ((AnnotatedBeanDefinition) beanDefinition).getMetadata();
				Map<String, Object> annotationAttributes =
						metadata.getAnnotationAttributes(GlobalChannelInterceptor.class.getName());
				if (CollectionUtils.isEmpty(annotationAttributes)
						&& beanDefinition.getSource() instanceof MethodMetadata) {
					MethodMetadata beanMethod = (MethodMetadata) beanDefinition.getSource();
					annotationAttributes =
							beanMethod.getAnnotationAttributes(GlobalChannelInterceptor.class.getName()); // NOSONAR not null
				}

				if (!CollectionUtils.isEmpty(annotationAttributes)) {
					BeanDefinitionBuilder builder =
							BeanDefinitionBuilder.genericBeanDefinition(GlobalChannelInterceptorWrapper.class,
											() -> createGlobalChannelInterceptorWrapper(beanName))
									.addConstructorArgReference(beanName)
									.addPropertyValue("patterns", annotationAttributes.get("patterns"))
									.addPropertyValue("order", annotationAttributes.get("order"));

					BeanDefinitionReaderUtils.registerWithGeneratedName(builder.getBeanDefinition(), registry);
				}
			}
		}
	}

	private GlobalChannelInterceptorWrapper createGlobalChannelInterceptorWrapper(String interceptorBeanName) {
		ChannelInterceptor interceptor = this.beanFactory.getBean(interceptorBeanName, ChannelInterceptor.class);
		return new GlobalChannelInterceptorWrapper(interceptor);
	}

}
