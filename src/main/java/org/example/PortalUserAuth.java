package org.example;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

import org.json.*;

public class PortalUserAuth {

    private static final String BASE_URL = "https://api.cloudframework.io/erp/portal-users/2.0/cloudframework/web-oauth/signin";

    public static void main(String[] args) {
        try {
            // Llamada directa al login
            String webToken = loginWithQueryParams("testclient", "Fran", "qwerty1234");
            System.out.println("Token obtenido: " + webToken);
        } catch (IOException e) {
            System.err.println("Error de conexión: " + e.getMessage());
        } catch (AuthAPIException e) {
            System.err.println("Error en la API (" + e.getStatusCode() + "): " + e.getMessage());
            System.err.println("Detalles: " + e.getResponseData());
        }
    }

    public static String loginWithQueryParams(String clientId, String username, String password)
            throws IOException, AuthAPIException {

        // Construir la URL con los parámetros como en Postman
        String queryUrl = BASE_URL +
                "?ClientId=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8) +
                "&username=" + URLEncoder.encode(username, StandardCharsets.UTF_8) +
                "&userpassword=" + URLEncoder.encode(password, StandardCharsets.UTF_8);

        URL url = new URL(queryUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("referer", "https://cloudframework.portaluser.cloud/");
        conn.setDoOutput(true);

        // Enviar cuerpo vacío
        conn.getOutputStream().close();

        return processResponse(conn);
    }

    private static String processResponse(HttpURLConnection conn)
            throws IOException, AuthAPIException {

        int status = conn.getResponseCode();
        String responseData;

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(status >= 400 ? conn.getErrorStream() : conn.getInputStream()))) {

            StringBuilder response = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
            responseData = response.toString();
        }

        if (status >= 400) {
            throw new AuthAPIException(status, "Error en la API", responseData);
        }

        // ✅ Extraer el web_token desde data.web_token
        JSONObject json = new JSONObject(responseData);
        if (!json.has("data") || !json.getJSONObject("data").has("web_token")) {
            throw new AuthAPIException(status, "No se encontró el campo 'data.web_token'", responseData);
        }

        return json.getJSONObject("data").getString("web_token");
    }

    // Excepción personalizada
    public static class AuthAPIException extends Exception {
        private final int statusCode;
        private final String responseData;

        public AuthAPIException(int statusCode, String message, String responseData) {
            super(message);
            this.statusCode = statusCode;
            this.responseData = responseData;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public String getResponseData() {
            return responseData;
        }
    }
}
