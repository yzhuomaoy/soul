package org.dromara.soul.plugin.customauth;

import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.dromara.soul.common.constant.Constants;
import org.dromara.soul.common.dto.convert.DivideUpstream;
import org.dromara.soul.common.enums.PluginEnum;
import org.dromara.soul.plugin.api.context.SoulContext;
import org.dromara.soul.plugin.base.AbstractSoulPlugin;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;

public abstract class AbstractCustomAuthPlugin extends AbstractSoulPlugin {

  @Override
  public Boolean skip(ServerWebExchange exchange) {
    if (HttpMethod.POST != exchange.getRequest().getMethod()) {
      return true;
    }

    MediaType mediaType = MediaType.valueOf(Optional.ofNullable(exchange
        .getRequest()
        .getHeaders().getFirst(HttpHeaders.CONTENT_TYPE))
        .orElse(null));
    return mediaType == null || !mediaType.includes(MediaType.APPLICATION_JSON);
  }

  @Override
  public int getOrder() {
    return PluginEnum.DIVIDE.getCode()-1;
  }

  @Override
  public String named() {
    return "customAuth";
  }

  protected String buildDomain(final DivideUpstream divideUpstream) {
    String protocol = divideUpstream.getProtocol();
    if (StringUtils.isBlank(protocol)) {
      protocol = "http://";
    }
    return protocol + divideUpstream.getUpstreamUrl().trim();
  }

  protected String buildRealURL(final String domain, final SoulContext soulContext, final ServerWebExchange exchange) {
    String path = domain;
    final String rewriteURI = (String) exchange.getAttributes().get(Constants.REWRITE_URI);
    if (StringUtils.isNoneBlank(rewriteURI)) {
      path = path + rewriteURI;
    } else {
      final String realUrl = soulContext.getRealUrl();
      if (StringUtils.isNoneBlank(realUrl)) {
        path = path + realUrl;
      }
    }
    String query = exchange.getRequest().getURI().getQuery();
    if (StringUtils.isNoneBlank(query)) {
      return path + "?" + query;
    }
    return path;
  }

  protected MediaType buildMediaType(final ServerWebExchange exchange) {
    return MediaType.valueOf(Optional.ofNullable(exchange
        .getRequest()
        .getHeaders().getFirst(HttpHeaders.CONTENT_TYPE))
        .orElse(MediaType.APPLICATION_JSON_VALUE));
  }

  protected ServerWebExchange buildNewExchange(ServerWebExchange exchange, Flux<DataBuffer> body) {
    ServerHttpRequestDecorator decorator = new ServerHttpRequestDecorator(exchange.getRequest()) {
      @Override
      public Flux<DataBuffer> getBody() {
        return body;
      }

      //复写getHeaders方法
      @Override
      public HttpHeaders getHeaders() {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.putAll(super.getHeaders());
        //由于修改了请求体的body，导致content-length长度不确定，因此需要删除原先的content-length
        httpHeaders.remove(HttpHeaders.CONTENT_LENGTH);
        httpHeaders.set(HttpHeaders.TRANSFER_ENCODING, "chunked");
        return httpHeaders;
      }
    };

    return exchange.mutate().request(decorator).build();
  }
}
