package nikonov.torrentclient.gui.domain;

import lombok.*;
import nikonov.torrentclient.gui.service.CurrentTorrentService;

import java.util.Objects;

/**
 * Данные о текущем торренте
 * Под текущим торрентом подразумавается торрент, информация о котором выведена на центральной панели
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@RequiredArgsConstructor
public class CurrentTorrentData {
    /**
     * Текущий торрент
     */
    @NonNull
    private Torrent torrent;
    /**
     * Текущий проигрываемый файл, если есть
     */
    private TorrentFile playFile;
    /**
     * Состояние текущего проигрываемого файла
     */
    private FilePlayState state = FilePlayState.NONE;


    public boolean equalsWithoutState(CurrentTorrentData o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CurrentTorrentData that = (CurrentTorrentData) o;
        return Objects.equals(torrent, that.torrent) &&
                Objects.equals(playFile, that.playFile);
    }
}
