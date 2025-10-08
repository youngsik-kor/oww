package oww.banking.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserVO {
	private String userEmail;
	 private String userEmailHash;
	private String name;
	private int userNo;
    private LocalDateTime createdAt;   
    private LocalDateTime updatedAt;   
    private boolean isActive;          
    private String providerId;        
    private String provider;          
    private String role; 
	
    public String getUserEmailHash() { return userEmailHash; }
    public void setUserEmailHash(String userEmailHash) { this.userEmailHash = userEmailHash; }
}


