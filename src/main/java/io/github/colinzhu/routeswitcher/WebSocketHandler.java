package io.github.colinzhu.routeswitcher;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.ServerWebSocket;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

@Slf4j
class WebSocketHandler implements Handler<ServerWebSocket> {
    private final Vertx vertx;

    public WebSocketHandler(Vertx vertx) {
        this.vertx = vertx;
        redirectStdOutToWeb();
    }

    public void handle(ServerWebSocket webSocket) {
        log.info("web socket connected");
        webSocket.writeTextMessage("Welcome to Route Switcher!");
        vertx.eventBus().consumer("console.log", message -> {
            webSocket.writeTextMessage((String) message.body()); // redirect the message to websocket (web)
        });
    }

    private void redirectStdOutToWeb() {
        OutputStream webConsoleOutputStream = new OutputStream() {
            private final OutputStream oriOutStream = System.out;
            private final StringBuilder sb = new StringBuilder();
            @Override
            public void write(int b) {
                if (b == '\n') {
                    vertx.eventBus().publish("console.log", sb.toString());
                    sb.setLength(0);
                } else {
                    sb.append((char) b);
                }
                try {
                    oriOutStream.write(b); //keep the original console output
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        System.setOut(new PrintStream(webConsoleOutputStream));
    }

}
