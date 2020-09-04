package nikonov.torrentclient.gui.service;

import nikonov.torrentclient.gui.domain.event.Event;

import java.util.ArrayList;
import java.util.List;

public class EventService {

    public interface EventListener {
        void handleEvent(Event event);
    }

    private final List<EventListener> listeners;

    public EventService() {
        this.listeners = new ArrayList<>();
    }

    public void publishEvent(Event event) {
        listeners.forEach(listener -> listener.handleEvent(event));
    }

    public void addEventListener(EventListener listener) {
        listeners.add(listener);
    }

    public void removeListener(EventListener listener) {
        listeners.remove(listener);
    }
}


