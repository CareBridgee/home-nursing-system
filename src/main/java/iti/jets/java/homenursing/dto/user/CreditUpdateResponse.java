package iti.jets.java.homenursing.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
@AllArgsConstructor
public class CreditUpdateResponse {

    private BigDecimal credit;
}