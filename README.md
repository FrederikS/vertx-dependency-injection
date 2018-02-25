# You might not need Dependency Injection in a Vertx application

For some time I heard/read a lot about the vertx toolkit and since I'm a real fan of reactive programming and event-driven architectures, but also spending most of my time in the java world, it sounded very promising to me and I decided to give it a try. So I started to create my first vertx application.

As always I thought it can't be that hard. I will just start with a practical example and will learn how the framework/toolkit works automatically, without reading the docs at all. **Really bad idea!** As vertx is really unopiniated, you can write the code in any style and it works. But you should at least follow some basic rules to profit from vertx. So my advice would be at least read the [core manual](http://vertx.io/docs/vertx-core/java/).

At some point (still hadn't read the docs completely) I thought about how to apply Dependency Injection when my application is going to be more complex. I had absolutely no idea, so I started to find an answer by using the search engine with search terms like 'vertx dependency injection' and so on. The results - there were just very few related - I got weren't official ones from the vertx team but some custom written libraries like vertx-guice.
That seems to be a way doing it. Let's try it out!


## The experiment

As an example I'm creating a fictional project with some pseudo CRUD operations around a `Foo` entity.
There is a `FooRepository` interface offering those methods. The implementation of them can be found at the `InMemoryFooRepository` class.
In all 3 different appraoches this is going to be our business logic - we only change the way of interacting with it.

## The traditional way

### Dependency Injection with Guice

Let's start with some code and the example descripted above. For our entity class we're using lombok to keep it clean and expressive.

```java
import lombok.Value;

@Value
public class Foo {
    public final String id;
    public final String bar;
}
```

We want to have reactive implementations, so we are going to use rxjava and our `FooRepository` interface is defined like this:

```java
import io.reactivex.Maybe;
import io.reactivex.Single;

public interface FooRepository {
    Single<Foo> save(Foo foo);
    Maybe<Foo> findById(String id);
}
```

The corresponding implementation with a `@Singleton` annotation looks like this:

```java
import io.reactivex.Maybe;
import io.reactivex.Single;

import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Singleton
class InMemoryFooRepository implements FooRepository {

    private final Map<String, Foo> store = new HashMap<>();

    @Override
    public Single<Foo> save(Foo foo) {
        store.put(foo.id, foo);
        return Single.just(foo);
    }

    @Override
    public Maybe<Foo> findById(String id) {
        return Single
                .just(store.get(id))
                .filter(Objects::nonNull);
    }
}
```

With Guice we're creating a `Module` to define which implementation should be bound to the `FooRepository` when we're going to inject it anywhere.
Here we go:

```java
import com.google.inject.AbstractModule;

public class FooModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(FooRepository.class).to(InMemoryFooRepository.class);
    }

}
```

All the guice setup is done and we are ready to use it in our main application.

```java
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
```

As usual in a vertx application our `MainVerticle` is extending from `AbstractVerticle`. We are overriding the `init` method to get a `FooRepository` instance here. When starting the verticle we are using this instance to execute some example operations. Basically we're saving one Foo entity and trying to find it by id again.
Our last class is a simple `Main` class launching our `MainVerticle` out of the IDE.

```java
import io.vertx.core.Launcher;

public class Main {

    public static void main(String[] args) {
        Launcher.executeCommand("run", MainVerticle.class.getName());
    }

}
```

Okay, let's get the output of our application.

```shell
Saved: Foo(id=7f4314e3-1578-44be-9135-a1e29fb9e453, bar=foobar)
Found: Foo(id=7f4314e3-1578-44be-9135-a1e29fb9e453, bar=foobar)
Saved and found foo with bar=foobar successfully.
```

Our implemenation seems to work and everything is fine. Let's go ahead.

## The vertx way

### Via the Eventbus directly

Whenever you'll read something about vertx you will see something like
> The event bus is the nervous system of Vert.x

You can rely on it and should devinitely use it a much as possible.
It supports message strategies like:
* pub/sub messaging
* point-to-point/request-response messaging

In our case we will use the last one.
Let's try this out.
Here is all the code that wasn't changed except of removing the `@Singleton` annotation form the `InMemoryFooRepository` class.

```java
import lombok.Value;

@Value
public class Foo {
    public final String id;
    public final String bar;
}
```

```java
import io.reactivex.Maybe;
import io.reactivex.Single;

public interface FooRepository {
    Single<Foo> save(Foo foo);
    Maybe<Foo> findById(String id);
}
```

```java
import io.reactivex.Maybe;
import io.reactivex.Single;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

class InMemoryFooRepository implements FooRepository {

    private final Map<String, Foo> store = new HashMap<>();

    @Override
    public Single<Foo> save(Foo foo) {
        store.put(foo.id, foo);
        return Single.just(foo);
    }

    @Override
    public Maybe<Foo> findById(String id) {
        return Single
                .just(store.get(id))
                .filter(Objects::nonNull);
    }

}
```

Of course we are removing the guice `FooModule` from the previous example and instead adding a further verticle called `FooVerticle`.

```java
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
```

So what are we doing here? We're instantiating a `InMemoryFooRepository` in the `init` method. So far so easy.
When starting the verticle we register a `JsonObject` consumer on the event bus for a certain address.
Depending on which action header is received from the incoming message we are invoking the corresponding
repository method and sending the result by replying to the message. That's mainly it, let's start using it.

```java
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
```

It looks really similar to the approach with guice. Instead of using the repository directly we are sending messages over the event bus.
Therefore we have to decorate our messages with specific action headers and the payload as a `JsonObject`.
We have a little bit more code but in this version we are completely decoupled from any implementation and any domain objects like `Foo`. That's cool!

```java
import io.vertx.core.Future;
import io.vertx.core.Launcher;
import io.vertx.di.foo.FooVerticle;
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
```

In our `Main` class we're deploying the `FooVerticle` before the `MainVerticle` to ensure that we're not missing any message.
Here is the output.

```shell
Saved: {"id":"ac571123-855b-4b2c-b9e9-059d65c273eb","bar":"foobar"}
Found: {"id":"ac571123-855b-4b2c-b9e9-059d65c273eb","bar":"foobar"}
Saved and found foo with bar=foobar successfully.
```

Our application is still working and it works as expected!

### Via vertx service proxy

For the last approach we are trying out the so called service proxies which are offered as a separate module by the vertx toolkit.
How do they work? Basically it's almost the same like those in the previous approach except we have less boilerplate code by using some code generation.
Our `Foo` class is the only untouched piece of code.

```java
import lombok.Value;

@Value
public class Foo {
    public final String id;
    public final String bar;
}
```

Due to some restrictions you have to follow when using service proxies, we have to change our `FooRepository` interface.

```java
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;

@ProxyGen
@VertxGen
public interface FooRepository {
    void save(JsonObject foo, Handler<AsyncResult<JsonObject>> resultHandler);
    void findById(String id, Handler<AsyncResult<JsonObject>> resultHandler);
}
```

You can only use primitive types, `JsonObject` and some other limited types as parameter - you can find them [here](http://vertx.io/docs/vertx-service-proxy/java/#_parameter_types) and the last parameter always has to be the result handler also returning one of the limited types.
Both annotations `@ProxyGen` and `@VertxGen` are used by vertx for code generation.
So we also have to add further dependencies and some build config to our project. To focus on code I won't list it here but you can find it in the [code repository](https://github.com/FrederikS/vertx-dependency-injection).
According to the changes in our `FooRepository` interface we also have to change the implementation.

```java
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
```

The logic remains but we have to use the result handler. To successfully generate the proxy code we have to add one further file - a `package-info.java`.

```java
@ModuleGen(name = "foo", groupPackage = "io.vertx.di.foo")
package io.vertx.di.foo;

import io.vertx.codegen.annotations.ModuleGen;
```

Enough conventions and limitations let's start using it. But first of all we have to register our service in the `FooVerticle`.

```java
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
```

That's cool - the registration is much cleaner than in the previous approach.
With the help of the `ServiceBinder` class we register our `InMemoryFooRepository` on a certain address we are going to use in our `MainVerticle`.

```java
import io.reactivex.Single;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.di.foo.FooVerticle;
import io.vertx.di.foo.reactivex.FooRepository;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.serviceproxy.ServiceProxyBuilder;
import java.util.UUID;

public class MainVerticle extends AbstractVerticle {

    private FooRepository fooRepository;

    @Override
    public void init(Vertx vertx, Context context) {
        super.init(vertx, context);
        fooRepository = new FooRepository(new ServiceProxyBuilder(vertx)
                .setAddress(FooVerticle.FOO_SERVICE_ADDRESS)
                .build(io.vertx.di.foo.FooRepository.class));
    }

    @Override
    public void start(Future<Void> startFuture) {
        JsonObject fooData = new JsonObject()
                .put("id", UUID.randomUUID().toString())
                .put("bar", "foobar");

        fooRepository.rxSave(fooData)
                .doOnSuccess(foo -> System.out.println("Saved: " + foo))
                .map(foo -> foo.getString("id"))
                .flatMap(fooRepository::rxFindById)
                .doOnSuccess(foo -> System.out.println("Found: " + foo))
                .map(storedFoo -> storedFoo.getString("bar"))
                .filter(bar -> bar.equals("foobar"))
                .switchIfEmpty(Single.error(new IllegalStateException("Expecting input bar value.")))
                .toCompletable()
                .doOnComplete(() -> System.out.println("Saved and found foo with bar=foobar successfully."))
                .subscribe(startFuture::complete, startFuture::fail);
    }

}
```

Via vertx code generation also reactive implementations can be generated. That's why we see the `rxSave` and `rxFindById` method calls here.
At the `init` method we are building these reactive service proxy clients via the `ServiceProxyBuilder` class.
All direct interactions with the event bus are isolated in the generated proxy classes.
When taking a look into these we notice that the implementation is pretty similar to ours from the previous approach.

```java
  public void handle(Message<JsonObject> msg) {
    try {
      JsonObject json = msg.body();
      String action = msg.headers().get("action");
      if (action == null) {
        throw new IllegalStateException("action not specified");
      }
      accessed();
      switch (action) {
        case "save": {
          service.save((io.vertx.core.json.JsonObject)json.getValue("foo"), createHandler(msg));
          break;
        }
        case "findById": {
          service.findById((java.lang.String)json.getValue("id"), createHandler(msg));
          break;
        }
        default: {
          throw new IllegalStateException("Invalid action: " + action);
        }
      }
    } catch (Throwable t) {
      msg.reply(new ServiceException(500, t.getMessage()));
      throw t;
    }
  }
```

```java
  @Override
  public void save(JsonObject foo, Handler<AsyncResult<JsonObject>> resultHandler) {
    if (closed) {
    resultHandler.handle(Future.failedFuture(new IllegalStateException("Proxy is closed")));
      return;
    }
    JsonObject _json = new JsonObject();
    _json.put("foo", foo);
    DeliveryOptions _deliveryOptions = (_options != null) ? new DeliveryOptions(_options) : new DeliveryOptions();
    _deliveryOptions.addHeader("action", "save");
    _vertx.eventBus().<JsonObject>send(_address, _json, _deliveryOptions, res -> {
      if (res.failed()) {
        resultHandler.handle(Future.failedFuture(res.cause()));
      } else {
        resultHandler.handle(Future.succeededFuture(res.result().body()));
      }
    });
  }
```

Here our switch statement on the action header with cases for each service method is.
And on the client-side we can also see that the params are put into separate properties of a `JsonObject` and the action header is set, too.
Our `Main` class is the same as before.

```java
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
```

Is the application still working? Here is the output.

```shell
Saved: {"id":"2e702451-4ebe-4ebe-aecc-5d26be9fc5ec","bar":"foobar"}
Found: {"id":"2e702451-4ebe-4ebe-aecc-5d26be9fc5ec","bar":"foobar"}
Saved and found foo with bar=foobar successfully.
```

Yes it is.

## Conclusion

Let's recap - With the event bus we get a mighty instrument from the vertx toolkit, which we should use.
It's almost as easy as guice to setup, the code isn't looking more complicated and is even stronger decoupled.
So my advice would be: In most cases you don't need a DI library within a vertx application and whenever you would create
a guice module start creating a new verticle exposing a service on the event bus.
You have to follow some restrictions like using limited types, but it isn't really a disadvantage since it also helps to stay decoupled and if you really need it, there is the `@DataObject` annotation to rescue.
The only limitation I found so far is that you can't stream results over the event bus but I could imagine that this will be added at some point.
That's it - hope it helps! :)

<p>
<hr>
<blockquote>
<strong>All code can be found <a href="https://github.com/FrederikS/vertx-dependency-injection">here</a></strong>
</blockquote>
</p>

### References

* http://vertx.io/docs/vertx-core/java/
* http://vertx.io/docs/vertx-service-proxy/java/
