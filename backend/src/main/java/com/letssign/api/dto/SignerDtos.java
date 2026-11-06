package com.letssign.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Mesmas regras do app (util/Validators): e-mail válido, telefone >= 8, senha >= 6. */
public final class SignerDtos {
    private SignerDtos() {}

    private static final String EMAIL = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";

    public record CreateSignerRequest(
            @NotBlank(message = "Informe o nome completo.") String fullName,
            @NotBlank(message = "Informe o e-mail.") @Pattern(regexp = EMAIL, message = "E-mail inválido.") String email,
            @NotBlank(message = "Informe o telefone.") @Size(min = 8, message = "Telefone inválido.") String phone,
            @NotBlank(message = "Informe a senha.") @Size(min = 6, message = "A senha deve ter ao menos 6 caracteres.") String password) {}

    public record UpdateSignerRequest(
            @NotBlank(message = "Informe o nome completo.") String fullName,
            @NotBlank(message = "Informe o e-mail.") @Pattern(regexp = EMAIL, message = "E-mail inválido.") String email,
            @NotBlank(message = "Informe o telefone.") @Size(min = 8, message = "Telefone inválido.") String phone,
            @Size(min = 6, message = "A senha deve ter ao menos 6 caracteres.") String newPassword) {}
}
