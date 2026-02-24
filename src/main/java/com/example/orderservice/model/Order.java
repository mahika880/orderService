package com.example.orderservice.model;


import com.fasterxml.jackson.annotation.JsonTypeId;
import jakarta.persistence.*;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@Entity
@Table(name = "orders",
    indexes = {
        @Index(name = "idx_item_name", columnList ="itemName" )
    })
public class Order {

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    @Id
    @Column(name = "order_id")
    private String orderId;
    private  String itemName;
    private int quantity;

    @Version
    private Long version;

    @Column(unique = true)
    private String idempotencyKey;
    protected Order(){

    }

    public Order(String orderId, String itemName, int quantity, String idempotencyKey) {
        this.orderId = orderId;
        this.itemName = itemName;
        this.quantity = quantity;
        this.idempotencyKey=idempotencyKey;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getItemName() {
        return itemName;
    }

    public int getQuantity() {
        return quantity;
    }


}
