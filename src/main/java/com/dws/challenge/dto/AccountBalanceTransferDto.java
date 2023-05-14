package com.dws.challenge.dto;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class AccountBalanceTransferDto {
    @NotNull
    private String senderAccountId;
    @NotNull
    private String receiverAccountId;

    @NotNull
    @Min(0)
    private BigDecimal amount;
}
