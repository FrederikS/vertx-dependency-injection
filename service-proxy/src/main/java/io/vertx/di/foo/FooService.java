package io.vertx.di.foo;

import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;

@ProxyGen
@VertxGen
public interface FooService {
    void createFoo(String bar, Handler<AsyncResult<JsonObject>> resultHandler);
    void findFoo(String id, Handler<AsyncResult<JsonObject>> resultHandler);
}
