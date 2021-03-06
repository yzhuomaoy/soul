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

package org.dromara.soul.admin.controller;

import org.dromara.soul.admin.config.properties.JwtProperties;
import org.dromara.soul.admin.service.DashboardUserService;
import org.dromara.soul.admin.service.EnumService;
import org.dromara.soul.admin.spring.SpringBeanUtils;
import org.dromara.soul.admin.utils.SoulResultMessage;
import org.dromara.soul.admin.vo.DashboardUserVO;
import org.dromara.soul.admin.vo.LoginDashboardUserVO;
import org.dromara.soul.common.exception.CommonErrorCode;
import org.dromara.soul.common.utils.DateUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


/**
 * test case for PlatformController.
 *
 * @author wangwenjie
 */
@RunWith(MockitoJUnitRunner.class)
public final class PlatformControllerTest {

    private MockMvc mockMvc;

    @InjectMocks
    private PlatformController platformController;

    @Mock
    private DashboardUserService dashboardUserService;

    @Mock
    private EnumService enumService;

    /**
     * dashboardUser mock data.
     */
    private final DashboardUserVO dashboardUserVO = new DashboardUserVO("1", "admin", "123456",
            1, true, DateUtils.localDateTimeToString(LocalDateTime.now()),
            DateUtils.localDateTimeToString(LocalDateTime.now()));

    /**
     * init mockmvc.
     */
    @Before
    public void setUp() {
        this.mockMvc = MockMvcBuilders.standaloneSetup(platformController).build();
    }

    /**
     * test method loginDashboardUser.
     */
    @Test
    public void testLoginDashboardUser() throws Exception {
        ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);
        SpringBeanUtils.getInstance().setCfgContext(context);
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setKey("2095132720951327");
        when(context.getBean(JwtProperties.class)).thenReturn(jwtProperties);

        final String loginUri = "/platform/login?userName=admin&password=123456";

        LoginDashboardUserVO loginDashboardUserVO = LoginDashboardUserVO.buildLoginDashboardUserVO(dashboardUserVO);
        given(this.dashboardUserService.login(eq("admin"), eq("123456"))).willReturn(loginDashboardUserVO);
        this.mockMvc.perform(MockMvcRequestBuilders.request(HttpMethod.GET, loginUri))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(CommonErrorCode.SUCCESSFUL)))
                .andExpect(jsonPath("$.message", is(SoulResultMessage.PLATFORM_LOGIN_SUCCESS)))
                .andExpect(jsonPath("$.data.id", is(loginDashboardUserVO.getId())))
                .andReturn();
    }

    /**
     * test method queryEnums.
     */
    @Test
    public void testQueryEnums() throws Exception {
        final String queryEnumsUri = "/platform/enum";

        this.mockMvc.perform(MockMvcRequestBuilders.request(HttpMethod.GET, queryEnumsUri))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(CommonErrorCode.SUCCESSFUL)))
                .andExpect(jsonPath("$.data", is(this.enumService.list())))
                .andReturn();
    }
}
