package io.vertx.di.foo;

import io.reactivex.Maybe;
import io.reactivex.Single;

import java.util.UUID;

class FooService {

    private final FooRepository fooRepository;

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
