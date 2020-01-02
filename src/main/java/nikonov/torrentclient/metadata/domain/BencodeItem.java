package nikonov.torrentclient.metadata.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Представление bencode-данных
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BencodeItem {
    private int indexStart;
    private int indexEnd;
    private Object value;
}