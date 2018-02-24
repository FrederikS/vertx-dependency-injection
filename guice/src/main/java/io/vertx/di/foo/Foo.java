package io.vertx.di.foo;

import java.util.Objects;

public class Foo {
    public final String id;
    public final String bar;

    Foo(String id, String bar) {
        this.id = id;
        this.bar = bar;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Foo foo = (Foo) o;
        return Objects.equals(id, foo.id)
                && Objects.equals(bar, foo.bar);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, bar);
    }

    @Override
    public String toString() {
        return "Foo{" +
                "id='" + id + '\'' +
                ", bar='" + bar + '\'' +
                '}';
    }

}
