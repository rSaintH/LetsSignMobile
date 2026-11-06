package com.letssign.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuração da API (application.yml, prefixo "letssign").
 *
 * @param storageDir    pasta onde ficam os PDFs e as imagens de assinatura
 * @param jwtSecret     segredo HS256 (>= 32 bytes)
 * @param jwtTtlMinutes validade do token
 * @param adminEmail    admin semeado na primeira subida (igual ao app)
 * @param adminPassword senha do admin semeado
 */
@ConfigurationProperties(prefix = "letssign")
public record LetsSignProperties(
        String storageDir,
        String jwtSecret,
        long jwtTtlMinutes,
        String adminEmail,
        String adminPassword
) {}
