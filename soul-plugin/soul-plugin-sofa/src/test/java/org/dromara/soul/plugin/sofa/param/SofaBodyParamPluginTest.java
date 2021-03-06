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

package org.dromara.soul.plugin.sofa.param;

import org.dromara.soul.common.constant.Constants;
import org.dromara.soul.common.enums.PluginEnum;
import org.dromara.soul.common.enums.RpcTypeEnum;
import org.dromara.soul.plugin.api.SoulPluginChain;
import org.dromara.soul.plugin.api.context.SoulContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.Assert.assertEquals;

/**
 * SofaBodyParamPluginTest.
 *
 * @author tydhot
 */
@RunWith(MockitoJUnitRunner.class)
public final class SofaBodyParamPluginTest {
    @Mock
    private SoulPluginChain chain;

    private ServerWebExchange simpleExchange;

    private ServerWebExchange exchange;

    private ServerWebExchange formExchange;

    private SofaBodyParamPlugin sofaBodyParamPlugin;

    @Before
    public void setup() {
        exchange = MockServerWebExchange.from(MockServerHttpRequest.post("localhost").contentType(MediaType.APPLICATION_JSON).body("{}"));
        formExchange = MockServerWebExchange.from(MockServerHttpRequest.post("localhost").contentType(MediaType.APPLICATION_FORM_URLENCODED).body("{}"));
        simpleExchange = MockServerWebExchange.from(MockServerHttpRequest.get("localhost").build());
    }

    @Test
    public void testNoBody() {
        Mockito.when(chain.execute(exchange)).thenReturn(Mono.empty());
        sofaBodyParamPlugin = new SofaBodyParamPlugin();
        StepVerifier.create(sofaBodyParamPlugin.execute(exchange, chain)).expectSubscription().verifyComplete();
    }

    @Test
    public void testSimpleBody() {
        Mockito.when(chain.execute(simpleExchange)).thenReturn(Mono.empty());
        sofaBodyParamPlugin = new SofaBodyParamPlugin();
        SoulContext context = new SoulContext();
        context.setRpcType(RpcTypeEnum.SOFA.getName());
        simpleExchange.getAttributes().put(Constants.CONTEXT, context);
        StepVerifier.create(sofaBodyParamPlugin.execute(simpleExchange, chain)).expectSubscription().verifyComplete();
    }

    @Test
    public void testJsonBody() {
        Mockito.when(chain.execute(exchange)).thenReturn(Mono.empty());
        sofaBodyParamPlugin = new SofaBodyParamPlugin();
        SoulContext context = new SoulContext();
        context.setRpcType(RpcTypeEnum.SOFA.getName());
        exchange.getAttributes().put(Constants.CONTEXT, context);
        StepVerifier.create(sofaBodyParamPlugin.execute(exchange, chain)).expectSubscription().verifyComplete();
    }

    @Test
    public void testFormBody() {
        Mockito.when(chain.execute(formExchange)).thenReturn(Mono.empty());
        sofaBodyParamPlugin = new SofaBodyParamPlugin();
        SoulContext context = new SoulContext();
        context.setRpcType(RpcTypeEnum.SOFA.getName());
        formExchange.getAttributes().put(Constants.CONTEXT, context);
        StepVerifier.create(sofaBodyParamPlugin.execute(formExchange, chain)).expectSubscription().verifyComplete();
    }

    @Test
    public void testGetOrder() {
        sofaBodyParamPlugin = new SofaBodyParamPlugin();
        final int result = sofaBodyParamPlugin.getOrder();
        assertEquals(PluginEnum.SOFA.getCode() - 1, result);
    }

    @Test
    public void testNamed() {
        sofaBodyParamPlugin = new SofaBodyParamPlugin();
        final String result = sofaBodyParamPlugin.named();
        assertEquals("sofa-body-param", result);
    }
}
