package com.stowflow.inventra.config;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "inventra.gemini")
@Getter
@Setter
public class GeminiProperties {

    private boolean enabled = true;

    /** 2e appel Gemini pour reformuler la réponse (sinon template Java uniquement). */
    private boolean generateReplies = true;

    private String apiKey = "";

    private String model = "gemini-2.5-flash";

    private Duration timeout = Duration.ofSeconds(30);

    /** Limite tokens pour la classification JSON (intent). */
    private int maxOutputTokensIntent = 512;

    /** Limite tokens pour la reformulation de réponse. */
    private int maxOutputTokensReply = 350;
}
