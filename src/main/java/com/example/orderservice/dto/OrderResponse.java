package com.example.orderservice.dto;

public class OrderResponse {

    public String getOrderId() {
        return orderId;
    }

    public String getItemName() {
        return itemName;
    }

    public int getQuantity() {
        return quantity;
    }

    private String orderId;
    private String itemName;
    private int quantity;

    public OrderResponse(String orderId, String itemName, int quantity){

        this.orderId = orderId;
        this.itemName = itemName;
        this.quantity = quantity;
    }

}
