package dev.selimsahin.kunefe.starter.listener;

import dev.selimsahin.kunefe.client.KunefeClient;
import dev.selimsahin.kunefe.client.consumer.KunefeConsumer;
import dev.selimsahin.kunefe.starter.annotation.KunefeListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import java.lang.reflect.Method;

/**
 * Scans the Spring application context for @KunefeListener annotated methods
 * and registers a consumer for each one.
 * <p>
 * Triggered after the Spring context is fully initialized via ContextRefreshedEvent.
 * Each listener runs on a dedicated virtual thread to avoid blocking the main thread.
 */
public class KunefeListenerProcessor implements ApplicationListener<ContextRefreshedEvent> {

    private static final Logger log = LoggerFactory.getLogger(KunefeListenerProcessor.class);

    private final KunefeClient client;
    private final ApplicationContext applicationContext;
    private volatile boolean initialized = false;

    public KunefeListenerProcessor(KunefeClient client, ApplicationContext applicationContext) {
        this.client = client;
        this.applicationContext = applicationContext;
    }

    /**
     * Called when the Spring context is refreshed.
     * <p>
     * Uses a volatile flag to prevent double-initialization on context refresh.
     */
    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (initialized) {
            return;
        }
        initialized = true;
        scanAndRegisterListeners();
    }

    /**
     * Scans all Spring beans for @KunefeListener annotated methods
     * and starts a consumer for each one.
     */
    private void scanAndRegisterListeners() {
        String[] beanNames = applicationContext.getBeanDefinitionNames();

        for (String beanName : beanNames) {
            Object bean = applicationContext.getBean(beanName);
            Class<?> beanClass = bean.getClass();

            for (Method method : beanClass.getDeclaredMethods()) {
                KunefeListener annotation = method.getAnnotation(KunefeListener.class);
                if (annotation != null) {
                    registerListener(bean, method, annotation);
                }
            }
        }
    }

    /**
     * Registers a consumer for the given @KunefeListener annotated method.
     * <p>
     * Each consumer runs on a dedicated virtual thread — blocking inside
     * the subscribe loop does not consume platform threads.
     */
    private void registerListener(Object bean, Method method, KunefeListener annotation) {
        String topic = annotation.topic();
        String group = annotation.group();
        String consumerId = annotation.consumerId().isEmpty()
                ? bean.getClass().getSimpleName() + "." + method.getName()
                : annotation.consumerId();
        long fromOffset = annotation.fromOffset();

        log.info("Registering KunefeListener — topic: '{}', group: '{}', consumerId: '{}'",
                topic, group, consumerId);

        KunefeConsumer consumer = client.consumer(group, consumerId);
        consumer.register(topic);

        Thread.ofVirtual().name("kunefe-listener-" + consumerId).start(() -> {
            consumer.subscribe(topic, fromOffset, message -> {
                try {
                    method.invoke(bean, message.getPayload().toByteArray());
                    consumer.commitOffset(topic, Long.parseLong(message.getMessageId()));
                } catch (Exception e) {
                    log.error("Error processing message on topic '{}' in listener '{}'",
                            topic, consumerId, e);
                }
            });
        });
    }
}