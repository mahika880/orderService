package com.example.orderservice.service;

import com.example.orderservice.event.OrderCreatedEvent;
import com.example.orderservice.mapper.OrderMapper;
import com.example.orderservice.model.Order;
import com.example.orderservice.model.OutboxEvent;
import com.example.orderservice.repository.OrderJpaRepository;

import com.example.orderservice.dto.OrderRequest;
import com.example.orderservice.dto.OrderResponse;
import com.example.orderservice.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.validation.Payload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class OrderService {

    private static final Logger log =
            LoggerFactory.getLogger(OrderService.class);

    private final OrderJpaRepository orderRepository;
   // private final OrderSideEffectService sideEffectService;

    private final OrderMapper orderMapper;

    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;

    private final Counter orderCreatedCounter;

    public OrderService(OrderJpaRepository orderRepository, OrderMapper orderMapper,  OutboxEventRepository outboxRepository, ObjectMapper objectMapper, MeterRegistry meterRegistry) {
        this.orderRepository = orderRepository;
       // this.sideEffectService = sideEffectService;
        this.orderMapper = orderMapper;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
        this.orderCreatedCounter =meterRegistry.counter("orderd.created.counter");
    }
   // @CachePut(value = "orders" , key = "#order.orderId")
    @Transactional
    public OrderResponse createOrder(OrderRequest request){
        Optional<Order> existing = orderRepository.findByIdempotencyKey(request.getIdempotencyKey());

        if(existing.isPresent()){
            log.info("Duplicate request detected.Returning existing order");
            return orderMapper.toResponse(existing.get());
        }

         log.info("Attempting to create order with: orderId={}, itemName= {}, quantity= {}",
                 request.getOrderId(),
                 request.getItemName(),
                 request.getQuantity());


        Order order = new Order(
                request.getOrderId(),
                request.getItemName(),
                request.getQuantity(),
                request.getIdempotencyKey()
        );

       Order saved = orderRepository.save(order);


       try {
           String payload = objectMapper.writeValueAsString(new OrderCreatedEvent(saved.getOrderId())
           );
           OutboxEvent event = new OutboxEvent(saved.getOrderId(),
                   "ORDER_CREATED",
                   payload);
           outboxRepository.save(event);

       }catch (Exception e){
           throw  new RuntimeException("Failed to serialize event", e);
       }
        orderCreatedCounter.increment();



       log.info("Order created successfully with: orderId={}, itemName={},quantity={}",
               saved.getOrderId(),
               saved.getItemName(),
               saved.getQuantity());

     //  sideEffectService.sendOrderConfirmation(saved.getOrderId());

       return orderMapper.toResponse(saved);



    }
    @CacheEvict(value = "orders", key = "#orderId")
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public void increaseQuantity(String orderId,int delta){

        if(delta <=0){
            throw new IllegalStateException("Delta must be positive");
        }

            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new OrderNotFoundException(orderId));

            log.info("Updating quantity fr orderId={}", orderId,delta);

            order.setQuantity(order.getQuantity() + delta);

    }

    @Cacheable(value = "orders",key = "#orderId")
    public OrderResponse getOrderById(String orderId){

        Order order =  orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

            log.debug("Fetched order with id={}", orderId);

        return orderMapper.toResponse(order);
    }

    public Page<OrderResponse> searchOrders(String itemName, Pageable pageable){
        log.debug("Searching orders itemName={}", itemName);

        Page<Order> page =  (itemName == null)
                ?orderRepository.findAll(pageable)
        :orderRepository.findByItemName(itemName, pageable);

        return page.map(orderMapper::toResponse);
    }


    public Page<OrderResponse> getOrders(int page , int size){

        log.debug("Fetching orders page={}, size={}", page,size);

        return  orderRepository.findAll(PageRequest.of(page , size))
                .map(orderMapper::toResponse);
    }

}
