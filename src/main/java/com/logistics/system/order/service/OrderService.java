package com.logistics.system.order.service;

import com.logistics.system.common.event.OrderCreatedEvent;
import com.logistics.system.common.exception.BusinessException;
import com.logistics.system.common.exception.ResourceNotFoundException;
import com.logistics.system.order.domain.Order;
import com.logistics.system.order.domain.OrderItem;
import com.logistics.system.order.domain.OrderStatus;
import com.logistics.system.order.dto.OrderDto;
import com.logistics.system.order.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private static final Map<OrderStatus, Set<OrderStatus>> VALID_TRANSITIONS = Map.of(
            OrderStatus.CREATED,        EnumSet.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED, OrderStatus.STOCK_RESERVED, OrderStatus.FAILED),
            OrderStatus.CONFIRMED,      EnumSet.of(OrderStatus.STOCK_RESERVED, OrderStatus.CANCELLED),
            OrderStatus.STOCK_RESERVED, EnumSet.of(OrderStatus.PROCESSING, OrderStatus.CANCELLED, OrderStatus.READY_TO_SHIP),
            OrderStatus.PROCESSING,     EnumSet.of(OrderStatus.READY_TO_SHIP, OrderStatus.FAILED),
            OrderStatus.READY_TO_SHIP,  EnumSet.of(OrderStatus.SHIPPED, OrderStatus.CANCELLED),
            OrderStatus.SHIPPED,        EnumSet.of(OrderStatus.DELIVERED, OrderStatus.RETURNED, OrderStatus.FAILED)
    );

    private final OrderRepository orderRepository;
    private final CustomerService customerService;
    private final ApplicationEventPublisher eventPublisher;

    public OrderService(OrderRepository orderRepository, CustomerService customerService,
                        ApplicationEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.customerService = customerService;
        this.eventPublisher = eventPublisher;
    }

    public Page<OrderDto.Response> findAll(Pageable pageable) {
        return orderRepository.findAll(pageable).map(this::toResponse);
    }

    public OrderDto.Response findById(Long id) {
        return toResponse(getOrder(id));
    }

    @Transactional
    public OrderDto.Response create(OrderDto.CreateRequest request) {
        if (request.items().isEmpty()) {
            throw new BusinessException("ORDER_VALIDATION_ERROR", "Order contains no items");
        }
        Order order = new Order();
        order.setCustomer(customerService.getCustomer(request.customerId()));
        order.setNotes(request.notes());
        request.items().forEach(itemReq -> {
            OrderItem item = new OrderItem();
            item.setSku(itemReq.sku());
            item.setProductName(itemReq.productName());
            item.setQuantity(itemReq.quantity());
            item.setUnitPrice(itemReq.unitPrice());
            order.addItem(item);
        });
        Order saved = orderRepository.save(order);
        log.info("Order created: id={}, customerId={}, items={}", saved.getId(), request.customerId(), request.items().size());
        eventPublisher.publishEvent(toOrderCreatedEvent(saved));
        return toResponse(saved);
    }

    @Transactional
    public OrderDto.Response updateStatus(Long id, OrderDto.UpdateStatusRequest request) {
        Order order = getOrder(id);
        OrderStatus current = order.getStatus();
        OrderStatus next = request.status();
        validateStatusTransition(current, next);
        order.setStatus(next);
        Order saved = orderRepository.save(order);
        log.info("Order status updated: id={}, from={}, to={}", id, current, next);
        return toResponse(saved);
    }

    @Transactional
    public void markReadyToShip(Long orderId) {
        Order order = getOrder(orderId);
        order.setStatus(OrderStatus.READY_TO_SHIP);
        orderRepository.save(order);
        log.info("Order ready to ship: id={}", orderId);
    }

    @Transactional
    public void markStockReserved(Long orderId) {
        Order order = getOrder(orderId);
        order.setStatus(OrderStatus.STOCK_RESERVED);
        orderRepository.save(order);
        log.info("Order stock reserved: id={}", orderId);
    }

    @Transactional
    public void markFailed(Long orderId, String reason) {
        Order order = getOrder(orderId);
        order.setStatus(OrderStatus.FAILED);
        order.setFailureReason(reason);
        orderRepository.save(order);
        log.info("Order failed: id={}, reason={}", orderId, reason);
    }

    private void validateStatusTransition(OrderStatus current, OrderStatus next) {
        Set<OrderStatus> allowed = VALID_TRANSITIONS.getOrDefault(current, EnumSet.noneOf(OrderStatus.class));
        if (!allowed.contains(next)) {
            throw new BusinessException("ORDER_STATUS_ERROR",
                    "Cannot transition order from " + current + " to " + next);
        }
    }

    private OrderCreatedEvent toOrderCreatedEvent(Order order) {
        List<OrderCreatedEvent.OrderItem> items = order.getItems().stream()
                .map(i -> new OrderCreatedEvent.OrderItem(i.getSku(), i.getProductName(), i.getQuantity(), i.getUnitPrice()))
                .toList();
        return new OrderCreatedEvent(order.getId(), order.getCustomer().getId(), items);
    }

    private Order getOrder(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", id));
    }

    private OrderDto.Response toResponse(Order o) {
        List<OrderDto.ItemResponse> items = o.getItems().stream()
                .map(i -> new OrderDto.ItemResponse(i.getId(), i.getSku(), i.getProductName(), i.getQuantity(), i.getUnitPrice()))
                .toList();
        return new OrderDto.Response(
                o.getId(),
                o.getCustomer().getId(),
                o.getCustomer().getName(),
                o.getStatus(),
                o.getCreatedAt(),
                o.getNotes(),
                o.getFailureReason(),
                items
        );
    }
}
