/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.adobe.acs.commons.splunk;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import com.splunk.logging.HttpEventCollectorLogbackAppender;
import com.splunk.logging.util.StandardErrorCallback;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import java.util.Dictionary;
import java.util.Hashtable;

@Component(configurationPolicy = ConfigurationPolicy.REQUIRE)
@Designate(ocd = SplunkHecAppenderActivator.Configuration.class)
public class SplunkHecAppenderActivator {

    @ObjectClassDefinition(name = "Splunk HEC Appender Configuration")
    @interface Configuration {
        @AttributeDefinition(name = "loggers", description = "The list of logger names to which the Appender must be attached.", type = AttributeType.STRING)
        String[] loggers() default {};

        @AttributeDefinition(name = "Message Pattern", description = "Message Pattern for formatting the log messages", type = AttributeType.STRING)
        String log_pattern();

        @AttributeDefinition(name = "url", description = "Indexing url, e.g. https://http-inputs-client-stack.splunkcloud.com:", type = AttributeType.STRING)
        String url();

        @AttributeDefinition(name = "token", description = "HEC token", type = AttributeType.PASSWORD)
        String token();

        @AttributeDefinition(name = "index", description = "Destination index", type = AttributeType.STRING)
        String index();

        @AttributeDefinition(name = "source", type = AttributeType.STRING)
        String source();

        @AttributeDefinition(name = "sourcetype", type = AttributeType.STRING)
        String sourcetype();

        @AttributeDefinition(name = "batch_size_count", description = "Max number of events in a batch", type = AttributeType.STRING)
        String batch_size_count();

        @AttributeDefinition(name = "batch_interval", description = "Batching delay", type = AttributeType.STRING)
        String batch_interval();

        @AttributeDefinition(name = "debug", description = "Attach an error listener that print errors to stdout", type = AttributeType.BOOLEAN)
        boolean debug() default true;
    }


    private ServiceRegistration<Appender> registration;

    @Activate
    public void activate(BundleContext context, Configuration configuration) {
        Dictionary<String, Object> props = new Hashtable<>();

        props.put("loggers", configuration.loggers());

        HttpEventCollectorLogbackAppender<ILoggingEvent> appender = new HttpEventCollectorLogbackAppender<>();
        appender.setUrl(configuration.url());
        appender.setToken(configuration.token());
        appender.setIndex(configuration.index());
        appender.setSource(configuration.source());
        appender.setSourcetype(configuration.sourcetype());
        appender.setDisableCertificateValidation("true");
        appender.setbatch_interval(configuration.batch_interval());
        appender.setbatch_size_count(configuration.batch_size_count());

        if(configuration.debug()) {
            appender.setErrorCallback(StandardErrorCallback.class.getName());
        }
        LoggerContext loggerContext = new LoggerContext();
        PatternLayout layout = new PatternLayout();
        layout.setPattern(configuration.log_pattern());
        layout.setContext(loggerContext);
        layout.start();
        appender.setLayout(layout);

        // To register an OSGi Appender we need to register a service that implements the ch.qos.logback.core.Appender interface,
        // see https://sling.apache.org/documentation/development/logging.html#appenders-as-osgi-services-1
        registration = context.registerService(Appender.class, appender, props);

    }

    @Deactivate
    public void deactivate() {
        registration.unregister();
    }

}
