package com.petsalud.backendpetsalud.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.util.HtmlUtils;

import java.io.IOException;
import java.util.Set;

/**
 * Defensa contra XSS almacenado: todo texto que entra en JSON se "escapa" a HTML antes de
 * llegar a la base de datos. Ej: "<script>alert(1)</script>" se guarda como
 * "&lt;script&gt;alert(1)&lt;/script&gt;", que un navegador muestra como texto y NO ejecuta.
 * Se excluyen campos que no se muestran (password) o que tienen formato propio (email).
 */
@Configuration
public class XssSanitizerConfig {

    private static final Set<String> CAMPOS_EXCLUIDOS = Set.of("password", "email");

    @Bean
    public SimpleModule xssModule() {
        SimpleModule module = new SimpleModule("XssSanitizerModule");
        module.addDeserializer(String.class, new JsonDeserializer<>() {
            @Override
            public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                String valor = p.getValueAsString();
                if (valor == null) {
                    return null;
                }
                String campo = p.currentName();
                if (campo != null && CAMPOS_EXCLUIDOS.contains(campo)) {
                    return valor.trim();
                }
                return HtmlUtils.htmlEscape(valor.trim());
            }
        });
        return module;
    }
}
