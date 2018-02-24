package io.vertx.di;

import io.reactivex.Single;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.di.foo.FooVerticle;
import io.vertx.di.foo.reactivex.FooService;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.serviceproxy.ServiceProxyBuilder;


public class MainVerticle extends AbstractVerticle {

    private FooService fooService;

    @Override
    public void init(Vertx vertx, Context context) {
        super.init(vertx, context);
        fooService = new FooService(new ServiceProxyBuilder(vertx)
                .setAddress(FooVerticle.FOO_SERVICE_ADDRESS)
                .build(io.vertx.di.foo.FooService.class));
    }

    @Override
    public void start(Future<Void> startFuture) {
        fooService.rxCreateFoo("foobar")
                .doOnSuccess(foo -> System.out.println("Saved: " + foo))
                .map(foo -> foo.getString("id"))
                .flatMap(fooService::rxFindFoo)
                .doOnSuccess(foo -> System.out.println("Found: " + foo))
                .map(storedFoo -> storedFoo.getString("bar"))
                .filter(bar -> bar.equals("foobar"))
                .switchIfEmpty(Single.error(new IllegalStateException("Expecting input bar value.")))
                .toCompletable()
                .doOnComplete(() -> System.out.println("Saved and found foo with bar=foobar successfully."))
                .subscribe(startFuture::complete, startFuture::fail);
    }

}
