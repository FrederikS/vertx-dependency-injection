package io.vertx.di;

import io.reactivex.Single;
import io.vertx.core.Future;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.eventbus.Message;

import java.util.UUID;

import static io.vertx.di.foo.FooVerticle.FOO_SERVICE_ADDRESS;

public class MainVerticle extends AbstractVerticle {

    @Override
    public void start(Future<Void> startFuture) {
        JsonObject fooMessage = new JsonObject()
                .put("id", UUID.randomUUID().toString())
                .put("bar", "foobar");

        vertx.eventBus().<JsonObject>rxSend(FOO_SERVICE_ADDRESS, fooMessage, action("save"))
                .map(Message::body)
                .doOnSuccess(foo -> System.out.println("Saved: " + foo))
                .map(foo -> new JsonObject().put("id", foo.getString("id")))
                .flatMap(idMessage -> vertx.eventBus().<JsonObject>rxSend(FOO_SERVICE_ADDRESS, idMessage, action("find")))
                .map(Message::body)
                .doOnSuccess(foo -> System.out.println("Found: " + foo))
                .map(storedFoo -> storedFoo.getString("bar"))
                .filter(bar -> bar.equals("foobar"))
                .switchIfEmpty(Single.error(new IllegalStateException("Expecting input bar value.")))
                .toCompletable()
                .doOnComplete(() -> System.out.println("Saved and found foo with bar=foobar successfully."))
                .subscribe(startFuture::complete, startFuture::fail);
    }

    private static DeliveryOptions action(String action) {
        return new DeliveryOptions().addHeader("action", action);
    }

}
