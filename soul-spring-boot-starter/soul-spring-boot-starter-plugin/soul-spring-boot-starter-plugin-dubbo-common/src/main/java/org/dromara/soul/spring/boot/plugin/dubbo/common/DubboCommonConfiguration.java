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

package org.dromara.soul.spring.boot.plugin.dubbo.common;

import org.dromara.soul.plugin.api.SoulPlugin;
import org.dromara.soul.plugin.api.context.SoulContextDecorator;
import org.dromara.soul.plugin.api.param.BodyParamResolveService;
import org.dromara.soul.plugin.dubbo.common.context.DubboSoulContextDecorator;
import org.dromara.soul.plugin.dubbo.common.param.DubboBodyParamResolveServiceImpl;
import org.dromara.soul.plugin.dubbo.common.param.DubboParamPlugin;
import org.dromara.soul.plugin.dubbo.common.response.DubboResponsePlugin;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.SearchStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The type Dubbo common configuration.
 * 
 * @author xiaoyu
 */
@Configuration
public class DubboCommonConfiguration {
    
    /**
     * Dubbo soul context decorator soul context decorator.
     *
     * @return the soul context decorator
     */
    @Bean
    public SoulContextDecorator dubboSoulContextDecorator() {
        return new DubboSoulContextDecorator();
    }
    
    /**
     * Dubbo param plugin soul plugin.
     *
     * @return the soul plugin
     */
    @Bean
    public SoulPlugin dubboParamPlugin() {
        return new DubboParamPlugin();
    }
    
    /**
     * Dubbo response plugin soul plugin.
     *
     * @return the soul plugin
     */
    @Bean
    public SoulPlugin dubboResponsePlugin() {
        return new DubboResponsePlugin();
    }
    
    /**
     * Generic param resolve service dubbo param resolve service.
     *
     * @return the dubbo param resolve service
     */
    @Bean
    @ConditionalOnMissingBean(value = DubboBodyParamResolveServiceImpl.class, search = SearchStrategy.ALL)
    public BodyParamResolveService dubboBodyParamResolveService() {
        return new DubboBodyParamResolveServiceImpl();
    }
}
