package com.example.orderservice.service;

import com.example.orderservice.dto.OrderRequest;
import com.example.orderservice.mapper.OrderMapper;
import com.example.orderservice.model.Order;
import com.example.orderservice.repository.OrderJpaRepository;
import com.example.orderservice.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.internal.matchers.Equals;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class OrderServiceTest {
    private OrderJpaRepository orderRepository;
    private OrderService orderService;
    private OrderMapper orderMapper;
    private OutboxEventRepository outboxEvent;
    private ObjectMapper objectMapper;
    private MeterRegistry meterRegistry;

    @BeforeEach
    void setup() {
        orderRepository = mock(OrderJpaRepository.class);
        orderMapper = mock(OrderMapper.class);
        outboxEvent = mock(OutboxEventRepository.class);
        objectMapper = mock(ObjectMapper.class);
        meterRegistry = mock(MeterRegistry.class);

        orderService = new OrderService(orderRepository, orderMapper, outboxEvent, objectMapper, meterRegistry);
    }

    @Test
    void shouldCreateOrderSuccessfully(){
        OrderRequest request = new OrderRequest("ord-1", "book", 2, "abc-123");
        when(orderRepository.existsById("ord-1")).thenReturn(false);
        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation ->  invocation.getArgument(0));

        var response = orderService.createOrder(request);

        assertEquals("ord-1", response.getOrderId());
        assertEquals("book" ,response.getItemName());
        assertEquals(2 ,response.getQuantity());

        verify(orderRepository).save(any(Order.class));

    }

    @Test
    void shouldThrowExceptionIfExceptionAlreadyExists(){

        OrderRequest request = new OrderRequest("ord-1", "book",2, "abc-123");
        when(orderRepository.existsById("ord-1")).thenReturn(true);

        assertThrows(IllegalStateException.class,
                () -> orderService.createOrder(request));

        verify(orderRepository, never()).save(any());
    }

    @Test
    void shouldIncreaseQuantity(){
        Order order = new Order("ord-1", "book",2, "abc-123");
        when(orderRepository.findById("ord-1"))
                .thenReturn(java.util.Optional.of(order));
        orderService.increaseQuantity("ord-1", 3);

        assertEquals(5,order.getQuantity());
    }
}
