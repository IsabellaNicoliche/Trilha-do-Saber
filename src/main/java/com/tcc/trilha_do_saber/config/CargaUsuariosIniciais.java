package com.tcc.trilha_do_saber.config;

import com.tcc.trilha_do_saber.model.AdmGeral;
import com.tcc.trilha_do_saber.repository.AdmGeralRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Profile("!import")
@Component
public class CargaUsuariosIniciais implements CommandLineRunner {

    private final AdmGeralRepository admGeralRepository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String senha;

    public CargaUsuariosIniciais(AdmGeralRepository admGeralRepository, PasswordEncoder passwordEncoder,
                                  @Value("${app.admin-inicial.email:}") String email,
                                  @Value("${app.admin-inicial.senha:}") String senha){
        this.admGeralRepository = admGeralRepository;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.senha = senha;
    }

    @Override
    public void run(String... args){
        if (email == null || email.isBlank() || senha == null || senha.isBlank()) {
            return;
        }
        if (admGeralRepository.count() > 0) {
            return;
        }

        AdmGeral admin = new AdmGeral("Admin Sistema", email.trim(), passwordEncoder.encode(senha));
        admin.setAtivo(true);
        admGeralRepository.save(admin);
        System.out.println("Administrador inicial criado: " + email.trim());
    }
}
