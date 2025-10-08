package oww.banking.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransferHistoryVO {
    
    @JsonProperty("txId")
    private int txId;
    
    @JsonProperty("accountId")
    private int accountId;
    
    @JsonProperty("txType") 
    private String txType; // "TRANSFER_OUT", "TRANSFER_IN"
    
    @JsonProperty("amount")
    private BigDecimal amount;
    
    @JsonProperty("memo")
    private String memo;
    
    @JsonProperty("txDate")
    private LocalDateTime txDate;
    
    @JsonProperty("transferId")  
    private Integer transferId;
    public Integer getTransferId() {
        return transferId;
    }

    public void setTransferId(Integer transferId) {
        this.transferId = transferId;
    }

    // 추가 정보 (JSON에 없는 필드들)
    private String accountNumber;
    private String otherAccountNumber; // 상대방 계좌번호
    private String otherUserName; // 상대방 이름
}