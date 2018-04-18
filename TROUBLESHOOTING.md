# Troubleshooting

## Docker

The following are some common problems that only apply to the Docker artifacts.

### Permission Denied

I receive a permission error when trying to run a connector:

```
docker: Got permission denied while trying to connect to the Docker daemon socket at unix:///var/run/docker.sock: Post http://%2Fvar%2Frun%2Fdocker.sock/v1.35/containers/create?name=jira-connector: dial unix /var/run/docker.sock: connect: permission denied.
See 'docker run --help'.
```

On Linux, docker requires root privileges.  Run your docker command as root by either switching to the root user or prefixing your command with `sudo`.

### Conflict

I receive a conflict error when trying to run my connector again (after stopping the connector or having a crash or startup failure of a previous run):

```
docker: Error response from daemon: Conflict. The container name "/jira-connector" is already in use by container "0d9502d5697ace72a8e8371eafd70a7c68651c9053cef7e38b926f76ac0104b6". You have to remove (or rename) that container to be able to reuse that name.
See 'docker run --help'.
```

When specifying a name for the connector's container, you need to clean up the old container before starting a new one by that name.  As mentioned in the error message, you can remove or rename it.

If the logs and current state of the container aren't important to you (during development & testing -- not recommended for production), you can specify the `--rm` parameter on the `docker run` command to delete the container when stopped/killed, or you can write a script to stop + remove before starting a fresh container:

```
#!/bin/bash

docker stop jira-connector
docker rm jira-connector
docker run --name jira-connector \
           -p 8080:8080 \
           -d \
           ws1connectors/jira-connector \
           --server.port=8080 \
           --security.oauth2.resource.jwt.key-uri="https://acme.vmwareidentity.com/SAAS/API/1.0/REST/auth/token?attribute=publicKey&format=pem"
```


### Port Is Already Allocated

I receive a port is already allocated error when trying to run my connector:

```
docker: Error response from daemon: driver failed programming external connectivity on endpoint jira-connector (54209a09060394406562471bc160b440ec480b44e01b2fe2eddb42c27a1e46cf): Bind for 0.0.0.0:8080 failed: port is already allocated.
```

You already have another program running on that port.  Double check that you aren't already running your connector by another container name on that port.  If you aren't, then you have to figure out what program is binding to that port and decide whether to stop the other program or just choose another port for your connector.



## RPM

The following are some common problems that only apply to the RPM artifacts.

### Connection Rejected After Install

I cannot connect to the connector after `yum install`.

Because the RPMs require you to specify the `security.oauth2.resource.jwt.key-uri` (preferably in `application.properties` under `/etc/...`) before startup, installing does not try to start the connector, so you have to manually do that the first time:

```
systemctl start jira-connector
```

Subsequent `yum upgrade`s, however, assume that you have already configured the key-uri and will restart the service for you after updating the package.



## Startup Problems In The Logs

The following are some common startup problems that apply to multiple types of artifacts.

### Missing key-uri Config

I receive the following error logs and the container/service/process stops:

```
2018-04-18 20:17:16.991  WARN 1 --- [           main] ConfigServletWebServerApplicationContext : Exception encountered during context initialization - cancelling refresh attempt: org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'vIdmPubKey' defined in class path resource [com/vmware/connectors/common/config/ConnectorsAutoConfiguration.class]: Bean instantiation via factory method failed; nested exception is org.springframework.beans.BeanInstantiationException: Failed to instantiate [java.lang.String]: Factory method 'vIdmPubKey' threw exception; nested exception is java.lang.IllegalArgumentException: Exactly one of security.oauth2.resource.jwt.key-uri/value must be configured
2018-04-18 20:17:17.021  INFO 1 --- [           main] o.apache.catalina.core.StandardService   : Stopping service [Tomcat]
2018-04-18 20:17:17.133  INFO 1 --- [           main] ConditionEvaluationReportLoggingListener :

Error starting ApplicationContext. To display the conditions report re-run your application with 'debug' enabled.
2018-04-18 20:17:17.170 ERROR 1 --- [           main] o.s.boot.SpringApplication               : Application run failed

org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'vIdmPubKey' defined in class path resource [com/vmware/connectors/common/config/ConnectorsAutoConfiguration.class]: Bean instantiation via factory method failed; nested exception is org.springframework.beans.BeanInstantiationException: Failed to instantiate [java.lang.String]: Factory method 'vIdmPubKey' threw exception; nested exception is java.lang.IllegalArgumentException: Exactly one of security.oauth2.resource.jwt.key-uri/value must be configured
    at org.springframework.beans.factory.support.ConstructorResolver.instantiateUsingFactoryMethod(ConstructorResolver.java:587) ~[spring-beans-5.0.4.RELEASE.jar!/:5.0.4.RELEASE]
    at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.instantiateUsingFactoryMethod(AbstractAutowireCapableBeanFactory.java:1250) ~[spring-beans-5.0.4.RELEASE.jar!/:5.0.4.RELEASE]
    at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.createBeanInstance(AbstractAutowireCapableBeanFactory.java:1099) ~[spring-beans-5.0.4.RELEASE.jar!/:5.0.4.RELEASE]
    at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.doCreateBean(AbstractAutowireCapableBeanFactory.java:545) ~[spring-beans-5.0.4.RELEASE.jar!/:5.0.4.RELEASE]
    at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.createBean(AbstractAutowireCapableBeanFactory.java:502) ~[spring-beans-5.0.4.RELEASE.jar!/:5.0.4.RELEASE]
    at org.springframework.beans.factory.support.AbstractBeanFactory.lambda$doGetBean$0(AbstractBeanFactory.java:312) ~[spring-beans-5.0.4.RELEASE.jar!/:5.0.4.RELEASE]
    at org.springframework.beans.factory.support.DefaultSingletonBeanRegistry.getSingleton(DefaultSingletonBeanRegistry.java:228) ~[spring-beans-5.0.4.RELEASE.jar!/:5.0.4.RELEASE]
    at org.springframework.beans.factory.support.AbstractBeanFactory.doGetBean(AbstractBeanFactory.java:310) ~[spring-beans-5.0.4.RELEASE.jar!/:5.0.4.RELEASE]
    at org.springframework.beans.factory.support.AbstractBeanFactory.getBean(AbstractBeanFactory.java:200) ~[spring-beans-5.0.4.RELEASE.jar!/:5.0.4.RELEASE]
    at org.springframework.beans.factory.support.DefaultListableBeanFactory.preInstantiateSingletons(DefaultListableBeanFactory.java:760) ~[spring-beans-5.0.4.RELEASE.jar!/:5.0.4.RELEASE]
    at org.springframework.context.support.AbstractApplicationContext.finishBeanFactoryInitialization(AbstractApplicationContext.java:868) ~[spring-context-5.0.4.RELEASE.jar!/:5.0.4.RELEASE]
    at org.springframework.context.support.AbstractApplicationContext.refresh(AbstractApplicationContext.java:549) ~[spring-context-5.0.4.RELEASE.jar!/:5.0.4.RELEASE]
    at org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext.refresh(ServletWebServerApplicationContext.java:140) ~[spring-boot-2.0.0.RELEASE.jar!/:2.0.0.RELEASE]
    at org.springframework.boot.SpringApplication.refresh(SpringApplication.java:752) [spring-boot-2.0.0.RELEASE.jar!/:2.0.0.RELEASE]
    at org.springframework.boot.SpringApplication.refreshContext(SpringApplication.java:388) [spring-boot-2.0.0.RELEASE.jar!/:2.0.0.RELEASE]
    at org.springframework.boot.SpringApplication.run(SpringApplication.java:327) [spring-boot-2.0.0.RELEASE.jar!/:2.0.0.RELEASE]
    at org.springframework.boot.SpringApplication.run(SpringApplication.java:1246) [spring-boot-2.0.0.RELEASE.jar!/:2.0.0.RELEASE]
    at org.springframework.boot.SpringApplication.run(SpringApplication.java:1234) [spring-boot-2.0.0.RELEASE.jar!/:2.0.0.RELEASE]
    at com.vmware.connectors.jira.JiraConnectorApplication.main(JiraConnectorApplication.java:25) [classes!/:2.1-SNAPSHOT]
    at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method) ~[na:1.8.0_162]
    at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62) ~[na:1.8.0_162]
    at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43) ~[na:1.8.0_162]
    at java.lang.reflect.Method.invoke(Method.java:498) ~[na:1.8.0_162]
    at org.springframework.boot.loader.MainMethodRunner.run(MainMethodRunner.java:48) [jira-connector-2.1-SNAPSHOT.jar:2.1-SNAPSHOT]
    at org.springframework.boot.loader.Launcher.launch(Launcher.java:87) [jira-connector-2.1-SNAPSHOT.jar:2.1-SNAPSHOT]
    at org.springframework.boot.loader.Launcher.launch(Launcher.java:50) [jira-connector-2.1-SNAPSHOT.jar:2.1-SNAPSHOT]
    at org.springframework.boot.loader.JarLauncher.main(JarLauncher.java:51) [jira-connector-2.1-SNAPSHOT.jar:2.1-SNAPSHOT]
Caused by: org.springframework.beans.BeanInstantiationException: Failed to instantiate [java.lang.String]: Factory method 'vIdmPubKey' threw exception; nested exception is java.lang.IllegalArgumentException: Exactly one of security.oauth2.resource.jwt.key-uri/value must be configured
    at org.springframework.beans.factory.support.SimpleInstantiationStrategy.instantiate(SimpleInstantiationStrategy.java:185) ~[spring-beans-5.0.4.RELEASE.jar!/:5.0.4.RELEASE]
    at org.springframework.beans.factory.support.ConstructorResolver.instantiateUsingFactoryMethod(ConstructorResolver.java:579) ~[spring-beans-5.0.4.RELEASE.jar!/:5.0.4.RELEASE]
    ... 26 common frames omitted
Caused by: java.lang.IllegalArgumentException: Exactly one of security.oauth2.resource.jwt.key-uri/value must be configured
    at com.vmware.connectors.common.config.ConnectorsAutoConfiguration.vIdmPubKey(ConnectorsAutoConfiguration.java:119) ~[connectors-config-2.1-SNAPSHOT.jar!/:2.1-SNAPSHOT]
    at com.vmware.connectors.common.config.ConnectorsAutoConfiguration$$EnhancerBySpringCGLIB$$33288ddc.CGLIB$vIdmPubKey$0(<generated>) ~[connectors-config-2.1-SNAPSHOT.jar!/:2.1-SNAPSHOT]
    at com.vmware.connectors.common.config.ConnectorsAutoConfiguration$$EnhancerBySpringCGLIB$$33288ddc$$FastClassBySpringCGLIB$$f6f46cd.invoke(<generated>) ~[connectors-config-2.1-SNAPSHOT.jar!/:2.1-SNAPSHOT]
    at org.springframework.cglib.proxy.MethodProxy.invokeSuper(MethodProxy.java:228) ~[spring-core-5.0.4.RELEASE.jar!/:5.0.4.RELEASE]
    at org.springframework.context.annotation.ConfigurationClassEnhancer$BeanMethodInterceptor.intercept(ConfigurationClassEnhancer.java:361) ~[spring-context-5.0.4.RELEASE.jar!/:5.0.4.RELEASE]
    at com.vmware.connectors.common.config.ConnectorsAutoConfiguration$$EnhancerBySpringCGLIB$$33288ddc.vIdmPubKey(<generated>) ~[connectors-config-2.1-SNAPSHOT.jar!/:2.1-SNAPSHOT]
    at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method) ~[na:1.8.0_162]
    at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62) ~[na:1.8.0_162]
    at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43) ~[na:1.8.0_162]
    at java.lang.reflect.Method.invoke(Method.java:498) ~[na:1.8.0_162]
    at org.springframework.beans.factory.support.SimpleInstantiationStrategy.instantiate(SimpleInstantiationStrategy.java:154) ~[spring-beans-5.0.4.RELEASE.jar!/:5.0.4.RELEASE]
    ... 27 common frames omitted
```

You forgot to configure your key-uri or have a typo in the setting.  Make sure the `security.oauth2.resource.jwt.key-uri` is specified either in the command line option of your docker-run/java command or in your properties file if using the RPM.

### Incorrect key-uri Config

I receive the following error logs and the container/service/process stops:

```
2018-04-18 19:30:04.350  WARN 1 --- [           main] ConfigServletWebServerApplicationContext : Exception encountered during context initialization - cancelling refresh attempt: org.springframework.beans.factory.UnsatisfiedDependencyException: Error creating bean with name 'org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfiguration': Unsatisfied dependency expressed through field 'tokenStore'; nested exception is org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'jwtTokenStore' defined in class path resource [org/springframework/boot/autoconfigure/security/oauth2/resource/ResourceServerTokenServicesConfiguration$JwtTokenServicesConfiguration.class]: Bean instantiation via factory method failed; nested exception is org.springframework.beans.BeanInstantiationException: Failed to instantiate [org.springframework.security.oauth2.provider.token.TokenStore]: Factory method 'jwtTokenStore' threw exception; nested exception is org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'jwtTokenEnhancer' defined in class path resource [org/springframework/boot/autoconfigure/security/oauth2/resource/ResourceServerTokenServicesConfiguration$JwtTokenServicesConfiguration.class]: Bean instantiation via factory method failed; nested exception is org.springframework.beans.BeanInstantiationException: Failed to instantiate [org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter]: Factory method 'jwtTokenEnhancer' threw exception; nested exception is org.springframework.web.client.HttpClientErrorException: 404 null
2018-04-18 19:30:04.353  INFO 1 --- [           main] o.apache.catalina.core.StandardService   : Stopping service [Tomcat]
2018-04-18 19:30:04.451  INFO 1 --- [           main] ConditionEvaluationReportLoggingListener :

Error starting ApplicationContext. To display the conditions report re-run your application with 'debug' enabled.
2018-04-18 19:30:04.475 ERROR 1 --- [           main] o.s.boot.SpringApplication               : Application run failed

org.springframework.beans.factory.UnsatisfiedDependencyException: Error creating bean with name 'org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfiguration': Unsatisfied dependency expressed through field 'tokenStore'; nested exception is org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'jwtTokenStore' defined in class path resource [org/springframework/boot/autoconfigure/security/oauth2/resource/ResourceServerTokenServicesConfiguration$JwtTokenServicesConfiguration.class]: Bean instantiation via factory method failed; nested exception is org.springframework.beans.BeanInstantiationException: Failed to instantiate [org.springframework.security.oauth2.provider.token.TokenStore]: Factory method 'jwtTokenStore' threw exception; nested exception is org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'jwtTokenEnhancer' defined in class path resource [org/springframework/boot/autoconfigure/security/oauth2/resource/ResourceServerTokenServicesConfiguration$JwtTokenServicesConfiguration.class]: Bean instantiation via factory method failed; nested exception is org.springframework.beans.BeanInstantiationException: Failed to instantiate [org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter]: Factory method 'jwtTokenEnhancer' threw exception; nested exception is org.springframework.web.client.HttpClientErrorException: 404 null
    at org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor$AutowiredFieldElement.inject(AutowiredAnnotationBeanPostProcessor.java:587) ~[spring-beans-5.0.4.RELEASE.jar!/:5.0.4.RELEASE]
    at org.springframework.beans.factory.annotation.InjectionMetadata.inject(InjectionMetadata.java:91) ~[spring-beans-5.0.4.RELEASE.jar!/:5.0.4.RELEASE]
    at org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor.postProcessPropertyValues(AutowiredAnnotationBeanPostProcessor.java:373) ~[spring-beans-5.0.4.RELEASE.jar!/:5.0.4.RELEASE]
    at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.populateBean(AbstractAutowireCapableBeanFactory.java:1344) ~[spring-beans-5.0.4.RELEASE.jar!/:5.0.4.RELEASE]
    at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.doCreateBean(AbstractAutowireCapableBeanFactory.java:582) ~[spring-beans-5.0.4.RELEASE.jar!/:5.0.4.RELEASE]
    at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.createBean(AbstractAutowireCapableBeanFactory.java:502) ~[spring-beans-5.0.4.RELEASE.jar!/:5.0.4.RELEASE]
    at org.springframework.beans.factory.support.AbstractBeanFactory.lambda$doGetBean$0(AbstractBeanFactory.java:312) ~[spring-beans-5.0.4.RELEASE.jar!/:5.0.4.RELEASE]
    at org.springframework.beans.factory.support.DefaultSingletonBeanRegistry.getSingleton(DefaultSingletonBeanRegistry.java:228) ~[spring-beans-5.0.4.RELEASE.jar!/:5.0.4.RELEASE]
    at org.springframework.beans.factory.support.AbstractBeanFactory.doGetBean(AbstractBeanFactory.java:310) ~[spring-beans-5.0.4.RELEASE.jar!/:5.0.4.RELEASE]
    at org.springframework.beans.factory.support.AbstractBeanFactory.getBean(AbstractBeanFactory.java:200) ~[spring-beans-5.0.4.RELEASE.jar!/:5.0.4.RELEASE]
    at org.springframework.beans.factory.support.DefaultListableBeanFactory.preInstantiateSingletons(DefaultListableBeanFactory.java:760) ~[spring-beans-5.0.4.RELEASE.jar!/:5.0.4.RELEASE]
    at org.springframework.context.support.AbstractApplicationContext.finishBeanFactoryInitialization(AbstractApplicationContext.java:868) ~[spring-context-5.0.4.RELEASE.jar!/:5.0.4.RELEASE]
    at org.springframework.context.support.AbstractApplicationContext.refresh(AbstractApplicationContext.java:549) ~[spring-context-5.0.4.RELEASE.jar!/:5.0.4.RELEASE]
    at org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext.refresh(ServletWebServerApplicationContext.java:140) ~[spring-boot-2.0.0.RELEASE.jar!/:2.0.0.RELEASE]
    at org.springframework.boot.SpringApplication.refresh(SpringApplication.java:752) [spring-boot-2.0.0.RELEASE.jar!/:2.0.0.RELEASE]
    at org.springframework.boot.SpringApplication.refreshContext(SpringApplication.java:388) [spring-boot-2.0.0.RELEASE.jar!/:2.0.0.RELEASE]
    at org.springframework.boot.SpringApplication.run(SpringApplication.java:327) [spring-boot-2.0.0.RELEASE.jar!/:2.0.0.RELEASE]
    at org.springframework.boot.SpringApplication.run(SpringApplication.java:1246) [spring-boot-2.0.0.RELEASE.jar!/:2.0.0.RELEASE]
    at org.springframework.boot.SpringApplication.run(SpringApplication.java:1234) [spring-boot-2.0.0.RELEASE.jar!/:2.0.0.RELEASE]
    at com.vmware.connectors.jira.JiraConnectorApplication.main(JiraConnectorApplication.java:25) [classes!/:2.1-SNAPSHOT]
    at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method) ~[na:1.8.0_162]
    at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62) ~[na:1.8.0_162]
    at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43) ~[na:1.8.0_162]
    at java.lang.reflect.Method.invoke(Method.java:498) ~[na:1.8.0_162]
    at org.springframework.boot.loader.MainMethodRunner.run(MainMethodRunner.java:48) [jira-connector-2.1-SNAPSHOT.jar:2.1-SNAPSHOT]
    at org.springframework.boot.loader.Launcher.launch(Launcher.java:87) [jira-connector-2.1-SNAPSHOT.jar:2.1-SNAPSHOT]
    at org.springframework.boot.loader.Launcher.launch(Launcher.java:50) [jira-connector-2.1-SNAPSHOT.jar:2.1-SNAPSHOT]
    at org.springframework.boot.loader.JarLauncher.main(JarLauncher.java:51) [jira-connector-2.1-SNAPSHOT.jar:2.1-SNAPSHOT]
Caused by: org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'jwtTokenStore' defined in class path resource [org/springframework/boot/autoconfigure/security/oauth2/resource/ResourceServerTokenServicesConfiguration$JwtTokenServicesConfiguration.class]: Bean instantiation via factory method failed; nested exception is org.springframework.beans.BeanInstantiationException: Failed to instantiate [org.springframework.security.oauth2.provider.token.TokenStore]: Factory method 'jwtTokenStore' threw exception; nested exception is org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'jwtTokenEnhancer' defined in class path resource [org/springframework/boot/autoconfigure/security/oauth2/resource/ResourceServerTokenServicesConfiguration$JwtTokenServicesConfiguration.class]: Bean instantiation via factory method failed; nested exception is org.springframework.beans.BeanInstantiationException: Failed to instantiate [org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter]: Factory method 'jwtTokenEnhancer' threw exception; nested exception is org.springframework.web.client.HttpClientErrorException: 404 null
    at org.springframework.beans.factory.support.ConstructorResolver.instantiateUsingFactoryMethod(ConstructorResolver.java:587) ~[spring-beans-5.0.4.RELEASE.jar!/:5.0.4.RELEASE]
    at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.instantiateUsingFactoryMethod(AbstractAutowireCapableBeanFactory.java:1250) ~[spring-beans-5.0.4.RELEASE.jar!/:5.0.4.RELEASE]
    at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.createBeanInstance(AbstractAutowireCapableBeanFactory.java:1099) ~[spring-beans-5.0.4.RELEASE.jar!/:5.0.4.RELEASE]
    at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.doCreateBean(AbstractAutowireCapableBeanFactory.java:545) ~[spring-beans-5.0.4.RELEASE.jar!/:5.0.4.RELEASE]
    at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.createBean(AbstractAutowireCapableBeanFactory.java:502) ~[spring-beans-5.0.4.RELEASE.jar!/:5.0.4.RELEASE]
    at org.springframework.beans.factory.support.AbstractBeanFactory.lambda$doGetBean$0(AbstractBeanFactory.java:312) ~[spring-beans-5.0.4.RELEASE.jar!/:5.0.4.RELEASE]
    at org.springframework.beans.factory.support.DefaultSingletonBeanRegistry.getSingleton(DefaultSingletonBeanRegistry.java:228) ~[spring-beans-5.0.4.RELEASE.jar!/:5.0.4.RELEASE]
    at org.springframework.beans.factory.support.AbstractBeanFactory.doGetBean(AbstractBeanFactory.java:310) ~[spring-beans-5.0.4.RELEASE.jar!/:5.0.4.RELEASE]
    at org.springframework.beans.factory.support.AbstractBeanFactory.getBean(AbstractBeanFactory.java:200) ~[spring-beans-5.0.4.RELEASE.jar!/:5.0.4.RELEASE]
    at org.springframework.beans.factory.config.DependencyDescriptor.resolveCandidate(DependencyDescriptor.java:251) ~[spring-beans-5.0.4.RELEASE.jar!/:5.0.4.RELEASE]
    at org.springframework.beans.factory.support.DefaultListableBeanFactory.doResolveDependency(DefaultListableBeanFactory.java:1138) ~[spring-beans-5.0.4.RELEASE.jar!/:5.0.4.RELEASE]
    at org.springframework.beans.factory.support.DefaultListableBeanFactory.resolveDependency(DefaultListableBeanFactory.java:1065) ~[spring-beans-5.0.4.RELEASE.jar!/:5.0.4.RELEASE]
    at org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor$AutowiredFieldElement.inject(AutowiredAnnotationBeanPostProcessor.java:584) ~[spring-beans-5.0.4.RELEASE.jar!/:5.0.4.RELEASE]
    ... 27 common frames omitted
Caused by: org.springframework.beans.BeanInstantiationException: Failed to instantiate [org.springframework.security.oauth2.provider.token.TokenStore]: Factory method 'jwtTokenStore' threw exception; nested exception is org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'jwtTokenEnhancer' defined in class path resource [org/springframework/boot/autoconfigure/security/oauth2/resource/ResourceServerTokenServicesConfiguration$JwtTokenServicesConfiguration.class]: Bean instantiation via factory method failed; nested exception is org.springframework.beans.BeanInstantiationException: Failed to instantiate [org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter]: Factory method 'jwtTokenEnhancer' threw exception; nested exception is org.springframework.web.client.HttpClientErrorException: 404 null
    at org.springframework.beans.factory.support.SimpleInstantiationStrategy.instantiate(SimpleInstantiationStrategy.java:185) ~[spring-beans-5.0.4.RELEASE.jar!/:5.0.4.RELEASE]
    at org.springframework.beans.factory.support.ConstructorResolver.instantiateUsingFactoryMethod(ConstructorResolver.java:579) ~[spring-beans-5.0.4.RELEASE.jar!/:5.0.4.RELEASE]
    ... 39 common frames omitted
Caused by: org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'jwtTokenEnhancer' defined in class path resource [org/springframework/boot/autoconfigure/security/oauth2/resource/ResourceServerTokenServicesConfiguration$JwtTokenServicesConfiguration.class]: Bean instantiation via factory method failed; nested exception is org.springframework.beans.BeanInstantiationException: Failed to instantiate [org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter]: Factory method 'jwtTokenEnhancer' threw exception; nested exception is org.springframework.web.client.HttpClientErrorException: 404 null
    at org.springframework.beans.factory.support.ConstructorResolver.instantiateUsingFactoryMethod(ConstructorResolver.java:587) ~[spring-beans-5.0.4.RELEASE.jar!/:5.0.4.RELEASE]
    at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.instantiateUsingFactoryMethod(AbstractAutowireCapableBeanFactory.java:1250) ~[spring-beans-5.0.4.RELEASE.jar!/:5.0.4.RELEASE]
    at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.createBeanInstance(AbstractAutowireCapableBeanFactory.java:1099) ~[spring-beans-5.0.4.RELEASE.jar!/:5.0.4.RELEASE]
    at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.doCreateBean(AbstractAutowireCapableBeanFactory.java:545) ~[spring-beans-5.0.4.RELEASE.jar!/:5.0.4.RELEASE]
    at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.createBean(AbstractAutowireCapableBeanFactory.java:502) ~[spring-beans-5.0.4.RELEASE.jar!/:5.0.4.RELEASE]
    at org.springframework.beans.factory.support.AbstractBeanFactory.lambda$doGetBean$0(AbstractBeanFactory.java:312) ~[spring-beans-5.0.4.RELEASE.jar!/:5.0.4.RELEASE]
    at org.springframework.beans.factory.support.DefaultSingletonBeanRegistry.getSingleton(DefaultSingletonBeanRegistry.java:228) ~[spring-beans-5.0.4.RELEASE.jar!/:5.0.4.RELEASE]
    at org.springframework.beans.factory.support.AbstractBeanFactory.doGetBean(AbstractBeanFactory.java:310) ~[spring-beans-5.0.4.RELEASE.jar!/:5.0.4.RELEASE]
    at org.springframework.beans.factory.support.AbstractBeanFactory.getBean(AbstractBeanFactory.java:200) ~[spring-beans-5.0.4.RELEASE.jar!/:5.0.4.RELEASE]
    at org.springframework.context.annotation.ConfigurationClassEnhancer$BeanMethodInterceptor.resolveBeanReference(ConfigurationClassEnhancer.java:392) ~[spring-context-5.0.4.RELEASE.jar!/:5.0.4.RELEASE]
    at org.springframework.context.annotation.ConfigurationClassEnhancer$BeanMethodInterceptor.intercept(ConfigurationClassEnhancer.java:364) ~[spring-context-5.0.4.RELEASE.jar!/:5.0.4.RELEASE]
    at org.springframework.boot.autoconfigure.security.oauth2.resource.ResourceServerTokenServicesConfiguration$JwtTokenServicesConfiguration$$EnhancerBySpringCGLIB$$1e43ad86.jwtTokenEnhancer(<generated>) ~[spring-security-oauth2-autoconfigure-2.0.0.RELEASE.jar!/:2.0.0.RELEASE]
    at org.springframework.boot.autoconfigure.security.oauth2.resource.ResourceServerTokenServicesConfiguration$JwtTokenServicesConfiguration.jwtTokenStore(ResourceServerTokenServicesConfiguration.java:267) ~[spring-security-oauth2-autoconfigure-2.0.0.RELEASE.jar!/:2.0.0.RELEASE]
    at org.springframework.boot.autoconfigure.security.oauth2.resource.ResourceServerTokenServicesConfiguration$JwtTokenServicesConfiguration$$EnhancerBySpringCGLIB$$1e43ad86.CGLIB$jwtTokenStore$1(<generated>) ~[spring-security-oauth2-autoconfigure-2.0.0.RELEASE.jar!/:2.0.0.RELEASE]
    at org.springframework.boot.autoconfigure.security.oauth2.resource.ResourceServerTokenServicesConfiguration$JwtTokenServicesConfiguration$$EnhancerBySpringCGLIB$$1e43ad86$$FastClassBySpringCGLIB$$18d246c.invoke(<generated>) ~[spring-security-oauth2-autoconfigure-2.0.0.RELEASE.jar!/:2.0.0.RELEASE]
    at org.springframework.cglib.proxy.MethodProxy.invokeSuper(MethodProxy.java:228) ~[spring-core-5.0.4.RELEASE.jar!/:5.0.4.RELEASE]
    at org.springframework.context.annotation.ConfigurationClassEnhancer$BeanMethodInterceptor.intercept(ConfigurationClassEnhancer.java:361) ~[spring-context-5.0.4.RELEASE.jar!/:5.0.4.RELEASE]
    at org.springframework.boot.autoconfigure.security.oauth2.resource.ResourceServerTokenServicesConfiguration$JwtTokenServicesConfiguration$$EnhancerBySpringCGLIB$$1e43ad86.jwtTokenStore(<generated>) ~[spring-security-oauth2-autoconfigure-2.0.0.RELEASE.jar!/:2.0.0.RELEASE]
    at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method) ~[na:1.8.0_162]
    at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62) ~[na:1.8.0_162]
    at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43) ~[na:1.8.0_162]
    at java.lang.reflect.Method.invoke(Method.java:498) ~[na:1.8.0_162]
    at org.springframework.beans.factory.support.SimpleInstantiationStrategy.instantiate(SimpleInstantiationStrategy.java:154) ~[spring-beans-5.0.4.RELEASE.jar!/:5.0.4.RELEASE]
    ... 40 common frames omitted
Caused by: org.springframework.beans.BeanInstantiationException: Failed to instantiate [org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter]: Factory method 'jwtTokenEnhancer' threw exception; nested exception is org.springframework.web.client.HttpClientErrorException: 404 null
    at org.springframework.beans.factory.support.SimpleInstantiationStrategy.instantiate(SimpleInstantiationStrategy.java:185) ~[spring-beans-5.0.4.RELEASE.jar!/:5.0.4.RELEASE]
    at org.springframework.beans.factory.support.ConstructorResolver.instantiateUsingFactoryMethod(ConstructorResolver.java:579) ~[spring-beans-5.0.4.RELEASE.jar!/:5.0.4.RELEASE]
    ... 62 common frames omitted
Caused by: org.springframework.web.client.HttpClientErrorException: 404 null
    at org.springframework.web.client.DefaultResponseErrorHandler.handleError(DefaultResponseErrorHandler.java:94) ~[spring-web-5.0.4.RELEASE.jar!/:5.0.4.RELEASE]
    at org.springframework.web.client.DefaultResponseErrorHandler.handleError(DefaultResponseErrorHandler.java:79) ~[spring-web-5.0.4.RELEASE.jar!/:5.0.4.RELEASE]
    at org.springframework.web.client.ResponseErrorHandler.handleError(ResponseErrorHandler.java:63) ~[spring-web-5.0.4.RELEASE.jar!/:5.0.4.RELEASE]
    at org.springframework.web.client.RestTemplate.handleResponse(RestTemplate.java:777) ~[spring-web-5.0.4.RELEASE.jar!/:5.0.4.RELEASE]
    at org.springframework.web.client.RestTemplate.doExecute(RestTemplate.java:730) ~[spring-web-5.0.4.RELEASE.jar!/:5.0.4.RELEASE]
    at org.springframework.web.client.RestTemplate.execute(RestTemplate.java:686) ~[spring-web-5.0.4.RELEASE.jar!/:5.0.4.RELEASE]
    at org.springframework.web.client.RestTemplate.exchange(RestTemplate.java:602) ~[spring-web-5.0.4.RELEASE.jar!/:5.0.4.RELEASE]
    at org.springframework.boot.autoconfigure.security.oauth2.resource.ResourceServerTokenServicesConfiguration$JwtTokenServicesConfiguration.getKeyFromServer(ResourceServerTokenServicesConfiguration.java:310) ~[spring-security-oauth2-autoconfigure-2.0.0.RELEASE.jar!/:2.0.0.RELEASE]
    at org.springframework.boot.autoconfigure.security.oauth2.resource.ResourceServerTokenServicesConfiguration$JwtTokenServicesConfiguration.jwtTokenEnhancer(ResourceServerTokenServicesConfiguration.java:275) ~[spring-security-oauth2-autoconfigure-2.0.0.RELEASE.jar!/:2.0.0.RELEASE]
    at org.springframework.boot.autoconfigure.security.oauth2.resource.ResourceServerTokenServicesConfiguration$JwtTokenServicesConfiguration$$EnhancerBySpringCGLIB$$1e43ad86.CGLIB$jwtTokenEnhancer$2(<generated>) ~[spring-security-oauth2-autoconfigure-2.0.0.RELEASE.jar!/:2.0.0.RELEASE]
    at org.springframework.boot.autoconfigure.security.oauth2.resource.ResourceServerTokenServicesConfiguration$JwtTokenServicesConfiguration$$EnhancerBySpringCGLIB$$1e43ad86$$FastClassBySpringCGLIB$$18d246c.invoke(<generated>) ~[spring-security-oauth2-autoconfigure-2.0.0.RELEASE.jar!/:2.0.0.RELEASE]
    at org.springframework.cglib.proxy.MethodProxy.invokeSuper(MethodProxy.java:228) ~[spring-core-5.0.4.RELEASE.jar!/:5.0.4.RELEASE]
    at org.springframework.context.annotation.ConfigurationClassEnhancer$BeanMethodInterceptor.intercept(ConfigurationClassEnhancer.java:361) ~[spring-context-5.0.4.RELEASE.jar!/:5.0.4.RELEASE]
    at org.springframework.boot.autoconfigure.security.oauth2.resource.ResourceServerTokenServicesConfiguration$JwtTokenServicesConfiguration$$EnhancerBySpringCGLIB$$1e43ad86.jwtTokenEnhancer(<generated>) ~[spring-security-oauth2-autoconfigure-2.0.0.RELEASE.jar!/:2.0.0.RELEASE]
    at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method) ~[na:1.8.0_162]
    at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62) ~[na:1.8.0_162]
    at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43) ~[na:1.8.0_162]
    at java.lang.reflect.Method.invoke(Method.java:498) ~[na:1.8.0_162]
    at org.springframework.beans.factory.support.SimpleInstantiationStrategy.instantiate(SimpleInstantiationStrategy.java:154) ~[spring-beans-5.0.4.RELEASE.jar!/:5.0.4.RELEASE]
    ... 63 common frames omitted
```

The path in your key-uri in `security.oauth2.resource.jwt.key-uri` is incorrect.  Spring tried to retrieve the public key at startup but the server responded with a not-found: `org.springframework.web.client.HttpClientErrorException: 404 null`.  Double check that your key-uri is correct and verify you can access it in a web browser, postman, or curl.

### Address Already In Use

I receive the following error logs and the service/process stops:

```
2018-04-18 16:39:57.141 ERROR 61628 --- [           main] o.apache.catalina.core.StandardService   : Failed to start connector [Connector[HTTP/1.1-8080]]

org.apache.catalina.LifecycleException: Failed to start component [Connector[HTTP/1.1-8080]]
    at org.apache.catalina.util.LifecycleBase.start(LifecycleBase.java:167) ~[tomcat-embed-core-8.5.28.jar!/:8.5.28]
    at org.apache.catalina.core.StandardService.addConnector(StandardService.java:225) ~[tomcat-embed-core-8.5.28.jar!/:8.5.28]
    at org.springframework.boot.web.embedded.tomcat.TomcatWebServer.addPreviouslyRemovedConnectors(TomcatWebServer.java:255) [spring-boot-2.0.0.RELEASE.jar!/:2.0.0.RELEASE]
    at org.springframework.boot.web.embedded.tomcat.TomcatWebServer.start(TomcatWebServer.java:197) [spring-boot-2.0.0.RELEASE.jar!/:2.0.0.RELEASE]
    at org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext.startWebServer(ServletWebServerApplicationContext.java:300) [spring-boot-2.0.0.RELEASE.jar!/:2.0.0.RELEASE]
    at org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext.finishRefresh(ServletWebServerApplicationContext.java:162) [spring-boot-2.0.0.RELEASE.jar!/:2.0.0.RELEASE]
    at org.springframework.context.support.AbstractApplicationContext.refresh(AbstractApplicationContext.java:552) [spring-context-5.0.4.RELEASE.jar!/:5.0.4.RELEASE]
    at org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext.refresh(ServletWebServerApplicationContext.java:140) [spring-boot-2.0.0.RELEASE.jar!/:2.0.0.RELEASE]
    at org.springframework.boot.SpringApplication.refresh(SpringApplication.java:752) [spring-boot-2.0.0.RELEASE.jar!/:2.0.0.RELEASE]
    at org.springframework.boot.SpringApplication.refreshContext(SpringApplication.java:388) [spring-boot-2.0.0.RELEASE.jar!/:2.0.0.RELEASE]
    at org.springframework.boot.SpringApplication.run(SpringApplication.java:327) [spring-boot-2.0.0.RELEASE.jar!/:2.0.0.RELEASE]
    at org.springframework.boot.SpringApplication.run(SpringApplication.java:1246) [spring-boot-2.0.0.RELEASE.jar!/:2.0.0.RELEASE]
    at org.springframework.boot.SpringApplication.run(SpringApplication.java:1234) [spring-boot-2.0.0.RELEASE.jar!/:2.0.0.RELEASE]
    at com.vmware.connectors.jira.JiraConnectorApplication.main(JiraConnectorApplication.java:25) [classes!/:2.1-SNAPSHOT]
    at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method) ~[na:1.8.0_144]
    at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62) ~[na:1.8.0_144]
    at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43) ~[na:1.8.0_144]
    at java.lang.reflect.Method.invoke(Method.java:498) ~[na:1.8.0_144]
    at org.springframework.boot.loader.MainMethodRunner.run(MainMethodRunner.java:48) [jira-connector-2.1-SNAPSHOT.jar:2.1-SNAPSHOT]
    at org.springframework.boot.loader.Launcher.launch(Launcher.java:87) [jira-connector-2.1-SNAPSHOT.jar:2.1-SNAPSHOT]
    at org.springframework.boot.loader.Launcher.launch(Launcher.java:50) [jira-connector-2.1-SNAPSHOT.jar:2.1-SNAPSHOT]
    at org.springframework.boot.loader.JarLauncher.main(JarLauncher.java:51) [jira-connector-2.1-SNAPSHOT.jar:2.1-SNAPSHOT]
Caused by: org.apache.catalina.LifecycleException: Protocol handler start failed
    at org.apache.catalina.connector.Connector.startInternal(Connector.java:1021) ~[tomcat-embed-core-8.5.28.jar!/:8.5.28]
    at org.apache.catalina.util.LifecycleBase.start(LifecycleBase.java:150) ~[tomcat-embed-core-8.5.28.jar!/:8.5.28]
    ... 21 common frames omitted
Caused by: java.net.BindException: Address already in use
    at sun.nio.ch.Net.bind0(Native Method) ~[na:1.8.0_144]
    at sun.nio.ch.Net.bind(Net.java:433) ~[na:1.8.0_144]
    at sun.nio.ch.Net.bind(Net.java:425) ~[na:1.8.0_144]
    at sun.nio.ch.ServerSocketChannelImpl.bind(ServerSocketChannelImpl.java:223) ~[na:1.8.0_144]
    at sun.nio.ch.ServerSocketAdaptor.bind(ServerSocketAdaptor.java:74) ~[na:1.8.0_144]
    at org.apache.tomcat.util.net.NioEndpoint.bind(NioEndpoint.java:210) ~[tomcat-embed-core-8.5.28.jar!/:8.5.28]
    at org.apache.tomcat.util.net.AbstractEndpoint.start(AbstractEndpoint.java:1150) ~[tomcat-embed-core-8.5.28.jar!/:8.5.28]
    at org.apache.coyote.AbstractProtocol.start(AbstractProtocol.java:591) ~[tomcat-embed-core-8.5.28.jar!/:8.5.28]
    at org.apache.catalina.connector.Connector.startInternal(Connector.java:1018) ~[tomcat-embed-core-8.5.28.jar!/:8.5.28]
    ... 22 common frames omitted

2018-04-18 16:39:57.145  INFO 61628 --- [           main] o.apache.catalina.core.StandardService   : Stopping service [Tomcat]
2018-04-18 16:39:57.160  INFO 61628 --- [           main] ConditionEvaluationReportLoggingListener :

Error starting ApplicationContext. To display the conditions report re-run your application with 'debug' enabled.
2018-04-18 16:39:57.162 ERROR 61628 --- [           main] o.s.b.d.LoggingFailureAnalysisReporter   :

***************************
APPLICATION FAILED TO START
***************************

Description:

The Tomcat connector configured to listen on port 8080 failed to start. The port may already be in use or the connector may be misconfigured.

Action:

Verify the connector's configuration, identify and stop any process that's listening on port 8080, or configure this application to listen on another port.

2018-04-18 16:39:57.163  INFO 61628 --- [           main] ConfigServletWebServerApplicationContext : Closing org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext@3d8c7aca: startup date [Wed Apr 18 16:39:50 EDT 2018]; root of context hierarchy
2018-04-18 16:39:57.167  INFO 61628 --- [           main] o.s.j.e.a.AnnotationMBeanExporter        : Unregistering JMX-exposed beans on shutdown
```

The address + port is already being used by another application.  You have to figure out what program is binding to that port and decide whether to stop the other program or to just choose another port for your connector.

### Permission Denied

I receive the following error logs and the service/process stops:

```
2018-04-18 16:46:35.089 ERROR 64323 --- [           main] o.apache.catalina.core.StandardService   : Failed to start connector [Connector[HTTP/1.1-80]]

org.apache.catalina.LifecycleException: Failed to start component [Connector[HTTP/1.1-80]]
    at org.apache.catalina.util.LifecycleBase.start(LifecycleBase.java:167) ~[tomcat-embed-core-8.5.28.jar!/:8.5.28]
    at org.apache.catalina.core.StandardService.addConnector(StandardService.java:225) ~[tomcat-embed-core-8.5.28.jar!/:8.5.28]
    at org.springframework.boot.web.embedded.tomcat.TomcatWebServer.addPreviouslyRemovedConnectors(TomcatWebServer.java:255) [spring-boot-2.0.0.RELEASE.jar!/:2.0.0.RELEASE]
    at org.springframework.boot.web.embedded.tomcat.TomcatWebServer.start(TomcatWebServer.java:197) [spring-boot-2.0.0.RELEASE.jar!/:2.0.0.RELEASE]
    at org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext.startWebServer(ServletWebServerApplicationContext.java:300) [spring-boot-2.0.0.RELEASE.jar!/:2.0.0.RELEASE]
    at org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext.finishRefresh(ServletWebServerApplicationContext.java:162) [spring-boot-2.0.0.RELEASE.jar!/:2.0.0.RELEASE]
    at org.springframework.context.support.AbstractApplicationContext.refresh(AbstractApplicationContext.java:552) [spring-context-5.0.4.RELEASE.jar!/:5.0.4.RELEASE]
    at org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext.refresh(ServletWebServerApplicationContext.java:140) [spring-boot-2.0.0.RELEASE.jar!/:2.0.0.RELEASE]
    at org.springframework.boot.SpringApplication.refresh(SpringApplication.java:752) [spring-boot-2.0.0.RELEASE.jar!/:2.0.0.RELEASE]
    at org.springframework.boot.SpringApplication.refreshContext(SpringApplication.java:388) [spring-boot-2.0.0.RELEASE.jar!/:2.0.0.RELEASE]
    at org.springframework.boot.SpringApplication.run(SpringApplication.java:327) [spring-boot-2.0.0.RELEASE.jar!/:2.0.0.RELEASE]
    at org.springframework.boot.SpringApplication.run(SpringApplication.java:1246) [spring-boot-2.0.0.RELEASE.jar!/:2.0.0.RELEASE]
    at org.springframework.boot.SpringApplication.run(SpringApplication.java:1234) [spring-boot-2.0.0.RELEASE.jar!/:2.0.0.RELEASE]
    at com.vmware.connectors.jira.JiraConnectorApplication.main(JiraConnectorApplication.java:25) [classes!/:2.1-SNAPSHOT]
    at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method) ~[na:1.8.0_144]
    at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62) ~[na:1.8.0_144]
    at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43) ~[na:1.8.0_144]
    at java.lang.reflect.Method.invoke(Method.java:498) ~[na:1.8.0_144]
    at org.springframework.boot.loader.MainMethodRunner.run(MainMethodRunner.java:48) [jira-connector-2.1-SNAPSHOT.jar:2.1-SNAPSHOT]
    at org.springframework.boot.loader.Launcher.launch(Launcher.java:87) [jira-connector-2.1-SNAPSHOT.jar:2.1-SNAPSHOT]
    at org.springframework.boot.loader.Launcher.launch(Launcher.java:50) [jira-connector-2.1-SNAPSHOT.jar:2.1-SNAPSHOT]
    at org.springframework.boot.loader.JarLauncher.main(JarLauncher.java:51) [jira-connector-2.1-SNAPSHOT.jar:2.1-SNAPSHOT]
Caused by: org.apache.catalina.LifecycleException: Protocol handler start failed
    at org.apache.catalina.connector.Connector.startInternal(Connector.java:1021) ~[tomcat-embed-core-8.5.28.jar!/:8.5.28]
    at org.apache.catalina.util.LifecycleBase.start(LifecycleBase.java:150) ~[tomcat-embed-core-8.5.28.jar!/:8.5.28]
    ... 21 common frames omitted
Caused by: java.net.SocketException: Permission denied
    at sun.nio.ch.Net.bind0(Native Method) ~[na:1.8.0_144]
    at sun.nio.ch.Net.bind(Net.java:433) ~[na:1.8.0_144]
    at sun.nio.ch.Net.bind(Net.java:425) ~[na:1.8.0_144]
    at sun.nio.ch.ServerSocketChannelImpl.bind(ServerSocketChannelImpl.java:223) ~[na:1.8.0_144]
    at sun.nio.ch.ServerSocketAdaptor.bind(ServerSocketAdaptor.java:74) ~[na:1.8.0_144]
    at org.apache.tomcat.util.net.NioEndpoint.bind(NioEndpoint.java:210) ~[tomcat-embed-core-8.5.28.jar!/:8.5.28]
    at org.apache.tomcat.util.net.AbstractEndpoint.start(AbstractEndpoint.java:1150) ~[tomcat-embed-core-8.5.28.jar!/:8.5.28]
    at org.apache.coyote.AbstractProtocol.start(AbstractProtocol.java:591) ~[tomcat-embed-core-8.5.28.jar!/:8.5.28]
    at org.apache.catalina.connector.Connector.startInternal(Connector.java:1018) ~[tomcat-embed-core-8.5.28.jar!/:8.5.28]
    ... 22 common frames omitted

2018-04-18 16:46:35.092  INFO 64323 --- [           main] o.apache.catalina.core.StandardService   : Stopping service [Tomcat]
2018-04-18 16:46:35.105  INFO 64323 --- [           main] ConditionEvaluationReportLoggingListener :

Error starting ApplicationContext. To display the conditions report re-run your application with 'debug' enabled.
2018-04-18 16:46:35.107 ERROR 64323 --- [           main] o.s.b.d.LoggingFailureAnalysisReporter   :

***************************
APPLICATION FAILED TO START
***************************

Description:

The Tomcat connector configured to listen on port 80 failed to start. The port may already be in use or the connector may be misconfigured.

Action:

Verify the connector's configuration, identify and stop any process that's listening on port 80, or configure this application to listen on another port.

2018-04-18 16:46:35.108  INFO 64323 --- [           main] ConfigServletWebServerApplicationContext : Closing org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext@21bcffb5: startup date [Wed Apr 18 16:46:29 EDT 2018]; root of context hierarchy
2018-04-18 16:46:35.112  INFO 64323 --- [           main] o.s.j.e.a.AnnotationMBeanExporter        : Unregistering JMX-exposed beans on shutdown
```

Ports below 1024 are privileged ports and require root permissions.  You can either run your process as root/sudo, or you can choose a different non-privileged port to bind to.
