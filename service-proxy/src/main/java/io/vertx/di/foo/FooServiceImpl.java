package io.vertx.di.foo;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.MaybeHelper;
import io.vertx.reactivex.SingleHelper;

import java.util.UUID;

class FooServiceImpl implements FooService {

    private final FooRepository fooRepository;

    FooServiceImpl(FooRepository fooRepository) {
        this.fooRepository = fooRepository;
    }

    @Override
    public void createFoo(String bar, Handler<AsyncResult<JsonObject>> resultHandler) {
        fooRepository
                .save(new Foo(UUID.randomUUID().toString(), bar))
                .map(JsonObject::mapFrom)
                .subscribe(SingleHelper.toObserver(resultHandler));
    }

    @Override
    public void findFoo(String id, Handler<AsyncResult<JsonObject>> resultHandler) {
        fooRepository
                .findById(id)
                .map(JsonObject::mapFrom)
                .subscribe(MaybeHelper.toObserver(resultHandler));
    }

}
