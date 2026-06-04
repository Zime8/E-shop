package org.example.control;

import org.example.dao.UserRepository;
import org.example.models.RegisterValidationResult;

public class RegisterAppController {
    private final UserRepository userRepository;

    public RegisterAppController(UserRepository userRepository){
        this.userRepository = userRepository;
    }

    public RegisterValidationResult validateAndRegister(String username, String password,
                                                        String confirmPassword, String email,
                                                        String phone, String role) {

        username = safeTrim(username);
        password = safeTrim(password);
        confirmPassword = safeTrim(confirmPassword);
        email = safeTrim(email);
        phone = safeTrim(phone);
        role = safeTrim(role);

        // Validazioni
        if (isAnyFieldEmpty(username, password, confirmPassword, email, phone, role)) {
            return RegisterValidationResult.EMPTY_FIELDS;
        }

        if (!password.equals(confirmPassword)) {
            return RegisterValidationResult.PASSWORD_MISMATCH;
        }

        if (!isValidEmail(email)) {
            return RegisterValidationResult.INVALID_EMAIL;
        }

        if (!isValidPhone(phone)) {
            return RegisterValidationResult.INVALID_PHONE;
        }

        if (userRepository.isUsernameTaken(username)) {
            return RegisterValidationResult.USERNAME_TAKEN;
        }

        if (userRepository.isEmailTaken(email)) {
            return RegisterValidationResult.EMAIL_TAKEN;
        }

        boolean success = userRepository.registerUser(username, password, role, email, phone);
        return success ? RegisterValidationResult.SUCCESS : RegisterValidationResult.DATABASE_ERROR;
    }

    private boolean isAnyFieldEmpty(String... fields) {
        for (String field : fields) {
            if (field == null || field.isBlank()) return true;
        }
        return false;
    }

    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    }

    private boolean isValidPhone(String phone) {
        return phone != null && phone.matches("^\\d{7,12}$");
    }

    private String safeTrim(String value) {
        return value == null ? null : value.trim();
    }
}
