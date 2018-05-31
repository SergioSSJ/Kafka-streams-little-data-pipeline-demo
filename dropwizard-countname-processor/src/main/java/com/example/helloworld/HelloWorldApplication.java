package com.example.helloworld;


import com.example.helloworld.kafkaConsumers.JoinedConsumer;
import com.example.helloworld.kafkaProcessors.SimpleStreamsJoinDemo;
import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.db.DataSourceFactory;

import io.dropwizard.migrations.MigrationsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.views.ViewBundle;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;

import java.io.IOException;
import java.util.Map;

public class HelloWorldApplication extends Application<HelloWorldConfiguration> {
    public static void main(String[] args) throws Exception {
        new HelloWorldApplication().run(args);
    }



    @Override
    public String getName() {
        return "hello-world";
    }

    @Override
    public void initialize(Bootstrap<HelloWorldConfiguration> bootstrap) {
        // Enable variable substitution with environment variables
        bootstrap.setConfigurationSourceProvider(
                new SubstitutingSourceProvider(
                        bootstrap.getConfigurationSourceProvider(),
                        new EnvironmentVariableSubstitutor(false)
                )
        );
        //target/classes/resources/static


        bootstrap.addBundle(new AssetsBundle("/assets/","/","index.html","home"));
        bootstrap.addBundle(new AssetsBundle("/assets/","/login","index.html","login"));
        bootstrap.addBundle(new MigrationsBundle<HelloWorldConfiguration>() {
            @Override
            public DataSourceFactory getDataSourceFactory(HelloWorldConfiguration configuration) {
                return configuration.getDataSourceFactory();
            }
        });

        bootstrap.addBundle(new ViewBundle<HelloWorldConfiguration>() {
            @Override
            public Map<String, Map<String, String>> getViewConfiguration(HelloWorldConfiguration configuration) {
                return configuration.getViewRendererConfiguration();
            }
        });
    }

    @Override
    public void run(HelloWorldConfiguration configuration, Environment environment) throws IOException {
        environment.jersey().register(RolesAllowedDynamicFeature.class);
        //environment.lifecycle().manage(new SimpleConsumer());
        //environment.lifecycle().manage(new CountNameProcessor());
        environment.lifecycle().manage(new SimpleStreamsJoinDemo());
        environment.lifecycle().manage(new JoinedConsumer());
    }
}
