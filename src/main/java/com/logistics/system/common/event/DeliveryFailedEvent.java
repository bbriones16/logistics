package com.logistics.system.common.event;

public record DeliveryFailedEvent(Long deliveryId, Long shipmentId, String reason) {}
