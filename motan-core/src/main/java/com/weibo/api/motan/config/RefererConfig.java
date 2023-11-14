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

import com.weibo.api.motan.cluster.Cluster;
import com.weibo.api.motan.cluster.support.ClusterSupport;
import com.weibo.api.motan.common.MotanConstants;
import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.config.annotation.ConfigDesc;
import com.weibo.api.motan.config.handler.ConfigHandler;
import com.weibo.api.motan.core.extension.ExtensionLoader;
import com.weibo.api.motan.exception.MotanErrorMsgConstant;
import com.weibo.api.motan.exception.MotanFrameworkException;
import com.weibo.api.motan.proxy.ProxyFactory;
import com.weibo.api.motan.registry.RegistryService;
import com.weibo.api.motan.rpc.URL;
import com.weibo.api.motan.util.CollectionUtil;
import com.weibo.api.motan.util.LoggerUtil;
import com.weibo.api.motan.util.NetUtils;
import com.weibo.api.motan.util.UrlUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Referer config.
 *
 * @author fishermen
 * @version V1.0 created at: 2013-5-17
 */

public class RefererConfig<T> extends AbstractRefererConfig {

    private static final long serialVersionUID = -2299754608229467887L;

    private Class<T> interfaceClass;

    private String serviceInterface;

    public String getServiceInterface() {
        return serviceInterface;
    }

    public void setServiceInterface(String serviceInterface) {
        this.serviceInterface = serviceInterface;
    }

    // 具体到方法的配置
    protected List<MethodConfig> methods;

    // 点对点直连服务提供地址
    private String directUrl;

    private AtomicBoolean initialized = new AtomicBoolean(false);

    private T ref;

    private BasicRefererInterfaceConfig basicReferer;

    private List<ClusterSupport<T>> clusterSupports;

    public List<MethodConfig> getMethods() {
        return methods;
    }

    public void setMethods(List<MethodConfig> methods) {
        this.methods = methods;
    }

    public void setMethods(MethodConfig methods) {
        this.methods = Collections.singletonList(methods);
    }

    public boolean hasMethods() {
        return this.methods != null && !this.methods.isEmpty();
    }

    public T getRef() {
        if (ref == null) {
            initRef();
        }
        return ref;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public synchronized void initRef() {
        if (initialized.get()) {
            return;
        }
        // common check
        try {
            interfaceClass = (Class) Class.forName(interfaceClass.getName(), true, Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException e) {
            throw new MotanFrameworkException("RefererConfig initRef Error: Class not found " + interfaceClass.getName(), e,
                    MotanErrorMsgConstant.FRAMEWORK_INIT_ERROR);
        }
        checkInterfaceAndMethods(interfaceClass, methods);

        if (meshClient != null) { // use mesh client
            initMeshClientRef();
        } else { // use cluster
            initClusterRef();
        }
        initialized.set(true);
    }

    private void initClusterRef() {
        if (CollectionUtil.isEmpty(protocols)) {
            throw new MotanFrameworkException(String.format("%s RefererConfig is malformed, for protocol not set correctly!",
                    interfaceClass.getName()));
        }

        clusterSupports = new ArrayList<>(protocols.size());
        List<Cluster<T>> clusters = new ArrayList<>(protocols.size());
        String proxy = null;

        ConfigHandler configHandler = ExtensionLoader.getExtensionLoader(ConfigHandler.class).getExtension(MotanConstants.DEFAULT_VALUE);

        loadRegistryUrls();
        String localIp = getLocalHostAddress();
        for (ProtocolConfig protocol : protocols) {
            Map<String, String> params = new HashMap<>();
            params.put(URLParamType.nodeType.getName(), MotanConstants.NODE_TYPE_REFERER);
            params.put(URLParamType.version.getName(), URLParamType.version.getValue());
            params.put(URLParamType.refreshTimestamp.getName(), String.valueOf(System.currentTimeMillis()));

            collectConfigParams(params, protocol, basicReferer, extConfig, this);
            collectMethodConfigParams(params, this.getMethods());

            String path = StringUtils.isBlank(serviceInterface) ? interfaceClass.getName() : serviceInterface;
            URL refUrl = new URL(protocol.getName(), localIp, MotanConstants.DEFAULT_INT_VALUE, path, params);
            ClusterSupport<T> clusterSupport = createClusterSupport(refUrl, configHandler);

            clusterSupports.add(clusterSupport);
            clusters.add(clusterSupport.getCluster());

            if (proxy == null) {
                proxy = getProxyType(refUrl);
            }
        }

        ref = configHandler.refer(interfaceClass, clusters, proxy);
    }

    private void initMeshClientRef() {
        Map<String, String> params = new HashMap<>();
        params.put(URLParamType.nodeType.getName(), MotanConstants.NODE_TYPE_REFERER);
        collectConfigParams(params, basicReferer, extConfig, this);
        String path = StringUtils.isBlank(serviceInterface) ? interfaceClass.getName() : serviceInterface;
        // TODO check if the protocol config is compatible with mesh client
        URL refUrl = new URL(MotanConstants.PROTOCOL_MOTAN2, getLocalHostAddress(), MotanConstants.DEFAULT_INT_VALUE, path, params);
        ProxyFactory proxyFactory = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getExtension(getProxyType(refUrl));
        ref = proxyFactory.getProxy(interfaceClass, refUrl, meshClient);
        LoggerUtil.info("init mesh client referer finish. url:" + refUrl.toFullStr());
    }

    private String getProxyType(URL refUrl) {
        String defaultProxy = StringUtils.isBlank(serviceInterface) ? URLParamType.proxy.getValue() : MotanConstants.PROXY_COMMON;
        return refUrl.getParameter(URLParamType.proxy.getName(), defaultProxy);
    }

    private ClusterSupport<T> createClusterSupport(URL refUrl, ConfigHandler configHandler) {
        LoggerUtil.info("create cluster for refer url :" + refUrl.toFullStr());
        List<URL> regUrls = new ArrayList<>();

        // 如果用户指定directUrls 或者 injvm协议访问，则使用local registry
        if (StringUtils.isNotBlank(directUrl) || MotanConstants.PROTOCOL_INJVM.equals(refUrl.getProtocol())) {
            URL regUrl =
                    new URL(MotanConstants.REGISTRY_PROTOCOL_LOCAL, NetUtils.LOCALHOST, MotanConstants.DEFAULT_INT_VALUE,
                            RegistryService.class.getName());
            if (StringUtils.isNotBlank(directUrl)) {
                List<URL> directUrls = new ArrayList<>();
                String[] dus = MotanConstants.COMMA_SPLIT_PATTERN.split(directUrl);
                for (String du : dus) {
                    if (du.contains(":")) {
                        String[] hostPort = du.split(":");
                        URL durl = refUrl.createCopy();
                        durl.setHost(hostPort[0].trim());
                        durl.setPort(Integer.parseInt(hostPort[1].trim()));
                        durl.addParameter(URLParamType.nodeType.getName(), MotanConstants.NODE_TYPE_SERVICE);
                        directUrls.add(durl);
                    }
                }
                String directUrlsString = UrlUtils.urlsToString(directUrls);
                if (StringUtils.isNotBlank(directUrlsString)) {
                    regUrl.addParameter(URLParamType.directUrl.getName(), directUrlsString);
                } else {
                    LoggerUtil.warn("parse directUrl string is empty. directUrl:" + directUrl);
                }
            }
            regUrls.add(regUrl);
        } else { // 通过注册中心配置拼装URL，注册中心可能在本地，也可能在远端
            if (registryUrls == null || registryUrls.isEmpty()) {
                throw new IllegalStateException(
                        String.format(
                                "No registry to reference %s on the consumer %s , please config <motan:registry address=\"...\" /> in your spring config.",
                                interfaceClass, NetUtils.LOCALHOST));
            }
            for (URL url : registryUrls) {
                regUrls.add(url.createCopy());
            }
        }

        return configHandler.buildClusterSupport(interfaceClass, regUrls, refUrl);
    }

    public synchronized void destroy() {
        if (clusterSupports != null) {
            for (ClusterSupport<T> clusterSupport : clusterSupports) {
                clusterSupport.destroy();
            }
        }
        // Mesh client will not be destroyed with the referer。Its life cycle is consistent with the spring context
        ref = null;
        initialized.set(false);
    }

    public void setInterface(Class<T> interfaceClass) {
        if (interfaceClass != null && !interfaceClass.isInterface()) {
            throw new IllegalStateException("The interface class " + interfaceClass + " is not a interface!");
        }
        this.interfaceClass = interfaceClass;
    }

    public Class<?> getInterface() {
        return interfaceClass;
    }

    public String getDirectUrl() {
        return directUrl;
    }

    public void setDirectUrl(String directUrl) {
        this.directUrl = directUrl;
    }

    @ConfigDesc(excluded = true)
    public BasicRefererInterfaceConfig getBasicReferer() {
        return basicReferer;
    }

    public void setBasicReferer(BasicRefererInterfaceConfig basicReferer) {
        this.basicReferer = basicReferer;
    }

    public List<ClusterSupport<T>> getClusterSupports() {
        return clusterSupports;
    }

    public AtomicBoolean getInitialized() {
        return initialized;
    }

}
