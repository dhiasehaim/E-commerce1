package com.example.inventory.controller;

import com.example.inventory.model.InventoryItem;
import com.example.inventory.service.InventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/inventory")
public class InventoryController {

    @Autowired
    private InventoryService service;

    @GetMapping
    public List<InventoryItem> getAll() {
        return service.getAllItems();
    }

    @GetMapping("/{id}")
    public InventoryItem getById(@PathVariable Long id) {
        return service.getItem(id);
    }

    @PostMapping
    public InventoryItem create(@RequestBody InventoryItem item) {
        return service.createItem(item);
    }

    @PutMapping("/{id}")
    public InventoryItem update(@PathVariable Long id, @RequestBody InventoryItem item) {
        return service.updateItem(id, item);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.deleteItem(id);
    }
}
