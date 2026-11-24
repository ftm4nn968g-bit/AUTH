package kz.onlineshop.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Тіркелу формасы. Тексеру (validation) серверде жүреді — браузерге сенбейміз. */
public class RegisterForm {

    @NotBlank(message = "Email енгізіңіз")
    @Email(message = "Email форматы қате")
    @Size(max = 254, message = "Email тым ұзын")
    private String email;

    @NotBlank(message = "Құпия сөз енгізіңіз")
    @Size(max = 128, message = "Құпия сөз тым ұзын")
    private String password;

    @NotBlank(message = "Құпия сөзді қайталаңыз")
    private String confirmPassword;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
}
