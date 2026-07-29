package com.example.Stage.Config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;

@Component
public class RateLimitingFilter implements Filter {

    // On définit la limite : 10 requêtes par minute
    private final Bucket bucket = Bucket.builder()
            .addLimit(Bandwidth.classic(10, Refill.intervally(10, Duration.ofMinutes(1))))
            .build();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // OPTIONNEL : On n'applique le limiteur QUE sur l'API de l'Agent IA
        // pour ne pas bloquer le chargement des autres pages.
        if (httpRequest.getRequestURI().startsWith("/api/agent")) {

            if (bucket.tryConsume(1)) {
                // S'il reste des jetons, on continue la requête
                chain.doFilter(request, response);
            } else {
                // Plus de jetons : on renvoie l'erreur 429 (Too Many Requests)
                httpResponse.setStatus(429);
                httpResponse.setContentType("text/plain");
                httpResponse.getWriter().write("Trop de messages envoyés à l'IA. Veuillez attendre une minute.");
            }
        } else {
            // Pour les autres requêtes (images, polices, etc.), on laisse passer sans limite
            chain.doFilter(request, response);
        }
    }
}