/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.dromara.soul.springboot.starter.plugin.customauth;

import java.util.Objects;
import org.dromara.soul.plugin.api.SoulPlugin;
import org.dromara.soul.plugin.base.handler.PluginDataHandler;
import org.dromara.soul.plugin.customauth.NettyClientCustomAuthPlugin;
import org.dromara.soul.plugin.customauth.WebClientCustomAuthPlugin;
import org.dromara.soul.plugin.customauth.handle.CustomAuthPluginDataHandler;
import org.dromara.soul.plugin.divide.DividePlugin;
import org.dromara.soul.plugin.divide.handler.DividePluginDataHandler;
import org.dromara.soul.plugin.httpclient.NettyHttpClientPlugin;
import org.dromara.soul.plugin.httpclient.WebClientPlugin;
import org.dromara.soul.plugin.httpclient.response.NettyClientResponsePlugin;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Configuration
public class CustomAuthPluginConfiguration {

    @Bean
    public PluginDataHandler customAuthPluginDataHandler() {
        return new CustomAuthPluginDataHandler();
    }

    @Configuration
    @ConditionalOnProperty(name = "soul.httpclient.strategy", havingValue = "webClient", matchIfMissing = true)
    static class WebClientCustomAuthConfiguration {

        @Bean
        public SoulPlugin customAuthPlugin(final ObjectProvider<HttpClient> httpClient) {
            WebClient webClient = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(Objects.requireNonNull(httpClient.getIfAvailable())))
                .build();
            return new WebClientCustomAuthPlugin(webClient);
        }
    }

    @Configuration
    @ConditionalOnProperty(name = "soul.httpclient.strategy", havingValue = "netty")
    static class NettyHttpClientCustomAuthConfiguration {

        @Bean
        public SoulPlugin customAuthPlugin(final ObjectProvider<HttpClient> httpClient) {
            return new NettyClientCustomAuthPlugin(httpClient.getIfAvailable());
        }

    }
    
}
