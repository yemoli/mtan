/*
 *
 *   Copyright 2009-2023 Weibo, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package com.weibo.api.motan.config.springsupport;

import com.weibo.api.motan.config.MeshClientConfig;
import com.weibo.api.motan.transport.MeshClient;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;

/**
 * @author zhanglei28
 * @date 2022/12/27.
 */
public class MeshClientConfigBean extends MeshClientConfig implements FactoryBean<MeshClient>, DisposableBean {

    @Override
    public void destroy() throws Exception {
        super.destroy();
    }

    @Override
    public MeshClient getObject() throws Exception {
        return getMeshClient();
    }

    @Override
    public Class<?> getObjectType() {
        return MeshClient.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
