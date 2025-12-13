// file:
// /Users/carl/workspace/backend/Infrastructure/infrastructure-component/pulsar/src/test/java/org/carl/infrastructure/pulsar/model/TestOrderItem.java
package org.carl.infrastructure.pulsar.model;

import java.math.BigDecimal;

public class TestOrderItem {
    private Long itemId;
    private Long orderId;
    private String productName;
    private Integer quantity;
    private BigDecimal price;
    private BigDecimal subtotal;

    public TestOrderItem() {}

    public TestOrderItem(
            Long itemId,
            Long orderId,
            String productName,
            Integer quantity,
            BigDecimal price,
            BigDecimal subtotal) {
        this.itemId = itemId;
        this.orderId = orderId;
        this.productName = productName;
        this.quantity = quantity;
        this.price = price;
        this.subtotal = subtotal;
    }

    // Getters and Setters
    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TestOrderItem that = (TestOrderItem) o;

        if (itemId != null ? !itemId.equals(that.itemId) : that.itemId != null) return false;
        if (orderId != null ? !orderId.equals(that.orderId) : that.orderId != null) return false;
        if (productName != null ? !productName.equals(that.productName) : that.productName != null)
            return false;
        if (quantity != null ? !quantity.equals(that.quantity) : that.quantity != null)
            return false;
        if (price != null ? !price.equals(that.price) : that.price != null) return false;
        return subtotal != null ? subtotal.equals(that.subtotal) : that.subtotal == null;
    }

    @Override
    public int hashCode() {
        int result = itemId != null ? itemId.hashCode() : 0;
        result = 31 * result + (orderId != null ? orderId.hashCode() : 0);
        result = 31 * result + (productName != null ? productName.hashCode() : 0);
        result = 31 * result + (quantity != null ? quantity.hashCode() : 0);
        result = 31 * result + (price != null ? price.hashCode() : 0);
        result = 31 * result + (subtotal != null ? subtotal.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "TestOrderItem{"
                + "itemId="
                + itemId
                + ", orderId="
                + orderId
                + ", productName='"
                + productName
                + '\''
                + ", quantity="
                + quantity
                + ", price="
                + price
                + ", subtotal="
                + subtotal
                + '}';
    }
}
