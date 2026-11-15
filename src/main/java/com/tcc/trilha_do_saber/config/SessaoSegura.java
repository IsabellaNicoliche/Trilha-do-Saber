package com.tcc.trilha_do_saber.config;

import jakarta.servlet.http.HttpServletRequest;

public final class SessaoSegura {

    private SessaoSegura() {
    }

    public static void renovar(HttpServletRequest request) {
        if (request != null && request.getSession(false) != null) {
            request.changeSessionId();
        }
    }
}
