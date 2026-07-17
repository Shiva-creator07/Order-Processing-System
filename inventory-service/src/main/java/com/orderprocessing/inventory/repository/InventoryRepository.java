package com.orderprocessing.inventory.repository;

import com.orderprocessing.inventory.model.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryRepository extends JpaRepository<InventoryItem, String> {
}
