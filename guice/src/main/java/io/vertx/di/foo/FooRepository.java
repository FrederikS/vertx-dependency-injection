package io.vertx.di.foo;

import io.reactivex.Maybe;
import io.reactivex.Single;

public interface FooRepository {
    Single<Foo> save(Foo foo);
    Maybe<Foo> findById(String id);
}
