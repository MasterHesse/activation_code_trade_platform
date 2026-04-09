package com.actrade.activationrunner.mq;

public record MessageHandleResult(
        Action action,
        String reason
) {

    public enum Action {
        ACK,
        REQUEUE,
        REJECT
    }

    public static MessageHandleResult ack(String reason) {
        return new MessageHandleResult(Action.ACK, reason);
    }

    public static MessageHandleResult requeue(String reason) {
        return new MessageHandleResult(Action.REQUEUE, reason);
    }

    public static MessageHandleResult reject(String reason) {
        return new MessageHandleResult(Action.REJECT, reason);
    }
}