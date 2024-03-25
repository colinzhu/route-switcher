# 路由切换器

路由切换器是一个使用Java和Vert.x开发的反向代理服务器应用程序。它的设计目的是根据指定的路由规则将网络流量路由到不同的目的地。

## 特性

- 根据URI前缀动态将网络流量路由到不同的目的地。
- Web界面可以动态添加、更新和删除路由规则。
- Web界面可以监控和调试您的网络流量。

## 设置和运行

要设置和运行应用程序，您需要安装Java和Maven。然后，您可以克隆存储库并使用Maven运行应用程序。

```bash
git clone https://github.com/colinzhu/routeswitcher.git
cd routeswitcher
# 更新config.json以设置端口
mvn clean install
mvn exec:java
```

应用程序将启动并监听配置的端口。

## 使用UI

一旦应用程序运行起来，您可以打开浏览器并导航到`http://localhost:<port>`来访问路由切换器界面。在这里，您可以管理您的路由规则。

每个规则都由一个`uriPrefix`和一个`target`组成。`uriPrefix`是规则应匹配的URI的开头，`target`是应将匹配`uriPrefix`的请求路由到的服务器。