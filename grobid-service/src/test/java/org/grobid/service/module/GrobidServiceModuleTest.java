/*
 * Copyright 2008-2026 GROBID contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.grobid.service.module;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.dropwizard.configuration.ConfigurationFactory;
import io.dropwizard.configuration.DefaultConfigurationFactoryFactory;
import io.dropwizard.configuration.FileConfigurationSourceProvider;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.jackson.Jackson;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.hibernate.validator.HibernateValidator;

import org.grobid.service.GrobidServiceConfiguration;
import org.grobid.service.modules.GrobidServiceModule;

public class GrobidServiceModuleTest extends GrobidServiceModule {

    public static final String TEST_CONFIG_FILE = "src/test/resources/setup/config/test-config.yaml";

    public GrobidServiceModuleTest() {
        super();
    }

    @Override
    public void configure() {
        super.configure();
    }

    @Provides
    @Singleton
    @Override
    public GrobidServiceConfiguration configuration() {
        ObjectMapper objectMapper = Jackson.newObjectMapper();

        ValidatorFactory validatorFactory = Validation
                .byProvider(HibernateValidator.class)
                .configure()
                //                .addValidatedValueHandler(new OptionalValidatedValueUnwrapper())
                .buildValidatorFactory();

        final ConfigurationFactory<GrobidServiceConfiguration> configurationFactory = new DefaultConfigurationFactoryFactory<GrobidServiceConfiguration>()
                .create(
                        GrobidServiceConfiguration.class,
                        validatorFactory.getValidator(),
                        objectMapper,
                        "dw");

        try {
            return configurationFactory.build(new FileConfigurationSourceProvider(), TEST_CONFIG_FILE);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    @Provides
    protected Environment environment() {
        return new Environment("test-grobid-service-env", new ObjectMapper(), null, new MetricRegistry(),
                this.getClass().getClassLoader(), null, configuration());
    }

}
