// file:
// /Users/carl/workspace/backend/Infrastructure/infrastructure-component/pulsar/src/test/java/org/carl/infrastructure/pulsar/model/TestOrder.java
package org.carl.infrastructure.pulsar.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class TestOrder {
    private Long orderId;
    private String orderNumber;
    private Long userId;
    private BigDecimal totalAmount;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private List<TestOrderItem> items;

    public TestOrder() {}

    public TestOrder(
            Long orderId,
            String orderNumber,
            Long userId,
            BigDecimal totalAmount,
            String status,
            LocalDateTime createTime,
            LocalDateTime updateTime) {
        this.orderId = orderId;
        this.orderNumber = orderNumber;
        this.userId = userId;
        this.totalAmount = totalAmount;
        this.status = status;
        this.createTime = createTime;
        this.updateTime = updateTime;
    }

    // Getters and Setters
    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public List<TestOrderItem> getItems() {
        return items;
    }

    public void setItems(List<TestOrderItem> items) {
        this.items = items;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TestOrder testOrder = (TestOrder) o;

        if (orderId != null ? !orderId.equals(testOrder.orderId) : testOrder.orderId != null)
            return false;
        if (orderNumber != null
                ? !orderNumber.equals(testOrder.orderNumber)
                : testOrder.orderNumber != null) return false;
        if (userId != null ? !userId.equals(testOrder.userId) : testOrder.userId != null)
            return false;
        if (totalAmount != null
                ? !totalAmount.equals(testOrder.totalAmount)
                : testOrder.totalAmount != null) return false;
        if (status != null ? !status.equals(testOrder.status) : testOrder.status != null)
            return false;
        if (createTime != null
                ? !createTime.equals(testOrder.createTime)
                : testOrder.createTime != null) return false;
        return updateTime != null
                ? updateTime.equals(testOrder.updateTime)
                : testOrder.updateTime == null;
    }

    @Override
    public int hashCode() {
        int result = orderId != null ? orderId.hashCode() : 0;
        result = 31 * result + (orderNumber != null ? orderNumber.hashCode() : 0);
        result = 31 * result + (userId != null ? userId.hashCode() : 0);
        result = 31 * result + (totalAmount != null ? totalAmount.hashCode() : 0);
        result = 31 * result + (status != null ? status.hashCode() : 0);
        result = 31 * result + (createTime != null ? createTime.hashCode() : 0);
        result = 31 * result + (updateTime != null ? updateTime.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "TestOrder{"
                + "orderId="
                + orderId
                + ", orderNumber='"
                + orderNumber
                + '\''
                + ", userId="
                + userId
                + ", totalAmount="
                + totalAmount
                + ", status='"
                + status
                + '\''
                + ", createTime="
                + createTime
                + ", updateTime="
                + updateTime
                + '}';
    }
}
