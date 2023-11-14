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

package com.weibo.api.motan.registry.support;

import com.weibo.api.motan.common.MotanConstants;
import com.weibo.api.motan.core.extension.ExtensionLoader;
import com.weibo.api.motan.core.extension.SpiMeta;
import com.weibo.api.motan.registry.Registry;
import com.weibo.api.motan.rpc.URL;

/**
 * 
 * 类说明
 *
 * @author fishermen
 * @version V1.0 created at: 2013-5-31
 */
@SpiMeta(name = "local")
public class LocalRegistryFactory extends AbstractRegistryFactory {

    @Override
    protected Registry createRegistry(URL url) {
        return ExtensionLoader.getExtensionLoader(Registry.class).getExtension(MotanConstants.REGISTRY_PROTOCOL_LOCAL);
    }


}
