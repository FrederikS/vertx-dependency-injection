package io.vertx.di.foo;

import io.reactivex.Single;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.eventbus.MessageConsumer;

public class FooVerticle extends AbstractVerticle {

    public static final String FOO_SERVICE_ADDRESS = "foo-service";

    private FooRepository fooRepository;
    private MessageConsumer<JsonObject> consumer;

    @Override
    public void init(Vertx vertx, Context context) {
        super.init(vertx, context);
        fooRepository = new InMemoryFooRepository();
    }

    @Override
    public void start(Future<Void> startFuture) {
        consumer = vertx.eventBus().consumer(FOO_SERVICE_ADDRESS, message -> {
            switch (message.headers().get("action")) {
                case "save":
                    fooRepository
                            .save(message.body().mapTo(Foo.class))
                            .map(JsonObject::mapFrom)
                            .subscribe(message::reply, e -> message.fail(500, e.getMessage()));
                    break;
                case "find":
                    fooRepository
                            .findById(message.body().getString("id"))
                            .switchIfEmpty(Single.error(new RuntimeException("Foo not found.")))
                            .map(JsonObject::mapFrom)
                            .subscribe(message::reply, e -> message.fail(404, e.getMessage()));
                    break;
                default:
                    message.fail(400, "Unknown action.");
            }
        });

        consumer.completionHandler(startFuture);
    }

    @Override
    public void stop(Future<Void> stopFuture) {
        consumer.unregister(stopFuture);
    }
}
