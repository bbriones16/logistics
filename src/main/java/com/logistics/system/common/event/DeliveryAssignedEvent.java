package com.logistics.system.common.event;

public record DeliveryAssignedEvent(Long deliveryId, Long shipmentId) {}
