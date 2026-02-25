package com.taobao.logistics.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "login")
public class LoginConfig {
    
    private boolean enabled = true;
    private String username = "admin";
    private String password = "admin123";
    private int sessionTimeout = 30;
    
    public List<UserAccount> getUserAccounts() {
        List<UserAccount> accounts = new ArrayList<>();
        String[] usernames = username.split(",");
        String[] passwords = password.split(",");
        
        int length = Math.min(usernames.length, passwords.length);
        for (int i = 0; i < length; i++) {
            accounts.add(new UserAccount(usernames[i].trim(), passwords[i].trim()));
        }
        
        return accounts;
    }
    
    public boolean validateUser(String username, String password) {
        List<UserAccount> accounts = getUserAccounts();
        for (UserAccount account : accounts) {
            if (account.getUsername().equals(username) && account.getPassword().equals(password)) {
                return true;
            }
        }
        return false;
    }
    
    @Data
    public static class UserAccount {
        private String username;
        private String password;
        
        public UserAccount(String username, String password) {
            this.username = username;
            this.password = password;
        }
    }
}
