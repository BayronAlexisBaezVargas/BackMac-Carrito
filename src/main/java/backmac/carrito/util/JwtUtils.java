package backmac.carrito.util;

import backmac.carrito.exception.UnauthorizedException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Base64;

public class JwtUtils {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static String extraerUsuarioId(String token) {
        try {
            // Remover "Bearer " si viene incluido
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            // Un JWT tiene 3 partes separadas por puntos: header.payload.signature
            String[] partes = token.split("\\.");
            if (partes.length < 2) {
                throw new UnauthorizedException("Token JWT inválido o malformado");
            }

            String base64Payload = partes[1];
            while (base64Payload.length() % 4 != 0) {
                base64Payload += "=";
            }
            String payload = new String(Base64.getUrlDecoder().decode(base64Payload));
            
            JsonNode jsonNode = objectMapper.readTree(payload);
            
            // Generalmente el ID del usuario viene en "sub" (subject) o "email", o "oid" (Azure AD)
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
            throw new UnauthorizedException("Error al decodificar el token de autenticación");
        }
    }
}
