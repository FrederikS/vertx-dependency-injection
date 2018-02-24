package io.vertx.di.foo;

import io.reactivex.Maybe;
import io.reactivex.Single;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

class InMemoryFooRepository implements FooRepository {

    private final Map<String, Foo> store = new HashMap<>();

    @Override
    public Single<Foo> save(Foo foo) {
        store.put(foo.id, foo);
        return Single.just(foo);
    }

    @Override
    public Maybe<Foo> findById(String id) {
        return Single
                .just(store.get(id))
                .filter(Objects::nonNull);
    }

}
