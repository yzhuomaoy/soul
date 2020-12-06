package org.dromara.soul.plugin.customauth;

import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.dromara.soul.common.constant.Constants;
import org.dromara.soul.common.dto.RuleData;
import org.dromara.soul.common.dto.SelectorData;
import org.dromara.soul.common.dto.convert.DivideUpstream;
import org.dromara.soul.common.dto.convert.rule.DivideRuleHandle;
import org.dromara.soul.common.utils.GsonUtils;
import org.dromara.soul.plugin.api.SoulPluginChain;
import org.dromara.soul.plugin.api.context.SoulContext;
import org.dromara.soul.plugin.api.result.SoulResultEnum;
import org.dromara.soul.plugin.base.utils.SoulResultWrap;
import org.dromara.soul.plugin.base.utils.WebFluxResultUtils;
import org.dromara.soul.plugin.divide.balance.utils.LoadBalanceUtils;
import org.dromara.soul.plugin.divide.cache.UpstreamCacheManager;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.NettyDataBuffer;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

@Slf4j
public class NettyClientCustomAuthPlugin extends AbstractCustomAuthPlugin {

  private final HttpClient httpClient;

  public NettyClientCustomAuthPlugin(final HttpClient httpClient) {
    this.httpClient = httpClient;
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

    ServerHttpRequest request = exchange.getRequest();
    HttpHeaders filtered = request.getHeaders();
    final DefaultHttpHeaders httpHeaders = new DefaultHttpHeaders();
    filtered.forEach(httpHeaders::set);
    final HttpMethod method = HttpMethod.valueOf(request.getMethodValue());
    Duration duration = Duration.ofMillis(timeout);
    return this.httpClient.headers(headers -> headers.add(httpHeaders))
        .request(method).uri(urlPath).send((req, nettyOutbound) ->
            nettyOutbound.send(request.getBody().map(dataBuffer -> ((NettyDataBuffer) dataBuffer) .getNativeBuffer())))
        .responseConnection((res, connection) -> {
          HttpStatus status = HttpStatus.resolve(res.status().code());

          if (status != null && status == HttpStatus.OK) {
            ServerHttpResponse response = exchange.getResponse();
            NettyDataBufferFactory factory = (NettyDataBufferFactory) response.bufferFactory();
            final Flux<DataBuffer> body = connection
                .inbound()
                .receive()
                .retain()
                .map(factory::wrap);
            ServerWebExchange newExchange = buildNewExchange(exchange, body);
            return chain.execute(newExchange);
          }

          Object error = SoulResultWrap.error(SoulResultEnum.CANNOT_FIND_URL.getCode(), SoulResultEnum.CANNOT_FIND_URL.getMsg(), null);
          return WebFluxResultUtils.result(exchange, error);
        }).timeout(duration,
            Mono.error(new TimeoutException("Response took longer than timeout: " + duration)))
        .onErrorMap(TimeoutException.class, th -> new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, th.getMessage(), th))
        .then();
  }

}
