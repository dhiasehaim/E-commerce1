package com.example.customer.service;

import com.example.customer.model.Customer;
import com.example.customer.repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomerService {

    @Autowired
    private CustomerRepository repository;

    public List<Customer> getAllCustomers() {
        return repository.findAll();
    }

    public Customer getCustomer(Long id) {
        return repository.findById(id).orElse(null);
    }

    public Customer createCustomer(Customer c) {
        return repository.save(c);
    }

    public Customer updateCustomer(Long id, Customer c) {
        c.setId(id);
        return repository.save(c);
    }

    public void deleteCustomer(Long id) {
        repository.deleteById(id);
    }
}
