package io.vertx.di.foo;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;

import java.util.HashMap;
import java.util.Map;

class InMemoryFooRepository implements FooRepository {

    private final Map<String, Foo> store = new HashMap<>();

    @Override
    public void save(JsonObject fooData, Handler<AsyncResult<JsonObject>> resultHandler) {
        Foo foo = fooData.mapTo(Foo.class);
        store.put(foo.id, foo);
        resultHandler.handle(Future.succeededFuture(JsonObject.mapFrom(foo)));
    }

    @Override
    public void findById(String id, Handler<AsyncResult<JsonObject>> resultHandler) {
        Future<JsonObject> future = store.containsKey(id)
                ? Future.succeededFuture(JsonObject.mapFrom(store.get(id)))
                : Future.succeededFuture();
        resultHandler.handle(future);
    }

}
