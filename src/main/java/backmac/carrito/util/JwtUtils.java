package backmac.carrito.util;

import backmac.carrito.exception.UnauthorizedException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Base64;
import java.util.List;
import java.util.ArrayList;

public class JwtUtils {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static JsonNode decodificarPayload(String token) throws Exception {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        String[] partes = token.split("\\.");
        if (partes.length < 2) {
            throw new UnauthorizedException("Token JWT invalido o malformado");
        }
        String base64Payload = partes[1];
        while (base64Payload.length() % 4 != 0) {
            base64Payload += "=";
        }
        String payload = new String(Base64.getUrlDecoder().decode(base64Payload));
        System.out.println("DEBUG JWT Payload: " + payload);
        return objectMapper.readTree(payload);
    }

    public static String extraerUsuarioId(String token) {
        try {
            JsonNode jsonNode = decodificarPayload(token);
            
            if (jsonNode.has("sub")) {
                return jsonNode.get("sub").asText();
            } else if (jsonNode.has("email")) {
                return jsonNode.get("email").asText();
            } else if (jsonNode.has("preferred_username")) {
                return jsonNode.get("preferred_username").asText();
            } else if (jsonNode.has("oid")) {
                return jsonNode.get("oid").asText();
            }
            
            throw new UnauthorizedException("No se pudo encontrar el identificador del usuario en el token");
            
        } catch (UnauthorizedException e) {
            throw e;
        } catch (Exception e) {
            throw new UnauthorizedException("Error al decodificar el token de autenticacion");
        }
    }

    public static void validarRol(String token, String rolRequerido) {
        try {
            JsonNode jsonNode = decodificarPayload(token);
            List<String> roles = new ArrayList<>();
            
            // Azure AD usa "roles" o "scp" para los scopes
            if (jsonNode.has("roles")) {
                jsonNode.get("roles").forEach(rol -> roles.add(rol.asText()));
            } else if (jsonNode.has("scp")) {
                String[] scpRoles = jsonNode.get("scp").asText().split(" ");
                for (String r : scpRoles) roles.add(r);
            }
            
            // Verificacion estricta de rol
            boolean tieneRol = roles.stream().anyMatch(r -> r.contains(rolRequerido));
            if (!tieneRol) {
                System.out.println("Roles en el token: " + roles + " | Esperado: " + rolRequerido);
                throw new UnauthorizedException("Acceso denegado: Se requiere el rol " + rolRequerido);
            }
            
        } catch (UnauthorizedException e) {
            throw e;
        } catch (Exception e) {
            throw new UnauthorizedException("Error al validar roles del token de autenticacion");
        }
    }
}
