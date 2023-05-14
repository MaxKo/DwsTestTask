package com.dws.challenge.web;

import com.dws.challenge.dto.AccountBalanceTransferDto;
import com.dws.challenge.service.AccountBalanceTransferService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/v1/accounts/balanceTransfer")
@Slf4j
@AllArgsConstructor
public class AccountBalanceTransferController {
    private final AccountBalanceTransferService transferService;

    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> transfer(@Valid @RequestBody AccountBalanceTransferDto amount) {
        log.info("Transfer {} ", amount);

        this.transferService.transferThreadSafe(amount);

        return new ResponseEntity<>(HttpStatus.OK);
    }
}
