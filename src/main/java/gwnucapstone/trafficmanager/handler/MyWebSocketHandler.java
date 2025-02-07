package gwnucapstone.trafficmanager.handler;

import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

public class MyWebSocketHandler extends TextWebSocketHandler {

    private static final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        sessions.put(sessionId, session);
        System.out.println("WebSocket 연결 - sessionId: " + sessionId);

        session.sendMessage(new TextMessage("{\"type\":\"sessionId\", \"sessionId\":\"" + sessionId + "\"}"));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = session.getId();
        if (sessions.containsKey(sessionId)) {
            sessions.remove(sessionId);
            System.out.println("WebSocket 연결 종료 - sessionId: " + sessionId);
        }
    }

    public void sendMessageToSession(String sessionId, String message) {
        WebSocketSession session = sessions.get(sessionId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(message));
            } catch (IOException e) {
                System.err.println("메시지 전송 실패 - sessionId: " + sessionId);
                e.printStackTrace();
            }
        } else {
            System.err.println("세션이 열려 있지 않거나 존재하지 않습니다 - sessionId: " + sessionId);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String sessionId = session.getId();
        sessions.remove(sessionId);
        System.err.println("WebSocket 오류 발생, 세션 제거됨 - sessionId: " + sessionId);
        exception.printStackTrace();
    }
}
