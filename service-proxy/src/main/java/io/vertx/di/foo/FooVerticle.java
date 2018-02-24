package io.vertx.di.foo;

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.serviceproxy.ServiceBinder;

public class FooVerticle extends AbstractVerticle {

    public static final String FOO_SERVICE_ADDRESS = "foo-service";

    private FooRepository fooRepository;
    private MessageConsumer<JsonObject> binder;

    @Override
    public void init(Vertx vertx, Context context) {
        super.init(vertx, context);
        fooRepository = new InMemoryFooRepository();
    }

    @Override
    public void start(Future<Void> startFuture) {
        binder = new ServiceBinder(vertx.getDelegate())
                .setAddress(FOO_SERVICE_ADDRESS)
                .register(FooRepository.class, fooRepository);

        binder.completionHandler(startFuture);
    }

    @Override
    public void stop(Future<Void> stopFuture) {
        binder.unregister(stopFuture);
    }
}
