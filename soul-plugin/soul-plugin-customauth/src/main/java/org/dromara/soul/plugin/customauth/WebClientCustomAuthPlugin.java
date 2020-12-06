package org.dromara.soul.plugin.customauth;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.dromara.soul.common.constant.Constants;
import org.dromara.soul.common.dto.RuleData;
import org.dromara.soul.common.dto.SelectorData;
import org.dromara.soul.common.dto.convert.DivideUpstream;
import org.dromara.soul.common.dto.convert.rule.DivideRuleHandle;
import org.dromara.soul.common.enums.PluginEnum;
import org.dromara.soul.common.utils.GsonUtils;
import org.dromara.soul.plugin.api.SoulPluginChain;
import org.dromara.soul.plugin.api.context.SoulContext;
import org.dromara.soul.plugin.api.result.SoulResultEnum;
import org.dromara.soul.plugin.base.AbstractSoulPlugin;
import org.dromara.soul.plugin.base.utils.SoulResultWrap;
import org.dromara.soul.plugin.base.utils.WebFluxResultUtils;
import org.dromara.soul.plugin.divide.balance.utils.LoadBalanceUtils;
import org.dromara.soul.plugin.divide.cache.UpstreamCacheManager;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
public class WebClientCustomAuthPlugin extends AbstractCustomAuthPlugin {

  private final WebClient webClient;

  public WebClientCustomAuthPlugin(final WebClient webClient) {
    this.webClient = webClient;
  }

  @Override
  protected Mono<Void> doExecute(ServerWebExchange exchange, SoulPluginChain chain, SelectorData selector,
      RuleData rule) {
    final SoulContext soulContext = exchange.getAttribute(Constants.CONTEXT);
    assert soulContext != null;
    final DivideRuleHandle ruleHandle = GsonUtils.getInstance().fromJson(rule.getHandle(), DivideRuleHandle.class);
    final List<DivideUpstream> upstreamList = UpstreamCacheManager.getInstance().findUpstreamListBySelectorId(selector.getId());
    if (CollectionUtils.isEmpty(upstreamList)) {
      log.error("customAuth upstream configuration error：{}", rule.toString());
      Object error = SoulResultWrap
          .error(SoulResultEnum.CANNOT_FIND_URL.getCode(), SoulResultEnum.CANNOT_FIND_URL.getMsg(), null);
      return WebFluxResultUtils.result(exchange, error);
    }
    final String ip = Objects.requireNonNull(exchange.getRequest().getRemoteAddress()).getAddress().getHostAddress();
    DivideUpstream divideUpstream = LoadBalanceUtils.selector(upstreamList, ruleHandle.getLoadBalance(), ip);
    if (Objects.isNull(divideUpstream)) {
      log.error("customAuth has no upstream");
      Object error = SoulResultWrap.error(SoulResultEnum.CANNOT_FIND_URL.getCode(), SoulResultEnum.CANNOT_FIND_URL.getMsg(), null);
      return WebFluxResultUtils.result(exchange, error);
    }
    //设置一下 http url
    String domain = buildDomain(divideUpstream);
    String urlPath = buildRealURL(domain, soulContext, exchange);
    log.info("you request,The resulting urlPath is :{}", urlPath);

    long timeout = Optional.ofNullable(ruleHandle.getTimeout()).orElse(3000L);

    HttpMethod method = HttpMethod.valueOf(exchange.getRequest().getMethodValue());
    WebClient.RequestBodySpec requestBodySpec = webClient.method(method).uri(urlPath);
    return handleRequestBody(requestBodySpec, exchange, timeout, chain);
  }

  private Mono<Void> handleRequestBody(final WebClient.RequestBodySpec requestBodySpec,
      final ServerWebExchange exchange,
      final long timeout,
      final SoulPluginChain chain) {
    return requestBodySpec.headers(httpHeaders -> {
      httpHeaders.addAll(exchange.getRequest().getHeaders());
      httpHeaders.remove(HttpHeaders.HOST);
    })
        .contentType(buildMediaType(exchange))
        .body(BodyInserters.fromDataBuffers(exchange.getRequest().getBody()))
        .exchange()
        .doOnError(e -> log.error(e.getMessage()))
        .timeout(Duration.ofMillis(timeout))
        .flatMap(e -> doNext(e, exchange, chain));
  }

  private Mono<Void> doNext(final ClientResponse res, final ServerWebExchange exchange, final SoulPluginChain chain) {
    if (res.statusCode() == HttpStatus.OK) {
      ServerWebExchange newExchange = buildNewExchange(exchange, res.bodyToFlux(DataBuffer.class));
      return chain.execute(newExchange);
    }

    Object error = SoulResultWrap.error(SoulResultEnum.CANNOT_FIND_URL.getCode(), SoulResultEnum.CANNOT_FIND_URL.getMsg(), null);
    return WebFluxResultUtils.result(exchange, error);
  }

}
