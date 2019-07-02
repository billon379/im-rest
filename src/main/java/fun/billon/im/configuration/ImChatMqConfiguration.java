package fun.billon.im.configuration;

import com.alibaba.fastjson.JSON;
import fun.billon.im.socketio.service.IImSocketService;
import fun.billon.mq.api.constant.MqConstant;
import fun.billon.mq.api.message.ImChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.handler.annotation.Payload;

import javax.annotation.Resource;

/**
 * im聊天消息MQ配置
 * 注意：
 * 1)@RabbitListener注解添加到方法上,添加在类上的话通过rabbit管理平台发送消息会异常
 * 2)消息队列是动态创建的,队列名称是queue.im.chat.${ip}
 *
 * @author billon
 * @version 1.0.0
 * @since 1.0.0
 */
@Configuration
@SuppressWarnings("all")
public class ImChatMqConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImChatMqConfiguration.class);

    @Value("${billon.im.host}")
    private String ip;

    @Resource
    private IImSocketService imSocketService;

    /**
     * 聊天消息队列配置
     */
    @Bean(name = MqConstant.MQ_IM_CHAT_DYNAMIC_QUEUE_PREFIX + "${billon.im.host}")
    public Queue randomChatQueue() {
        return new Queue(MqConstant.MQ_IM_CHAT_DYNAMIC_QUEUE_PREFIX + ip, false, true, false);
    }

    @Bean(name = MqConstant.MQ_IM_CHAT_EXCHANGE)
    public DirectExchange chatExchange() {
        return new DirectExchange(MqConstant.MQ_IM_CHAT_EXCHANGE);
    }

    @Bean
    public Binding randomChatBindingExchangeMessage() {
        return BindingBuilder.bind(randomChatQueue()).to(chatExchange()).with(MqConstant.MQ_IM_CHAT_ROUTING_KEY);
    }

    @RabbitListener(queues = {MqConstant.MQ_IM_CHAT_DYNAMIC_QUEUE_PREFIX + "${billon.im.host}"})
    @RabbitHandler
    public void process(@Payload String message) {
        LOGGER.debug("收到IM消息:{}", message);
        imSocketService.processMessage(JSON.parseObject(message, ImChatMessage.class));
    }

}