package fun.billon.im;


import com.corundumstudio.socketio.SocketIOServer;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

import javax.annotation.Resource;

/**
 * springboot启动类
 * <p>
 * 1.EnableEurekaClient 注册Eureka客户端
 * 2.EnableDiscoveryClient 开启服务发现功能
 * 3.MapperScan 扫描mapper文件
 * 4.支持SocketIO,继承CommandLineRunner,启动SocketIOServer
 *
 * @author billon
 * @version 1.0.0
 * @since 1.0.0
 */
@SpringBootApplication
@EnableEurekaClient
@MapperScan({"fun.billon.im.rest.dao"})
@EnableFeignClients(basePackages = {
        "fun.billon.auth.api.feign",
        "fun.billon.member.api.feign"})
@ComponentScan(basePackages = {
        "fun.billon.im",
        "fun.billon.auth.api.hystrix",
        "fun.billon.member.api.hystrix",
        "fun.billon.mq.api.producer"})
public class ImApplication implements CommandLineRunner {

    @Resource
    private SocketIOServer server;

    public static void main(String[] args) {
        SpringApplication.run(ImApplication.class, args);
    }

    @Override
    public void run(String... args) {
        server.start();
    }

}