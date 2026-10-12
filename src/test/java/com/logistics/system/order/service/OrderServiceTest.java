package com.logistics.system.order.service;

import com.logistics.system.common.event.OrderCreatedEvent;
import com.logistics.system.common.exception.BusinessException;
import com.logistics.system.common.exception.ResourceNotFoundException;
import com.logistics.system.order.domain.Customer;
import com.logistics.system.order.domain.Order;
import com.logistics.system.order.domain.OrderStatus;
import com.logistics.system.order.dto.OrderDto;
import com.logistics.system.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CustomerService customerService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private OrderService orderService;

    private Customer customer;
    private Order order;

    @BeforeEach
    void setUp() {
        customer = new Customer();
        customer.setName("John Doe");
        customer.setEmail("john@example.com");

        order = new Order();
        order.setCustomer(customer);
        order.setStatus(OrderStatus.CREATED);
    }

    @Test
    void findAll_returnsAllOrders() {
        org.springframework.data.domain.PageImpl<Order> page =
                new org.springframework.data.domain.PageImpl<>(List.of(order));
        when(orderRepository.findAll(any(org.springframework.data.domain.Pageable.class))).thenReturn(page);

        org.springframework.data.domain.Page<OrderDto.Response> result =
                orderService.findAll(org.springframework.data.domain.Pageable.unpaged());

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).status()).isEqualTo(OrderStatus.CREATED);
    }

    @Test
    void findById_existingId_returnsOrder() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        OrderDto.Response result = orderService.findById(1L);

        assertThat(result.status()).isEqualTo(OrderStatus.CREATED);
        assertThat(result.customerName()).isEqualTo("John Doe");
    }

    @Test
    void findById_nonExistingId_throwsResourceNotFoundException() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void create_validRequest_returnsCreatedOrder() {
        when(customerService.getCustomer(1L)).thenReturn(customer);
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        OrderDto.CreateRequest request = new OrderDto.CreateRequest(
                1L,
                List.of(new OrderDto.ItemRequest("SKU-001", "Product A", 2, BigDecimal.valueOf(10.00))),
                "Test order"
        );

        OrderDto.Response result = orderService.create(request);

        assertThat(result.status()).isEqualTo(OrderStatus.CREATED);
        verify(orderRepository).save(any(Order.class));
        verify(eventPublisher).publishEvent(any(OrderCreatedEvent.class));
    }

    @Test
    void create_emptyItems_throwsBusinessException() {
        OrderDto.CreateRequest request = new OrderDto.CreateRequest(1L, List.of(), null);

        assertThatThrownBy(() -> orderService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("no items");

        verify(orderRepository, never()).save(any());
    }

    @Test
    void create_customerNotFound_throwsResourceNotFoundException() {
        when(customerService.getCustomer(99L)).thenThrow(new ResourceNotFoundException("Customer", 99L));

        OrderDto.CreateRequest request = new OrderDto.CreateRequest(
                99L,
                List.of(new OrderDto.ItemRequest("SKU-001", "Product A", 1, BigDecimal.valueOf(10.00))),
                null
        );

        assertThatThrownBy(() -> orderService.create(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void updateStatus_validTransition_updatesStatus() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        OrderDto.Response result = orderService.updateStatus(1L,
                new OrderDto.UpdateStatusRequest(OrderStatus.CONFIRMED));

        assertThat(result.status()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void updateStatus_cancelledOrder_throwsBusinessException() {
        order.setStatus(OrderStatus.CANCELLED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateStatus(1L,
                new OrderDto.UpdateStatusRequest(OrderStatus.CONFIRMED)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("CANCELLED");
    }

    @Test
    void updateStatus_deliveredOrder_throwsBusinessException() {
        order.setStatus(OrderStatus.DELIVERED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateStatus(1L,
                new OrderDto.UpdateStatusRequest(OrderStatus.SHIPPED)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("DELIVERED");
    }

    @Test
    void updateStatus_nonExistingOrder_throwsResourceNotFoundException() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.updateStatus(99L,
                new OrderDto.UpdateStatusRequest(OrderStatus.CONFIRMED)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void markStockReserved_updatesStatusToStockReserved() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        orderService.markStockReserved(1L);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.STOCK_RESERVED);
        verify(orderRepository).save(order);
    }

    @Test
    void markFailed_updatesStatusAndSetsReason() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        orderService.markFailed(1L, "Insufficient stock for SKU-001");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.FAILED);
        assertThat(order.getFailureReason()).isEqualTo("Insufficient stock for SKU-001");
        verify(orderRepository).save(order);
    }

    @Test
    void markReadyToShip_updatesStatusToReadyToShip() {
        order.setStatus(OrderStatus.STOCK_RESERVED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        orderService.markReadyToShip(1L);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.READY_TO_SHIP);
        verify(orderRepository).save(order);
    }

    @Test
    void updateStatus_invalidTransition_throwsBusinessException() {
        order.setStatus(OrderStatus.CONFIRMED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateStatus(1L,
                new OrderDto.UpdateStatusRequest(OrderStatus.DELIVERED)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("CONFIRMED");
    }

    @Test
    void updateStatus_failedOrder_throwsBusinessException() {
        order.setStatus(OrderStatus.FAILED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateStatus(1L,
                new OrderDto.UpdateStatusRequest(OrderStatus.CONFIRMED)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("FAILED");
    }
}
