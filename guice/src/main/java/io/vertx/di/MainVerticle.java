package io.vertx.di;

import com.google.inject.Guice;
import io.reactivex.Single;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.di.foo.Foo;
import io.vertx.di.foo.FooModule;
import io.vertx.di.foo.FooRepository;
import io.vertx.reactivex.core.AbstractVerticle;

import java.util.UUID;

public class MainVerticle extends AbstractVerticle {

    private FooRepository fooRepository;

    @Override
    public void init(Vertx vertx, Context context) {
        super.init(vertx, context);
        fooRepository = Guice
                .createInjector(new FooModule())
                .getInstance(FooRepository.class);
    }

    @Override
    public void start(Future<Void> startFuture) {
        fooRepository.save(new Foo(UUID.randomUUID().toString(), "foobar"))
                .doOnSuccess(foo -> System.out.println("Saved: " + foo))
                .map(foo -> foo.id)
                .flatMapMaybe(fooRepository::findById)
                .doOnSuccess(foo -> System.out.println("Found: " + foo))
                .map(storedFoo -> storedFoo.bar)
                .filter(bar -> bar.equals("foobar"))
                .switchIfEmpty(Single.error(new IllegalStateException("Expecting input bar value.")))
                .toCompletable()
                .doOnComplete(() -> System.out.println("Saved and found foo with bar=foobar successfully."))
                .subscribe(startFuture::complete, startFuture::fail);
    }

}
