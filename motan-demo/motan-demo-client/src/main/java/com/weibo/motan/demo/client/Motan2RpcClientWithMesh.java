/*
 *
 *   Copyright 2009-2016 Weibo, Inc.
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

package com.weibo.motan.demo.client;

import com.weibo.motan.demo.service.MotanDemoService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class Motan2RpcClientWithMesh {

    public static void main(String[] args) throws Throwable {
        //需要先部署mesh agent。可以参考https://github.com/weibocom/motan-go。agent配置中需要有匹配的注册中心配置
        ApplicationContext ctx = new ClassPathXmlApplicationContext(new String[]{"classpath:motan2_demo_client_mesh.xml"});
        MotanDemoService service;

        service = (MotanDemoService) ctx.getBean("motanDemoReferer");
        for (int i = 0; i < 30; i++) {
            System.out.println(service.hello("motan" + i));
            Thread.sleep(1000);
        }

        System.out.println("motan demo is finish.");
        System.exit(0);
    }

}
