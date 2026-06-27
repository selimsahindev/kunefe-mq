package dev.selimsahin.kunefe.starter.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as a Kunefe message listener.
 * <p>
 * The annotated method will be automatically registered as a consumer
 * for the specified topic and group. The method must accept a single
 * parameter of type {@code byte[]}.
 * <p>
 * Usage:
 * <pre>
 *   {@literal @}KunefeListener(topic = "orders", group = "order-service")
 *   public void onOrder(byte[] message) {
 *       // process message
 *   }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface KunefeListener {

    /**
     * The topic to subscribe to.
     */
    String topic();

    /**
     * The consumer group identifier.
     */
    String group();

    /**
     * The consumer instance identifier.
     * Defaults to the fully qualified method name if not specified.
     */
    String consumerId() default "";

    /**
     * The offset to start consuming from.
     * Defaults to 0 — start from the beginning.
     */
    long fromOffset() default 0;
}