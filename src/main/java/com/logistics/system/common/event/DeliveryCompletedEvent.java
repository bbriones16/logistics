package com.logistics.system.common.event;

public record DeliveryCompletedEvent(Long deliveryId, Long shipmentId, String proofOfDelivery) {}
