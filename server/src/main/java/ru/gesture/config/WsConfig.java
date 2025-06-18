package ru.gesture.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.converter.*;
import org.springframework.messaging.simp.config.*;
import org.springframework.web.socket.config.annotation.*;

import java.util.List;

@Configuration
@EnableWebSocketMessageBroker
public class WsConfig implements WebSocketMessageBrokerConfigurer {

    /* JSON⇆DTO остаётся как было */
    @Override
    public boolean configureMessageConverters(List<MessageConverter> converters) {
        converters.add(new MappingJackson2MessageConverter());
        return false;
    }

    /* точка подключения /ws + cookie-интерсептор */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry r) {
        r.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .addInterceptors(new UserHandshakeInterceptor());
    }

    /* префиксы брокера без изменений */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry r) {
        r.enableSimpleBroker("/topic", "/queue");
        r.setApplicationDestinationPrefixes("/app");
        r.setUserDestinationPrefix("/user");
    }

    /* ───── НОВЫЙ блок ───── — увеличиваем лимиты  */
    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration r) {
        r.setMessageSizeLimit(256 * 1024);      // входящие кадры до 256 КБ
        r.setSendBufferSizeLimit(512 * 1024);   // исходящий буфер
        r.setSendTimeLimit(20_000);             // 20 сек на отправку
    }
}
