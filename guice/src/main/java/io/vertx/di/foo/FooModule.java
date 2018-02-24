package io.vertx.di.foo;

import com.google.inject.AbstractModule;

public class FooModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(FooRepository.class).to(InMemoryFooRepository.class);
    }

}
