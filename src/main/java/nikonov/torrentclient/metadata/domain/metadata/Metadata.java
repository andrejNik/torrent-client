package nikonov.torrentclient.metadata.domain.metadata;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Описание метаданных торрента
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Metadata {
    /**
     * Announce-URL трекеров
     */
    private List<TrackerAnnounce> trackerAnnounces;
    /**
     * Дата создания торрента
     */
    private Instant creationDate;
    /**
     * Комментарий от автора
     */
    private String comment;
    /**
     * Имя и версия программы, которая использовалась для создания торрента
     */
    private String createdBy;
    /**
     * info словарь торрента
     */
    private Info info;

    public List<TrackerAnnounce> getTrackerAnnounces() {
        if(trackerAnnounces == null) {
            trackerAnnounces = new ArrayList<>();
        }
        return trackerAnnounces;
    }
}
