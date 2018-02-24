package io.vertx.di;

import com.google.inject.Guice;
import io.reactivex.Single;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.di.foo.FooModule;
import io.vertx.di.foo.FooService;
import io.vertx.reactivex.core.AbstractVerticle;

public class MainVerticle extends AbstractVerticle {

    private FooService fooService;

    @Override
    public void init(Vertx vertx, Context context) {
        super.init(vertx, context);
        fooService = Guice
                .createInjector(new FooModule())
                .getInstance(FooService.class);
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        fooService.createFoo("foobar")
                .doOnSuccess(foo -> System.out.println("Saved: " + foo))
                .map(foo -> foo.id)
                .flatMapMaybe(fooService::findFoo)
                .doOnSuccess(foo -> System.out.println("Found: " + foo))
                .map(storedFoo -> storedFoo.bar)
                .filter(bar -> bar.equals("foobar"))
                .switchIfEmpty(Single.error(new IllegalStateException("Expecting input bar value.")))
                .toCompletable()
                .doOnComplete(() -> System.out.println("Saved and found foo with bar=foobar successfully."))
                .subscribe(startFuture::complete, startFuture::fail);
    }

}
