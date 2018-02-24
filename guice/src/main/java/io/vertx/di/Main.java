package io.vertx.di;

import io.vertx.core.Launcher;

public class Main {

    public static void main(String[] args) {
        Launcher.executeCommand("run", MainVerticle.class.getName());
    }

}
