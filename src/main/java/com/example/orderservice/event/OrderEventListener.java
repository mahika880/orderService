package com.example.orderservice.event;

import com.example.orderservice.integration.InventoryClient;
import com.example.orderservice.port.InventoryPort;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.ConcurrentModificationException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class OrderEventListener {

    private static final Logger log = LoggerFactory.getLogger(OrderEventListener.class);
    private final InventoryPort inventoryPort;
    private static volatile int failureCount = 0;
    private static volatile boolean circuitOpen = false;
    private final Set<String> processedOrders = ConcurrentHashMap.newKeySet();
    private static final int FAILURE_THRESHOLD = 3;
    private final Counter inventoryFailureCounter;



    public OrderEventListener(InventoryPort inventoryPort, MeterRegistry meterRegistry) {
        this.inventoryPort =inventoryPort;
      this.inventoryFailureCounter = meterRegistry.counter("inventory.failure.count");
      meterRegistry.gauge("inventory.circuit.open", this, listener -> circuitOpen?1:0);
    }

    @Async("orderExecutor")
    @EventListener
    @Retryable(value = RuntimeException.class, maxAttempts = 3,backoff = @Backoff(delay = 1000))
    public void handleOrderCreaed(OrderCreatedEvent event) throws InterruptedException {
        log.info("Processing orderId={}",event.getOrderId());
        if(!processedOrders.add(event.getOrderId())){
            log.warn("Duplicate event detected for orderId={}",event.getOrderId());
            return;
        }


        log.info("OrderCreatedEvent AFTERCOMMIT for orderId={}",event.getOrderId());


        if (circuitOpen) {
            log.warn("Circuit is OPEN. Skipping inventory call for orderId={}",
                    event.getOrderId());
            return;
        }



        try {
        if(Math.random()>0.7){
            throw new RuntimeException("Simulate failure");
        }
            inventoryPort.reserveStock(event.getOrderId());
        failureCount = 0;

            log.info("Inventory reserved successfully for orderId={}",
                    event.getOrderId());


            Thread.sleep(2000);
        }catch (InterruptedException ex){
            inventoryFailureCounter.increment();
            failureCount++;
            log.error("Failure count={}", failureCount);

            if(failureCount>=FAILURE_THRESHOLD){
                circuitOpen = true;
                log.error("Circuit opened due to many failures");

            }

            throw ex;


        }
        log.info("Finished async processing for orderId={}",event.getOrderId(), Thread.currentThread().getName());


    }


}
