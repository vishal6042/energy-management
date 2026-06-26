package com.dems.orchestrator.config;

import com.dems.orchestrator.client.BackendApi;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class AppConfig {

    /** Spring AI fluent client over the auto-configured Ollama ChatModel. */
    @Bean
    ChatClient chatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }

    /** Declarative HTTP-interface client to the backend (Spring HTTP Interface). */
    @Bean
    BackendApi backendApi(RestClient.Builder builder, OrchestratorProperties props) {
        RestClient client = builder.baseUrl(props.backend().baseUrl()).build();
        HttpServiceProxyFactory factory =
                HttpServiceProxyFactory.builderFor(RestClientAdapter.create(client)).build();
        return factory.createClient(BackendApi.class);
    }
}
