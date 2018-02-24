package io.vertx.di.foo;

import io.reactivex.Maybe;
import io.reactivex.Single;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.UUID;

@Singleton
public class FooService {

    private final FooRepository fooRepository;

    @Inject
    FooService(FooRepository fooRepository) {
        this.fooRepository = fooRepository;
    }

    public Single<Foo> createFoo(String bar) {
        return fooRepository.save(new Foo(UUID.randomUUID().toString(), bar));
    }

    public Maybe<Foo> findFoo(String id) {
        return fooRepository.findById(id);
    }

}
