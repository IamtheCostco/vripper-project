package tn.mnlr.vripper.web.wsendpoints;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tn.mnlr.vripper.entities.Image;
import tn.mnlr.vripper.entities.Post;
import tn.mnlr.vripper.entities.mixin.ui.ImageUIMixin;
import tn.mnlr.vripper.entities.mixin.ui.PostUIMixin;
import tn.mnlr.vripper.services.AppStateService;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
public class WebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketHandler.class);

    @Getter
    private static class WSMessage {

        private String cmd;
        private String payload;

        enum CMD {
            POSTS_SUB,
            POST_DETAILS_SUB,
            POSTS_UNSUB,
            POST_DETAILS_UNSUB
        }
    }

    public WebSocketHandler() {
        om.addMixIn(Image.class, ImageUIMixin.class).addMixIn(Post.class, PostUIMixin.class);
    }

    private Map<String, Disposable> postsSubscriptions = new ConcurrentHashMap<>();
    private Map<String, Disposable> postDetailsSubscriptions = new ConcurrentHashMap<>();
    private ObjectMapper om = new ObjectMapper();

    @Autowired
    private AppStateService appStateService;

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {

        WSMessage wsMessage = om.readValue(message.getPayload(), WSMessage.class);
        WSMessage.CMD cmd = WSMessage.CMD.valueOf(wsMessage.getCmd());
        switch (cmd) {
            case POSTS_SUB:
                subscribeForPosts(session);
                break;
            case POST_DETAILS_SUB:
                subscribeForPostDetails(session, wsMessage.getPayload());
                break;
            case POST_DETAILS_UNSUB:
                logger.info(String.format("Client %s unsubscribed from post details", session.getId()));
                Optional.ofNullable(postDetailsSubscriptions.remove(session.getId())).ifPresent(d -> d.dispose());
                break;
            case POSTS_UNSUB:
                logger.info(String.format("Client %s unsubscribed from posts", session.getId()));
                Optional.ofNullable(postsSubscriptions.remove(session.getId())).ifPresent(d -> d.dispose());
                break;
        }
    }

    private void subscribeForPosts(WebSocketSession session) {

        logger.info(String.format("Client %s subscribed for posts", session.getId()));
        if (postsSubscriptions.containsKey(session.getId())) {
            postsSubscriptions.get(session.getId()).dispose();
        }

        try {
            send(session, new TextMessage(om.writeValueAsString(appStateService.getCurrentPosts().values())));
        } catch (Exception e) {
            logger.error("Unexpected error occurred", e);
        }

        postsSubscriptions.put(session.getId(), appStateService.getLivePostsState()
                .onBackpressureBuffer()
                .observeOn(Schedulers.io())
                .buffer(2, TimeUnit.SECONDS)
                .filter(e -> !e.isEmpty())
                .map(e -> e.stream().distinct().collect(Collectors.toList()))
                .map(om::writeValueAsString)
                .map(TextMessage::new)
                .subscribe(msg -> send(session, msg), e -> logger.error("Failed to send data to client", e))
        );
    }

    private void subscribeForPostDetails(WebSocketSession session, String postId) {

        logger.info(String.format("Client %s subscribed for post details with id = %s", session.getId(), postId));
        if (postDetailsSubscriptions.containsKey(session.getId())) {
            postDetailsSubscriptions.get(session.getId()).dispose();
        }

        try {
            send(session, new TextMessage(om.writeValueAsString(
                    appStateService.getCurrentImages()
                            .values()
                            .stream()
                            .filter(e -> e.getPostId().equals(postId))
                            .collect(Collectors.toList())))
            );
        } catch (Exception e) {
            logger.error("Unexpected error occurred", e);
        }

        postDetailsSubscriptions.put(session.getId(), appStateService.getLiveImageUpdates()
                .onBackpressureBuffer()
                .observeOn(Schedulers.io())
                .filter(e -> e.getPostId().equals(postId))
                .buffer(2, TimeUnit.SECONDS)
                .filter(e -> !e.isEmpty())
                .map(e -> e.stream().distinct().collect(Collectors.toList()))
                .map(om::writeValueAsString)
                .map(TextMessage::new)
                .subscribe(msg -> send(session, msg), e -> logger.error("Failed to send data to client", e))
        );
    }

    private synchronized void send(WebSocketSession session, TextMessage message) throws IOException {
        session.sendMessage(message);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {

    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {

        Optional.ofNullable(postsSubscriptions.remove(session.getId())).ifPresent(d -> d.dispose());
        Optional.ofNullable(postDetailsSubscriptions.remove(session.getId())).ifPresent(d -> d.dispose());
    }
}