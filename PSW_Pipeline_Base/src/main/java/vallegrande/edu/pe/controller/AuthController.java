package vallegrande.edu.pe.controller;


import org.springframework.web.bind.annotation.*;
import vallegrande.edu.pe.model.LoginRequest;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/login")
public class AuthController {

    @PostMapping
    public Map<String, Object> login(@RequestBody LoginRequest request) {

        Map<String, Object> response = new HashMap<>();

        if ("admin".equals(request.getUsername())
                && "123456".equals(request.getPassword())) {

            response.put("success", true);
            response.put("message", "Login correcto");
            response.put("token", "ABC123XYZ");

        } else {

            response.put("success", false);
            response.put("message", "Credenciales incorrectas");
        }

        return response;
    }
}