package nikonov.torrentclient.base.metadata.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Файл торрента
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class File {
    /**
     * размер файла в байтах
     */
    private long length;
    /**
     * Имя файла
     */
    private List<String> path;

    public List<String> getPath() {
        if(path == null) {
            path = new ArrayList<>();
        }
        return path;
    }
}
