package nikonov.torrentclient.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import nikonov.torrentclient.metadata.domain.metadata.Metadata;

import java.util.Map;
import java.util.Set;

/**
 * Данные для загрузки
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DownloadData {
    /**
     * Метадата скачиваемых данных
     */
    private Metadata metadata;
    /**
     * Индексы файлов (начиная с 0), указанные в metadata, которые необходимо загрузить
     */
    private Set<Integer> fileToDownloadIndexes;
    /**
     * Индекс файла -> полное имя
     */
    private Map<Integer, String> fileNameMap;
}
