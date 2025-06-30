package org.carl.infrastructure.broadcast;

import java.math.BigDecimal;

public class TransferEvent {
    BigDecimal amount;
    String from;
    String to;

    public BigDecimal getAmount() {
        return amount;
    }

    public TransferEvent setAmount(BigDecimal amount) {
        this.amount = amount;
        return this;
    }

    public String getFrom() {
        return from;
    }

    public TransferEvent setFrom(String from) {
        this.from = from;
        return this;
    }

    public String getTo() {
        return to;
    }

    public TransferEvent setTo(String to) {
        this.to = to;
        return this;
    }

    @Override
    public String toString() {
        return "{"
                + "        \"amount\":\""
                + amount
                + "\""
                + ",         \"from\":\""
                + from
                + "\""
                + ",         \"to\":\""
                + to
                + "\""
                + "}";
    }
}
