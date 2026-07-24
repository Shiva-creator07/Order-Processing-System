package com.orderprocessing.inventory.controller;

import com.orderprocessing.inventory.model.InventoryItem;
import com.orderprocessing.inventory.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Read-only endpoint purely for demoing/verifying stock levels move as orders flow through Kafka.
 */
@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryRepository inventoryRepository;

    @GetMapping
    public List<InventoryItem> getAll() {
        return inventoryRepository.findAll();
    }
}
