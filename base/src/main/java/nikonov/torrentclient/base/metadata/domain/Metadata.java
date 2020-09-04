package nikonov.torrentclient.base.metadata.domain;

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

    /**
     * Суммарная длина файлов торрента
     */
    public long summaryLength() {
        return info.getFiles().stream().mapToLong(File::getLength).sum();
    }

    /**
     * Количетсво кусков
     */
    public int countPiece() {
        return (int)(Math.ceil(summaryLength() / (double) info.getPieceLength()));
    }

    /**
     * Длина куска в байтах
     *
     * @param pieceIndex - идентификатор куска
     */
    public int pieceLength(int pieceIndex) {
        var lastPieceLength = (summaryLength() % info.getPieceLength()) == 0 ?
                info.getPieceLength() : (int) (summaryLength() % info.getPieceLength());
        return pieceIndex == countPiece() - 1 ? lastPieceLength : info.getPieceLength();
    }
}
