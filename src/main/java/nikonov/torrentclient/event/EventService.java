package nikonov.torrentclient.event;

import java.util.ArrayList;
import java.util.List;

public class EventService {

    private final List<EventListener> listeners;

    public EventService() {
        listeners = new ArrayList<>();
    }

    public void registerListener(EventListener listener) {
        listeners.add(listener);
    }

    public void publishEvent(Object event) {
        listeners.forEach(listener -> listener.handleEvent(event));
    }
}
