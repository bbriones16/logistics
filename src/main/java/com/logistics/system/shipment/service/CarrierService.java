package com.logistics.system.shipment.service;

import com.logistics.system.common.exception.BusinessException;
import com.logistics.system.common.exception.ResourceNotFoundException;
import com.logistics.system.shipment.domain.Carrier;
import com.logistics.system.shipment.domain.ShipmentMedium;
import com.logistics.system.shipment.dto.CarrierDto;
import com.logistics.system.shipment.repository.CarrierRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CarrierService {

    private static final Logger log = LoggerFactory.getLogger(CarrierService.class);

    private final CarrierRepository carrierRepository;

    public CarrierService(CarrierRepository carrierRepository) {
        this.carrierRepository = carrierRepository;
    }

    public List<CarrierDto.Response> findAll() {
        return carrierRepository.findByActiveTrue().stream().map(this::toResponse).toList();
    }

    public List<CarrierDto.Response> findByMedium(ShipmentMedium medium) {
        return carrierRepository.findByMediumAndActiveTrue(medium).stream().map(this::toResponse).toList();
    }

    public CarrierDto.Response findById(Long id) {
        return toResponse(getCarrier(id));
    }

    @Transactional
    public CarrierDto.Response create(CarrierDto.CreateRequest request) {
        if (carrierRepository.existsByCode(request.code())) {
            throw new BusinessException("CARRIER_DUPLICATE_CODE", "Carrier with code already exists: " + request.code());
        }
        Carrier carrier = new Carrier();
        carrier.setCode(request.code());
        carrier.setName(request.name());
        carrier.setMedium(request.medium());
        carrier.setCompanyOwned(request.companyOwned());
        carrier.setContactName(request.contactName());
        carrier.setContactPhone(request.contactPhone());
        Carrier saved = carrierRepository.save(carrier);
        log.info("Carrier created: id={}, code={}, companyOwned={}", saved.getId(), saved.getCode(), saved.isCompanyOwned());
        return toResponse(saved);
    }

    public Carrier getCarrier(Long id) {
        return carrierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Carrier", id));
    }

    private CarrierDto.Response toResponse(Carrier c) {
        return new CarrierDto.Response(c.getId(), c.getCode(), c.getName(), c.getMedium(),
                c.isCompanyOwned(), c.getContactName(), c.getContactPhone(), c.isActive());
    }
}
