

INSERT INTO aluno (nome, email, senha, ativo, anonimizado, consentimento_lgpd, data_consentimento, registro_aluno, curso, semestre) VALUES
    ('Ana Beatriz Souza', 'ana.souza@aluno.umc.br', '$2b$12$vxe32s6/AAeN7CrMeUi5GuIsEfhmJFT8Dse6fcwe.36fhOaGcykrO', true, false, true, now(), '2023001', 'Sistemas de Informação', 8)
ON CONFLICT (email) DO NOTHING;

INSERT INTO aluno (nome, email, senha, ativo, anonimizado, consentimento_lgpd, data_consentimento, registro_aluno, curso, semestre) VALUES
    ('João Pedro Lima', 'joao.lima@aluno.umc.br', '$2b$12$vxe32s6/AAeN7CrMeUi5GuIsEfhmJFT8Dse6fcwe.36fhOaGcykrO', true, false, true, now(), '2023002', 'Sistemas de Informação', 6)
ON CONFLICT (email) DO NOTHING;

INSERT INTO professor (nome, email, senha, ativo, anonimizado, consentimento_lgpd, data_consentimento, registro_profissional, disciplina) VALUES
    ('Carlos Eduardo Mota', 'carlos.mota@umc.br', '$2b$12$vxe32s6/AAeN7CrMeUi5GuIsEfhmJFT8Dse6fcwe.36fhOaGcykrO', true, false, true, now(), 'PROF-0456', 'Engenharia de Software')
ON CONFLICT (email) DO NOTHING;