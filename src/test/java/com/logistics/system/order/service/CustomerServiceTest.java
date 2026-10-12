package com.logistics.system.order.service;

import com.logistics.system.common.exception.BusinessException;
import com.logistics.system.common.exception.ResourceNotFoundException;
import com.logistics.system.order.domain.Customer;
import com.logistics.system.order.dto.CustomerDto;
import com.logistics.system.order.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerService customerService;

    private Customer customer;

    @BeforeEach
    void setUp() {
        customer = new Customer();
        customer.setName("John Doe");
        customer.setEmail("john@example.com");
        customer.setPhone("1234567890");
        customer.setAddress("123 Main St");
    }

    @Test
    void findAll_returnsAllCustomers() {
        when(customerRepository.findAll()).thenReturn(List.of(customer));

        List<CustomerDto.Response> result = customerService.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).email()).isEqualTo("john@example.com");
    }

    @Test
    void findById_existingId_returnsCustomer() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));

        CustomerDto.Response result = customerService.findById(1L);

        assertThat(result.email()).isEqualTo("john@example.com");
        assertThat(result.name()).isEqualTo("John Doe");
    }

    @Test
    void findById_nonExistingId_throwsResourceNotFoundException() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void create_validRequest_returnsCreatedCustomer() {
        when(customerRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenReturn(customer);

        CustomerDto.CreateRequest request = new CustomerDto.CreateRequest(
                "John Doe", "john@example.com", "1234567890", "123 Main St");

        CustomerDto.Response result = customerService.create(request);

        assertThat(result.email()).isEqualTo("john@example.com");
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    void create_duplicateEmail_throwsBusinessException() {
        when(customerRepository.existsByEmail("john@example.com")).thenReturn(true);

        CustomerDto.CreateRequest request = new CustomerDto.CreateRequest(
                "John Doe", "john@example.com", null, null);

        assertThatThrownBy(() -> customerService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already exists");

        verify(customerRepository, never()).save(any());
    }
}
