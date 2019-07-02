package fun.billon.im.boot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringApplicationRunListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;

/**
 * 应用启动的监听,将一些动态属性加载到属性文件
 * 1)获取主机ip,将属性设置到billon.host.ip
 *
 * @author billon
 * @version 1.0.0
 * @since 1.0.0
 */
public class ImApplicationRunListener implements SpringApplicationRunListener, PriorityOrdered {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImApplicationRunListener.class);

    private SpringApplication application;

    private String[] args;

    /**
     * 通过反射创建该实例对象的，构造方法中的参数要加上如下参数
     */
    public ImApplicationRunListener(SpringApplication application, String[] args) {
        this.application = application;
        this.args = args;
    }

    @Override
    public void starting() {
    }

    @Override
    public void environmentPrepared(ConfigurableEnvironment configurableEnvironment) {
        String ip = null;
        try {
            InetAddress addr = InetAddress.getLocalHost();
            ip = addr.getHostAddress();
            LOGGER.debug("billon.im.host:{}", ip);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        // PropertySource是资源加载的核心
        MutablePropertySources propertySources = configurableEnvironment.getPropertySources();
        // 这里添加最后
        Properties properties = new Properties();
        properties.setProperty("billon.im.host", ip);
        PropertiesPropertySource propertySource = new PropertiesPropertySource("props", properties);
        propertySources.addLast(propertySource);
    }

    @Override
    public void contextPrepared(ConfigurableApplicationContext configurableApplicationContext) {
    }

    @Override
    public void contextLoaded(ConfigurableApplicationContext configurableApplicationContext) {
    }

    @Override
    public void started(ConfigurableApplicationContext context) {
    }

    @Override
    public void running(ConfigurableApplicationContext context) {
    }

    @Override
    public void failed(ConfigurableApplicationContext configurableApplicationContext, Throwable throwable) {
    }

    @Override
    public int getOrder() {
        return 1;
    }

}