package io.vertx.di;

import io.vertx.di.foo.FooVerticle;
import io.vertx.core.Future;
import io.vertx.core.Launcher;
import io.vertx.reactivex.core.AbstractVerticle;

public class Main {

    public static void main(String[] args) {
        Launcher.executeCommand("run", LauncherVerticle.class.getName());
    }

    public static class LauncherVerticle extends AbstractVerticle {

        @Override
        public void start(Future<Void> startFuture) {
            vertx.rxDeployVerticle(FooVerticle.class.getName())
                    .toCompletable()
                    .andThen(vertx.rxDeployVerticle(MainVerticle.class.getName()))
                    .toCompletable()
                    .subscribe(startFuture::complete, startFuture::fail);
        }

    }

}
