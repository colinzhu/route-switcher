package io.github.colinzhu.routeswitcher;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.*;
import io.vertx.ext.web.Router;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TestHttpServerVerticle extends AbstractVerticle {
    @Override
    public void start() {
        int port = config().getInteger("port");

        HttpServer originServer = vertx.createHttpServer();

        Router router = Router.router(vertx);
        router.route("/test").handler(ctx -> {
            log.info("/test");
            HttpServerResponse response = ctx.response();
            response.putHeader("content-type", "text/plain");
            response.end("Hello World from Vert.x-Web! --- /test");
        });
        router.route("/local/test").handler(ctx -> {
            log.info("/local/test");
            HttpServerResponse response = ctx.response();
            response.putHeader("content-type", "text/plain");
            response.end("Hello World from Vert.x-Web! --- /local/test");
        });
        router.route().handler(ctx -> {
            HttpServerResponse response = ctx.response();
            response.putHeader("content-type", "text/plain");
            response.end("Hello World from Vert.x-Web! ---- other");
        });

        originServer.requestHandler(router).listen(port)
                .onSuccess(res -> log.info("target server started at port: {}", port))
                .onFailure(err -> log.error("error start target server", err));

    }
}
