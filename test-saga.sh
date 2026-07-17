#!/bin/bash
# Quick smoke test for the order saga.
# Requires: docker compose up --build (all services healthy) and `jq` for pretty output.

BASE_ORDER=http://localhost:8081
BASE_INVENTORY=http://localhost:8082
BASE_PAYMENT=http://localhost:8083

echo "== 1. Current inventory levels =="
curl -s "$BASE_INVENTORY/api/inventory" | jq

echo ""
echo "== 2. Placing an order that should SUCCEED (PROD-001 has 50 in stock) =="
ORDER_ID=$(curl -s -X POST "$BASE_ORDER/api/orders" \
  -H "Content-Type: application/json" \
  -d '{"customerId":"CUST-100","productId":"PROD-001","quantity":2,"totalAmount":49.98}' | jq -r '.id')
echo "Created order: $ORDER_ID"

echo ""
echo "== 3. Placing an order that should FAIL inventory (PROD-003 only has 5 in stock) =="
FAILING_ORDER_ID=$(curl -s -X POST "$BASE_ORDER/api/orders" \
  -H "Content-Type: application/json" \
  -d '{"customerId":"CUST-101","productId":"PROD-003","quantity":999,"totalAmount":9990.00}' | jq -r '.id')
echo "Created order: $FAILING_ORDER_ID"

echo ""
echo "Waiting 5s for the saga (order -> inventory -> payment -> order) to settle..."
sleep 5

echo ""
echo "== 4. Order statuses (watch status move PENDING -> INVENTORY_RESERVED -> CONFIRMED, or -> CANCELLED) =="
echo "Order $ORDER_ID:"
curl -s "$BASE_ORDER/api/orders/$ORDER_ID" | jq
echo "Order $FAILING_ORDER_ID:"
curl -s "$BASE_ORDER/api/orders/$FAILING_ORDER_ID" | jq

echo ""
echo "== 5. Inventory after processing =="
curl -s "$BASE_INVENTORY/api/inventory" | jq

echo ""
echo "== 6. Payment transactions =="
curl -s "$BASE_PAYMENT/api/payments" | jq

echo ""
echo "Tail the logs to see the full event trail:"
echo "  docker compose logs -f order-service inventory-service payment-service notification-service"
echo ""
echo "Browse topics/messages visually at http://localhost:8090 (Kafka UI)"
