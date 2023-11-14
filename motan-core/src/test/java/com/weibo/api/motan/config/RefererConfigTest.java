/*
 *  Copyright 2009-2016 Weibo, Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.weibo.api.motan.config;

import com.weibo.api.motan.BaseTestCase;
import com.weibo.api.motan.common.MotanConstants;
import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.mock.MockClient;
import com.weibo.api.motan.protocol.example.IWorld;
import com.weibo.api.motan.protocol.example.MockWorld;
import com.weibo.api.motan.rpc.RpcContext;
import com.weibo.api.motan.rpc.URL;
import com.weibo.api.motan.transport.DefaultMeshClient;
import com.weibo.api.motan.transport.MeshClient;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * refererConfig unit test.
 *
 * @author fishermen
 * @version V1.0 created at: 2013-6-18
 */

public class RefererConfigTest extends BaseTestCase {

    private RefererConfig<IWorld> refererConfig = null;
    private ServiceConfig<IWorld> serviceConfig = null;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        RegistryConfig registryConfig = mockLocalRegistryConfig();

        serviceConfig = mockIWorldServiceConfig();
        serviceConfig.setProtocol(mockProtocolConfig(MotanConstants.PROTOCOL_INJVM));
        serviceConfig.setRegistry(registryConfig);
        serviceConfig.setExport(MotanConstants.PROTOCOL_INJVM);

        refererConfig = mockIWorldRefererConfig();
        refererConfig.setProtocol(mockProtocolConfig(MotanConstants.PROTOCOL_INJVM));
        refererConfig.setRegistry(registryConfig);

        refererConfig.setCheck("false");
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        if (refererConfig != null) {
            refererConfig.destroy();
        }
        if (serviceConfig != null) {
            serviceConfig.unexport();
        }
    }

    @Test
    public void testGetRef() {
        MockWorld mWorld = new MockWorld();
        serviceConfig.setRef(mWorld);
        serviceConfig.export();

        IWorld ref = refererConfig.getRef();
        assertNotNull(ref);
        assertEquals(refererConfig.getClusterSupports().size(), 1);

        int times = 3;
        for (int i = 0; i < times; i++) {
            ref.world("test");
        }
        assertEquals(times, mWorld.stringCount.get());
        serviceConfig.unexport();

        // destroy
        refererConfig.destroy();
        assertFalse(refererConfig.getInitialized().get());
    }

    @Test
    public void testException() {
        IWorld ref = null;

        // protocol empty
        List<ProtocolConfig> protocols = new ArrayList<ProtocolConfig>();
        refererConfig.setProtocols(protocols);
        try {
            ref = refererConfig.getRef();
            assertTrue(false);
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("protocol not set correctly"));
        }

        // protocol not exists
        protocols.add(mockProtocolConfig("notExist"));
        try {
            ref = refererConfig.getRef();
            assertTrue(false);
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("get extension fail"));
        }
        protocols.add(mockProtocolConfig("notExist"));

        // method config wrong
        MethodConfig mConfig = new MethodConfig();
        mConfig.setName("notExist");
        refererConfig.setMethods(mConfig);
        try {
            ref = refererConfig.getRef();
            assertTrue(false);
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("not found method"));
        }
    }

    @Test
    public void testMultiProtocol() {
        List<ProtocolConfig> protocols = getMultiProtocols(MotanConstants.PROTOCOL_INJVM, MotanConstants.PROTOCOL_MOTAN);
        refererConfig.setProtocols(protocols);
        IWorld ref = refererConfig.getRef();
        assertNotNull(ref);
        assertEquals(protocols.size(), refererConfig.getClusterSupports().size());

    }

    @Test
    public void testMultiRegitstry() {
        List<RegistryConfig> registries =
                getMultiRegister(MotanConstants.REGISTRY_PROTOCOL_LOCAL, MotanConstants.REGISTRY_PROTOCOL_ZOOKEEPER);
        refererConfig.setRegistries(registries);
        refererConfig.loadRegistryUrls();
        assertEquals(registries.size(), refererConfig.getRegistryUrls().size());
    }

    @Test
    public void testMeshClientRef() {
        int timeout = 300;
        URL mockMeshUrl = new URL("motan2", "localhost", 18002, MeshClient.class.getName(), DefaultMeshClient.getDefaultParams());
        mockMeshUrl.addParameter(URLParamType.endpointFactory.getName(), "mockEndpoint");
        DefaultMeshClient meshClient = new DefaultMeshClient(mockMeshUrl);
        meshClient.init();
        refererConfig.setRequestTimeout(timeout);
        refererConfig.setMeshClient(meshClient);
        IWorld ref = refererConfig.getRef();
        assertNotNull(ref);
        assertNotNull(refererConfig.getMeshClient());

        int times = 3;
        for (int i = 0; i < times; i++) {
            RpcContext.destroy();
            ref.world("test");
            assertEquals(String.valueOf(timeout), RpcContext.getContext().getRequest().getAttachments().get(MotanConstants.M2_TIMEOUT));
        }
        assertEquals(times, MockClient.urlMap.get(meshClient.getUrl()).get());

        // destroy
        refererConfig.destroy();
        assertFalse(refererConfig.getInitialized().get());
    }

}
