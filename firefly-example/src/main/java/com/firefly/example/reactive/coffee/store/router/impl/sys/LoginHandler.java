package com.firefly.example.reactive.coffee.store.router.impl.sys;

import com.firefly.annotation.Component;
import com.firefly.annotation.Inject;
import com.firefly.example.reactive.coffee.store.ProjectConfig;
import com.firefly.example.reactive.coffee.store.vo.UserInfo;
import com.firefly.server.http2.router.HTTPSession;
import com.firefly.server.http2.router.Handler;
import com.firefly.server.http2.router.RoutingContext;
import com.firefly.utils.log.slf4j.ext.LazyLogger;
import com.firefly.utils.pattern.Pattern;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Pengtao Qiu
 */
@Component("loginHandler")
public class LoginHandler implements Handler {

    private static final LazyLogger logger = LazyLogger.create();

    private List<Pattern> uriWhitelist = Stream.of(
            "/",
            "/favicon.ico",
            "/static/*",
            "/products").map(p -> Pattern.compile(p, "*")).collect(Collectors.toList());

    @Inject
    private ProjectConfig config;

    @Override
    public void handle(RoutingContext ctx) {
        Mono.fromFuture(ctx.getSession()).subscribe(session -> {
            if (ctx.getURI().getPath().equals(config.getLoginURL())) {
                renderLoginPage(ctx);
                return;
            }

            if (skipVerify(ctx.getURI().getPath())) {
                ctx.next();
            } else {
                verifyLoginUser(ctx, session);
            }
        }, ctx::fail);
    }

    private void renderLoginPage(RoutingContext ctx) {
        ctx.renderTemplate(config.getTemplateRoot() + "/login.mustache");
        ctx.succeed(true);
    }

    private void verifyLoginUser(RoutingContext ctx, HTTPSession session) {
        UserInfo userInfo = (UserInfo) session.getAttributes().get(config.getLoginUserKey());
        if (userInfo != null) {
            ctx.setAttribute(config.getLoginUserKey(), userInfo);
            ctx.next();
        } else {
            ctx.redirect(config.getLoginURL());
            ctx.succeed(true);
        }
    }

    private boolean skipVerify(String uri) {
        return uriWhitelist.parallelStream().anyMatch(p -> p.match(uri) != null);
    }
}