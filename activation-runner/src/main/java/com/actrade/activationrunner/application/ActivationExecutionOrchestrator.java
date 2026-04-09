package com.actrade.activationrunner.application;

import com.actrade.activationrunner.mq.ActivationTaskDispatchMessage;
import com.actrade.activationrunner.mq.MessageHandleResult;

public interface ActivationExecutionOrchestrator {

    MessageHandleResult handle(ActivationTaskDispatchMessage message);
}