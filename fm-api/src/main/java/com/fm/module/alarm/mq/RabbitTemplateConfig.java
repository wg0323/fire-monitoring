package com.fm.module.alarm.mq;

import com.fm.module.alarm.service.impl.CommandServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate.ConfirmCallback;
import org.springframework.amqp.rabbit.core.RabbitTemplate.ReturnsCallback;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitTemplate 可靠性配置：开启 mandatory，并在模板初始化阶段注册确认/退回回调。
 */
@Slf4j
@Configuration
public class RabbitTemplateConfig {

    /**
     * Spring Boot 2.7.18 尚未提供 org.springframework.boot.autoconfigure.amqp.RabbitTemplateCustomizer
     * （该接口自 Boot 3.x 起才提供）。按团队规范在此定义等价函数式接口，由下方 BeanPostProcessor
     * 在 RabbitTemplate 初始化完成时统一应用，语义与高版本 Customizer 一致。
     */
    @FunctionalInterface
    public interface RabbitTemplateCustomizer {
        void customize(RabbitTemplate template);
    }

    @Bean
    public RabbitTemplateCustomizer commandRabbitTemplateCustomizer(
            ObjectProvider<CommandServiceImpl> commandServiceProvider) {
        return template -> {
            template.setMandatory(true);
            ConfirmCallback confirmCallback = (correlationData, ack, cause) ->
                    commandServiceProvider.getObject().confirm(correlationData, ack, cause);
            ReturnsCallback returnsCallback = returned ->
                    commandServiceProvider.getObject().returnedMessage(returned);
            template.setConfirmCallback(confirmCallback);
            template.setReturnsCallback(returnsCallback);
            log.info("rabbit template customized: mandatory=true, confirm/returns callbacks registered");
        };
    }

    /**
     * 相比 @PostConstruct（配置类自身初始化时才注册，存在模板已被使用但回调未挂上的空窗期），
     * BeanPostProcessor 在每个 RabbitTemplate 初始化完成、被其他 Bean 使用前就套用全部 customizer。
     * static @Bean 避免 BeanPostProcessor 导致配置类过早实例化；ObjectProvider 延迟解析
     * CommandServiceImpl（它依赖 RabbitTemplate），回调运行时才取 Bean，不产生启动期循环依赖。
     */
    @Bean
    public static BeanPostProcessor rabbitTemplateCustomizerPostProcessor(
            ObjectProvider<RabbitTemplateCustomizer> customizers) {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (bean instanceof RabbitTemplate) {
                    RabbitTemplate template = (RabbitTemplate) bean;
                    customizers.orderedStream().forEach(customizer -> customizer.customize(template));
                }
                return bean;
            }
        };
    }
}
