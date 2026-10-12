package com.logistics.system.order.service;

import com.logistics.system.common.exception.BusinessException;
import com.logistics.system.common.exception.ResourceNotFoundException;
import com.logistics.system.order.domain.Customer;
import com.logistics.system.order.dto.CustomerDto;
import com.logistics.system.order.repository.CustomerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CustomerService {

    private static final Logger log = LoggerFactory.getLogger(CustomerService.class);

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public List<CustomerDto.Response> findAll() {
        return customerRepository.findAll().stream().map(this::toResponse).toList();
    }

    public CustomerDto.Response findById(Long id) {
        return toResponse(getCustomer(id));
    }

    @Transactional
    public CustomerDto.Response create(CustomerDto.CreateRequest request) {
        if (customerRepository.existsByEmail(request.email())) {
            throw new BusinessException("CUSTOMER_EMAIL_EXISTS", "A customer with this email already exists");
        }
        Customer customer = new Customer();
        customer.setName(request.name());
        customer.setEmail(request.email());
        customer.setPhone(request.phone());
        customer.setAddress(request.address());
        CustomerDto.Response response = toResponse(customerRepository.save(customer));
        log.info("Customer created: id={}, email={}", response.id(), response.email());
        return response;
    }

    Customer getCustomer(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", id));
    }

    private CustomerDto.Response toResponse(Customer c) {
        return new CustomerDto.Response(c.getId(), c.getName(), c.getEmail(), c.getPhone(), c.getAddress());
    }
}
