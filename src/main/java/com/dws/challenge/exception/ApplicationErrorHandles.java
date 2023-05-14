package com.dws.challenge.exception;


import com.dws.challenge.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class ApplicationErrorHandles {

    private final NotificationService notificationService;

    @ExceptionHandler(value = {InsufficientBalanceAmountException.class})
    public ResponseEntity<HttpEntity> handleBindException(InsufficientBalanceAmountException exception) {

        notificationService.notifyAboutTransfer(exception.getReceiverAccount(), exception.getReceiverMessage());
        notificationService.notifyAboutTransfer(exception.getSenderAccount(), exception.getSenderMessage());

        return new ResponseEntity<>(BAD_REQUEST);
    }
}
