package org.example.controllers.app;

import org.example.dao.UserDAO;
import org.example.models.RegisterValidationResult;

public class RegisterAppController {
    private final UserDAO userDao = new UserDAO();

    public RegisterValidationResult validateAndRegister(String username, String password,
                                                        String confirmPassword, String email,
                                                        String phone, String role) {

        username = username.trim();
        email = email.trim();
        phone = phone.trim();

        // Validazioni
        if (isAnyFieldEmpty(username, password, confirmPassword, email, phone)) {
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

        if (userDao.isUsernameTaken(username)) {
            return RegisterValidationResult.USERNAME_TAKEN;
        }

        if (userDao.isEmailTaken(email)) {
            return RegisterValidationResult.EMAIL_TAKEN;
        }

        boolean success = userDao.registerUser(username, password, role, email, phone);
        return success ? RegisterValidationResult.SUCCESS : RegisterValidationResult.DATABASE_ERROR;
    }

    private boolean isAnyFieldEmpty(String... fields) {
        for (String field : fields) {
            if (field == null || field.trim().isEmpty()) return true;
        }
        return false;
    }

    private boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    }

    private boolean isValidPhone(String phone) {
        return phone.matches("^\\d{7,12}$");
    }
}
