# 什么是微服务架构

微服务架构是将单一应用程序拆分为多个小服务的方法，每个服务：

围绕单一业务开发

独立部署、独立运行，拥有自己的数据库

在自己的进程中运行

通过 HTTP 等轻量级机制通信

# 微服务和单体架构的主要区别是什么

对比单体架构

按业务拆分成多个独立项目

各个服务独立打包、单独部署

每个服务一般拥有独立数据库

针对压力大的服务单独扩容

不同服务可以选用不同技术

# 为什么本课程先实现单体系统，再逐步拆分为微服务

单体架构简单，不用一开始就面对分布式、服务调用、注册中心等复杂问题，可以先熟悉业务逻辑、基础开发，学习从单体渐进式迁移到微服务的方法

# 为什么作业需要提供可重复运行的测试或验证脚本

保证结果可以复现，快速验证功能的正确性



# 环境检查

![604301cef6feaf14cee7ea0ec62b4347](D:\microservices‑practice‑2412190216\docs\homework\week-01\screenshots\604301cef6feaf14cee7ea0ec62b4347.png)

![9819700e369abffe5835f80092d232a5](D:\microservices‑practice‑2412190216\docs\homework\week-01\screenshots\9819700e369abffe5835f80092d232a5.png)



![6dbc5b8e3752c13c7ecdc0f256a878bd](D:\microservices‑practice‑2412190216\docs\homework\week-01\screenshots\6dbc5b8e3752c13c7ecdc0f256a878bd.png)

![22952768771e1c977c1aeb87f1aa855f](D:\microservices‑practice‑2412190216\docs\homework\week-01\screenshots\22952768771e1c977c1aeb87f1aa855f.png)![674b425e7ae3e5d1941c5788f0a7e3fa](D:\microservices‑practice‑2412190216\docs\homework\week-01\screenshots\674b425e7ae3e5d1941c5788f0a7e3fa.png)